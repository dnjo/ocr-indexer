package net.dnjo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Get;
import net.dnjo.Jest;
import net.dnjo.model.GatewayResponse;
import net.dnjo.model.Image;
import net.dnjo.model.JsonGatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class GetImageHandler implements RequestHandler<Map, GatewayResponse> {
    private static final Logger logger = LoggerFactory.getLogger(GetImageHandler.class);

    @Override
    public GatewayResponse handleRequest(Map input, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try {
            Map pathParameters = (Map) input.get("pathParameters");
            String imageId = (String) pathParameters.get("image_id");
            Get getAction = new Get.Builder("results", imageId).build();
            logger.info("Getting image document with ID {}", imageId);
            DocumentResult imageDocument = Jest.CLIENT.execute(getAction);
            JsonObject imageJson = imageDocument.getJsonObject().getAsJsonObject("_source");
            Image image = new Image(
                    imageId,
                    LocalDateTime.parse(imageJson.get("createdAt").getAsString()),
                    getJsonValue(imageJson, "text", JsonElement::getAsString),
                    getJsonValue(imageJson, "ocrText", JsonElement::getAsString),
                    imageJson.get("type").getAsString());
            return new JsonGatewayResponse(image, headers, 200);
        } catch (Exception e) {
            logger.error("Got an error while getting image document", e);
            return new GatewayResponse("{}", headers, 500);
        }
    }

    private <T> T getJsonValue(JsonObject object, String memberName, Function<JsonElement, T> elementValueProvider) {
        return object.get(memberName) != null ? elementValueProvider.apply(object.get(memberName)) : null;
    }
}
