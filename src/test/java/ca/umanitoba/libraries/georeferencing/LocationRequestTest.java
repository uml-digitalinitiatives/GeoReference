package ca.umanitoba.libraries.georeferencing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import ca.umanitoba.libraries.georeferencing.exceptions.LocationRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test of LocationRequest.
 * @author whikloj
 */
public class LocationRequestTest {

    private static ObjectMapper objectMapper;

    private Map<String, String> parts;

    private String requestBody;

    @BeforeAll
    public static void beforeClass() {
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    public void setUp() {
        parts = new HashMap<>();
    }

    /**
     * Make a JSON string based on the keys and values in the parts map.
     */
    private void prepareRequest() {
        requestBody = "";
        for (final Map.Entry<String, String> part : parts.entrySet()) {
            requestBody += (requestBody.isEmpty() ? "" : ", ") +
                    String.format("\"%s\": \"%s\"", part.getKey(), part.getValue());
        }
        requestBody = String.format("{ %s }", requestBody);
    }

    /**
     * Do standard equality checks
     * @param request the location request.
     */
    private void assertRequest(final LocationRequest request) {
        if (parts.containsKey("country")) {
            assertEquals(parts.get("country"), request.getCountry());
        }
        if (parts.containsKey("province")) {
            assertEquals(parts.get("province"), request.getProvince());
        }
        if (parts.containsKey("county")) {
            assertEquals(parts.get("county"), request.getCounty());
        }
        if (parts.containsKey("city")) {
            assertEquals(parts.get("city"), request.getCity());
        }
        if (parts.containsKey("citySection")) {
            assertEquals(parts.get("citySection"), request.getCitySection());
        }
    }

    @Test
    public void testNoCountry() throws Exception {
        parts.put("province", "manitoba");
        parts.put("city", "oakbank");
        parts.put("county", "rm of springfield");
        parts.put("citySection", "northside");
        prepareRequest();
        final LocationRequest request = objectMapper.readValue(requestBody, LocationRequest.class);
        assertRequest(request);
        assertDoesNotThrow(request::validateLocation);
    }

    @Test
    public void testNoProvince() throws Exception {
        parts.put("country", "canada");
        parts.put("city", "oakbank");
        parts.put("county", "rm of springfield");
        parts.put("citySection", "northside");
        prepareRequest();
        final LocationRequest request = objectMapper.readValue(requestBody, LocationRequest.class);
        assertRequest(request);
        assertDoesNotThrow(request::validateLocation);
    }

    @Test
    public void testNoCity() throws Exception {
        parts.put("country", "canada");
        parts.put("province", "manitoba");
        parts.put("county", "rm of springfield");
        parts.put("citySection", "northside");
        prepareRequest();
        final LocationRequest request = objectMapper.readValue(requestBody, LocationRequest.class);
        assertRequest(request);
        assertDoesNotThrow(request::validateLocation);
    }
    @Test
    public void testNoCounty() throws Exception {
        parts.put("country", "canada");
        parts.put("province", "manitoba");
        parts.put("city", "oakbank");
        parts.put("citySection", "northside");
        prepareRequest();
        final LocationRequest request = objectMapper.readValue(requestBody, LocationRequest.class);
        assertRequest(request);
        assertDoesNotThrow(request::validateLocation);
    }

    @Test
    public void testNoCitySection() throws Exception {
        parts.put("country", "canada");
        parts.put("province", "manitoba");
        parts.put("county", "rm of springfield");
        parts.put("city", "oakbank");
        prepareRequest();
        final LocationRequest request = objectMapper.readValue(requestBody, LocationRequest.class);
        assertRequest(request);
        assertDoesNotThrow(request::validateLocation);
    }

    @Test
    public void testFailure() throws Exception {
        parts.put("county", "rm of springfield");
        parts.put("citySection", "northside");
        prepareRequest();
        final LocationRequest request = objectMapper.readValue(requestBody, LocationRequest.class);
        assertRequest(request);
        assertThrows(LocationRequestException.class, request::validateLocation);
    }
}
