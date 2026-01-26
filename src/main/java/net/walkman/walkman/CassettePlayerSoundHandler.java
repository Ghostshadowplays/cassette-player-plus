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
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class CassettePlayerSoundHandler {
    private static final Map<UUID, SoundInstance> PLAYING_SOUNDS = new HashMap<>();

    public static class CassetteSoundInstance extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
        private float targetVolume;

        public CassetteSoundInstance(ResourceLocation soundLocation, float volume) {
            super(SoundEvent.createVariableRangeEvent(soundLocation), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.volume = volume;
            this.targetVolume = volume;
            this.looping = false;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        public void setVolume(float volume) {
            this.targetVolume = volume;
        }

        @Override
        public void tick() {
            this.volume = this.targetVolume;
            if (Minecraft.getInstance().player == null || Minecraft.getInstance().player.isRemoved()) {
                this.stop();
            }
        }
    }

    public static void setVolume(UUID playerUUID, float volume) {
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
        stopMusic(playerUUID);

        ResourceLocation soundLoc = null;
        ResourceLocation recordableId = CassetteItem.getRecordableId(cassetteStack);

        var playable = cassetteStack.get(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE);
        if (playable != null) {
            try {
                var songKey = playable.song().key();
                if (songKey != null) {
                    ResourceLocation songId = songKey.location();
                    soundLoc = ResourceLocation.fromNamespaceAndPath(songId.getNamespace(), "music_disc." + songId.getPath());

                    if (Minecraft.getInstance().level != null) {
                        var songRegistry = Minecraft.getInstance().level.registryAccess().registry(net.minecraft.core.registries.Registries.JUKEBOX_SONG);
                        if (songRegistry.isPresent()) {
                            var song = songRegistry.get().get(songId);
                            if (song != null) {
                                var soundHolder = song.soundEvent();
                                if (soundHolder != null) {
                                    if (soundHolder.unwrapKey().isPresent()) {
                                        soundLoc = soundHolder.unwrapKey().get().location();
                                    } else if (soundHolder.isBound()) {
                                        soundLoc = soundHolder.value().getLocation();
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (soundLoc == null && recordableId != null) {
            RecordableData data = CassetteRegistry.get(recordableId);
            if (data != null && data.soundEvent() != null) {
                soundLoc = data.soundEvent();
            }
        }

        if (soundLoc == null && recordableId != null) {
            String path = recordableId.getPath();
            String namespace = recordableId.getNamespace();

            if (path.startsWith("music_disc_")) {
                soundLoc = ResourceLocation.fromNamespaceAndPath(namespace, "music_disc." + path.substring("music_disc_".length()));
            } else if (!path.contains(".")) {
                String targetNamespace = (namespace.equals("music") || namespace.equals("minecraft")) ? "minecraft" : namespace;
                soundLoc = ResourceLocation.fromNamespaceAndPath(targetNamespace, "music_disc." + path);
            } else {
                soundLoc = recordableId;
            }
        }

        if (soundLoc != null) {
            playCassetteSound(playerUUID, soundLoc, volume);
        }
    }

    private static void playCassetteSound(UUID playerUUID, ResourceLocation soundLocation, float volume) {
        CassetteSoundInstance sound = new CassetteSoundInstance(soundLocation, volume);
        Minecraft.getInstance().getSoundManager().play(sound);
        PLAYING_SOUNDS.put(playerUUID, sound);
    }

    public static void stopMusic(UUID playerUUID) {
        SoundInstance sound = PLAYING_SOUNDS.remove(playerUUID);
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().stop(sound);
        }
    }


    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        UUID uuid = mc.player.getUUID();

        if (!PLAYING_SOUNDS.containsKey(uuid)) return;

        boolean hasPlayer = false;
        for (ItemStack stack : mc.player.getInventory().items) {
            if (stack.getItem() instanceof CassettePlayerItem) {
                hasPlayer = true;
                break;
            }
        }

        if (!hasPlayer) {
            stopMusic(uuid);
        }
    }
}
