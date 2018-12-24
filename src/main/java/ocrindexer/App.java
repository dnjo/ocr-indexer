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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<S3EventNotification, Object> {
    public Object handleRequest(final S3EventNotification input, final Context context) {
        AWSSimpleSystemsManagement ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
        GetParameterResult ocrEsUrl = ssmClient.getParameter(new GetParameterRequest().withName("ocrEsUrl"));

        System.out.println("Got here 1");
        System.out.println(input.toJson());
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder(ocrEsUrl.getParameter().getValue())
                .build());
        JestClient client = factory.getObject();

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try {
            for (S3EventNotification.S3EventNotificationRecord record : input.getRecords()) {
                S3EventNotification.S3Entity s3Input = record.getS3();
                AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
                String inputObject = s3.getObjectAsString(s3Input.getBucket().getName(), s3Input.getObject().getKey());

                System.out.println(String.format("Got input object: '%s'", inputObject));
                System.out.println("Got here 2");

                Pattern pattern = Pattern.compile("([^/]+)$");
                Matcher matcher = pattern.matcher(s3Input.getObject().getKey());
                matcher.find();
                String updateContent = Strings.toString(jsonBuilder().startObject()
                        .field("doc_as_upsert", true)
                        .startObject("doc")
                        .field("ocrText", inputObject)
                        .endObject()
                        .endObject());
                Update update = new Update.Builder(updateContent)
                        .id(matcher.group(1))
                        .index("results")
                        .type("result")
                        .build();
                client.execute(update);

                System.out.println("Got here 3");
            }

            String output = "{ \"success\": \"true\" }";
            return new GatewayResponse(output, headers, 200);
        } catch (Exception e) {
            System.out.println("Got error");
            System.out.println(e);
            return new GatewayResponse("{}", headers, 500);
        }
    }
}
