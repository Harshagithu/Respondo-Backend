package com.respondo.exception;

/**
 * Not thrown anywhere yet — reserved for the incident workflow guard in
 * Phase 3. Declared now alongside its siblings so the exception package
 * is complete rather than growing piecemeal.
 */
public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
