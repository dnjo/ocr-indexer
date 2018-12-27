package net.dnjo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.searchbox.core.Update;
import net.dnjo.Jest;
import net.dnjo.Jest.FieldValue;
import net.dnjo.model.GatewayResponse;
import net.dnjo.model.JsonGatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static net.dnjo.Jest.buildUpsertAction;

public class DeleteImageHandler implements RequestHandler<Map, GatewayResponse> {
    private static final Logger logger = LoggerFactory.getLogger(DeleteImageHandler.class);

    @Override
    public GatewayResponse handleRequest(final Map input, final Context context) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try {
            final Map pathParameters = (Map) input.get("pathParameters");
            final String imageId = (String) pathParameters.get("image_id");
            final Update upsertAction = buildUpsertAction(imageId, new FieldValue("present", false));
            logger.info("Delete image with ID {}", imageId);
            Jest.CLIENT.execute(upsertAction);
            final Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return new JsonGatewayResponse(result, headers, 200);
        } catch (Exception e) {
            logger.error("Got error when deleting image", e);
            return new GatewayResponse("{}", headers, 500);
        }
    }
}
