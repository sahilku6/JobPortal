package com.jobportal.auth.config;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class DotenvBootstrap {

    private static final String DOTENV_FILENAME = ".env";
    private static final int MAX_PARENT_SEARCH_DEPTH = 6;

    public static void load() {
        Path dotenvPath = findDotenvFile();
        if (dotenvPath == null) {
            return;
        }

        Map<String, String> values = parse(dotenvPath);
        values.forEach((key, value) -> {
            if (System.getProperty(key) == null && System.getenv(key) == null) {
                System.setProperty(key, value);
            }
        });
    }

    private Path findDotenvFile() {
        Path current = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath();
        for (int i = 0; i <= MAX_PARENT_SEARCH_DEPTH && current != null; i++) {
            Path candidate = current.resolve(DOTENV_FILENAME);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    private Map<String, String> parse(Path dotenvPath) {
        Map<String, String> values = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(dotenvPath, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int equalsIndex = line.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }

                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();
                values.put(key, stripMatchingQuotes(value));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read .env file from " + dotenvPath, ex);
        }
        return values;
    }

    private String stripMatchingQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}