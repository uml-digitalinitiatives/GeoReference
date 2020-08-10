package ca.umanitoba.libraries.georeferencing.exceptions;

/**
 * Simple exception if we don't have enough information in the LocationRequest to process.
 * @author whikloj
 */
public class LocationRequestException extends Exception {

    /**
     * Basic constructor
     *
     * @param message The error message.
     */
    public LocationRequestException(final String message) {
        super(message);
    }
}
