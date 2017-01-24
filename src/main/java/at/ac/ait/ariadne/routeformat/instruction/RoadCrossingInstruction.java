package at.ac.ait.ariadne.routeformat.instruction;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.base.Preconditions;

import at.ac.ait.ariadne.routeformat.Constants.RoadCrossing;
import at.ac.ait.ariadne.routeformat.Constants.TurnDirection;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONCoordinate;

// Gehen Sie [beim Landmark] (geradeaus | [leicht] rechts/links) über [die
// Kreuzung | den Zebrastreifen | die Ampel].
// 
// At the POI go straight over the crosswalk. Before the church go slight left
// over the traffic light. Before the church turn slight left over the traffic
// light.
/**
 * Instructions for crossing a road or several roads that are close together.
 * Typically relevant for foot and bicycle traffic using zebra crossings or
 * bicycle crossings.
 * <p>
 * For the simple case just switching the side of a road see
 * {@link RoadInstruction.SubType#SWITCH_SIDE_OF_ROAD}.
 * <p>
 * This instruction deliberately has no continue-part as it only covers a more
 * or less complex crossing and should directly be followed by a
 * {@link RoadInstruction} which then explains where to turn / continue after
 * the crossing.
 * <p>
 * In its minimal form it consists of a position and a turn direction.
 * <p>
 * Exemplary EBNF of how this instruction can be transformed into human-readable
 * text and what's mandatory / optional. Elements ending with STRING are
 * terminal (not defined any further).
 * <p>
 * 
 * <pre>
 * {@code
 * ROAD_CROSSING_INSTRUCTION = [LANDMARK_PART], DIRECTION, "over the", [ROAD_CROSSING];
 * LANDMARK_PART = PREPOSITION, "the", LANDMARK_STRING;
 * PREPOSITION = "before" | "at" | "after";
 * DIRECTION = STRAIGHT | ("turn", TURN_DIRECTION);
 * STRAIGHT = "go straight";
 * TURN_DIRECTION = ["slight"], ("left" | "right");
 * ROAD_CROSSING = "crossing" | "zebra crossing" | "traffic light";
 * }
 * </pre>
 * 
 * @author AIT Austrian Institute of Technology GmbH
 */
@JsonInclude(Include.NON_EMPTY)
public class RoadCrossingInstruction extends Instruction<RoadCrossingInstruction> {

    private TurnDirection turnDirection;
    private Optional<Landmark> landmark = Optional.empty();
    private Optional<RoadCrossing> roadCrossing = Optional.empty();

    // -- getters

    public TurnDirection getTurnDirection() {
        return turnDirection;
    }

    public Optional<Landmark> getLandmark() {
        return landmark;
    }

    public Optional<RoadCrossing> getRoadCrossing() {
        return roadCrossing;
    }

    // -- setters

    public RoadCrossingInstruction setTurnDirection(TurnDirection turnDirection) {
        this.turnDirection = turnDirection;
        return this;
    }

    public RoadCrossingInstruction setLandmark(Landmark landmark) {
        this.landmark = Optional.ofNullable(landmark);
        return this;
    }

    public RoadCrossingInstruction setRoadCrossing(RoadCrossing roadCrossing) {
        this.roadCrossing = Optional.ofNullable(roadCrossing);
        return this;
    }

    // --

    public static RoadCrossingInstruction createMinimal(GeoJSONCoordinate position, TurnDirection turnDirection) {
        return new RoadCrossingInstruction().setPosition(position).setTurnDirection(turnDirection);
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkArgument(turnDirection != null, "turnDirection is mandatory but missing");
    }

    @Override
    public String toString() {
        return super.toString() + " -> RoadCrossingInstruction [turnDirection=" + turnDirection + ", landmark="
                + landmark + ", roadCrossing=" + roadCrossing + "]";
    }

}
