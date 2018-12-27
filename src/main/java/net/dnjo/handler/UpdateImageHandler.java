package net.dnjo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dnjo.dao.ImageDao;
import net.dnjo.model.GatewayResponse;
import net.dnjo.model.Image;
import net.dnjo.model.ImageUpdate;
import net.dnjo.model.JsonGatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class UpdateImageHandler implements RequestHandler<Map, GatewayResponse> {
    private static final Logger logger = LoggerFactory.getLogger(UpdateImageHandler.class);

    private final ImageDao imageDao = new ImageDao();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public GatewayResponse handleRequest(final Map input, final Context context) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try {
            final Map pathParameters = (Map) input.get("pathParameters");
            final String imageId = (String) pathParameters.get("image_id");
            final Map body = objectMapper.readValue((String) input.get("body"), HashMap.class);
            final String text = (String) body.get("text");
            final String ocrText = (String) body.get("ocrText");
            logger.info("Updating image with ID {}", imageId);
            final Image image = imageDao.updateImage(imageId, new ImageUpdate(text, ocrText));
            return new JsonGatewayResponse(image, headers, 200);
        } catch (Exception e) {
            logger.error("Got an error while updating image", e);
            return new GatewayResponse("{}", headers, 500);
        }
    }
}
