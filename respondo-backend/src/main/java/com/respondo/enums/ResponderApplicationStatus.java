package com.respondo.enums;

/**
 * Status of a user's application to become a responder. A user only
 * gains Role.RESPONDER once this reaches APPROVED (see Section 6/28
 * business rules: responder role is granted only after admin approval).
 */
public enum ResponderApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
