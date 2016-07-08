package at.ac.ait.ariadne.routeformat;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

import at.ac.ait.ariadne.routeformat.Constants.GeneralizedModeOfTransportType;
import at.ac.ait.ariadne.routeformat.RoutingRequest.Builder;
import at.ac.ait.ariadne.routeformat.location.Location;
import at.ac.ait.ariadne.routeformat.util.Utils;

/**
 * A {@link RoutingRequest} encapsulates typically required request parameters
 * for an intermodal routing service. Additional parameters, that will be
 * different for each concrete routing service, can be added via
 * {@link #getAdditionalInfo()}: global parameters in this class, parameters
 * valid only for a certain mode of transport via
 * {@link RequestModeOfTransport#getAdditionalInfo()}. These additional
 * parameters could e.g. include:
 * 
 * <pre>
 * only_use_mots_provided_by = ÖBB
 * exclude_public_transport_line = WL_28A;WL_29A
 * exclude_provider = CAT
 * regional_bus = no
 * accepted_delay_minutes = 10
 * </pre>
 * 
 * @author AIT Austrian Institute of Technology GmbH
 */
@JsonDeserialize(builder = Builder.class)
@JsonInclude(Include.NON_EMPTY)
public class RoutingRequest {
    public static final String DEFAULT_OPTIMIZED_FOR = "TRAVELTIME";

    private final String serviceId;
    private final Location from;
    private final List<Location> via;
    private final Location to;
    private final List<RequestModeOfTransport> modesOfTransport;
    private final Optional<RequestModeOfTransport> startModeOfTransport;
    private final Optional<RequestModeOfTransport> endModeOfTransport;
    private final String optimizedFor;
    private final Optional<Integer> maximumTransfers;
    private final Optional<ZonedDateTime> departureTime;
    private final Optional<ZonedDateTime> arrivalTime;
    private final List<Constants.AccessibilityRestriction> accessibilityRestrictions;
    private final Optional<String> language;
    private final Map<String, Object> additionalInfo;

    /**
     * Defines which routing service (a combination of maps, timeseries,..) will
     * be / was used for routing
     */
    @JsonProperty(required = true)
    public String getServiceId() {
        return serviceId;
    }

    @JsonProperty(required = true)
    public Location getFrom() {
        return from;
    }

    public List<Location> getVia() {
        return via;
    }

    @JsonProperty(required = true)
    public Location getTo() {
        return to;
    }

    /**
     * One or more modes of transport and their options that will be / were used
     * for routing. In case of a single mode of transport unimodal routing is
     * requested, in case of several modes of transport intermodal routing is
     * requested.
     * <p>
     * In case of intermodal routing it is guaranteed that the returned set
     * contains a mode of transport with
     * {@link GeneralizedModeOfTransportType#FOOT}.
     */
    @JsonProperty(required = true)
    public List<RequestModeOfTransport> getModesOfTransport() {
        return modesOfTransport;
    }

    /**
     * One of the modes of transport in {@link #getModesOfTransport()}, which
     * the route must start with. Only useful for intermodal routing, where
     * routes start and end with with foot by default.
     */
    @JsonProperty
    public Optional<RequestModeOfTransport> getStartModeOfTransport() {
        return startModeOfTransport;
    }

    /**
     * One of the modes of transport in {@link #getModesOfTransport()}, which
     * the route must end with. Only useful for intermodal routing, where routes
     * start and end with with foot by default.
     */
    @JsonProperty
    public Optional<RequestModeOfTransport> getEndModeOfTransport() {
        return endModeOfTransport;
    }

    /**
     * Criteria the route will be / was optimized for, e.g. shortest travel
     * time, which is also the default
     */
    @JsonProperty(required = true)
    public String getOptimizedFor() {
        return optimizedFor;
    }

    /**
     * @return maximum number of transfers not including the first and last
     *         'transfer' to walking, i.e. walking to the a bike-sharing
     *         station, riding the bike, walking to the final destination counts
     *         as zero transfers (default = 3)
     */
    @JsonProperty
    public Optional<Integer> getMaximumTransfers() {
        return maximumTransfers;
    }

