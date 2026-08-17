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
}
