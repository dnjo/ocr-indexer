package net.dnjo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.core.Update;
import net.dnjo.GatewayResponse;
import net.dnjo.Jest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DeleteImageHandler implements RequestHandler<Map, GatewayResponse> {
    private static final Logger logger = LoggerFactory.getLogger(DeleteImageHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public GatewayResponse handleRequest(Map input, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        Map<String, Object> result = new HashMap<>();
        boolean success = false;

        try {
            Map pathParameters = (Map) input.get("pathParameters");
            String imageId = (String) pathParameters.get("image_id");
            Update upsertAction = Jest.buildUpsertAction(imageId, new Jest.FieldValue("present", false));
            logger.info("Delete image with ID {}", imageId);
            Jest.CLIENT.execute(upsertAction);
            success = true;
        } catch (Exception ignored) { }

        try {
            result.put("success", success);
            return new GatewayResponse(objectMapper.writeValueAsString(result), headers, success ? 200 : 500);
        } catch (JsonProcessingException e) {
            return new GatewayResponse("Failed to serialize result object", headers, 500);
        }
    }
}
