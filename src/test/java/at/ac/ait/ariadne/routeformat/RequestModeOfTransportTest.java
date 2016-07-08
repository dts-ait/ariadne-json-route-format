package at.ac.ait.ariadne.routeformat;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import at.ac.ait.ariadne.routeformat.Constants.DetailedModeOfTransportType;
import at.ac.ait.ariadne.routeformat.geojson.CoordinatePoint;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONFeature;
import at.ac.ait.ariadne.routeformat.location.Location;

public class RequestModeOfTransportTest {

    private static Location location = Location.builder()
            .withCoordinate(GeoJSONFeature.newPointFeature(new CoordinatePoint("16.40073", "48.25625"))).build();

    @SuppressWarnings("unused")
    @Test
    public void testRequestBuilding() {
        Map<String, Object> additionalInfo;

        additionalInfo = ImmutableMap.of("preferParks", true);
        RequestModeOfTransport foot = RequestModeOfTransport.builder()
                .withModeOfTransport(ModeOfTransport.STANDARD_FOOT).withAdditionalInfo(additionalInfo).build();

        additionalInfo = ImmutableMap.of("preferParks", true);
        ModeOfTransport bicycleMot = ModeOfTransport.builder().withDetailedType(DetailedModeOfTransportType.BICYCLE)
                .withElectric(true).withId("My fast Rotwild bicycle").build();
        RequestModeOfTransport bicycle = RequestModeOfTransport.builder().withModeOfTransport(bicycleMot)
                .withLocations(Arrays.asList(location)).withAdditionalInfo(additionalInfo).build();

        RequestPublicTransportModeOfTransport.builder().withModeOfTransport(ModeOfTransport.STANDARD_PUBLIC_TRANSPORT)
                .withExcludedPublicTransportModes(Sets.newHashSet(DetailedModeOfTransportType.CABLE_CAR)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMandatoryField() {
        RequestPublicTransportModeOfTransport.builder()
                .withExcludedPublicTransportModes(Sets.newHashSet(DetailedModeOfTransportType.CABLE_CAR)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonPublicTransportMot() {
        RequestPublicTransportModeOfTransport.builder().withModeOfTransport(ModeOfTransport.STANDARD_CAR).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonPublicTransportExclusionMots() {
        RequestPublicTransportModeOfTransport.builder().withModeOfTransport(ModeOfTransport.STANDARD_PUBLIC_TRANSPORT)
                .withExcludedPublicTransportModes(Sets.newHashSet(DetailedModeOfTransportType.FOOT)).build();
    }

}