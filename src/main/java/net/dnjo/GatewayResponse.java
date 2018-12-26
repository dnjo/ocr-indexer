package net.dnjo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * POJO containing response object for API Gateway.
 */
public class GatewayResponse {

    private final String body;
    private final Map<String, String> headers;
    private final int statusCode;
    private final boolean isBase64Encoded;

    public GatewayResponse(final String body, final Map<String, String> headers, final int statusCode) {
        this(body, headers, statusCode, false);
    }

    public GatewayResponse(final String body, final Map<String, String> headers, final int statusCode, final boolean isBase64Encoded) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
        this.isBase64Encoded = isBase64Encoded;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isBase64Encoded() {
        return isBase64Encoded;
    }
}
