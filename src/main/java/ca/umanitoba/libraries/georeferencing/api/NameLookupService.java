package ca.umanitoba.libraries.georeferencing.api;

import ca.umanitoba.libraries.georeferencing.Coordinates;
import ca.umanitoba.libraries.georeferencing.LocationRequest;
import ca.umanitoba.libraries.georeferencing.exceptions.NameLookupException;

/**
 * Service to take a LocationRequest and return coordinates.
 * @author whikloj
 */
public interface NameLookupService {

    /**
     * Lookup a location and return set of coordinates
     * @param location the location parts to use for the lookup
     * @return a set of Coordinates.
     *
     * @throws NameLookupException If location is not found.
     */
    public Coordinates lookup(final LocationRequest location) throws NameLookupException;
}
