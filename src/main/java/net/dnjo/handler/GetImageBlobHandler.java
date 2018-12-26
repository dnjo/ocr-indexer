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
    public GatewayResponse handleRequest(Map input, Context context) {
        Map<String, String> headers = new HashMap<>();

        try {
            Map pathParameters = (Map) input.get("pathParameters");
            String imageId = (String) pathParameters.get("image_id");
            Get getAction = new Get.Builder("results", imageId).build();
            logger.info("Getting image document with ID {}", imageId);
            DocumentResult imageDocument = Jest.CLIENT.execute(getAction);
            JsonObject imageJson = imageDocument.getJsonObject().getAsJsonObject("_source");
            String bucket = imageJson.get("s3Bucket").getAsString();
            String key = imageJson.get("s3Key").getAsString();

            AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
            logger.info("Getting image object in bucket {} with key {}", bucket, key);
            S3Object imageObject = s3.getObject(bucket, key);
            String contentType = imageObject.getObjectMetadata().getContentType();

            headers.put("Content-Type", contentType);
            headers.put("X-Custom-Header", contentType);
            logger.info("Returning image with content type {}", contentType);
            return new GatewayResponse(Base64.getEncoder().encodeToString(IOUtils.toByteArray(imageObject.getObjectContent())), headers, 200, true);
        } catch (Exception e) {
            logger.error("Got an error while getting image", e);
            headers.put("Content-Type", "application/json");
            headers.put("X-Custom-Header", "application/json");
            return new GatewayResponse("{ \"success\": false }", headers, 500);
        }
    }
}
