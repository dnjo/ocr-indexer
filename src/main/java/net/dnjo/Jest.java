package net.dnjo;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Get;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singleton;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public class Jest {
    private static final String INDEX = "documents";
    private static final String TYPE = "document";

    private static final Logger logger = LoggerFactory.getLogger(Jest.class);
    private static final AWSSimpleSystemsManagement ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();

    public static final JestClient CLIENT = buildJestClient();

    public static class FieldValue {
        private final String name;
        private final Object value;

        public FieldValue(final String name, final Object value) {
            this.name = name;
            this.value = value;
        }
    }

    public static Update buildUpsertAction(final String documentId, final String userId, final FieldValue... fieldValues) throws IOException {
        final XContentBuilder docBuilder = jsonBuilder().startObject()
                .field("doc_as_upsert", true)
                .startObject("doc");
        for (final FieldValue field : fieldValues) {
            docBuilder.field(field.name, field.value);
        }
        final String updateContent = Strings.toString(docBuilder.endObject().endObject());
        return new Update.Builder(updateContent)
                .id(documentId)
                .index(indexName(userId))
                .type(TYPE)
                .build();
    }

    public static Map findById(final String id) throws IOException {
        final BoolQueryBuilder query = QueryBuilders.boolQuery();
        query.should(termQuery("_id", id));
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(query);
        final Search search = new Search.Builder(searchSourceBuilder.toString()).addIndex(indexName(null)).build();
        final SearchResult result = CLIENT.execute(search);
        final List<SearchResult.Hit<Map, Void>> hits = result.getHits(Map.class);
        return hits.get(0).source;
    }

    public static Get buildGetAction(final String documentId, final String userId) {
        return new Get.Builder(indexName(userId), documentId).build();
    }

    public static RawSearch buildRawSearchAction(final String method,
                                                 final String query,
                                                 final String userId) {
        return new RawSearch(method, "POST", query, indexName(userId));
    }

    private static String indexName(final String userId) {
        final String suffix = userId != null ? userId : "*";
        return String.format("%s-%s", INDEX, suffix);
    }

    private static JestClient buildJestClient() {
        logger.info("Configuring Jest client");
        logger.info("Getting Elasticsearch URL parameter");
        final GetParameterResult ocrEsUrl = ssmClient.getParameter(new GetParameterRequest().withName("ocrEsUrl"));
        final GetParameterResult ocrEsAuth = ssmClient.getParameter(new GetParameterRequest().withName("ocrEsAuth"));
        final JestClientFactory factory = new JestClientFactory() {
            @Override
            protected HttpClientBuilder configureHttpClient(final HttpClientBuilder builder) {
                builder.setRetryHandler(new DefaultHttpRequestRetryHandler(5, true));
                final BasicHeader authorization = new BasicHeader("Authorization", ocrEsAuth.getParameter().getValue());
                builder.setDefaultHeaders(singleton(authorization));
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
