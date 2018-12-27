package net.dnjo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import io.searchbox.core.Update;
import net.dnjo.Jest;
import net.dnjo.Jest.FieldValue;
import net.dnjo.model.GatewayResponse;
import net.dnjo.model.JsonGatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.dnjo.Jest.buildUpsertAction;

public class IndexImageOcrResultHandler implements RequestHandler<S3EventNotification, GatewayResponse> {
    private static final Logger logger = LoggerFactory.getLogger(IndexImageOcrResultHandler.class);

    public GatewayResponse handleRequest(final S3EventNotification input, final Context context) {
        logger.debug("Got S3 data event: '{}'", input);

        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try {
            final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

            for (final S3EventNotification.S3EventNotificationRecord record : input.getRecords()) {
                final S3EventNotification.S3Entity s3Input = record.getS3();
                logger.info("Getting object in bucket {} with key {}", s3Input.getBucket().getName(), s3Input.getObject().getKey());
                final String inputObject = s3.getObjectAsString(s3Input.getBucket().getName(), s3Input.getObject().getKey());
                logger.debug("Got object to index: {}", inputObject);

                final String documentId = parseDocumentIdFromKey(s3Input.getObject().getKey());
                final Update updateAction = buildUpsertAction(documentId, new FieldValue("ocrText", inputObject));
                logger.info("Updating document with ID {}", documentId);
                Jest.CLIENT.execute(updateAction);
            }

            final Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return new JsonGatewayResponse(result, headers, 200);
        } catch (Exception e) {
            logger.error("Got error when indexing OCR result", e);
            return new GatewayResponse("{}", headers, 500);
        }
    }

    private String parseDocumentIdFromKey(final String key) {
        final Pattern pattern = Pattern.compile("([^/]+)$");
        final Matcher matcher = pattern.matcher(key);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not parse document ID from key");
        }
        return matcher.group(1);
    }
}
