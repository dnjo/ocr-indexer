package ocrindexer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import io.searchbox.core.Update;
import ocrindexer.Jest.FieldValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ocrindexer.Jest.buildUpsertAction;

/**
 * Handler for requests to Lambda function.
 */
public class OcrIndexer implements RequestHandler<S3EventNotification, Object> {
    private static final Logger logger = LoggerFactory.getLogger(OcrIndexer.class);

    public Object handleRequest(final S3EventNotification input, final Context context) {
        logger.debug("Got S3 data event: '{}'", input);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try {
            AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

            for (S3EventNotification.S3EventNotificationRecord record : input.getRecords()) {
                S3EventNotification.S3Entity s3Input = record.getS3();
                logger.info("Getting object in bucket '{}' with key '{}'", s3Input.getBucket().getName(), s3Input.getObject().getKey());
                String inputObject = s3.getObjectAsString(s3Input.getBucket().getName(), s3Input.getObject().getKey());
                logger.debug("Got object to index: '{}'", inputObject);

                String documentId = parseDocumentIdFromKey(s3Input.getObject().getKey());
                Update updateAction = buildUpsertAction(documentId, new FieldValue("ocrText", inputObject));
                logger.info("Updating document with ID '{}'", documentId);
                Jest.CLIENT.execute(updateAction);
            }

            logger.info("Finished indexing");
            String output = "{ \"success\": \"true\" }";
            return new GatewayResponse(output, headers, 200);
        } catch (Exception e) {
            logger.error("Got an error while indexing", e);
            return new GatewayResponse("{}", headers, 500);
        }
    }

    private String parseDocumentIdFromKey(String key) {
        Pattern pattern = Pattern.compile("([^/]+)$");
        Matcher matcher = pattern.matcher(key);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not parse document ID from key");
        }
        return matcher.group(1);
    }
}
