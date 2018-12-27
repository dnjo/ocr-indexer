package net.dnjo;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.google.common.base.Supplier;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Update;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vc.inreach.aws.request.AWSSigner;
import vc.inreach.aws.request.AWSSigningRequestInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class Jest {
    private static final Logger logger = LoggerFactory.getLogger(Jest.class);
    private static final AWSSimpleSystemsManagement ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
    private static final AWSCredentialsProvider esCredentialsProvider = new AWSCredentialsProvider() {
        @Override
        public AWSCredentials getCredentials() {
            return new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    logger.info("Getting Elasticsearch IAM access key");
                    return ssmClient.getParameter(new GetParameterRequest().withName("ocrEsAccessKey")).getParameter().getValue();
                }

                @Override
                public String getAWSSecretKey() {
                    logger.info("Getting Elasticsearch IAM secret key");
                    return ssmClient.getParameter(new GetParameterRequest().withName("ocrEsSecretKey")).getParameter().getValue();
                }
            };
        }

        @Override
        public void refresh() {

        }
    };

    public static final JestClient CLIENT = buildJestClient();

    public static class FieldValue {
        private final String name;
        private final Object value;

        public FieldValue(final String name, final Object value) {
            this.name = name;
            this.value = value;
        }
    }

    public static Update buildUpsertAction(final String documentId, final FieldValue... fieldValues) throws IOException {
        final XContentBuilder docBuilder = jsonBuilder().startObject()
                .field("doc_as_upsert", true)
                .startObject("doc");
        for (final FieldValue field : fieldValues) {
            docBuilder.field(field.name, field.value);
        }
        final String updateContent = Strings.toString(docBuilder.endObject().endObject());
        return new Update.Builder(updateContent)
                .id(documentId)
                .index("results")
                .type("result")
                .build();
    }

    private static JestClient buildJestClient() {
        logger.info("Configuring Jest client");
        logger.info("Getting Elasticsearch URL parameter");
        final GetParameterResult ocrEsUrl = ssmClient.getParameter(new GetParameterRequest().withName("ocrEsUrl"));
        final Supplier<LocalDateTime> clock = () -> LocalDateTime.now(ZoneOffset.UTC);
        final AWSSigner awsSigner = new AWSSigner(esCredentialsProvider, System.getenv("AWS_REGION"), "es", clock);
        final AWSSigningRequestInterceptor requestInterceptor = new AWSSigningRequestInterceptor(awsSigner);
        final JestClientFactory factory = new JestClientFactory() {
            @Override
            protected HttpClientBuilder configureHttpClient(final HttpClientBuilder builder) {
                builder.setRetryHandler(new DefaultHttpRequestRetryHandler(5, true));
                builder.addInterceptorLast(requestInterceptor);
                return builder;
            }
        };
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder(ocrEsUrl.getParameter().getValue())
                .build());
        logger.info("Building Jest client");
        final JestClient client = factory.getObject();
        logger.info("Returning Jest client");
        return client;
    }
}
