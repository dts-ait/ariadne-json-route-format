package at.ac.ait.ariadne.routeformat.instruction;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import at.ac.ait.ariadne.routeformat.Constants.CompassDirection;
import at.ac.ait.ariadne.routeformat.Constants.FormOfWay;
import at.ac.ait.ariadne.routeformat.Constants.GeneralizedModeOfTransportType;
import at.ac.ait.ariadne.routeformat.Constants.RoadCrossing;
import at.ac.ait.ariadne.routeformat.Constants.Tunnel;
import at.ac.ait.ariadne.routeformat.Constants.TurnDirection;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONCoordinate;

/**
 * A {@link RoadInstruction} contains episodes with classic-style turn
 * navigations for street-based modes of transport such as walking, cycling and
 * driving (keep straight, turn left/right, make a u-turn).
 * <p>
 * In its minimal form it consists of a position, a {@link #getSubType()} and at
 * least one of {@link #getOntoStreetName()} and {@link #getOntoFormOfWay()}.
 * <p>
 * Exemplary EBNF of how this instruction can be transformed into human-readable
 * text and what's mandatory / optional. Elements ending with STRING are
 * terminal (not defined any further).
 * <p>
 * UNTIL_LANDMARK_STRING must be retrieved from the next {@link Instruction}, it
 * can be a classic landmark or also the type of the next instruction, e.g.
 * roundabout.
 * 
 * <pre>
 * {@code
 * ROAD_INSTRUCTION = ROUTE_START | ROUTE_START_UNMENTIONED | ROUTE_END | STRAIGHT | TURN | U_TURN | SWITCH_SIDE_OF_ROAD;
 * 
 * ROUTE_START = "Start", [LANDMARK_PART], "on", NAME_TYPE, [HEADING], [CONTINUE];
 * ROUTE_START_UNMENTIONED = [LANDMARK_PART], "on", NAME_TYPE, [HEADING], [CONTINUE];
 * STRAIGHT = [LANDMARK_PART], "Go straight", [CROSSING_PART], "on", NAME_TYPE, [HEADING], [CONTINUE];
 * TURN = [LANDMARK_PART], "Turn", ["slight"], DIRECTION, [CROSSING_PART], "on", NAME_TYPE, [HEADING], [CONTINUE];
 * U_TURN = [LANDMARK_PART], "Make a u-turn", [CROSSING_PART], "on", NAME_TYPE, [HEADING], [CONTINUE];
 * SWITCH_SIDE_OF_ROAD = [LANDMARK_PART], "Switch to the", DIRECTION, "side of", NAME_TYPE, [CROSSING_PART], [HEADING], [CONTINUE];
 * ROUTE_END = "You reached your destination", [LANDMARK_PART], "on", NAME_TYPE;
 * 
 * NAME_TYPE = [STREET_NAME_STRING], [FORM_OF_WAY_STRING], [onto the bridge], [into the TUNNEL_STRING];
 * CROSSING_PART = "over the intersection" | "at the traffic light" | ...
 * HEADING = "heading", COMPASS_STRING;
 *
 * CONTINUE = "and continue", [ROAD_SIDE], ["for", UNIT], [CONFIRMATION_LANDMARK_PART], [UNTIL]; (* at least one of the options *)
 * ROAD_SIDE = "on the", "left" | "right", "side of the road;
 * UNIT = [DISTANCE_STRING], [TIME_STRING]; (* at least one of the two *)
 * UNTIL = "until", [INTERSECTING_ROAD_STRING], [UNTIL_LANDMARK_PART]; 
 * 
 * LANDMARK_PART = PREPOSITION, LANDMARK_STRING;
 * CONFIRMATION_LANDMARK_PART = CONFIRMATION_PREPOSITION, CONFIRMATION_LANDMARK_STRING;
 * UNTIL_LANDMARK_PART = PREPOSITION, UNTIL_LANDMARK_STRING;
 * 
 * PREPOSITION = "before" | "at" | "after";
 * CONFIRMATION_PREPOSITION = "towards" | "through" | "along" | "past";
 * 
 * DIRECTION = "left" | "right";
 * }
 * </pre>
 * 
 * @author AIT Austrian Institute of Technology GmbH
 */
@JsonInclude(Include.NON_ABSENT)
public class RoadInstruction extends Instruction<RoadInstruction> {

    public enum SubType {
        ROUTE_START,
        /**
         * This instruction is the first in a route, but the instruction should
         * not mention this explicitly. A use case for this type is e.g.
         * rerouting: in the background a new route is calculated but from the
         * perspective of the user this is not a route start
         */
        ROUTE_START_UNMENTIONED, ROUTE_END, STRAIGHT, TURN, U_TURN,
        /**
         * Instruction for switching the side of a road - typically one and the
         * same - and continuing in the <b>same</b> direction. The change can
         * happen at a junction or on the open road. Typically relevant for foot
         * and bicycle traffic using zebra crossings or bicycle crossings, but
         * it is also possible that this is just a recommended place to cross
         * the road (without any marked crossing).
         */
        SWITCH_SIDE_OF_ROAD
    }

