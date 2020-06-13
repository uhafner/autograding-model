package edu.hm.hafner.grading;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Facade for Jackson that does wrap an exception into a {@link RuntimeException}.
 *
 * @author Ullrich Hafner
 */
public class JacksonFacade {
    private final ObjectMapper mapper;

    /**
     * Creates a new instance of {@link JacksonFacade}.
     */
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

    /**
     * Creates a bean from the specified JSON string.
     *
     * @param json
     *         the bean properties given as JSON string
     * @param type
     *         the type of the bean
     * @param <T>
     *         type of the bean
     *
     * @return the JSON representation (as a String)
     */
    public <T> T fromJson(final String json, final Class<T> type) {
        try {
            return mapper.readValue(json, type);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    String.format("Can't convert JSON '%s' to bean", json), exception);
        }
    }

    /**
     * Creates a bean from the specified JSON node.
     *
     * @param jsonNode
     *         the JSON node providing all properties of the bean
     * @param type
     *         the type of the bean
     * @param <T>
     *         type of the bean
     *
     * @return the JSON representation (as a String)
     */
    public <T> T fromJson(final JsonNode jsonNode, final Class<T> type) {
        try {
            return mapper.treeToValue(jsonNode, type);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    String.format("Can't convert JSON '%s' to bean", jsonNode.asText()), exception);
        }
    }
}
