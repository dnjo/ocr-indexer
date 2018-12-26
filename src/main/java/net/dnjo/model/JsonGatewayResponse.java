package net.dnjo.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

public class JsonGatewayResponse extends GatewayResponse {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    public JsonGatewayResponse(Object body, Map<String, String> headers, int statusCode) throws JsonProcessingException {
        super(objectMapper.writeValueAsString(body), headers, statusCode);
    }
}