    private SubType subType;
    private Optional<GeneralizedModeOfTransportType> modeOfTransport = Optional.empty();
    private Optional<TurnDirection> turnDirection = Optional.empty();
    private Optional<CompassDirection> compassDirection = Optional.empty();
    private Optional<Boolean> roadChange = Optional.empty();
    private Optional<String> ontoStreetName = Optional.empty();
    private Optional<FormOfWay> ontoFormOfWay = Optional.empty();
    private Optional<Boolean> enterBridge = Optional.empty();
    private Optional<Tunnel> enterTunnel = Optional.empty();
    private Optional<Boolean> ontoRightSideOfRoad = Optional.empty();
    private Optional<RoadCrossing> crossing = Optional.empty();
    private Optional<Integer> continueMeters = Optional.empty(), continueSeconds = Optional.empty();
    private Optional<String> continueUntilIntersectingStreetName = Optional.empty();
    private Optional<Landmark> landmark = Optional.empty(), confirmationLandmark = Optional.empty();

    // -- getters

    @JsonProperty(required = true)
    public SubType getSubType() {
        return subType;
    }

    /**
     * @return the mode of transport for the turn (walk left, cycle left,..)
     */
    public Optional<GeneralizedModeOfTransportType> getModeOfTransport() {
        return modeOfTransport;
    }

    /**
     * @return the turn direction relative to the direction until this point
     */
    public Optional<TurnDirection> getTurnDirection() {
        return turnDirection;
    }

    /**
     * @return the heading after this point
     */
    public Optional<CompassDirection> getCompassDirection() {
        return compassDirection;
    }

    /** @return <code>true</code> if the road name or type has changed */
    public Optional<Boolean> getRoadChange() {
        return roadChange;
    }

    public Optional<String> getOntoStreetName() {
        return ontoStreetName;
    }

    public Optional<FormOfWay> getOntoFormOfWay() {
        return ontoFormOfWay;
    }

    /**
     * @return information if this instruction marks the entrance to a bridge
     */
    public Optional<Boolean> getEnterBridge() {
        return enterBridge;
    }

    /**
     * @return information if this instruction marks the entrance to a tunnal
     *         (and which kind of tunnel)
     */
    public Optional<Tunnel> getEnterTunnel() {
        return enterTunnel;
    }

    /**
     * Defines the side of the road where the route continues. This is mostly
     * relevant for pedestrians and maybe also for cyclists (e.g. when
     * bidirectional cycle paths run along a road on both sides). For other
     * modes of transport an empty Optional shall be returned.
     * 
     * @return <code>true</code> for the right side, <code>false</code> for the
     *         left side of the road (in moving direction), empty if unknown or
     *         not relevant
     */
    public Optional<Boolean> getOntoRightSideOfRoad() {
        return ontoRightSideOfRoad;
    }

    /**
     * @return a {@link RoadCrossing} in case the instruction starts with a road
     *         crossing
     */
    public Optional<RoadCrossing> getCrossing() {
        return crossing;
    }

    public Optional<Integer> getContinueMeters() {
        return continueMeters;
    }

    public Optional<Integer> getContinueSeconds() {
        return continueSeconds;
    }

    /**
     * @return the name of an intersecting road at the end of the current
     *         instruction, i.e. the place where the next instruction is
     */
    public Optional<String> getContinueUntilIntersectingStreetName() {
        return continueUntilIntersectingStreetName;
    }

    /**
     * @return the landmark at begin of the instruction, i.e. at the turn, or at
     *         the begin (for {@link SubType#ROUTE_START}) or at the end (for
     *         {@link SubType#ROUTE_END}) of the route. At the same time this
     *         landmark is the continue-landmark for the previous instruction,
     *         i.e. the landmark after {@link #getContinueMeters()}.
     */
    public Optional<Landmark> getLandmark() {
        return landmark;
    }

    /**
     * @return a landmark between this and the next instruction (or a global
     *         landmark in the general direction after this instruction) that
     *         helps users to stay on track
     */
    public Optional<Landmark> getConfirmationLandmark() {
        return confirmationLandmark;
    }

    // -- setters

    public RoadInstruction setSubType(SubType subType) {
        this.subType = subType;
        return this;
    }

    public RoadInstruction setModeOfTransport(GeneralizedModeOfTransportType modeOfTransport) {
        this.modeOfTransport = Optional.ofNullable(modeOfTransport);
        return this;
    }

