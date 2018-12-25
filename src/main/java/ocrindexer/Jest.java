package ocrindexer;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Update;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class Jest {
    static JestClient buildJestClient() {
        AWSSimpleSystemsManagement ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
        GetParameterResult ocrEsUrl = ssmClient.getParameter(new GetParameterRequest().withName("ocrEsUrl"));
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder(ocrEsUrl.getParameter().getValue())
                .build());
        return factory.getObject();
    }

    static class FieldValue {
        private final String name;
        private final Object value;

        FieldValue(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }

    static Update buildUpsertAction(String documentId, FieldValue... fieldValues) throws IOException {
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
}
