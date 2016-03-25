package at.ac.ait.ariadne.routeformat.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.collect.BoundType;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import at.ac.ait.ariadne.routeformat.ModeOfTransport;
import at.ac.ait.ariadne.routeformat.RouteSegment;
import at.ac.ait.ariadne.routeformat.RouteSegment.Builder;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONFeature;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONLineString;

/**
 * Merges a list of a list of consecutive segments (i.e. roughly without jumps
 * such as the end of one segment is 1km away from the start of the next segment
 * ), where the latter is considered a Route.
 * <p>
 * Between two routes time gaps are handled as follows:
 * <ul>
 * <li>for positive gaps the second route will get the gap added as boarding
 * time in the first segment not included in
 * {@link #getWriteWaitingTimePreferableNotInto()}.</li>
 * <li>for negative gaps (=overlaps) the second route will be shifted (and a
 * warning will be logged if the shifted routes contain non-foot segments
 * because this may lead to wrong routes, i.e. shifting public transport routes
 * is probably not useful)</li>
 * </ul>
 * <p>
 * With {@link #setMergeSegmentsWithSameMot(boolean)} merging of adjacent
 * segments with (exactly!) the same mode of transport, which is activated by
 * default, can be (de)activated. <b>Note</b>, that for merged segments boarding
 * and alighting time is simply summed up and the geometry is simply
 * concatenated without recalculating the distance.
 * <p>
 * With {@link #setAdditionalAlightingSecondsBetweenRoutes(List)} additional
 * alighting seconds can be added.
 */
public class RouteSegmentMerger {

	private static final Logger LOGGER = Logger.getLogger(RouteSegmentMerger.class.getName());

	private final List<LinkedList<RouteSegment>> routes;
	private List<Integer> additionalAlightingSecondsBetweenRoutes;
	private boolean mergeSegmentsWithSameMot = true;
	private Set<ModeOfTransport> writeWaitingTimePreferableNotInto;

	/**
	 * @param routes
	 *            the routes to be merged into one route. lists must not be
	 *            empty.
	 */
	public RouteSegmentMerger(List<List<RouteSegment>> routes) {
		this.routes = routes.stream().map(l -> new LinkedList<>(l)).collect(Collectors.toList());
		this.additionalAlightingSecondsBetweenRoutes = new ArrayList<>();
		for (int i = 0; i < routes.size() - 1; i++)
			additionalAlightingSecondsBetweenRoutes.add(0);
		this.writeWaitingTimePreferableNotInto = new HashSet<>();
		this.writeWaitingTimePreferableNotInto.add(ModeOfTransport.STANDARD_FOOT);
		this.writeWaitingTimePreferableNotInto.add(ModeOfTransport.STANDARD_TRANSFER);
	}

	public boolean isMergeSegmentsWithSameMot() {
		return mergeSegmentsWithSameMot;
	}

	public void setMergeSegmentsWithSameMot(boolean mergeSegmentsWithSameMot) {
		this.mergeSegmentsWithSameMot = mergeSegmentsWithSameMot;
	}

	/**
	 * @return a mutable copy of the internal set
	 */
	public Set<ModeOfTransport> getWriteWaitingTimePreferableNotInto() {
		return new HashSet<>(writeWaitingTimePreferableNotInto);
	}

	public void setWriteWaitingTimePreferableNotInto(Set<ModeOfTransport> writeWaitingTimePreferableNotInto) {
		this.writeWaitingTimePreferableNotInto = writeWaitingTimePreferableNotInto;
	}

	/**
	 * @return a mutable copy of the internal list
	 */
	public List<Integer> getAdditionalAlightingSecondsBetweenRoutes() {
		return new ArrayList<>(additionalAlightingSecondsBetweenRoutes);
	}

	public void setAdditionalAlightingSecondsBetweenRoutes(List<Integer> additionalAlightingSecondsBetweenRoutes) {
		if (additionalAlightingSecondsBetweenRoutes.size() != routes.size() - 1)
			throw new IllegalArgumentException(
					"alighting seconds must be given for exactly each change between routes");
		this.additionalAlightingSecondsBetweenRoutes = additionalAlightingSecondsBetweenRoutes;
	}

	public List<RouteSegment> createMergedSegments() {
		return mergeAllSegments();
	}

