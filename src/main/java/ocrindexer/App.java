package ocrindexer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Update;
import org.elasticsearch.common.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<S3EventNotification, Object> {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public Object handleRequest(final S3EventNotification input, final Context context) {
        logger.debug("Got S3 data event: '{}'", input);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try {
            JestClient client = buildJestClient();
            AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

            for (S3EventNotification.S3EventNotificationRecord record : input.getRecords()) {
                S3EventNotification.S3Entity s3Input = record.getS3();
                logger.info("Getting object in bucket '{}' with key '{}'", s3Input.getBucket().getName(), s3Input.getObject().getKey());
                String inputObject = s3.getObjectAsString(s3Input.getBucket().getName(), s3Input.getObject().getKey());
                logger.debug("Got object to index: '{}'", inputObject);

                String updateContent = Strings.toString(jsonBuilder().startObject()
                        .field("doc_as_upsert", true)
                        .startObject("doc")
                        .field("ocrText", inputObject)
                        .endObject()
                        .endObject());
                String documentId = parseDocumentIdFromKey(s3Input.getObject().getKey());
                Update update = new Update.Builder(updateContent)
                        .id(documentId)
                        .index("results")
                        .type("result")
                        .build();

                logger.info("Updating document with ID '{}'", documentId);
                client.execute(update);
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

    private JestClient buildJestClient() {
        AWSSimpleSystemsManagement ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
        GetParameterResult ocrEsUrl = ssmClient.getParameter(new GetParameterRequest().withName("ocrEsUrl"));
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder(ocrEsUrl.getParameter().getValue())
                .build());
        return factory.getObject();
    }
}
