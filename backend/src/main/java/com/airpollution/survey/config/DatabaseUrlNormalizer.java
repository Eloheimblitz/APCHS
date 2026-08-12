package com.airpollution.survey.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class DatabaseUrlNormalizer {
    private DatabaseUrlNormalizer() {
    }

    public static void apply() {
        String databaseUrl = firstNonBlank(System.getenv("DATABASE_URL"), System.getenv("DATABASE_PUBLIC_URL"));
        if (databaseUrl == null || databaseUrl.startsWith("jdbc:")) {
            return;
        }
        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            return;
        }

        URI uri = URI.create(databaseUrl);
        String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + "/" + database;
        System.setProperty("spring.datasource.url", jdbcUrl);

        if (isBlank(System.getenv("DB_USERNAME")) || isBlank(System.getenv("DB_PASSWORD"))) {
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                if (parts.length > 0 && !parts[0].isBlank()) {
                    System.setProperty("spring.datasource.username", decode(parts[0]));
                }
                if (parts.length > 1 && !parts[1].isBlank()) {
                    System.setProperty("spring.datasource.password", decode(parts[1]));
                }
            }
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first;
        }
        return isBlank(second) ? null : second;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
