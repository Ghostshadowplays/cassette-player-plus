package net.walkman.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.walkman.cassette.RecordableDataLoader;

public class ModDataLoaders {

    public static void register(IEventBus forgeBus) {
        forgeBus.addListener(ModDataLoaders::onAddReloadListeners);
    }

    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        System.out.println("[DEBUG_LOG] Registering RecordableDataLoader");
        event.addListener(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.walkman.music.Music.MODID, "recordables"), new RecordableDataLoader());
    }
}
