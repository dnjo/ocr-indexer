package net.dnjo.dao;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Get;
import io.searchbox.core.Update;
import net.dnjo.Jest;
import net.dnjo.Jest.FieldValue;
import net.dnjo.model.Image;
import net.dnjo.model.ImageUpdate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.function.Function;

import static net.dnjo.Jest.buildUpsertAction;

public class ImageDao {
    public Image findImage(final String imageId, final String userId) throws IOException {
        final Get getAction = Jest.buildGetAction(imageId, userId);
        final DocumentResult imageDocument = Jest.CLIENT.execute(getAction);
        final JsonObject imageJson = imageDocument.getJsonObject().getAsJsonObject("_source");
        return new Image(
                imageId,
                LocalDateTime.parse(imageJson.get("createdAt").getAsString()),
                getJsonValue(imageJson, "updatedAt", e -> LocalDateTime.parse(e.getAsString())),
                getJsonValue(imageJson, "text", JsonElement::getAsString),
                getJsonValue(imageJson, "ocrText", JsonElement::getAsString),
                imageJson.get("type").getAsString());
    }

    public Image updateImage(final String imageId, final String userId, final ImageUpdate imageUpdate) throws IOException {
        final LocalDateTime now = LocalDateTime.now();
        final Update update = buildUpsertAction(
                imageId,
                userId,
                new FieldValue("text", imageUpdate.getText()),
                new FieldValue("ocrText", imageUpdate.getOcrText()),
                new FieldValue("updatedAt", now));
        Jest.CLIENT.execute(update);
        final Image image = findImage(imageId, userId);
        return new Image(imageId, image.getCreatedAt(), now, imageUpdate.getText(), imageUpdate.getOcrText(), image.getType());
    }

    private <T> T getJsonValue(final JsonObject object, final String memberName, final Function<JsonElement, T> elementValueProvider) {
        final JsonElement element = object.get(memberName);
        return element != null && !element.isJsonNull() ? elementValueProvider.apply(element) : null;
    }
}
