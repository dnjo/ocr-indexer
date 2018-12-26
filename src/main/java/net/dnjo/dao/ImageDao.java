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
    public Image findImage(String imageId) throws IOException {
        Get getAction = new Get.Builder("results", imageId).build();
        DocumentResult imageDocument = Jest.CLIENT.execute(getAction);
        JsonObject imageJson = imageDocument.getJsonObject().getAsJsonObject("_source");
        return new Image(
                imageId,
                LocalDateTime.parse(imageJson.get("createdAt").getAsString()),
                getJsonValue(imageJson, "updatedAt", e -> LocalDateTime.parse(e.getAsString())),
                getJsonValue(imageJson, "text", JsonElement::getAsString),
                getJsonValue(imageJson, "ocrText", JsonElement::getAsString),
                imageJson.get("type").getAsString());
    }

    public Image updateImage(String imageId, ImageUpdate imageUpdate) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        Update update = buildUpsertAction(
                imageId,
                new FieldValue("text", imageUpdate.getText()),
                new FieldValue("ocrText", imageUpdate.getOcrText()),
                new FieldValue("updatedAt", now));
        Jest.CLIENT.execute(update);
        Image image = findImage(imageId);
        return new Image(imageId, image.getCreatedAt(), now, imageUpdate.getText(), imageUpdate.getOcrText(), image.getType());
    }

    private <T> T getJsonValue(JsonObject object, String memberName, Function<JsonElement, T> elementValueProvider) {
        JsonElement element = object.get(memberName);
        return element != null && !element.isJsonNull() ? elementValueProvider.apply(element) : null;
    }
}