    /**
     * Requested departure time for the route. Mutual exclusive with
     * {@link #getArrivalTime()}, it is guaranteed that exactly one of the two
     * times is set.
     * <p>
     * If neither departure time nor arrival time were set in the builder a
     * departure time of 'now' is automatically added.
     * <p>
     * The supported formats are defined in {@link ZonedDateTime} which uses ISO
     * 8601 with time zone. One example is "YYYY-MM-DDTHH:MMZ", where T is the
     * letter T, Z is the time zone (in either HH:MM, HHMM, HH format or the
     * letter Z for UTC). E.g. "2015-01-31T18:05+0100". As output the default
     * toString() of {@link ZonedDateTime} is used.
     */
    @JsonProperty
    public Optional<String> getDepartureTime() {
        return departureTime.map(time -> time.toString());
    }

    /**
     * @see #getDepartureTime()
     */
    @JsonIgnore
    public Optional<ZonedDateTime> getDepartureTimeAsZonedDateTime() {
        return departureTime;
    }

    /**
     * Requested arrival time for the route. Mutual exclusive with
     * {@link #getDepartureTime()}, it is guaranteed that exactly one of the two
     * times is set.
     * <p>
     * The format is the same as for {@link #getDepartureTime()}.
     */
    @JsonProperty
    public Optional<String> getArrivalTime() {
        return arrivalTime.map(time -> time.toString());
    }

    /**
     * @see #getArrivalTime()
     */
    @JsonIgnore
    public Optional<ZonedDateTime> getArrivalTimeAsZonedDateTime() {
        return arrivalTime;
    }

    public List<Constants.AccessibilityRestriction> getAccessibilityRestrictions() {
        return accessibilityRestrictions;
    }

    /**
     * @return the preferred language of the user. E.g. street or POI names can
     *         be provided in this language if available
     */
    public Optional<String> getLanguage() {
        return language;
    }

