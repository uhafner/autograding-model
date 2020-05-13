package edu.hm.hafner.grading;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Facade for Jackson that does wrap an exception into a {@link RuntimeException}.
 *
 * @author Ullrich Hafner
 */
public class JacksonFacade {
    private final ObjectMapper mapper;

    public JacksonFacade() {
        mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Creates a JSON representation of the specified bean using Jackson data binding.
     *
     * @param bean
     *         the bean to convert
     *
     * @return the JSON representation (as a String)
     */
    public String toJson(final Object bean) {
        try {
            return mapper.writeValueAsString(bean);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    String.format("Can't convert %s to JSON object", bean), exception);
        }
    }

    public <T> T fromJson(final String json, final Class<T> type) {
        try {
            return mapper.readValue(json, type);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    String.format("Can't convert JSON '%s' to bean", json), exception);
        }
    }
}
