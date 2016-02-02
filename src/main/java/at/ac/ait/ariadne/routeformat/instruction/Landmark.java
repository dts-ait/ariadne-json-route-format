package at.ac.ait.ariadne.routeformat.instruction;

/**
 * TODO replace with a new class extending Location?
 * 
 * @author AIT Austrian Institute of Technology GmbH
 */
public class Landmark {

	public enum Preposition {
		BEFORE, AT, AFTER
	}

	public final Preposition preposition;
	public final String description;

	public Landmark(Preposition preposition, String description) {
		super();
		this.preposition = preposition;
		this.description = description;
	}

}