    /**
     * @return a map of parameters to be considered during the routing process
     */
    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    private RoutingRequest(Builder builder) {
        this.serviceId = builder.serviceId;
        this.from = builder.from;
        this.via = builder.via;
        this.to = builder.to;
        this.modesOfTransport = builder.modesOfTransport;
        this.startModeOfTransport = builder.startModeOfTransport;
        this.endModeOfTransport = builder.endModeOfTransport;
        this.optimizedFor = builder.optimizedFor;
        this.maximumTransfers = builder.maximumTransfers;
        this.departureTime = builder.departureTime;
        this.arrivalTime = builder.arrivalTime;
        this.accessibilityRestrictions = builder.accessibilityRestrictions;
        this.language = builder.language;
        this.additionalInfo = builder.additionalInfo;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String serviceId;
        private Location from;
        private List<Location> via = Collections.emptyList();
        private Location to;
        private List<RequestModeOfTransport> modesOfTransport = Collections.emptyList();
        private Optional<RequestModeOfTransport> startModeOfTransport = Optional.empty();
        private Optional<RequestModeOfTransport> endModeOfTransport = Optional.empty();
        private String optimizedFor;
        private Optional<Integer> maximumTransfers = Optional.empty();
        private Optional<ZonedDateTime> departureTime = Optional.empty();
        private Optional<ZonedDateTime> arrivalTime = Optional.empty();
        private List<Constants.AccessibilityRestriction> accessibilityRestrictions = Collections.emptyList();
        private Optional<String> language = Optional.empty();
        private Map<String, Object> additionalInfo = Collections.emptyMap();

        public Builder withServiceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder withFrom(Location from) {
            this.from = from;
            return this;
        }

        public Builder withVia(List<Location> via) {
            this.via = ImmutableList.copyOf(via);
            return this;
        }

        public Builder withTo(Location to) {
            this.to = to;
            return this;
        }

        public Builder withModesOfTransport(List<RequestModeOfTransport> modesOfTransport) {
            this.modesOfTransport = ImmutableList.copyOf(modesOfTransport);
            return this;
        }

        public Builder withStartModeOfTransport(RequestModeOfTransport startModeOfTransport) {
            this.startModeOfTransport = Optional.ofNullable(startModeOfTransport);
            return this;
        }

        public Builder withEndModeOfTransport(RequestModeOfTransport endModeOfTransport) {
            this.endModeOfTransport = Optional.ofNullable(endModeOfTransport);
            return this;
        }

        public Builder withOptimizedFor(String optimizedFor) {
            this.optimizedFor = optimizedFor;
            return this;
        }

        public Builder withMaximumTransfers(Integer maximumTransfers) {
            this.maximumTransfers = Optional.ofNullable(maximumTransfers);
            return this;
        }

        @JsonIgnore
        public Builder withDepartureTime(ZonedDateTime departureTime) {
            this.departureTime = Optional.ofNullable(departureTime.truncatedTo(ChronoUnit.SECONDS));
            return this;
        }

        @JsonProperty
        public Builder withDepartureTime(String departureTime) {
            if (departureTime == null) {
                this.departureTime = Optional.empty();
            } else if (departureTime.equalsIgnoreCase("NOW")) {
                this.departureTime = Optional.of(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            } else {
                this.departureTime = Optional.of(Utils.parseZonedDateTime(departureTime, "departureTime"));
            }
            return this;
        }

        @JsonIgnore
        public Builder withArrivalTime(ZonedDateTime arrivalTime) {
            this.arrivalTime = Optional.ofNullable(arrivalTime.truncatedTo(ChronoUnit.SECONDS));
            return this;
        }

        @JsonProperty
        public Builder withArrivalTime(String arrivalTime) {
            if (arrivalTime == null) {
                this.arrivalTime = Optional.empty();
            } else if (arrivalTime.equalsIgnoreCase("NOW")) {
                this.arrivalTime = Optional.of(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            } else {
                this.arrivalTime = Optional.of(Utils.parseZonedDateTime(arrivalTime, "arrivalTime"));
            }
            return this;
        }

        public Builder withAccessibilityRestrictions(
                List<Constants.AccessibilityRestriction> accessibilityRestrictions) {
            this.accessibilityRestrictions = ImmutableList.copyOf(accessibilityRestrictions);
            return this;
        }

        public Builder withLanguage(String language) {
            this.language = Optional.ofNullable(language);
            return this;
        }

        public Builder withAdditionalInfo(Map<String, Object> additionalInfo) {
            this.additionalInfo = ImmutableSortedMap.copyOf(additionalInfo);
            return this;
        }

        public RoutingRequest build() {
            validate();
            return new RoutingRequest(this);
        }

        private void validate() {
            Preconditions.checkArgument(serviceId != null, "serviceId is mandatory but missing");
            Preconditions.checkArgument(from != null, "from is mandatory but missing");
            Preconditions.checkArgument(to != null, "to is mandatory but missing");
            Preconditions.checkArgument(modesOfTransport != null || modesOfTransport.isEmpty(),
                    "modesOfTransport is mandatory but missing/empty");
            Preconditions.checkArgument(modesOfTransport.size() >= 1, ">= 1 modesOfTransport must be used");
            if (modesOfTransport.size() > 1) {
                Set<GeneralizedModeOfTransportType> types = modesOfTransport.stream()
                        .map(m -> m.getModeOfTransport().getGeneralizedType()).collect(Collectors.toSet());
                if (!types.contains(GeneralizedModeOfTransportType.FOOT)) {
                    RequestModeOfTransport foot = RequestModeOfTransport.builder()
                            .withModeOfTransport(ModeOfTransport.STANDARD_FOOT).build();
                    modesOfTransport = ImmutableList.<RequestModeOfTransport> builder().addAll(modesOfTransport)
                            .add(foot).build();
                }
            }

            if (optimizedFor == null) {
                optimizedFor = DEFAULT_OPTIMIZED_FOR;
            }

            maximumTransfers = Utils.enforcePositiveInteger(maximumTransfers, "maximumTransfers");

            Preconditions.checkArgument(!(departureTime.isPresent() && arrivalTime.isPresent()),
                    "departureTime and arrivalTime are mutually exclusive, only one can be set at once");
            if (!departureTime.isPresent() && !arrivalTime.isPresent()) {
                departureTime = Optional.of(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            }
        }
    }

}
