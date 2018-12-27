package net.dnjo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.google.gson.JsonObject;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Get;
import net.dnjo.model.GatewayResponse;
import net.dnjo.Jest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class GetImageBlobHandler implements RequestHandler<Map, GatewayResponse> {
    private static final Logger logger = LoggerFactory.getLogger(GetImageBlobHandler.class);

    @Override
    public GatewayResponse handleRequest(final Map input, final Context context) {
        final Map<String, String> headers = new HashMap<>();

        try {
            final Map pathParameters = (Map) input.get("pathParameters");
            final String imageId = (String) pathParameters.get("image_id");
            final Get getAction = new Get.Builder("results", imageId).build();
            logger.info("Getting image document with ID {}", imageId);
            final DocumentResult imageDocument = Jest.CLIENT.execute(getAction);
            final JsonObject imageJson = imageDocument.getJsonObject().getAsJsonObject("_source");
            final String bucket = imageJson.get("s3Bucket").getAsString();
            final String key = imageJson.get("s3Key").getAsString();

            final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
            logger.info("Getting image object in bucket {} with key {}", bucket, key);
            final S3Object imageObject = s3.getObject(bucket, key);
            final String contentType = imageObject.getObjectMetadata().getContentType();

            headers.put("Content-Type", contentType);
            headers.put("X-Custom-Header", contentType);
            logger.info("Returning image with content type {}", contentType);
            final String base64Image = Base64.getEncoder().encodeToString(IOUtils.toByteArray(imageObject.getObjectContent()));
            return new GatewayResponse(base64Image, headers, 200, true);
        } catch (Exception e) {
            logger.error("Got an error while getting image", e);
            headers.put("Content-Type", "application/json");
            headers.put("X-Custom-Header", "application/json");
            return new GatewayResponse("{ \"success\": false }", headers, 500);
        }
    }
}