    public RoadInstruction setTurnDirection(TurnDirection turnDirection) {
        this.turnDirection = Optional.ofNullable(turnDirection);
        return this;
    }

    public RoadInstruction setCompassDirection(CompassDirection compassDirection) {
        this.compassDirection = Optional.ofNullable(compassDirection);
        return this;
    }

    public RoadInstruction setRoadChange(Boolean roadChange) {
        this.roadChange = Optional.ofNullable(roadChange);
        return this;
    }

    public RoadInstruction setOntoStreetName(String ontoStreetName) {
        this.ontoStreetName = Optional.ofNullable(ontoStreetName);
        return this;
    }

    public RoadInstruction setOntoFormOfWay(FormOfWay ontoFormOfWay) {
        this.ontoFormOfWay = Optional.ofNullable(ontoFormOfWay);
        return this;
    }

    public RoadInstruction setEnterBridge(Boolean enterBridge) {
        this.enterBridge = Optional.ofNullable(enterBridge);
        return this;
    }

    public RoadInstruction setEnterTunnel(Tunnel enterTunnel) {
        this.enterTunnel = Optional.ofNullable(enterTunnel);
        return this;
    }

    public RoadInstruction setOntoRightSideOfRoad(Boolean ontoRightSideOfRoad) {
        this.ontoRightSideOfRoad = Optional.ofNullable(ontoRightSideOfRoad);
        return this;
    }

    public RoadInstruction setCrossing(RoadCrossing crossing) {
        this.crossing = Optional.ofNullable(crossing);
        return this;
    }

    public RoadInstruction setContinueMeters(Integer continueMeters) {
        this.continueMeters = Optional.ofNullable(continueMeters);
        return this;
    }

    public RoadInstruction setContinueSeconds(Integer continueSeconds) {
        this.continueSeconds = Optional.ofNullable(continueSeconds);
        return this;
    }

    public RoadInstruction setContinueUntilIntersectingStreetName(String continueUntilIntersectingStreetName) {
        this.continueUntilIntersectingStreetName = Optional.ofNullable(continueUntilIntersectingStreetName);
        return this;
    }

    /**
     * @param landmark
     *            the landmark at the start point, end point, or decision point
     */
    public RoadInstruction setLandmark(Landmark landmark) {
        this.landmark = Optional.ofNullable(landmark);
        return this;
    }

    public RoadInstruction setConfirmationLandmark(Landmark confirmationLandmark) {
        this.confirmationLandmark = Optional.ofNullable(confirmationLandmark);
        return this;
    }

    // --

    /**
     * either street name or form of way must be present
     */
    public static RoadInstruction createMinimalRouteStart(GeoJSONCoordinate position, Optional<String> ontoStreetName,
            Optional<FormOfWay> ontoFormOfWay) {
        return new RoadInstruction().setPosition(position).setSubType(SubType.ROUTE_START)
                .setOntoStreetName(ontoStreetName.orElse(null)).setOntoFormOfWay(ontoFormOfWay.orElse(null));
    }

    /**
     * either street name or form of way must be present
     */
    public static RoadInstruction createMinimalOnRoute(GeoJSONCoordinate position, TurnDirection turnDirection,
            Optional<String> ontoStreetName, Optional<FormOfWay> ontoFormOfWay) {
        return new RoadInstruction().setPosition(position).setSubType(getSubType(turnDirection))
                .setTurnDirection(turnDirection).setOntoStreetName(ontoStreetName.orElse(null))
                .setOntoFormOfWay(ontoFormOfWay.orElse(null));
    }

    /**
     * either street name or form of way (of the destination) must be present
     */
    public static RoadInstruction createMinimalRouteEnd(GeoJSONCoordinate position, Optional<String> onStreetName,
            Optional<FormOfWay> onFormOfWay) {
        return new RoadInstruction().setPosition(position).setSubType(SubType.ROUTE_END)
                .setOntoStreetName(onStreetName.orElse(null)).setOntoFormOfWay(onFormOfWay.orElse(null));
    }

