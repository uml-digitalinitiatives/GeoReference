package ca.umanitoba.libraries.georeferencing.controller;

import javax.inject.Inject;

import ca.umanitoba.libraries.georeferencing.Coordinates;
import ca.umanitoba.libraries.georeferencing.GeoNamesFeatureCodes;
import ca.umanitoba.libraries.georeferencing.LocationRequest;
import ca.umanitoba.libraries.georeferencing.exceptions.InternalApplicationError;
import ca.umanitoba.libraries.georeferencing.exceptions.LocationRequestException;
import ca.umanitoba.libraries.georeferencing.exceptions.MissingCountryCodeException;
import ca.umanitoba.libraries.georeferencing.exceptions.NameLookupException;
import ca.umanitoba.libraries.georeferencing.api.CountryCode;
import ca.umanitoba.libraries.georeferencing.api.CountryCodeLookupService;
import ca.umanitoba.libraries.georeferencing.api.NameLookupService;
import ca.umanitoba.libraries.georeferencing.impl.DataHubCountryCodeLookupService;
import ca.umanitoba.libraries.georeferencing.impl.NameLookupServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rest controller for the web service.
 * @author whikloj
 */
@RestController
public class DereferenceController {

    @Inject
    private CountryCodeLookupService ccLookup;

    @Inject
    private NameLookupService nameLookup;

    @Inject
    private GeoNamesFeatureCodes geoNamesFeatureCodes;

    /**
     * Handle post requests.
     * @param request the requested LocationRequest.
     * @return coordinates as JSON.
     */
    @PostMapping(value = "/lookup", produces = "application/json")
    public Coordinates greeting(@RequestBody final LocationRequest request) {
        try {
            request.validateLocation();
            final CountryCode countryCode = ccLookup.lookupCountry(request.getCountry());

            // Set the alpha country code.
            request.setIso3361Alpha2(countryCode.getIso3316Alpha2());
            final Coordinates coordinates = nameLookup.lookup(request);
            if (coordinates == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Could not find the location");
            }
            return coordinates;
        } catch (final NameLookupException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (final MissingCountryCodeException | LocationRequestException | IllegalArgumentException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        } catch (final InternalApplicationError error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    error.getMessage());
        }
    }

    /**
     * Reset the database, and reload from the files.
     * @return A text message.
     */
    @PostMapping(value = "/reset", produces = "text/plain")
    public String reset() {
        try {
            geoNamesFeatureCodes.reset();
            ((DataHubCountryCodeLookupService) ccLookup).reset();
            ((NameLookupServiceImpl) nameLookup).reset();
            return "All tables reset";
        } catch (final InternalApplicationError e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }
    }
}
