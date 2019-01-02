package net.dnjo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import io.searchbox.core.Update;
import net.dnjo.Jest;
import net.dnjo.Jest.FieldValue;
import net.dnjo.model.GatewayResponse;
import net.dnjo.model.JsonGatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.dnjo.AuthorizationUtils.parseSubjectFromJwt;
import static net.dnjo.Jest.buildUpsertAction;
import static net.dnjo.MapUtils.buildCaseInsensitiveMap;

public class UploadImageHandler implements RequestHandler<Map, GatewayResponse> {
    private static final Logger logger = LoggerFactory.getLogger(UploadImageHandler.class);

    private static class BodyDecoder {
        private final String contentType;
        private final byte[] decodedBody;

        private BodyDecoder(final Map headers, final String body) {
            final String requestContentType = (String) headers.get("Content-Type");
            final String encodedText;
            if (requestContentType.equals("application/json")) {
                final String[] bodyParts = body.split(",");
                final String contentMetadata = bodyParts[0];
                contentType = parseContentType(contentMetadata);
                encodedText = bodyParts[1];
            } else {
                contentType = requestContentType;
                encodedText = body;
            }
            decodedBody = decodeBody(encodedText);
        }

        private String parseContentType(final String contentMetadata) {
            final Pattern pattern = Pattern.compile("data:(.+);");
            final Matcher matcher = pattern.matcher(contentMetadata);
            if (!matcher.find()) {
                throw new IllegalArgumentException("Could not parse content type from string " + contentMetadata);
            }
            return matcher.group(1);
        }

        private byte[] decodeBody(final String body) {
            return Base64.getMimeDecoder().decode(body);
        }
    }

    @Override
    public GatewayResponse handleRequest(final Map input, final Context context) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try {
            final Map inputHeaders = buildCaseInsensitiveMap((Map) input.get("headers"));
            final String userId = parseSubjectFromJwt(inputHeaders);
            logger.info("Input headers: {}", inputHeaders);
            final BodyDecoder bodyDecoder = new BodyDecoder(inputHeaders, (String) input.get("body"));
            final String contentLanguage = (String) inputHeaders.get("Content-Language");
            logger.info("Image content type: {}", bodyDecoder.contentType);
            logger.info("Image language: {}", contentLanguage);
            final ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(bodyDecoder.contentType);
            objectMetadata.setContentLanguage(contentLanguage);

            logger.info("Image size: {}", bodyDecoder.decodedBody.length);
            objectMetadata.setContentLength(bodyDecoder.decodedBody.length);

            final LocalDateTime now = LocalDateTime.now();
            final String id = UUID.randomUUID().toString();
            final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
            final String bucket = System.getenv("S3_BUCKET");
            final String key = formatObjectKey(id, userId, now);
            logger.info("Uploading image with ID {} to bucket {} with key {}", id, bucket, key);
            s3.putObject(bucket, key, new ByteArrayInputStream(bodyDecoder.decodedBody), objectMetadata);

            final Update updateAction = buildUpsertAction(
                    id,
                    userId,
                    new FieldValue("createdAt", now),
                    new FieldValue("language", contentLanguage),
                    new FieldValue("type", bodyDecoder.contentType),
                    new FieldValue("s3Bucket", bucket),
                    new FieldValue("s3Key", key));
            logger.info("Indexing document with ID {}", id);
            Jest.CLIENT.execute(updateAction);

            final Map<String, Object> result = new HashMap<>();
            result.put("id", id);
            result.put("bucket", bucket);
            result.put("key", key);
            return new JsonGatewayResponse(result, headers, 200);
        } catch (Exception e) {
            logger.error("Got error when uploading image", e);
            return new GatewayResponse("{}", headers, 500);
        }
    }

    private String formatObjectKey(final String name, final String userId, final LocalDateTime timestamp) {
        final String prefix = System.getenv("S3_PREFIX");
        return String.format(
                "%s/user-%s/%d/%d/%d/%d/%s",
                prefix,
                userId,
                timestamp.getYear(),
                timestamp.getMonthValue(),
                timestamp.getDayOfMonth(),
                timestamp.getHour(),
                name);
    }
}