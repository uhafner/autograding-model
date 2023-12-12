package edu.hm.hafner.grading;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Facade for Jackson that does wrap an exception into a {@link RuntimeException}.
 *
 * @author Ullrich Hafner
 */
class JacksonFacade {
    static final JacksonFacade JACKSON_FACADE = new JacksonFacade();

    static JacksonFacade get() {
        return JACKSON_FACADE;
    }

    private final ObjectMapper mapper;

    /**
     * Creates a new instance of {@link JacksonFacade}.
     */
    JacksonFacade() {
        mapper = JsonMapper.builder().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true).build()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.INDENT_OUTPUT, true);
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

    /**
     * Creates a tree of JSON objects from the specified JSON string.
     *
     * @param json
     *         the JSON string
     *
     * @return the JSON representation (as a String)
     */
    public JsonNode readJson(final String json) {
        try {
            return mapper.readTree(json);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    String.format("Can't convert JSON '%s' to JSON node", json), exception);
        }
    }

    /**
     * Returns the text value of the specified JSON property.
     *
     * @param json
     *         the JSON object to extract the property value from
     * @param property
     *         the name of the propety
     * @param defaultValue
     *         the default value if the property is undefined or invalid
     *
     * @return the value of the property
     */
    public String getStringValue(final String json, final String property, final String defaultValue) {
        try {
            ObjectNode node = mapper.readValue(json, ObjectNode.class);
            JsonNode typeNode = node.get(property);
            if (typeNode != null) {
                return typeNode.asText(defaultValue);
            }
        }
        catch (JsonProcessingException exception) {
            // ignore
        }

        return defaultValue;
    }
}
