package net.walkman.cassette;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record RecordableData(ResourceLocation soundEvent, String displayName, ResourceLocation discItem) {

    public static RecordableData fromJson(JsonObject json) {
        ResourceLocation sound = json.has("sound_event") ? ResourceLocation.tryParse(json.get("sound_event").getAsString()) : null;
        String name = json.has("display_name") ? json.get("display_name").getAsString() : "Unknown Disc";
        ResourceLocation disc = json.has("disc_item") ? ResourceLocation.tryParse(json.get("disc_item").getAsString()) : null;

        return new RecordableData(sound, name, disc);
    }
}
