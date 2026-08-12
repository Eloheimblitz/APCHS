package com.airpollution.survey.service;

public final class SurveyCatalog {
    public static final String[] SYMPTOM_KEYS = {
            "HEADACHE", "EYE_IRRITATION", "RHINITIS", "SNEEZING", "SINUSITIS",
            "SORE_THROAT", "COLD", "FEVER", "DRY_COUGH", "WET_COUGH",
            "WHEEZING", "BREATHLESSNESS", "CHEST_DISCOMFORT", "SLEEP_DISTURBANCE",
            "SKIN_IRRITATION"
    };

    public static final String[] CONDITION_KEYS = {
            "ASTHMA", "INHALER_USE", "TUBERCULOSIS", "HEART_PROBLEMS",
            "DIABETES", "HIGH_BP", "CANCER"
    };

    private SurveyCatalog() {
    }

    public static boolean isSymptomKey(String key) {
        return contains(SYMPTOM_KEYS, key);
    }

    public static boolean isConditionKey(String key) {
        return contains(CONDITION_KEYS, key);
    }

    private static boolean contains(String[] values, String key) {
        if (key == null) return false;
        for (String value : values) {
            if (value.equals(key)) return true;
        }
        return false;
    }
}