	List<RouteSegment> mergeAllSegments() {
		List<RouteSegment> mergedSegments = new ArrayList<>();

		for (int i = 0; i < routes.size() - 1; i++) {
			int alightingSeconds = additionalAlightingSecondsBetweenRoutes.get(i);
			if (alightingSeconds > 0) {
				RouteSegment segmentToProlong = routes.get(i).removeLast();
				RouteSegment prolongedSegment = RouteSegment.builder(segmentToProlong)
						.withAlightingSeconds(segmentToProlong.getAlightingSeconds().orElse(0) + alightingSeconds)
						.withDurationSeconds(segmentToProlong.getDurationSeconds() + alightingSeconds)
						.withArrivalTime(segmentToProlong.getArrivalTimeAsZonedDateTime().plus(alightingSeconds,
								ChronoUnit.SECONDS))
						.build();
				routes.get(i).addLast(prolongedSegment);
			}
		}

		Map<Integer, Integer> index2waitingSeconds = new HashMap<>();
		index2waitingSeconds.put(0, 0);
		for (int i = 0; i < routes.size() - 1; i++) {
			int waitingTime = (int) Math.round(Duration.between(routes.get(i).getLast().getArrivalTimeAsZonedDateTime(),
					routes.get(i + 1).getFirst().getDepartureTimeAsZonedDateTime()).toMillis() / 1000);
			index2waitingSeconds.put(i + 1, waitingTime);
		}

		for (int i = 0; i < routes.size(); i++) {
			List<RouteSegment> segmentsToAdd;

			int waitingSeconds = index2waitingSeconds.get(i);

			if (waitingSeconds > 0)
				segmentsToAdd = prependWaitingTime(routes.get(i), waitingSeconds);
			else if (waitingSeconds < 0)
				segmentsToAdd = shiftInTime(routes.get(i), -waitingSeconds);
			else
				segmentsToAdd = routes.get(i);

			mergedSegments.addAll(segmentsToAdd);
		}

		if (mergeSegmentsWithSameMot)
			mergedSegments = mergeSegmentsWithSameMot(mergedSegments);

		return getNewSegmentsWithFixedNr(mergedSegments);
	}

	/**
	 * Prepends waiting time (boarding time) to the first segment that is not in
	 * the black list (and use the last segment if all are on the black list)
	 */
	private List<RouteSegment> prependWaitingTime(List<RouteSegment> segments, int waitingSeconds) {
		int firstMatchingSegmentIndex = 0;
		while (firstMatchingSegmentIndex < segments.size()) {
			ModeOfTransport mot = segments.get(firstMatchingSegmentIndex).getModeOfTransport();
			if (writeWaitingTimePreferableNotInto.contains(mot)) {
				firstMatchingSegmentIndex++;
			} else {
				break;
			}
		}
		if (firstMatchingSegmentIndex >= segments.size())
			firstMatchingSegmentIndex = segments.size() - 1;

		List<RouteSegment> modifiedSegments = new ArrayList<>(segments);
		RouteSegment old = modifiedSegments.get(firstMatchingSegmentIndex);

		// add waiting time to chosen segment
		Builder builder = RouteSegment.builder(old);
		builder.withBoardingSeconds(old.getBoardingSeconds().orElse(0) + waitingSeconds);
		builder.withDurationSeconds(old.getDurationSeconds() + waitingSeconds);
		builder.withDepartureTime(old.getDepartureTimeAsZonedDateTime().minus(waitingSeconds, ChronoUnit.SECONDS));
		modifiedSegments.set(firstMatchingSegmentIndex, builder.build());

		// shift arrival/departure times for segments
		// before the modified segment
		for (int i = 0; i < firstMatchingSegmentIndex; i++) {
			old = modifiedSegments.get(i);
			builder = RouteSegment.builder(old);
			builder.withDepartureTime(old.getDepartureTimeAsZonedDateTime().minus(waitingSeconds, ChronoUnit.SECONDS));
			builder.withArrivalTime(old.getArrivalTimeAsZonedDateTime().minus(waitingSeconds, ChronoUnit.SECONDS));
			modifiedSegments.set(i, builder.build());
		}

		return modifiedSegments;
	}

