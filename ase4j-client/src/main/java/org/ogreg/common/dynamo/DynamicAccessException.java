package org.ogreg.common.dynamo;

/**
 * Signals that a {@link DynamicObject} access has failed.
 *
 * @author  Gergely Kiss
 */
public class DynamicAccessException extends RuntimeException {
    private static final long serialVersionUID = -5075774466447656L;

    public DynamicAccessException(String message) {
        super(message);
    }

    public DynamicAccessException(Throwable cause) {
        super(cause);
    }

    public DynamicAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
