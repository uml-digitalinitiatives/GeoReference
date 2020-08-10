package ca.umanitoba.libraries.georeferencing.exceptions;

/**
 * Holds internal errors to expose to the client.
 * @author whikloj
 */
public class InternalApplicationError extends RuntimeException {

    /**
     * Basic Constructor.
     * @param msg Error message.
     */
    public InternalApplicationError(final String msg) {
        super(msg);
    }

    /**
     * Constructor.
     * @param e the underlying cause.
     */
    public InternalApplicationError(final Throwable e) {
        super(e.getMessage(), e);
    }
}