	/**
	 * @param shiftSeconds
	 *            seconds the segments should be shifted in time
	 */
	private List<RouteSegment> shiftInTime(List<RouteSegment> segments, int shiftSeconds) {
		List<RouteSegment> modifiedSegments = new ArrayList<>();
		for (RouteSegment segment : segments) {
			Builder builder = RouteSegment.builder(segment);
			builder.withDepartureTime(segment.getDepartureTimeAsZonedDateTime().plus(shiftSeconds, ChronoUnit.SECONDS));
			builder.withArrivalTime(segment.getArrivalTimeAsZonedDateTime().plus(shiftSeconds, ChronoUnit.SECONDS));
			modifiedSegments.add(builder.build());
			if (!ModeOfTransport.STANDARD_FOOT.equals(segment.getModeOfTransport()))
				LOGGER.warning("shifted segment for " + shiftSeconds + " for mot " + segment.getModeOfTransport());
		}
		return modifiedSegments;
	}

	private List<RouteSegment> mergeSegmentsWithSameMot(List<RouteSegment> segments) {
		RangeSet<Integer> rangeSet = TreeRangeSet.create();
		for (int i = 0; i < segments.size() - 1; i++) {
			if (segments.get(i).getModeOfTransport().equals(segments.get(i + 1).getModeOfTransport())) {
				rangeSet.add(Range.closed(i, i + 1).canonical(DiscreteDomain.integers()));
			}
		}

		Map<Integer, Range<Integer>> start2Range = new HashMap<>();
		for (Range<Integer> range : rangeSet.asRanges())
			start2Range.put(lowerBound(range), range);

		List<RouteSegment> mergedSegments = new ArrayList<>();
		for (int i = 0; i < segments.size(); i++) {
			if (start2Range.containsKey(i)) {
				Range<Integer> range = start2Range.get(i);
				RouteSegment prev = segments.get(i);
				while (i < upperBound(range)) {
					prev = mergeTwoSegments(prev, segments.get(++i));
				}
				mergedSegments.add(prev);
			} else {
				mergedSegments.add(segments.get(i));
			}
		}
		return mergedSegments;
	}

	/**
	 * @return a single merged {@link RouteSegment} with the main attributes
	 *         from the first segment but the combined duration, distance and
	 *         geometry
	 */
	private RouteSegment mergeTwoSegments(RouteSegment a, RouteSegment b) {
		// mot, start & departure time e.g. from a
		Builder builder = RouteSegment.builder(a);

		// adapt time
		int totalSeconds = a.getDurationSeconds() + b.getDurationSeconds();
		builder.withBoardingSeconds(a.getBoardingSeconds().orElse(0) + b.getBoardingSeconds().orElse(0));
		builder.withAlightingSeconds(a.getAlightingSeconds().orElse(0) + b.getAlightingSeconds().orElse(0));
		builder.withDurationSeconds(totalSeconds);
		builder.withArrivalTime(b.getArrivalTime());

		// adapt geometry & length
		builder.withTo(b.getTo());
		GeoJSONFeature<GeoJSONLineString> newlineString = GeoJSONFeature.newLineStringFeature(new ArrayList<>());
		a.getGeometryGeoJson().ifPresent(g -> newlineString.geometry.coordinates.addAll(g.geometry.coordinates));
		b.getGeometryGeoJson().ifPresent(g -> newlineString.geometry.coordinates.addAll(g.geometry.coordinates));
		builder.withGeometryGeoJson(newlineString);
		builder.withLengthMeters(a.getLengthMeters() + b.getLengthMeters());

		return builder.build();
	}

	/**
	 * fix segment nrs
	 */
	private List<RouteSegment> getNewSegmentsWithFixedNr(List<RouteSegment> segments) {
		List<RouteSegment> fixedSegments = new ArrayList<>();
		for (int i = 0; i < segments.size(); i++) {
			RouteSegment segment = segments.get(i);
			RouteSegment.Builder segmentBuilder = RouteSegment.builder(segment);
			segmentBuilder.withNr(i + 1);
			fixedSegments.add(segmentBuilder.build());
		}
		return fixedSegments;
	}

	static int lowerBound(Range<Integer> range) {
		int lower = range.lowerEndpoint();
		if (range.lowerBoundType().equals(BoundType.OPEN))
			++lower;
		return lower;
	}

	static int upperBound(Range<Integer> range) {
		int upper = range.upperEndpoint();
		if (range.upperBoundType().equals(BoundType.OPEN))
			--upper;
		return upper;
	}

}
