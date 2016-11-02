package at.ac.ait.ariadne.routeformat.geojson;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GeoJSONLineStringTest {

	private static GeoJSONLineString lineString;

	@BeforeClass
	public static void setUp() {
		List<Coordinate> points = Arrays.asList(new Coordinate(1.111, 1.2), new Coordinate(2.1, 2.2),
				new Coordinate(3.1, 3.2), new Coordinate(4.1, 4.2));
		lineString = new GeoJSONLineString(points);
	}

	@Test
	public void subLineStringTest() {
		GeoJSONLineString subLineString = lineString.subLineString(0, 4);
		Assert.assertEquals(4, subLineString.coordinates.size());

		subLineString = lineString.subLineString(1, 2);
		Assert.assertEquals(1, subLineString.coordinates.size());
		Assert.assertEquals(2.1, subLineString.coordinates.get(0).get(0).doubleValue(), 0.001);
	}

	@Test
	public void wktTest() {
		Assert.assertEquals("LINESTRING (1.111 1.2, 2.1 2.2, 3.1 3.2, 4.1 4.2)", lineString.toWKT());
	}

	@Test
	public void emptyWktTest() {
		GeoJSONLineString empty = new GeoJSONLineString();
		Assert.assertTrue(empty.isEmpty());
		Assert.assertEquals("LINESTRING EMPTY", empty.toWKT());
	}

}
