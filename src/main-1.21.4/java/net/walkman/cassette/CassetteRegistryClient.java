package net.walkman.cassette;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

public class CassetteRegistryClient {
    public static ResourceLocation getSoundLocation(ResourceLocation songId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                var songRegistry = mc.level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG);
                var songHolder = songRegistry.get(net.minecraft.resources.ResourceKey.create(Registries.JUKEBOX_SONG, songId));
                if (songHolder.isPresent()) {
                    var song = songHolder.get().value();
                    try {
                        return song.soundEvent().value().location();
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
