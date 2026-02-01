package net.walkman.cassette;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

public class CassetteRegistryClient {
    public static ResourceLocation getSoundLocation(ResourceLocation songId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                var songRegistry = mc.level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG);
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
            }
        } catch (Throwable t) {}
        return null;
    }
}
