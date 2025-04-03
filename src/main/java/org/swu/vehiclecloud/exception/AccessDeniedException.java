package org.swu.vehiclecloud.exception;

public class AccessDeniedException extends RuntimeException {

    // Default constructor
    public AccessDeniedException() {
        super("Access Denied: Insufficient Permissions");
    }

    // Constructor with custom message
    public AccessDeniedException(String message) {
        super(message);
    }

    // Constructor with custom message and cause
    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

    // Constructor with cause
    public AccessDeniedException(Throwable cause) {
        super(cause);
    }
}

