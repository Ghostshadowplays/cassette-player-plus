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
    private static boolean discoveryAttempted = false;


    public static void registerRecordable(ResourceLocation id, RecordableData data) {
        RECORDABLES.put(id, data);
    }
    
    public static boolean hasDiscoveryAttempted() {
        return discoveryAttempted;
    }


    public static void discoverDiscs() {
        discoveryAttempted = true;

        var musicDiscsTag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.withDefaultNamespace("music_discs"));

        // Use a list to avoid concurrent issues if discoverDiscs is called from within a loop or similar
        java.util.List<java.util.Map.Entry<ResourceLocation, net.minecraft.world.item.Item>> items = 
            new java.util.ArrayList<>(BuiltInRegistries.ITEM.entrySet().stream()
                .map(e -> java.util.Map.entry(e.getKey().location(), e.getValue()))
                .toList());

        for (var entry : items) {
            var item = entry.getValue();
            var itemStack = new ItemStack(item);

            boolean isJukeboxPlayable = itemStack.has(DataComponents.JUKEBOX_PLAYABLE) ||
                                       item.components().has(DataComponents.JUKEBOX_PLAYABLE);
            boolean isMusicDiscTag = itemStack.is(musicDiscsTag);
            
            // Fallback: check path for common keywords if not found by components/tags
            boolean matchesFallback = false;
            if (!isJukeboxPlayable && !isMusicDiscTag) {
                String path = entry.getKey().getPath().toLowerCase();
                // More restrictive fallback to avoid "disc_fragment", "fragment", etc.
                if ((path.contains("music_disc") || path.contains("record") || path.contains("disc") || path.contains("disk")) 
                        && !path.contains("fragment") && !path.contains("part")
                        && !entry.getKey().getNamespace().equals(net.walkman.music.Music.MODID)) { // Don't match our own items
                    matchesFallback = true;
                }
            }

            if (isJukeboxPlayable || isMusicDiscTag || matchesFallback) {
                ResourceLocation itemId = entry.getKey();

                // Don't override JSON-defined ones unless we are on client and existing has no sound
                boolean alreadyPresent = false;
                RecordableData existing = RECORDABLES.get(itemId);
                if (existing == null) {
                    for (RecordableData data : RECORDABLES.values()) {
                        if (data.discItem() != null && data.discItem().equals(itemId)) {
                            existing = data;
                            break;
                        }
                    }
                }

                if (existing != null) {
                    // If we are on client and the existing entry has no sound, we might want to try again
                    if (existing.soundEvent() != null || net.neoforged.fml.loading.FMLEnvironment.dist != net.neoforged.api.distmarker.Dist.CLIENT) {
                        alreadyPresent = true;
                    }
                }

                if (!alreadyPresent) {
                    // Extract display name
                    String displayName = item.getName(itemStack).getString();

                    // Try to resolve sound event
                    ResourceLocation soundLoc = null;
                    var playable = itemStack.get(DataComponents.JUKEBOX_PLAYABLE);
                    if (playable == null) playable = item.components().get(DataComponents.JUKEBOX_PLAYABLE);

                    if (playable != null) {
                        try {
                            Object holder = playable.song();
                            ResourceLocation songId = null;

                            // Robust Holder ID resolution
                            try {
                                java.lang.reflect.Method unwrapKeyMethod = holder.getClass().getMethod("unwrapKey");
                                java.util.Optional<?> optionalKey = (java.util.Optional<?>) unwrapKeyMethod.invoke(holder);
                                if (optionalKey.isPresent()) {
                                    Object keyObj = optionalKey.get();
                                    java.lang.reflect.Method locationMethod = keyObj.getClass().getMethod("location");
                                    songId = (ResourceLocation) locationMethod.invoke(keyObj);
                                }
                            } catch (Exception ignored) {
                                try {
                                    java.lang.reflect.Method keyMethod = holder.getClass().getMethod("key");
                                    Object keyObj = keyMethod.invoke(holder);
                                    java.lang.reflect.Method locationMethod = keyObj.getClass().getMethod("location");
                                    songId = (ResourceLocation) locationMethod.invoke(keyObj);
                                } catch (Exception ignored2) {
                                    try {
                                        java.lang.reflect.Method unwrapMethod = holder.getClass().getMethod("unwrap");
                                        com.mojang.datafixers.util.Either<?, ?> either = (com.mojang.datafixers.util.Either<?, ?>) unwrapMethod.invoke(holder);
                                        java.util.Optional<?> left = (java.util.Optional<?>) either.left();
                                        if (left.isPresent()) {
                                            Object keyObj = left.get();
                                            java.lang.reflect.Method locationMethod = keyObj.getClass().getMethod("location");
                                            songId = (ResourceLocation) locationMethod.invoke(keyObj);
                                        }
                                    } catch (Exception ignored3) {}
                                }
                            }

                            if (songId != null) {
                                // Try to get actual sound event from registry if available
                                if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                                    try {
                                        Class<?> helper = Class.forName("net.walkman.cassette.CassetteRegistryClient");
                                        java.lang.reflect.Method method = helper.getMethod("getSoundLocation", ResourceLocation.class);
                                        soundLoc = (ResourceLocation) method.invoke(null, songId);
                                    } catch (Throwable ignored) {}
                                }

                                if (soundLoc == null) {
                                    soundLoc = ResourceLocation.fromNamespaceAndPath(songId.getNamespace(), "music_disc." + songId.getPath());
                                }
                            
                                // If it was exactly "Music Disc", let's use the song ID path as the name
                                if (displayName.equalsIgnoreCase("Music Disc") || displayName.isEmpty() || displayName.contains("item.minecraft.music_disc")) {
                                    String songPath = songId.getPath();
                                    if (songPath.contains("/")) songPath = songPath.substring(songPath.lastIndexOf('/') + 1);
                                    // Capitalize manually
                                    if (!songPath.isEmpty()) {
                                        songPath = songPath.replace('_', ' ');
                                        String[] words = songPath.split(" ");
                                        StringBuilder sb = new StringBuilder();
                                        for (String word : words) {
                                            if (!word.isEmpty()) {
                                                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
                                            }
                                        }
                                        displayName = sb.toString().trim();
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    if (soundLoc != null || isJukeboxPlayable || isMusicDiscTag) {
                        RECORDABLES.put(itemId, new RecordableData(soundLoc, displayName, itemId));
                    }
                }
            }
        }
    }

    public static void clear() {
        RECORDABLES.clear();
        discoveryAttempted = false;
    }

    public static Map<ResourceLocation, RecordableData> getAll() {
        return Collections.unmodifiableMap(RECORDABLES);
    }

    // Get recordable data
    public static RecordableData get(ResourceLocation id) {
        return RECORDABLES.get(id);
    }
}