    private static SubType getSubType(TurnDirection turnDirection) {
        if (turnDirection == TurnDirection.U_TURN)
            return SubType.U_TURN;
        else if (turnDirection == TurnDirection.STRAIGHT)
            return SubType.STRAIGHT;
        else
            return SubType.TURN;
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkArgument(subType != null, "subType is mandatory but missing");
        Preconditions.checkArgument(ontoStreetName.isPresent() || ontoFormOfWay.isPresent(),
                "at least one onto-type is required");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((compassDirection == null) ? 0 : compassDirection.hashCode());
        result = prime * result + ((confirmationLandmark == null) ? 0 : confirmationLandmark.hashCode());
        result = prime * result + ((continueMeters == null) ? 0 : continueMeters.hashCode());
        result = prime * result + ((continueSeconds == null) ? 0 : continueSeconds.hashCode());
        result = prime * result
                + ((continueUntilIntersectingStreetName == null) ? 0 : continueUntilIntersectingStreetName.hashCode());
        result = prime * result + ((crossing == null) ? 0 : crossing.hashCode());
        result = prime * result + ((enterBridge == null) ? 0 : enterBridge.hashCode());
        result = prime * result + ((enterTunnel == null) ? 0 : enterTunnel.hashCode());
        result = prime * result + ((landmark == null) ? 0 : landmark.hashCode());
        result = prime * result + ((modeOfTransport == null) ? 0 : modeOfTransport.hashCode());
        result = prime * result + ((ontoFormOfWay == null) ? 0 : ontoFormOfWay.hashCode());
        result = prime * result + ((ontoRightSideOfRoad == null) ? 0 : ontoRightSideOfRoad.hashCode());
        result = prime * result + ((ontoStreetName == null) ? 0 : ontoStreetName.hashCode());
        result = prime * result + ((roadChange == null) ? 0 : roadChange.hashCode());
        result = prime * result + ((subType == null) ? 0 : subType.hashCode());
        result = prime * result + ((turnDirection == null) ? 0 : turnDirection.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        RoadInstruction other = (RoadInstruction) obj;
        if (compassDirection == null) {
            if (other.compassDirection != null)
                return false;
        } else if (!compassDirection.equals(other.compassDirection))
            return false;
        if (confirmationLandmark == null) {
            if (other.confirmationLandmark != null)
                return false;
        } else if (!confirmationLandmark.equals(other.confirmationLandmark))
            return false;
        if (continueMeters == null) {
            if (other.continueMeters != null)
                return false;
        } else if (!continueMeters.equals(other.continueMeters))
            return false;
        if (continueSeconds == null) {
            if (other.continueSeconds != null)
                return false;
        } else if (!continueSeconds.equals(other.continueSeconds))
            return false;
        if (continueUntilIntersectingStreetName == null) {
            if (other.continueUntilIntersectingStreetName != null)
                return false;
        } else if (!continueUntilIntersectingStreetName.equals(other.continueUntilIntersectingStreetName))
            return false;
        if (crossing == null) {
            if (other.crossing != null)
                return false;
        } else if (!crossing.equals(other.crossing))
            return false;
        if (enterBridge == null) {
            if (other.enterBridge != null)
                return false;
        } else if (!enterBridge.equals(other.enterBridge))
            return false;
        if (enterTunnel == null) {
            if (other.enterTunnel != null)
                return false;
        } else if (!enterTunnel.equals(other.enterTunnel))
            return false;
        if (landmark == null) {
            if (other.landmark != null)
                return false;
        } else if (!landmark.equals(other.landmark))
            return false;
        if (modeOfTransport == null) {
            if (other.modeOfTransport != null)
                return false;
        } else if (!modeOfTransport.equals(other.modeOfTransport))
            return false;
        if (ontoFormOfWay == null) {
            if (other.ontoFormOfWay != null)
                return false;
        } else if (!ontoFormOfWay.equals(other.ontoFormOfWay))
            return false;
        if (ontoRightSideOfRoad == null) {
            if (other.ontoRightSideOfRoad != null)
                return false;
        } else if (!ontoRightSideOfRoad.equals(other.ontoRightSideOfRoad))
            return false;
        if (ontoStreetName == null) {
            if (other.ontoStreetName != null)
                return false;
        } else if (!ontoStreetName.equals(other.ontoStreetName))
            return false;
        if (roadChange == null) {
            if (other.roadChange != null)
                return false;
        } else if (!roadChange.equals(other.roadChange))
            return false;
        if (subType != other.subType)
            return false;
        if (turnDirection == null) {
            if (other.turnDirection != null)
                return false;
        } else if (!turnDirection.equals(other.turnDirection))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + " -> RoadInstruction [subType=" + subType + ", modeOfTransport=" + modeOfTransport
                + ", turnDirection=" + turnDirection + ", compassDirection=" + compassDirection + ", roadChange="
                + roadChange + ", ontoStreetName=" + ontoStreetName + ", ontoFormOfWay=" + ontoFormOfWay
                + ", enterBridge=" + enterBridge + ", enterTunnel=" + enterTunnel + ", ontoRightSideOfRoad="
                + ontoRightSideOfRoad + ", continueMeters=" + continueMeters + ", continueSeconds=" + continueSeconds
                + ", continueUntilIntersectingStreetName=" + continueUntilIntersectingStreetName + ", landmark="
                + landmark + ", confirmationLandmark=" + confirmationLandmark + "]";
    }

}
