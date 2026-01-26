package net.walkman.cassette;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CassetteRegistry {


    private static final Map<ResourceLocation, RecordableData> RECORDABLES = new HashMap<>();


    public static void registerRecordable(ResourceLocation id, RecordableData data) {
        RECORDABLES.put(id, data);
    }


    public static void discoverDiscs() {
        System.out.println("[DEBUG_LOG] Discovering music discs...");
        int before = RECORDABLES.size();


        var musicDiscsTag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.withDefaultNamespace("music_discs"));

        BuiltInRegistries.ITEM.entrySet().forEach(entry -> {
            var item = entry.getValue();
            var itemStack = new ItemStack(item);


            if (itemStack.has(DataComponents.JUKEBOX_PLAYABLE) ||
                item.components().has(DataComponents.JUKEBOX_PLAYABLE) ||
                itemStack.is(musicDiscsTag)) {

                ResourceLocation itemId = entry.getKey().location();

                // Don't override JSON-defined ones
                boolean alreadyPresent = false;
                for (RecordableData data : RECORDABLES.values()) {
                    if (data.discItem() != null && data.discItem().equals(itemId)) {
                        alreadyPresent = true;
                        break;
                    }
                }

                if (!alreadyPresent) {
                    // Extract display name
                    String displayName = itemStack.getHoverName().getString();

                    // Try to resolve sound event
                    ResourceLocation soundLoc = null;
                    var playable = itemStack.get(DataComponents.JUKEBOX_PLAYABLE);
                    if (playable == null) playable = item.components().get(DataComponents.JUKEBOX_PLAYABLE);

                    if (playable != null) {
                        try {
                            var songKey = playable.song().key();
                            if (songKey != null) {
                                ResourceLocation songId = songKey.location();

                                soundLoc = ResourceLocation.fromNamespaceAndPath(songId.getNamespace(), "music_disc." + songId.getPath());
                            }
                        } catch (Exception ignored) {}
                    }

                    RECORDABLES.put(itemId, new RecordableData(soundLoc, displayName, itemId));
                    System.out.println("[DEBUG_LOG] Discovered disc: " + itemId + " (" + displayName + ") sound: " + soundLoc);
                }
            }
        });
        System.out.println("[DEBUG_LOG] Discovery finished. Added " + (RECORDABLES.size() - before) + " new discs. Total: " + RECORDABLES.size());
    }

    public static void clear() {
        RECORDABLES.clear();
    }

    public static Map<ResourceLocation, RecordableData> getAll() {
        return Collections.unmodifiableMap(RECORDABLES);
    }

    // Get recordable data
    public static RecordableData get(ResourceLocation id) {
        return RECORDABLES.get(id);
    }
}
