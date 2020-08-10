package ca.umanitoba.libraries.georeferencing.exceptions;

/**
 * Problems finding a location from the service.
 * @author whikloj
 */
public class NameLookupException extends Exception {

    /**
     * Basic constructor.
     */
    public NameLookupException(final String msg) {
        super(msg);
    }
}
