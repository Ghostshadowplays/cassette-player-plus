package net.walkman.walkman;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.walkman.cassette.CassetteItem;
import net.walkman.cassette.CassetteRegistry;
import net.walkman.cassette.RecordableData;
import net.walkman.walkman.CassettePlayerItem; // <-- make sure this import exists
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class CassettePlayerSoundHandler {
    private static final Map<UUID, SoundInstance> PLAYING_SOUNDS = new HashMap<>();
    private static final Map<UUID, List<ResourceLocation>> MIX_TAPE_TRACKS = new HashMap<>();
    private static final Map<UUID, Integer> MIX_TAPE_INDEX = new HashMap<>();
    private static final Map<UUID, Float> PLAYER_VOLUMES = new HashMap<>();
    private static final Map<UUID, Boolean> REPEAT_MODES = new HashMap<>();

    public static void setRepeat(UUID playerUUID, boolean repeat) {
        REPEAT_MODES.put(playerUUID, repeat);
    }

    public static class CassetteSoundInstance extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
        private float targetVolume;
        private final UUID playerUUID;
        private boolean wasStoppedPreviously = false;
        private int tickCount = 0;

        public CassetteSoundInstance(ResourceLocation soundLocation, float volume, UUID playerUUID) {
            super(SoundEvent.createVariableRangeEvent(soundLocation), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.volume = volume;
            this.targetVolume = volume;
            this.looping = false;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.playerUUID = playerUUID;
        }

        public void setVolume(float volume) {
            this.targetVolume = volume;
        }

        @Override
        public void tick() {
            this.tickCount++;
            this.volume = this.targetVolume;
            if (Minecraft.getInstance().player == null || Minecraft.getInstance().player.isRemoved()) {
                this.stop();
                return;
            }

            if (this.isStopped()) {
                if (!wasStoppedPreviously && tickCount > 5) {
                    System.out.println("[DEBUG_LOG] Sound instance for player " + playerUUID + " reported isStopped() = true for the first time after " + tickCount + " ticks");
                    wasStoppedPreviously = true;
                    handleSoundEnd(playerUUID);
                }
            } else {
                wasStoppedPreviously = false;
            }
        }
    }

    private static void handleSoundEnd(UUID playerUUID) {
        if (MIX_TAPE_TRACKS.containsKey(playerUUID)) {
            List<ResourceLocation> tracks = MIX_TAPE_TRACKS.get(playerUUID);
            int currentIndex = MIX_TAPE_INDEX.getOrDefault(playerUUID, 0);
            int nextIndex = currentIndex + 1;
            boolean repeat = REPEAT_MODES.getOrDefault(playerUUID, false);
            
            System.out.println("[DEBUG_LOG] handleSoundEnd for " + playerUUID + ". Current index: " + currentIndex + ", Next index: " + nextIndex + ", Repeat: " + repeat + ", Total tracks: " + tracks.size());
            
            if (nextIndex < tracks.size()) {
                playTrackAtIndex(playerUUID, nextIndex);
            } else {
                if (repeat) {
                    System.out.println("[DEBUG_LOG] Looping back to start for " + playerUUID);
                    playTrackAtIndex(playerUUID, 0);
                } else {
                    System.out.println("[DEBUG_LOG] End of playlist reached for " + playerUUID);
                    stopMusic(playerUUID);
                }
            }
        } else {
            System.out.println("[DEBUG_LOG] No tracks found for " + playerUUID + " in handleSoundEnd");
        }
    }

    private static void playTrackAtIndex(UUID playerUUID, int index) {
        List<ResourceLocation> tracks = MIX_TAPE_TRACKS.get(playerUUID);
        if (tracks == null || index < 0 || index >= tracks.size()) {
            System.out.println("[DEBUG_LOG] playTrackAtIndex: Invalid tracks or index for " + playerUUID);
            return;
        }

        MIX_TAPE_INDEX.put(playerUUID, index);
        ResourceLocation trackId = tracks.get(index);
        float volume = PLAYER_VOLUMES.getOrDefault(playerUUID, 1.0f);
        
        ResourceLocation soundLoc = resolveSoundLocation(trackId);
        System.out.println("[DEBUG_LOG] Playing track at index " + index + ": " + trackId + " -> sound: " + soundLoc);
        
        // Stop current sound without clearing the playlist
        SoundInstance current = PLAYING_SOUNDS.remove(playerUUID);
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
        }

        if (soundLoc != null) {
            CassetteSoundInstance nextSound = new CassetteSoundInstance(soundLoc, volume, playerUUID);
            Minecraft.getInstance().getSoundManager().play(nextSound);
            PLAYING_SOUNDS.put(playerUUID, nextSound);
        } else {
            System.out.println("[DEBUG_LOG] Could not resolve sound location for " + trackId + ", skipping to next if available.");
            // Instead of stopping, try to move to next track if it's a mix tape
            int nextIdx = index + 1;
            if (nextIdx < tracks.size()) {
                playTrackAtIndex(playerUUID, nextIdx);
            } else {
                stopMusic(playerUUID);
            }
        }
    }

    public static void skipTrack(UUID playerUUID) {
        if (MIX_TAPE_TRACKS.containsKey(playerUUID)) {
            int currentIndex = MIX_TAPE_INDEX.getOrDefault(playerUUID, 0);
            int nextIndex = currentIndex + 1;
            List<ResourceLocation> tracks = MIX_TAPE_TRACKS.get(playerUUID);
            
            if (nextIndex < tracks.size()) {
                System.out.println("[DEBUG_LOG] Manual skip to next track for " + playerUUID);
                playTrackAtIndex(playerUUID, nextIndex);
            } else {
                // Loop back to start or stop? Let's loop for now as it's a "Next" button
                System.out.println("[DEBUG_LOG] Manual skip reached end, looping to start for " + playerUUID);
                playTrackAtIndex(playerUUID, 0);
            }
        }
    }

    public static void previousTrack(UUID playerUUID) {
        if (MIX_TAPE_TRACKS.containsKey(playerUUID)) {
            int currentIndex = MIX_TAPE_INDEX.getOrDefault(playerUUID, 0);
            int prevIndex = currentIndex - 1;
            
            if (prevIndex >= 0) {
                System.out.println("[DEBUG_LOG] Manual skip to previous track for " + playerUUID);
                playTrackAtIndex(playerUUID, prevIndex);
            } else {
                // Loop to end
                List<ResourceLocation> tracks = MIX_TAPE_TRACKS.get(playerUUID);
                System.out.println("[DEBUG_LOG] Manual skip reached start, looping to end for " + playerUUID);
                playTrackAtIndex(playerUUID, tracks.size() - 1);
            }
        }
    }

    public static void setVolume(UUID playerUUID, float volume) {
        PLAYER_VOLUMES.put(playerUUID, volume);
        SoundInstance sound = PLAYING_SOUNDS.get(playerUUID);
        if (sound instanceof CassetteSoundInstance cassetteSound) {
            cassetteSound.setVolume(volume);
        }
    }

    public static void toggleMusic(UUID playerUUID, ItemStack cassetteStack, float volume) {
        if (PLAYING_SOUNDS.containsKey(playerUUID)) {
            stopMusic(playerUUID);
        } else {
            playMusic(playerUUID, cassetteStack, volume);
        }
    }

    public static void playMusic(UUID playerUUID, ItemStack cassetteStack, float volume) {
        System.out.println("[DEBUG_LOG] playMusic called for " + playerUUID + " with volume " + volume);
        // Clear previous playlist state before starting new playback
        MIX_TAPE_TRACKS.remove(playerUUID);
        MIX_TAPE_INDEX.remove(playerUUID);
        stopMusic(playerUUID);
        PLAYER_VOLUMES.put(playerUUID, volume);

        if (cassetteStack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var tag = cassetteStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            
            // Handle repeat mode from item if available
            if (tag.contains("Repeat")) {
                REPEAT_MODES.put(playerUUID, tag.getBoolean("Repeat"));
            }

            if (tag.getBoolean("IsMixTape") && tag.contains("Tracks")) {
                List<ResourceLocation> tracks = new java.util.ArrayList<>();
                net.minecraft.nbt.ListTag tracksTag = tag.getList("Tracks", net.minecraft.nbt.Tag.TAG_COMPOUND);
                
                // If it was stored as strings in older version or fallback
                if (tracksTag.isEmpty() && tag.contains("Tracks", net.minecraft.nbt.Tag.TAG_LIST)) {
                     tracksTag = tag.getList("Tracks", net.minecraft.nbt.Tag.TAG_STRING);
                }
                
                System.out.println("[DEBUG_LOG] Loading Mix Tape tracks. Raw count: " + tracksTag.size());
                
                for (int i = 0; i < tracksTag.size(); i++) {
                    var element = tracksTag.get(i);
                    ResourceLocation resolvedSound = null;
                    
                    if (element instanceof net.minecraft.nbt.CompoundTag trackTag) {
                        // Prefer Item ID for mixtapes now, as it's what standard cassettes use and is proven to work.
                        // We still have Song ID as fallback if Item ID is missing.
                        ResourceLocation id = trackTag.contains("id") ? ResourceLocation.tryParse(trackTag.getString("id")) : null;
                        ResourceLocation songId = trackTag.contains("song") ? ResourceLocation.tryParse(trackTag.getString("song")) : null;
                        ResourceLocation storedSound = trackTag.contains("sound") ? ResourceLocation.tryParse(trackTag.getString("sound")) : null;
                        
                        // Try all possible ways to resolve the sound now
                        if (storedSound != null) resolvedSound = resolveSoundLocation(storedSound);
                        if (resolvedSound == null && id != null) resolvedSound = resolveSoundLocation(id);
                        if (resolvedSound == null && songId != null) resolvedSound = resolveSoundLocation(songId);
                        
                        System.out.println("[DEBUG_LOG] Track " + i + ": id=" + id + ", song=" + songId + ", storedSound=" + storedSound + " -> resolved: " + resolvedSound);
                    } else if (element instanceof net.minecraft.nbt.StringTag stringTag) {
                        ResourceLocation id = ResourceLocation.tryParse(stringTag.getAsString());
                        if (id != null) resolvedSound = resolveSoundLocation(id);
                        System.out.println("[DEBUG_LOG] Track " + i + " (string): " + id + " -> resolved: " + resolvedSound);
                    }
                    
                    if (resolvedSound != null) {
                        tracks.add(resolvedSound);
                    } else {
                        System.out.println("[DEBUG_LOG] FAILED to resolve sound for track " + i);
                    }
                }
                
                System.out.println("[DEBUG_LOG] Detected Mix Tape with " + tracks.size() + " resolved tracks.");
                if (!tracks.isEmpty()) {
                    MIX_TAPE_TRACKS.put(playerUUID, tracks);
                    // playTrackAtIndex will set the index and start playback
                    playTrackAtIndex(playerUUID, 0);
                    return;
                } else {
                    System.out.println("[DEBUG_LOG] Mix Tape has no valid tracks, falling back to standard playback.");
                }
            }
        }

        // Ensure discs are discovered on the client side
        CassetteRegistry.discoverDiscs();

        ResourceLocation recordableId = CassetteItem.getRecordableId(cassetteStack);
        System.out.println("[DEBUG_LOG] Standard cassette detected. RecordableId: " + recordableId);
        ResourceLocation soundLoc = null;

        var playable = cassetteStack.get(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE);
        if (playable != null) {
            soundLoc = resolveSoundFromPlayable(playable);
            System.out.println("[DEBUG_LOG] Found JukeboxPlayable. Resolved soundLoc: " + soundLoc);
        }

        if (soundLoc == null && recordableId != null) {
            soundLoc = resolveSoundLocation(recordableId);
            System.out.println("[DEBUG_LOG] Fallback to resolveSoundLocation(recordableId): " + soundLoc);
        }

        if (soundLoc != null) {
            // Store as a 1-track mix tape for repeating support
            List<ResourceLocation> tracks = new java.util.ArrayList<>();
            tracks.add(recordableId != null ? recordableId : soundLoc);
            MIX_TAPE_TRACKS.put(playerUUID, tracks);
            MIX_TAPE_INDEX.put(playerUUID, 0);
            
            playCassetteSound(playerUUID, soundLoc, volume);
        } else {
            System.out.println("[DEBUG_LOG] FAILED to resolve sound location for cassette.");
        }
    }

    private static ResourceLocation resolveSoundFromPlayable(net.minecraft.world.item.JukeboxPlayable playable) {
        try {
            Object holder = playable.song();
            ResourceLocation songId = null;

            // Try multiple ways to get the ResourceLocation from the Holder
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
                // Try to get the actual sound event from the registry
                try {
                    var songRegistry = Minecraft.getInstance().level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.JUKEBOX_SONG);
                    var song = songRegistry.get(songId);
                    if (song != null) {
                        try {
                            return song.soundEvent().value().getLocation();
                        } catch (Throwable t) {
                            try {
                                java.lang.reflect.Method locationMethod = song.soundEvent().value().getClass().getMethod("location");
                                return (ResourceLocation) locationMethod.invoke(song.soundEvent().value());
                            } catch (Throwable t2) {}
                        }
                    }
                } catch (Exception e) {
                    // System.out.println("[DEBUG_LOG] Registry lookup failed for song " + songId + ": " + e.getMessage());
                }

                // Fallback: common naming convention
                return ResourceLocation.fromNamespaceAndPath(songId.getNamespace(), "music_disc." + songId.getPath());
            }
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Failed to resolve sound from playable: " + e.getMessage());
        }
        return null;
    }

    private static ResourceLocation resolveSoundLocation(ResourceLocation recordableId) {
        if (recordableId == null) return null;

        // 0. Check if it's already a valid SoundEvent ID (most reliable if we have it)
        if (net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(recordableId)) {
             return recordableId;
        }

        System.out.println("[DEBUG_LOG] resolveSoundLocation for: " + recordableId);

        // Ensure discs are discovered on the client side
        CassetteRegistry.discoverDiscs();

        // 1. Try CassetteRegistry (mapped by Item ID)
        RecordableData data = CassetteRegistry.get(recordableId);
        if (data != null && data.soundEvent() != null) {
            System.out.println("[DEBUG_LOG] [Strategy 1] Resolved from CassetteRegistry: " + data.soundEvent());
            return data.soundEvent();
        }

        // 2. Try Jukebox Song registry (if it's a song ID or happens to match)
        try {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                var songRegistry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.JUKEBOX_SONG);
                var song = songRegistry.get(recordableId);
                if (song != null) {
                    try {
                        ResourceLocation loc = song.soundEvent().value().getLocation();
                        System.out.println("[DEBUG_LOG] [Strategy 2] Resolved from JukeboxSong registry: " + loc);
                        return loc;
                    } catch (Throwable t) {
                        try {
                            java.lang.reflect.Method locationMethod = song.soundEvent().value().getClass().getMethod("location");
                            ResourceLocation loc = (ResourceLocation) locationMethod.invoke(song.soundEvent().value());
                            if (loc != null) {
                                System.out.println("[DEBUG_LOG] [Strategy 2] Resolved from JukeboxSong registry (reflection): " + loc);
                                return loc;
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. Try looking it up as an Item and extracting from JukeboxPlayable
        try {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(recordableId);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                ItemStack stack = item.getDefaultInstance();
                var playable = stack.get(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE);
                if (playable == null) {
                    playable = item.components().get(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE);
                }
                
                if (playable != null) {
                    ResourceLocation sound = resolveSoundFromPlayable(playable);
                    if (sound != null) {
                        System.out.println("[DEBUG_LOG] [Strategy 3] Resolved from Item JukeboxPlayable: " + sound);
                        return sound;
                    }
                }
            }
        } catch (Exception ignored) {}

        // 4. Fallback: common path patterns
        String path = recordableId.getPath();
        String namespace = recordableId.getNamespace();

        if (path.startsWith("music_disc_")) {
            String subPath = path.substring("music_disc_".length());
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(namespace, "music_disc." + subPath);
            System.out.println("[DEBUG_LOG] [Strategy 4] Guessed from music_disc_ prefix: " + loc);
            return loc;
        } else if (!path.contains(".")) {
            // If it doesn't look like a direct sound event ID, assume it's a name we need to prefix
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(namespace, "music_disc." + path);
            System.out.println("[DEBUG_LOG] [Strategy 4] Guessed with music_disc. prefix: " + loc);
            return loc;
        } else {
            System.out.println("[DEBUG_LOG] [Strategy 4] Returning original ID as last resort: " + recordableId);
            return recordableId;
        }
    }

    private static void playCassetteSound(UUID playerUUID, ResourceLocation soundLocation, float volume) {
        System.out.println("[DEBUG_LOG] playCassetteSound: " + soundLocation + " for player " + playerUUID);
        CassetteSoundInstance sound = new CassetteSoundInstance(soundLocation, volume, playerUUID);
        Minecraft.getInstance().getSoundManager().play(sound);
        PLAYING_SOUNDS.put(playerUUID, sound);
    }

    public static void stopMusic(UUID playerUUID) {
        SoundInstance sound = PLAYING_SOUNDS.remove(playerUUID);
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().stop(sound);
        }
        MIX_TAPE_TRACKS.remove(playerUUID);
        MIX_TAPE_INDEX.remove(playerUUID);
    }


    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        UUID uuid = mc.player.getUUID();

        if (!PLAYING_SOUNDS.containsKey(uuid)) return;

        boolean hasPlayer = false;
        // Check main inventory, hotbar, armor, and offhand
        for (ItemStack stack : mc.player.getInventory().items) {
            if (stack.getItem() instanceof CassettePlayerItem) {
                hasPlayer = true;
                break;
            }
        }
        if (!hasPlayer) {
            for (ItemStack stack : mc.player.getInventory().offhand) {
                if (stack.getItem() instanceof CassettePlayerItem) {
                    hasPlayer = true;
                    break;
                }
            }
        }
        if (!hasPlayer) {
            for (ItemStack stack : mc.player.getInventory().armor) {
                if (stack.getItem() instanceof CassettePlayerItem) {
                    hasPlayer = true;
                    break;
                }
            }
        }
        
        // Check carried item (being moved with cursor)
        if (!hasPlayer && mc.player.containerMenu != null) {
            if (mc.player.containerMenu.getCarried().getItem() instanceof CassettePlayerItem) {
                hasPlayer = true;
            }
        }

        if (!hasPlayer) {
            stopMusic(uuid);
        }
    }
}
