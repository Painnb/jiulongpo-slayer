package org.swu.vehiclecloud.exception;

public class JwtParseFailedException extends RuntimeException {
    public JwtParseFailedException() {}

    public JwtParseFailedException(String message) {
        super(message);
    }

    public JwtParseFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public JwtParseFailedException(Throwable cause) {
        super(cause);
    }
}
