package com.respondo.enums;

/**
 * One of the inputs to PriorityCalculationService (Phase 4). Represents
 * the baseline risk of the incident's location independent of the
 * incident itself (e.g. dense area, hazardous zone).
 */
public enum LocationRiskLevel {
    LOW,
    MODERATE,
    HIGH,
    SEVERE
}
