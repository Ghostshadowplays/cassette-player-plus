package net.walkman.cassette;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record RecordableData(ResourceLocation soundEvent, String displayName, ResourceLocation discItem) {

    public static RecordableData fromJson(JsonObject json) {

        ResourceLocation sound = ResourceLocation.tryParse(json.get("sound_event").getAsString());
        String name = json.get("display_name").getAsString();
        ResourceLocation disc = ResourceLocation.tryParse(json.get("disc_item").getAsString());

        return new RecordableData(sound, name, disc);
    }
}
