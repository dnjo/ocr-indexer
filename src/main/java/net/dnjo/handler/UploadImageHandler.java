package net.dnjo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import io.searchbox.core.Update;
import net.dnjo.Jest;
import net.dnjo.Jest.FieldValue;
import net.dnjo.model.GatewayResponse;
import net.dnjo.model.JsonGatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.dnjo.Jest.buildUpsertAction;

public class UploadImageHandler implements RequestHandler<Map, GatewayResponse> {
    private static final Logger logger = LoggerFactory.getLogger(UploadImageHandler.class);

    @Override
    public GatewayResponse handleRequest(final Map input, final Context context) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try {
            final Map inputHeaders = (Map) input.get("headers");
            final String contentType = (String) inputHeaders.get("Content-Type");
            final String contentLanguage = (String) inputHeaders.get("Content-Language");
            logger.info("Image content type: {}", contentType);
            logger.info("Image language: {}", contentLanguage);
            final ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(contentType);
            objectMetadata.setContentLanguage(contentLanguage);

            final byte[] decodedBody = Base64.getDecoder().decode((String) input.get("body"));
            logger.info("Image size: {}", decodedBody.length);
            objectMetadata.setContentLength(decodedBody.length);

            final LocalDateTime now = LocalDateTime.now();
            final String id = UUID.randomUUID().toString();
            final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
            final String bucket = System.getenv("S3_BUCKET");
            final String key = formatObjectKey(id, now);
            logger.info("Uploading image with ID {} to bucket {} with key {}", id, bucket, key);
            s3.putObject(bucket, key, new ByteArrayInputStream(decodedBody), objectMetadata);

            final Update updateAction = buildUpsertAction(
                    id,
                    new FieldValue("createdAt", now),
                    new FieldValue("language", contentLanguage),
                    new FieldValue("type", contentType),
                    new FieldValue("s3Bucket", bucket),
                    new FieldValue("s3Key", key));
            logger.info("Indexing document with ID {}", id);
            Jest.CLIENT.execute(updateAction);

            final Map<String, Object> result = new HashMap<>();
            result.put("id", id);
            result.put("bucket", bucket);
            result.put("key", key);
            return new JsonGatewayResponse(result, headers, 200);
        } catch (Exception e) {
            logger.error("Got error when uploading image", e);
            return new GatewayResponse("{}", headers, 500);
        }
    }

    private String formatObjectKey(final String name, final LocalDateTime timestamp) {
        final String prefix = System.getenv("S3_PREFIX");
        return String.format(
                "%s/%d/%d/%d/%d/%s",
                prefix,
                timestamp.getYear(),
                timestamp.getMonthValue(),
                timestamp.getDayOfMonth(),
                timestamp.getHour(),
                name);
    }
}