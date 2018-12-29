package net.dnjo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.searchbox.client.JestResult;
import net.dnjo.Jest;
import net.dnjo.RawSearch;
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
            final Map pathParameters = (Map) input.get("pathParameters");
            final String method = (String) pathParameters.get("method");
            final String indices = (String) pathParameters.get("indices");
            logger.info("Building search queries for index/indices {}", indices);
            final RawSearch search = new RawSearch(method, "POST", (String) input.get("body"), indices);
            logger.info("Running query");
            final JestResult result = Jest.CLIENT.execute(search);
            return new GatewayResponse(result.getJsonString(), headers, 200);
        } catch (Exception e) {
            logger.error("Got error while running query", e);
            return new GatewayResponse("{}", headers, 500);
        }
    }
}
