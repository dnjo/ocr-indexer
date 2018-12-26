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
import vc.inreach.aws.request.AWSSigner;
import vc.inreach.aws.request.AWSSigningRequestInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class Jest {
    public static final JestClient CLIENT = buildJestClient();

    public static class FieldValue {
        private final String name;
        private final Object value;

        public FieldValue(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }

    public static Update buildUpsertAction(String documentId, FieldValue... fieldValues) throws IOException {
        XContentBuilder docBuilder = jsonBuilder().startObject()
                .field("doc_as_upsert", true)
                .startObject("doc");
        for (FieldValue field : fieldValues) {
            docBuilder.field(field.name, field.value);
        }
        String updateContent = Strings.toString(docBuilder.endObject().endObject());
        return new Update.Builder(updateContent)
                .id(documentId)
                .index("results")
                .type("result")
                .build();
    }

    private static JestClient buildJestClient() {
        AWSSimpleSystemsManagement ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
        GetParameterResult ocrEsUrl = ssmClient.getParameter(new GetParameterRequest().withName("ocrEsUrl"));
        final Supplier<LocalDateTime> clock = () -> LocalDateTime.now(ZoneOffset.UTC);
        final AWSSigner awsSigner = new AWSSigner(new AWSCredentialsProvider() {
            @Override
            public AWSCredentials getCredentials() {
                return new AWSCredentials() {
                    @Override
                    public String getAWSAccessKeyId() {
                        return ssmClient.getParameter(new GetParameterRequest().withName("ocrEsAccessKey")).getParameter().getValue();
                    }

                    @Override
                    public String getAWSSecretKey() {
                        return ssmClient.getParameter(new GetParameterRequest().withName("ocrEsSecretKey")).getParameter().getValue();
                    }
                };
            }

            @Override
            public void refresh() {

            }
        }, System.getenv("AWS_REGION"), "es", clock);

        AWSSigningRequestInterceptor requestInterceptor = new AWSSigningRequestInterceptor(awsSigner);
        JestClientFactory factory = new JestClientFactory() {
            @Override
            protected HttpClientBuilder configureHttpClient(HttpClientBuilder builder) {
                builder.setRetryHandler(new DefaultHttpRequestRetryHandler(5, true));
                builder.addInterceptorLast(requestInterceptor);
                return builder;
            }
        };
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder(ocrEsUrl.getParameter().getValue())
                .build());
        return factory.getObject();
    }
}
