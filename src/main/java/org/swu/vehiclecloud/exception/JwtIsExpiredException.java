package org.swu.vehiclecloud.exception;

public class JwtIsExpiredException extends RuntimeException{
    public JwtIsExpiredException() {}

    public JwtIsExpiredException(String message) {
        super(message);
    }

    public JwtIsExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public JwtIsExpiredException(Throwable cause) {
        super(cause);
    }
}
