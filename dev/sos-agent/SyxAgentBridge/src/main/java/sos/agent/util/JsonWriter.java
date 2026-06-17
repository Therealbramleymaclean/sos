package sos.agent.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Thin Jackson wrapper.
 *
 * We configure the mapper once and reuse it (ObjectMapper is thread-safe
 * after configuration). Pretty-printing is on by default — the JSON is
 * small enough that readability for debugging is worth the extra bytes.
 *
 * Jackson is shaded into the output jar by maven-shade-plugin, so there's
 * no dependency on whatever the game bundles.
 */
public final class JsonWriter {

    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();

        // Serialise public fields directly (TelemetrySnapshot uses public fields, not getters)
        MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY);
        MAPPER.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);

        // Human-readable output
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);

        // Don't fail on empty beans (defensive for future additions)
        MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    private JsonWriter() {}

    /**
     * Serialise any object to a JSON string.
     *
     * @throws com.fasterxml.jackson.core.JsonProcessingException if serialisation fails
     */
    public static String toJson(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
        return MAPPER.writeValueAsString(value);
    }
}
