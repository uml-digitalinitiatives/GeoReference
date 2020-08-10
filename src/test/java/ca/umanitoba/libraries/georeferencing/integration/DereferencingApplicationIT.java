package ca.umanitoba.libraries.georeferencing.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.umanitoba.libraries.georeferencing.Coordinates;
import ca.umanitoba.libraries.georeferencing.LocationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DereferencingApplicationIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String lookupUri;

    @BeforeEach
    public void setUp() {
        lookupUri = "http://localhost:" + port + "/lookup";
    }

    @Test
    public void testNoCountry() {
        final LocationRequest request = new LocationRequest();
        request.setProvince("manitoba");
        request.setCity("Winnipeg");
        final ResponseEntity<String> responseEntity = this.restTemplate
                .postForEntity(lookupUri, request, String.class);
        assertEquals(400, responseEntity.getStatusCodeValue());
    }

    @Test
    public void testMissing() {
        final LocationRequest request = new LocationRequest();
        request.setCountry("Canada");
        request.setProvince("manitoba");
        request.setCity("Portage la prairie");
        final ResponseEntity<String> responseEntity = this.restTemplate
                .postForEntity(lookupUri, request, String.class);
        assertEquals(404, responseEntity.getStatusCodeValue());
    }

    @Test
    public void testWrongProvince() {
        final LocationRequest request = new LocationRequest();
        request.setCountry("Canada");
        request.setProvince("Alberta");
        request.setCity("Winnipeg");
        final ResponseEntity<String> responseEntity = this.restTemplate
                .postForEntity(lookupUri, request, String.class);
        assertEquals(404, responseEntity.getStatusCodeValue());
    }

    @Test
    public void testFound() {
        final LocationRequest request = new LocationRequest();
        request.setCountry("Canada");
        request.setProvince("manitoba");
        request.setCity("Winnipeg");
        final Coordinates coordinates = this.restTemplate.postForObject(lookupUri, request, Coordinates.class);
        assertEquals(49.8844, coordinates.getLatitude());
        assertEquals(-97.14704, coordinates.getLongitude());
    }
}
