package com.airpollution.survey.service;

import java.util.List;
import java.util.stream.Collectors;

final class TextFormat {
    private TextFormat() {
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static String bool(Boolean value) {
        if (value == null) return "";
        return value ? "Yes" : "No";
    }

    static String label(String value) {
        return value == null ? "" : value.replace('_', ' ');
    }

    static String labelList(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        return values.stream().map(TextFormat::label).collect(Collectors.joining(", "));
    }
}
