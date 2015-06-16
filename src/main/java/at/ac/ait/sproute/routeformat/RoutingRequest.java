package at.ac.ait.sproute.routeformat;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import at.ac.ait.sproute.routeformat.RouteSegment.ModeOfTransport;
import at.ac.ait.sproute.routeformat.RoutingRequest.Builder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

/**
 * @author AIT Austrian Institute of Technology GmbH
 */
@JsonDeserialize(builder = Builder.class)
@JsonInclude(Include.NON_EMPTY)
public class RoutingRequest {
	private String contextName;
	private Location from;
	private Location to;
	private Set<RouteSegment.ModeOfTransport> modesOfTransport;
	private String optimizedFor;
	private Optional<ZonedDateTime> departureTime;
	private Optional<ZonedDateTime> arrivalTime;
	
	/**
	 * Which context - a combination of maps, timeseries,.. - will be / was used
	 * for routing
	 */
	@JsonProperty(required = true)
	public String getContextName() {
		return contextName;
	}

	@JsonProperty(required = true)
	public Location getFrom() {
		return from;
	}

	@JsonProperty(required = true)
	public Location getTo() {
		return to;
	}
	
	/**
	 * One or more modes of transport that will be / were used for routing. In
	 * case of a single mode of transport unimodal routing is requested, in case
	 * of several modes of transport intermodal routing is requested.
	 * <p>
	 * In case of intermodal routing it is guaranteed that the returned set
	 * contains {@link ModeOfTransport#FOOT}.
	 */
	@JsonProperty(required = true)
	public Set<RouteSegment.ModeOfTransport> getModesOfTransport() {
		return modesOfTransport;
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

	private RoutingRequest(Builder builder) {
		this.contextName = builder.contextName;
		this.from = builder.from;
		this.to = builder.to;
		this.modesOfTransport = builder.modesOfTransport;
		this.optimizedFor = builder.optimizedFor;
		this.departureTime = builder.departureTime;
		this.arrivalTime = builder.arrivalTime;
	}
	
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String contextName;
		private Location from;
		private Location to;
		private Set<RouteSegment.ModeOfTransport> modesOfTransport;
		private String optimizedFor;
		private Optional<ZonedDateTime> departureTime = Optional.empty();
		private Optional<ZonedDateTime> arrivalTime = Optional.empty();

		public Builder withContextName(String context) {
			this.contextName = context;
			return this;
		}
		
		public Builder withFrom(Location from) {
			this.from = from;
			return this;
		}

		public Builder withTo(Location to) {
			this.to = to;
			return this;
		}

		@JsonIgnore
		public Builder withModesOfTransport(String modesOfTransport) {
			this.modesOfTransport = SprouteUtils.parseModesOfTransport(modesOfTransport, "modesOfTransport");
			return this;
		}
		
		@JsonProperty
		public Builder withModesOfTransport(Set<RouteSegment.ModeOfTransport> modesOfTransport) {
			this.modesOfTransport = new HashSet<>(modesOfTransport);
			return this;
		}
		
		public Builder withOptimizedFor(String optimizedFor) {
			this.optimizedFor = optimizedFor;
			return this;
		}

		@JsonIgnore
		public Builder withDepartureTime(ZonedDateTime departureTime) {
			this.departureTime = Optional.ofNullable(departureTime);
			return this;
		}
		
		@JsonProperty
        public Builder withDepartureTime(String departureTime) {
			if(departureTime == null)
				this.departureTime = Optional.empty();
			else if(departureTime.equalsIgnoreCase("NOW"))
				this.departureTime = Optional.of(ZonedDateTime.now());
			else
				this.departureTime = Optional.of(SprouteUtils.parseZonedDateTime(departureTime, "departureTime"));
            return this;
        }

        @JsonIgnore
		public Builder withArrivalTime(ZonedDateTime arrivalTime) {
			this.arrivalTime = Optional.ofNullable(arrivalTime);
			return this;
		}
		
        @JsonProperty
        public Builder withArrivalTime(String arrivalTime) {
        	if(arrivalTime == null)
        		this.arrivalTime = Optional.empty();
        	else if(arrivalTime.equalsIgnoreCase("NOW"))
				this.arrivalTime = Optional.of(ZonedDateTime.now());
        	else
        		this.arrivalTime = Optional.of(SprouteUtils.parseZonedDateTime(arrivalTime, "arrivalTime"));
            return this;
        }

		public RoutingRequest build() {
			validate();
			return new RoutingRequest(this);
		}

		private void validate() {
			Preconditions.checkArgument(contextName != null, "contextName is mandatory but missing");
			Preconditions.checkArgument(from != null, "from is mandatory but missing");
			Preconditions.checkArgument(to != null, "to is mandatory but missing");
			Preconditions.checkArgument(modesOfTransport != null, "modesOfTransport is mandatory but missing");
			Preconditions.checkArgument(modesOfTransport.size() >= 1, ">= 1 modesOfTransport must be used");
			if(modesOfTransport.size() > 1 && !modesOfTransport.contains(ModeOfTransport.FOOT)) {
				modesOfTransport.add(ModeOfTransport.FOOT);
			}
			
			if(optimizedFor == null)
				optimizedFor = "TRAVELTIME";
			
			Preconditions.checkArgument(!(departureTime.isPresent() && arrivalTime.isPresent()), "departureTime and arrivalTime are mutually exclusive, only one can be set at once");
			if(!departureTime.isPresent() && !arrivalTime.isPresent()) {
				departureTime = Optional.of(ZonedDateTime.now());
			}
		}
	}

}
