package ocrindexer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.core.Update;
import ocrindexer.Jest.FieldValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ocrindexer.Jest.buildJestClient;
import static ocrindexer.Jest.buildUpsertAction;

public class Uploader implements RequestHandler<Map, Object> {
    private static final Logger logger = LoggerFactory.getLogger(Uploader.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object handleRequest(Map input, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try {
            Map inputHeaders = (Map) input.get("headers");
            String contentType = (String) inputHeaders.get("Content-Type");
            String contentLanguage = (String) inputHeaders.get("Content-Language");
            logger.info("Image content type: {}", contentType);
            logger.info("Image language: {}", contentLanguage);
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(contentType);
            objectMetadata.setContentLanguage(contentLanguage);

            byte[] decodedBody = Base64.getDecoder().decode((String) input.get("body"));
            logger.info("Image size: {}", decodedBody.length);
            objectMetadata.setContentLength(decodedBody.length);

            LocalDateTime now = LocalDateTime.now();
            String id = UUID.randomUUID().toString();
            AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
            String bucket = System.getenv("S3_BUCKET");
            String key = formatObjectKey(id, now);
            logger.info("Uploading image with ID {} to bucket {} with key {}", id, bucket, key);
            s3.putObject(bucket, key, new ByteArrayInputStream(decodedBody), objectMetadata);

            Update updateAction = buildUpsertAction(
                    id,
                    new FieldValue("createdAt", now),
                    new FieldValue("language", contentLanguage),
                    new FieldValue("type", contentType),
                    new FieldValue("s3Bucket", bucket),
                    new FieldValue("s3Key", key));
            JestClient client = buildJestClient();
            logger.info("Indexing document with ID {}", id);
            client.execute(updateAction);

            Map<String, String> result = new HashMap<>();
            result.put("id", id);
            result.put("bucket", bucket);
            result.put("key", key);
            return new GatewayResponse(objectMapper.writeValueAsString(result), headers, 200);
        } catch (Exception e) {
            logger.error("Got an error while uploading file", e);
            return new GatewayResponse("{}", headers, 500);
        }
    }

    private String formatObjectKey(String name, LocalDateTime timestamp) {
        String prefix = System.getenv("S3_PREFIX");
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