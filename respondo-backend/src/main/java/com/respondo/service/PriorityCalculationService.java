package com.respondo.service;

import com.respondo.entity.Incident;
import com.respondo.enums.LocationRiskLevel;
import com.respondo.enums.Priority;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Section 9: automatic priority calculation, run once right after
 * dispatcher verification.
 *
 * Scoring strategy — three factors, each normalized to a 1-10 scale, summed
 * into a single 3-30 score:
 *
 *   1. categoryWeight   (1-10) — IncidentCategory.severityWeight, set by
 *                                admin per category (e.g. Medical
 *                                Emergency = 9, Traffic Accident = 5).
 *   2. locationRiskWeight (2-10) — from the dispatcher-provided
 *                                LocationRiskLevel (LOW=2 ... SEVERE=10).
 *   3. affectedFactor   (1-10) — affectedPeopleCount, capped at 10 so a
 *                                mass-casualty incident doesn't need an
 *                                unbounded scale to still register as
 *                                maximally severe.
 *
 * The summed score is then bucketed into the four Priority levels. The
 * thresholds were chosen so no single factor alone can push an incident
 * into CRITICAL — it takes at least two factors reading high together
 * (e.g. a high-severity category in a high-risk location), which keeps a
 * lone "10 people affected" report at a minor category from crowding out
 * genuinely severe incidents.
 */
@Service
public class PriorityCalculationService {

    private static final Map<LocationRiskLevel, Integer> LOCATION_RISK_WEIGHTS = Map.of(
            LocationRiskLevel.LOW, 2,
            LocationRiskLevel.MODERATE, 5,
            LocationRiskLevel.HIGH, 8,
            LocationRiskLevel.SEVERE, 10
    );

    private static final int LOW_MAX = 10;
    private static final int MEDIUM_MAX = 18;
    private static final int HIGH_MAX = 25;
    // score > HIGH_MAX (up to 30) => CRITICAL

    public Priority calculate(Incident incident) {
        int categoryWeight = incident.getCategory().getSeverityWeight();
        int locationRiskWeight = LOCATION_RISK_WEIGHTS.getOrDefault(incident.getLocationRiskLevel(), 5);
        int affectedFactor = Math.min(incident.getAffectedPeopleCount(), 10);

        int score = categoryWeight + locationRiskWeight + affectedFactor;

        return toPriority(score);
    }

    private Priority toPriority(int score) {
        if (score > HIGH_MAX) {
            return Priority.CRITICAL;
        }
        if (score > MEDIUM_MAX) {
            return Priority.HIGH;
        }
        if (score > LOW_MAX) {
            return Priority.MEDIUM;
        }
        return Priority.LOW;
    }
}
