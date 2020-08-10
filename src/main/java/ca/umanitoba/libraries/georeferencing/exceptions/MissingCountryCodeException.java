package ca.umanitoba.libraries.georeferencing.exceptions;

import java.security.InvalidKeyException;

/**
 * An exception if the country cannot be converted to a country code.
 * @author whikloj
 */
public class MissingCountryCodeException extends InvalidKeyException {

    /**
     * Basic constructor.
     * @param message the exception message.
     */
    public MissingCountryCodeException(final String message) {
        super(message);
    }
}
