package net.dnjo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import net.dnjo.dao.ImageDao;
import net.dnjo.model.GatewayResponse;
import net.dnjo.model.Image;
import net.dnjo.model.JsonGatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class GetImageHandler implements RequestHandler<Map, GatewayResponse> {
    private static final Logger logger = LoggerFactory.getLogger(GetImageHandler.class);

    private final ImageDao imageDao = new ImageDao();

    @Override
    public GatewayResponse handleRequest(final Map input, final Context context) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try {
            final Map pathParameters = (Map) input.get("pathParameters");
            final String imageId = (String) pathParameters.get("image_id");
            logger.info("Getting image document with ID {}", imageId);
            final Image image = imageDao.findImage(imageId);
            return new JsonGatewayResponse(image, headers, 200);
        } catch (Exception e) {
            logger.error("Got an error while getting image document", e);
            return new GatewayResponse("{}", headers, 500);
        }
    }
}
