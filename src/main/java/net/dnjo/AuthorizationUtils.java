package net.dnjo;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class AuthorizationUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String parseSubjectFromJwt(final Map headers) {
        final String encodedJwt = (String) headers.get("Authorization");
        final String[] jwtParts = encodedJwt.split("\\.");
        final String encodedClaims = jwtParts[1];
        final String claims = new String(Base64.getDecoder().decode(encodedClaims), StandardCharsets.UTF_8);
        try {
            return (String) objectMapper.readValue(claims, Map.class).get("sub");
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not parse JWT claims", e);
        }
    }
}
