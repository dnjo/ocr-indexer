package net.dnjo;

import com.google.gson.Gson;
import io.searchbox.action.AbstractAction;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.ElasticsearchVersion;

public class RawSearch extends AbstractAction<JestResult> {
    private final String searchMethod;
    private final String restMethodName;
    private final String query;

    public RawSearch(final String searchMethod, final String restMethodName, final String query, final String indices) {
        this.searchMethod = searchMethod;
        this.restMethodName = restMethodName;
        this.query = query;
        this.indexName = indices;
    }

    @Override
    public String getRestMethodName() {
        return restMethodName;
    }

    @Override
    public JestResult createNewElasticSearchResult(final String responseBody, final int statusCode, final String reasonPhrase, final Gson gson) {
        return createNewElasticSearchResult(new JestResult(gson), responseBody, statusCode, reasonPhrase, gson);
    }

    @Override
    protected String buildURI(final ElasticsearchVersion elasticsearchVersion) {
        return super.buildURI(elasticsearchVersion) + "/" + searchMethod;
    }

    @Override
    public String getData(final Gson gson) {
        return query;
    }
}
