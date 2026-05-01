package com.example.tictac.session.integration.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.core.io.ClassPathResource;

/**
 * Loads JSON fixtures from {@code src/test/resources} and applies optional variable substitution.
 */
public final class JsonFixtures {

	private static final String FIXTURE_ROOT = "/integration/";

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
			.registerModule(new JavaTimeModule())
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	private JsonFixtures() {
	}

	public static String getJsonFromFilePath(String path) {
		try {
			return IOUtils.toString(
					new ClassPathResource(normalize(path)).getInputStream(),
					StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to read fixture: " + path, ex);
		}
	}

	public static String getJsonFromFilePath(String path, Map<String, String> variables) {
		return StringSubstitutor.replace(getJsonFromFilePath(path), variables);
	}

	public static <T> T readObjectFromFilePath(String path,
																						 Class<T> type,
																						 Map<String, String> variables) {
		try {
			return MAPPER.readValue(getJsonFromFilePath(path, variables), type);
		} catch (IOException ex) {
			throw new UncheckedIOException(
					"Failed to deserialize fixture " + path + " into " + type.getName(), ex);
		}
	}

	private static String normalize(String path) {
		if (path == null || path.isBlank()) {
			throw new IllegalArgumentException("Fixture path must be non-blank");
		}
		return path.startsWith("/") ? path : FIXTURE_ROOT + path;
	}
}
