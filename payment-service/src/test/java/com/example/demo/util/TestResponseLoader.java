package com.example.demo.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for loading expected test responses from static JSON files
 */
public class TestResponseLoader {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Load expected response from JSON file
     */
    public static <T> T loadExpectedResponse(String filename, Class<T> responseType) throws IOException {
        ClassPathResource resource = new ClassPathResource("test-responses/" + filename);
        String jsonContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return objectMapper.readValue(jsonContent, responseType);
    }

    /**
     * Load expected response as JSON string
     */
    public static String loadExpectedResponseAsString(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource("test-responses/" + filename);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Load expected response and convert to Map
     */
    public static java.util.Map<String, Object> loadExpectedResponseAsMap(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource("test-responses/" + filename);
        String jsonContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return objectMapper.readValue(jsonContent, java.util.Map.class);
    }
}
