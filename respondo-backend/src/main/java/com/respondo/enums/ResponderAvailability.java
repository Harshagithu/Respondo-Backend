package com.respondo.enums;

/**
 * Field-availability of an approved responder. SUSPENDED is set by an
 * admin and blocks the responder from receiving new assignments even if
 * they would otherwise be AVAILABLE.
 */
public enum ResponderAvailability {
    AVAILABLE,
    BUSY,
    OFF_DUTY,
    SUSPENDED
}
