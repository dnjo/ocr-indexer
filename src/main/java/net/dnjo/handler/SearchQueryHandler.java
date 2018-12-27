package net.dnjo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import net.dnjo.Jest;
import net.dnjo.model.GatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SearchQueryHandler implements RequestHandler<Map, GatewayResponse> {
    private static final Logger logger = LoggerFactory.getLogger(SearchQueryHandler.class);

    @Override
    public GatewayResponse handleRequest(final Map input, final Context context) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try {
            final Search searchAction = new Search.Builder((String) input.get("body"))
                    .addIndex("results")
                    .build();
            logger.info("Running query");
            final SearchResult result = Jest.CLIENT.execute(searchAction);
            return new GatewayResponse(result.getJsonString(), headers, 200);
        } catch (Exception e) {
            logger.error("Got error while running query", e);
            return new GatewayResponse("{}", headers, 500);
        }
    }
}
