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
    private static final Map<String, SoundInstance> PLAYING_SOUNDS = new HashMap<>();
    private static final Map<String, List<ResourceLocation>> MIX_TAPE_TRACKS = new HashMap<>();
    private static final Map<String, Integer> MIX_TAPE_INDEX = new HashMap<>();
    private static final Map<String, Float> PLAYER_VOLUMES = new HashMap<>();
    private static final Map<String, Boolean> REPEAT_MODES = new HashMap<>();

    public static void setRepeat(UUID playerUUID, boolean repeat) {
        REPEAT_MODES.put(playerUUID.toString(), repeat);
    }

    public static class CassetteSoundInstance extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
        private float targetVolume;
        private final String sourceId;
        private final net.minecraft.core.BlockPos pos;
        private final net.minecraft.world.entity.player.Player attachedPlayer;
        private int tickCount = 0;

        public CassetteSoundInstance(ResourceLocation soundLocation, float volume, String sourceId) {
            this(soundLocation, volume, sourceId, null, null);
        }

        public CassetteSoundInstance(ResourceLocation soundLocation, float volume, String sourceId, net.minecraft.core.BlockPos pos) {
            this(soundLocation, volume, sourceId, pos, null);
        }

        public CassetteSoundInstance(ResourceLocation soundLocation, float volume, String sourceId, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player attachedPlayer) {
            super(SoundEvent.createVariableRangeEvent(soundLocation), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.volume = volume;
            this.targetVolume = volume;
            this.looping = false;
            this.delay = 0;
            this.sourceId = sourceId;
            this.pos = pos;
            this.attachedPlayer = attachedPlayer;

            if (pos != null || attachedPlayer != null) {
                this.x = (double)(pos != null ? (float)pos.getX() + 0.5F : (float)attachedPlayer.getX());
                this.y = (double)(pos != null ? (float)pos.getY() + 0.5F : (float)attachedPlayer.getY());
                this.z = (double)(pos != null ? (float)pos.getZ() + 0.5F : (float)attachedPlayer.getZ());
                this.relative = false;
                this.attenuation = Attenuation.LINEAR;
            } else {
                this.relative = true;
                this.attenuation = Attenuation.NONE;
            }
        }

        public void setVolume(float volume) {
            this.targetVolume = volume;
        }

        @Override
        public void tick() {
            this.tickCount++;
            this.volume = this.targetVolume;
            
            if (attachedPlayer != null) {
                if (attachedPlayer.isRemoved()) {
                    this.stop();
                    return;
                }
                this.x = (double)((float)attachedPlayer.getX());
                this.y = (double)((float)attachedPlayer.getY());
                this.z = (double)((float)attachedPlayer.getZ());

                // Music particles for handheld Boombox
                if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getGameTime() % 20 == 0) {
                    ItemStack main = attachedPlayer.getMainHandItem();
                    ItemStack off = attachedPlayer.getOffhandItem();
                    if (main.is(net.walkman.music.Music.BOOMBOX_ITEM.get()) || off.is(net.walkman.music.Music.BOOMBOX_ITEM.get())) {
                        // Move particles back based on player's facing
                        double radians = Math.toRadians(attachedPlayer.getYRot());
                        double dx = Math.sin(radians) * 0.4;
                        double dz = -Math.cos(radians) * 0.4;
                        
                        Minecraft.getInstance().level.addParticle(net.minecraft.core.particles.ParticleTypes.NOTE, 
                            this.x + dx, this.y + 1.2, this.z + dz, 
                            (double)Minecraft.getInstance().level.random.nextInt(24) / 24.0, 0.0, 0.0);
                    }
                }
            } else if (pos == null) {
                if (Minecraft.getInstance().player == null || Minecraft.getInstance().player.isRemoved()) {
                    this.stop();
                    return;
                }
            } else {
                // For boombox, check if block still exists or still playing
                var level = Minecraft.getInstance().level;
                if (level == null || !(level.getBlockEntity(pos) instanceof net.walkman.music.BoomboxBlockEntity boombox) || !boombox.isPlaying() || boombox.getCassette().isEmpty()) {
                    this.stop();
                }
            }
        }
    }

    private static void handleSoundEnd(String sourceId) {
        // Remove the finished sound from the playing sounds map
        PLAYING_SOUNDS.remove(sourceId);

        if (MIX_TAPE_TRACKS.containsKey(sourceId)) {
            List<ResourceLocation> tracks = MIX_TAPE_TRACKS.get(sourceId);
            int currentIndex = MIX_TAPE_INDEX.getOrDefault(sourceId, 0);
            int nextIndex = currentIndex + 1;
            boolean repeat = REPEAT_MODES.getOrDefault(sourceId, false);
            
            if (nextIndex < tracks.size()) {
                playTrackAtIndex(sourceId, nextIndex);
            } else {
                if (repeat) {
                    playTrackAtIndex(sourceId, 0);
                } else {
                    stopMusic(sourceId);
                    // If it's a boombox, update BE
                    updateBoomboxState(sourceId, false);
                }
            }
        } else {
            stopMusic(sourceId);
            updateBoomboxState(sourceId, false);
        }
    }

    private static void updateBoomboxState(String sourceId, boolean playing) {
        if (sourceId.startsWith("boombox-")) {
            String posStr = sourceId.substring("boombox-".length());
            String[] parts = posStr.split("_");
            if (parts.length == 3) {
                try {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    var level = Minecraft.getInstance().level;
                    if (level != null && level.getBlockEntity(pos) instanceof net.walkman.music.BoomboxBlockEntity boombox) {
                        // We can't easily set state on server from client like this without packets, 
                        // but we can at least make the client think it stopped.
                        // Actually, the server should manage this. 
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private static void playTrackAtIndex(String sourceId, int index) {
        List<ResourceLocation> tracks = MIX_TAPE_TRACKS.get(sourceId);
        if (tracks == null || index < 0 || index >= tracks.size()) {
            return;
        }

        MIX_TAPE_INDEX.put(sourceId, index);
        ResourceLocation trackId = tracks.get(index);
        float volume = PLAYER_VOLUMES.getOrDefault(sourceId, 1.0f);
        
        ResourceLocation soundLoc = resolveSoundLocation(trackId);
        
        // Stop current sound without clearing the playlist
        SoundInstance current = PLAYING_SOUNDS.remove(sourceId);
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
        }

        if (soundLoc != null) {
            net.minecraft.core.BlockPos pos = null;
            net.minecraft.world.entity.player.Player attachedPlayer = null;
            
            if (sourceId.startsWith("boombox-")) {
                String posStr = sourceId.substring("boombox-".length());
                String[] parts = posStr.split("_");
                if (parts.length == 3) {
                    pos = new net.minecraft.core.BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                }
            } else {
                // If it's a boombox in hand, we want to attach it positionally to the player.
                // We'll need a way to know if this is a boombox or a walkman.
                // For now, let's look for the player in the level.
                try {
                    UUID playerUUID = UUID.fromString(sourceId);
                    attachedPlayer = Minecraft.getInstance().level.getPlayerByUUID(playerUUID);
                    
                    // Only attach if it's NOT a walkman. 
                    if (attachedPlayer != null) {
                        ItemStack main = attachedPlayer.getMainHandItem();
                        ItemStack off = attachedPlayer.getOffhandItem();
                        boolean holdingBoombox = main.is(net.walkman.music.Music.BOOMBOX_ITEM.get()) || off.is(net.walkman.music.Music.BOOMBOX_ITEM.get());
                        if (!holdingBoombox) {
                            attachedPlayer = null; // Walkman is non-positional
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Not a UUID
                }
            }

            CassetteSoundInstance nextSound = new CassetteSoundInstance(soundLoc, volume, sourceId, pos, attachedPlayer);
            Minecraft.getInstance().getSoundManager().play(nextSound);
            PLAYING_SOUNDS.put(sourceId, nextSound);
        } else {
            // Instead of stopping, try to move to next track if it's a mix tape
            int nextIdx = index + 1;
            if (nextIdx < tracks.size()) {
                playTrackAtIndex(sourceId, nextIdx);
            } else {
                stopMusic(sourceId);
            }
        }
    }

    public static void skipTrack(UUID playerUUID) {
        skipTrack(playerUUID.toString());
    }

    public static void skipTrack(String sourceId) {
        if (MIX_TAPE_TRACKS.containsKey(sourceId)) {
            List<ResourceLocation> tracks = MIX_TAPE_TRACKS.get(sourceId);
            if (tracks.isEmpty()) return;
            
            int currentIndex = MIX_TAPE_INDEX.getOrDefault(sourceId, 0);
            int nextIndex = (currentIndex + 1) % tracks.size();
            
            playTrackAtIndex(sourceId, nextIndex);
        }
    }

    public static void previousTrack(UUID playerUUID) {
        String sourceId = playerUUID.toString();
        if (MIX_TAPE_TRACKS.containsKey(sourceId)) {
            List<ResourceLocation> tracks = MIX_TAPE_TRACKS.get(sourceId);
            if (tracks.isEmpty()) return;
            
            int currentIndex = MIX_TAPE_INDEX.getOrDefault(sourceId, 0);
            int prevIndex = (currentIndex - 1 + tracks.size()) % tracks.size();
            
            playTrackAtIndex(sourceId, prevIndex);
        }
    }

    public static void setVolume(UUID playerUUID, float volume) {
        String sourceId = playerUUID.toString();
        PLAYER_VOLUMES.put(sourceId, volume);
        SoundInstance sound = PLAYING_SOUNDS.get(sourceId);
        if (sound instanceof CassetteSoundInstance cassetteSound) {
            cassetteSound.setVolume(volume);
        }
    }

    public static void toggleMusic(UUID playerUUID, ItemStack cassetteStack, float volume) {
        String sourceId = playerUUID.toString();
        if (PLAYING_SOUNDS.containsKey(sourceId)) {
            stopMusic(sourceId);
        } else {
            playMusic(sourceId, cassetteStack, volume);
        }
    }

    public static void playMusic(UUID playerUUID, ItemStack cassetteStack, float volume) {
        playMusic(playerUUID.toString(), cassetteStack, volume);
    }

    public static void playMusic(String sourceId, ItemStack cassetteStack, float volume) {
        // Clear previous playlist state before starting new playback
        MIX_TAPE_TRACKS.remove(sourceId);
        MIX_TAPE_INDEX.remove(sourceId);
        stopMusic(sourceId);
        PLAYER_VOLUMES.put(sourceId, volume);

        if (cassetteStack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var tag = cassetteStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            
            // Handle repeat mode from item if available
            if (tag.contains("Repeat")) {
                REPEAT_MODES.put(sourceId, tag.getBoolean("Repeat"));
            }

            if (tag.getBoolean("IsMixTape") && tag.contains("Tracks")) {
                List<ResourceLocation> tracks = new java.util.ArrayList<>();
                net.minecraft.nbt.ListTag tracksTag = tag.getList("Tracks", net.minecraft.nbt.Tag.TAG_COMPOUND);
                
                // If it was stored as strings in older version or fallback
                if (tracksTag.isEmpty() && tag.contains("Tracks", net.minecraft.nbt.Tag.TAG_LIST)) {
                     tracksTag = tag.getList("Tracks", net.minecraft.nbt.Tag.TAG_STRING);
                }
                
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
                        
                    } else if (element instanceof net.minecraft.nbt.StringTag stringTag) {
                        ResourceLocation id = ResourceLocation.tryParse(stringTag.getAsString());
                        if (id != null) resolvedSound = resolveSoundLocation(id);
                    }
                    
                    if (resolvedSound != null) {
                        tracks.add(resolvedSound);
                    }
                }
                
                if (!tracks.isEmpty()) {
                    MIX_TAPE_TRACKS.put(sourceId, tracks);
                    // playTrackAtIndex will set the index and start playback
                    playTrackAtIndex(sourceId, 0);
                    return;
                }
            }
        }

        // Ensure discs are discovered on the client side
        CassetteRegistry.discoverDiscs();

        ResourceLocation recordableId = CassetteItem.getRecordableId(cassetteStack);
        ResourceLocation soundLoc = null;

        var playable = cassetteStack.get(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE);
        if (playable != null) {
            soundLoc = resolveSoundFromPlayable(playable);
        }

        if (soundLoc == null && recordableId != null) {
            soundLoc = resolveSoundLocation(recordableId);
        }

        if (soundLoc != null) {
            // Store as a 1-track mix tape for repeating support
            List<ResourceLocation> tracks = new java.util.ArrayList<>();
            tracks.add(recordableId != null ? recordableId : soundLoc);
            MIX_TAPE_TRACKS.put(sourceId, tracks);
            MIX_TAPE_INDEX.put(sourceId, 0);
            
            playCassetteSound(sourceId, soundLoc, volume);
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
        }
        return null;
    }

    private static ResourceLocation resolveSoundLocation(ResourceLocation recordableId) {
        if (recordableId == null) return null;

        // 0. Check if it's already a valid SoundEvent ID (most reliable if we have it)
        if (net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(recordableId)) {
             return recordableId;
        }

        // Ensure discs are discovered on the client side
        CassetteRegistry.discoverDiscs();

        // 1. Try CassetteRegistry (mapped by Item ID)
        RecordableData data = CassetteRegistry.get(recordableId);
        if (data != null && data.soundEvent() != null) {
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
                        return loc;
                    } catch (Throwable t) {
                        try {
                            java.lang.reflect.Method locationMethod = song.soundEvent().value().getClass().getMethod("location");
                            ResourceLocation loc = (ResourceLocation) locationMethod.invoke(song.soundEvent().value());
                            if (loc != null) {
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
            return ResourceLocation.fromNamespaceAndPath(namespace, "music_disc." + subPath);
        } else if (!path.contains(".")) {
            // If it doesn't look like a direct sound event ID, assume it's a name we need to prefix
            return ResourceLocation.fromNamespaceAndPath(namespace, "music_disc." + path);
        } else {
            return recordableId;
        }
    }

    private static void playCassetteSound(String sourceId, ResourceLocation soundLocation, float volume) {
        net.minecraft.world.entity.player.Player attachedPlayer = null;
        if (!sourceId.startsWith("boombox-")) {
            try {
                UUID playerUUID = UUID.fromString(sourceId);
                attachedPlayer = Minecraft.getInstance().level.getPlayerByUUID(playerUUID);
                if (attachedPlayer != null) {
                    ItemStack main = attachedPlayer.getMainHandItem();
                    ItemStack off = attachedPlayer.getOffhandItem();
                    boolean holdingBoombox = main.is(net.walkman.music.Music.BOOMBOX_ITEM.get()) || off.is(net.walkman.music.Music.BOOMBOX_ITEM.get());
                    if (!holdingBoombox) {
                        attachedPlayer = null; // Walkman is non-positional
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }
        CassetteSoundInstance sound = new CassetteSoundInstance(soundLocation, volume, sourceId, null, attachedPlayer);
        Minecraft.getInstance().getSoundManager().play(sound);
        PLAYING_SOUNDS.put(sourceId, sound);
    }

    private static boolean isSameBoombox(ItemStack stack, net.minecraft.core.BlockPos pos) {
        if (stack.is(net.walkman.music.Music.BOOMBOX_ITEM.get()) && stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA)) {
            var tag = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag();
            if (tag.contains("x") && tag.contains("y") && tag.contains("z")) {
                return tag.getInt("x") == pos.getX() && tag.getInt("y") == pos.getY() && tag.getInt("z") == pos.getZ();
            }
        }
        return false;
    }

    public static void stopMusic(UUID playerUUID) {
        stopMusic(playerUUID.toString());
    }

    public static void stopMusic(String sourceId) {
        SoundInstance sound = PLAYING_SOUNDS.remove(sourceId);
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().stop(sound);
        }
        MIX_TAPE_TRACKS.remove(sourceId);
        MIX_TAPE_INDEX.remove(sourceId);
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        String playerSourceId = mc.player.getUUID().toString();

        // 1. Cleanup phantom boombox sounds (blocks that no longer exist or stopped playing)
        java.util.Iterator<Map.Entry<String, SoundInstance>> it = PLAYING_SOUNDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SoundInstance> entry = it.next();
            String sourceId = entry.getKey();
            if (sourceId.startsWith("boombox-")) {
                String posStr = sourceId.substring("boombox-".length());
                String[] parts = posStr.split("_");
                if (parts.length == 3) {
                    try {
                        net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                        boolean stillThere = mc.level.getBlockEntity(p) instanceof net.walkman.music.BoomboxBlockEntity boombox && boombox.isPlaying() && !boombox.getCassette().isEmpty();
                        
                        // If we are holding this exact boombox, it shouldn't be playing as a block sound
                        ItemStack mainHand = mc.player.getMainHandItem();
                        ItemStack offHand = mc.player.getOffhandItem();
                        boolean isHeld = isSameBoombox(mainHand, p) || isSameBoombox(offHand, p);

                        if (!stillThere || isHeld) {
                            mc.getSoundManager().stop(entry.getValue());
                            it.remove();
                            MIX_TAPE_TRACKS.remove(sourceId);
                            MIX_TAPE_INDEX.remove(sourceId);
                        }
                    } catch (Exception e) {
                        it.remove();
                    }
                }
            } else if (!sourceId.equals(playerSourceId)) {
                // If it's a sound attached to ANOTHER player, check if they are still holding a boombox
                try {
                    UUID otherPlayerUUID = UUID.fromString(sourceId);
                    net.minecraft.world.entity.player.Player otherPlayer = mc.level.getPlayerByUUID(otherPlayerUUID);
                    if (otherPlayer == null || otherPlayer.isRemoved()) {
                        mc.getSoundManager().stop(entry.getValue());
                        it.remove();
                    } else {
                        ItemStack main = otherPlayer.getMainHandItem();
                        ItemStack off = otherPlayer.getOffhandItem();
                        boolean holdingBoombox = (main.is(net.walkman.music.Music.BOOMBOX_ITEM.get()) && isPlayingInHand(main)) || 
                                                 (off.is(net.walkman.music.Music.BOOMBOX_ITEM.get()) && isPlayingInHand(off));
                        if (!holdingBoombox) {
                            mc.getSoundManager().stop(entry.getValue());
                            it.remove();
                        }
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // 2. Check nearby Boomboxes to START music
        net.minecraft.core.BlockPos playerPos = mc.player.blockPosition();
        int radius = 16; // Search radius reduced for performance and logic
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    net.minecraft.core.BlockPos pos = playerPos.offset(x, y, z);
                    if (mc.level.getBlockEntity(pos) instanceof net.walkman.music.BoomboxBlockEntity boombox) {
                        String boomboxId = "boombox-" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
                        if (boombox.isPlaying() && !boombox.getCassette().isEmpty()) {
                            // If we are holding this exact boombox, don't start positional sound
                            ItemStack mainHand = mc.player.getMainHandItem();
                            ItemStack offHand = mc.player.getOffhandItem();
                            if (!isSameBoombox(mainHand, pos) && !isSameBoombox(offHand, pos)) {
                                if (!PLAYING_SOUNDS.containsKey(boomboxId)) {
                                    playMusic(boomboxId, boombox.getCassette(), 1.0f);
                                }
                            }
                        } else {
                            // Explicitly stop if found nearby but NOT playing or no cassette
                            if (PLAYING_SOUNDS.containsKey(boomboxId)) {
                                stopMusic(boomboxId);
                            }
                        }
                    }
                }
            }
        }

        // 3. Handle other players holding Boomboxes
        for (net.minecraft.world.entity.player.Player otherPlayer : mc.level.players()) {
            if (otherPlayer == mc.player) continue;
            String otherId = otherPlayer.getUUID().toString();
            
            ItemStack main = otherPlayer.getMainHandItem();
            ItemStack off = otherPlayer.getOffhandItem();
            ItemStack boomboxStack = ItemStack.EMPTY;
            if (main.is(net.walkman.music.Music.BOOMBOX_ITEM.get()) && isPlayingInHand(main)) boomboxStack = main;
            else if (off.is(net.walkman.music.Music.BOOMBOX_ITEM.get()) && isPlayingInHand(off)) boomboxStack = off;

            if (!boomboxStack.isEmpty()) {
                if (!PLAYING_SOUNDS.containsKey(otherId)) {
                    var tag = boomboxStack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag();
                    ItemStack cassette = ItemStack.parse(mc.level.registryAccess(), tag.getCompound("Cassette")).orElse(ItemStack.EMPTY);
                    if (!cassette.isEmpty()) {
                        playMusic(otherId, cassette, 1.0f);
                    }
                }
            } else {
                // If they were playing but aren't anymore
                if (PLAYING_SOUNDS.containsKey(otherId)) {
                    stopMusic(otherId);
                }
            }
        }

        // 4. Handle handheld music (Walkman or Boombox)
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        
        boolean holdingPlayingBoombox = false;
        ItemStack heldBoomboxCassette = ItemStack.EMPTY;
        String heldBoomboxSourceId = null;

        if (main.is(net.walkman.music.Music.BOOMBOX_ITEM.get()) && isPlayingInHand(main)) {
            var tag = main.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag();
            holdingPlayingBoombox = true;
            heldBoomboxCassette = ItemStack.parse(mc.level.registryAccess(), tag.getCompound("Cassette")).orElse(ItemStack.EMPTY);
            if (tag.contains("x") && tag.contains("y") && tag.contains("z")) {
                heldBoomboxSourceId = "boombox-" + tag.getInt("x") + "_" + tag.getInt("y") + "_" + tag.getInt("z");
            }
        }
        if (!holdingPlayingBoombox && off.is(net.walkman.music.Music.BOOMBOX_ITEM.get()) && isPlayingInHand(off)) {
            var tag = off.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag();
            holdingPlayingBoombox = true;
            heldBoomboxCassette = ItemStack.parse(mc.level.registryAccess(), tag.getCompound("Cassette")).orElse(ItemStack.EMPTY);
            if (tag.contains("x") && tag.contains("y") && tag.contains("z")) {
                heldBoomboxSourceId = "boombox-" + tag.getInt("x") + "_" + tag.getInt("y") + "_" + tag.getInt("z");
            }
        }

        ItemStack walkmanWithCassette = ItemStack.EMPTY;
        // Check main inventory, hotbar, armor, and offhand
        for (ItemStack stack : mc.player.getInventory().items) {
            if (stack.getItem() instanceof CassettePlayerItem && hasCassette(stack, mc)) {
                walkmanWithCassette = stack;
                break;
            }
        }
        if (walkmanWithCassette.isEmpty()) {
            for (ItemStack stack : mc.player.getInventory().offhand) {
                if (stack.getItem() instanceof CassettePlayerItem && hasCassette(stack, mc)) {
                    walkmanWithCassette = stack;
                    break;
                }
            }
        }
        if (walkmanWithCassette.isEmpty()) {
            for (ItemStack stack : mc.player.getInventory().armor) {
                if (stack.getItem() instanceof CassettePlayerItem && hasCassette(stack, mc)) {
                    walkmanWithCassette = stack;
                    break;
                }
            }
        }
        if (walkmanWithCassette.isEmpty() && mc.player.containerMenu != null) {
            ItemStack carried = mc.player.containerMenu.getCarried();
            if (carried.getItem() instanceof CassettePlayerItem && hasCassette(carried, mc)) {
                walkmanWithCassette = carried;
            }
        }

        if (holdingPlayingBoombox && !heldBoomboxCassette.isEmpty()) {
            // Ensure positional sound for this boombox is stopped
            if (heldBoomboxSourceId != null) {
                stopMusic(heldBoomboxSourceId);
            }
            // Start handheld music if not already playing
            // For a Boombox, we want it to be positional even when handheld.
            SoundInstance current = PLAYING_SOUNDS.get(playerSourceId);
            boolean isPositional = (current instanceof CassetteSoundInstance csi) && csi.attachedPlayer != null;
            
            if (!PLAYING_SOUNDS.containsKey(playerSourceId) || !isPositional) {
                playMusic(playerSourceId, heldBoomboxCassette, 1.0f);
            }
        } else if (!walkmanWithCassette.isEmpty()) {
            // If we have music playing on player but it's positional (Boombox style), 
            // and we are now only holding a Walkman, we should restart it as non-positional.
            SoundInstance current = PLAYING_SOUNDS.get(playerSourceId);
            boolean isPositional = (current instanceof CassetteSoundInstance csi) && csi.attachedPlayer != null;
            
            if (isPositional) {
                stopMusic(playerSourceId);
                playMusic(playerSourceId, walkmanWithCassette, 1.0f);
            }
        } else {
            // Neither holding a playing boombox nor have a walkman with a cassette
            stopMusic(playerSourceId);
        }

        // 5. Check for sound completion
        SoundInstance playing = PLAYING_SOUNDS.get(playerSourceId);
        if (playing instanceof CassetteSoundInstance cassetteSound) {
            if (cassetteSound.tickCount > 5 && (cassetteSound.isStopped() || !mc.getSoundManager().isActive(cassetteSound))) {
                handleSoundEnd(playerSourceId);
            }
        }
    }

    private static boolean isPlayingInHand(ItemStack stack) {
        if (stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA)) {
            var tag = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag();
            return tag.getBoolean("IsPlaying") && tag.contains("Cassette");
        }
        return false;
    }

    private static boolean hasCassette(ItemStack walkman, Minecraft mc) {
        if (walkman.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var tag = walkman.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("Cassette")) {
                var invTag = tag.getCompound("Cassette");
                if (invTag.contains("Items", net.minecraft.nbt.Tag.TAG_LIST)) {
                    var items = invTag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
                    for (int i = 0; i < items.size(); i++) {
                        var itemTag = items.getCompound(i);
                        if (itemTag.getByte("Slot") == 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
