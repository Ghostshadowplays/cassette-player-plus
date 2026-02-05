package net.walkman.music;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.walkman.cassette.CassetteItem;
import net.walkman.walkman.CassettePlayerScreen;
import net.walkman.walkman.WalkmanConfig;
import net.walkman.registry.ModMenus;

public class ClientModEvents {
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientModEvents::onClientSetup);
        modEventBus.addListener(ClientModEvents::onRegisterMenuScreens);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        WalkmanConfig.load();
        event.enqueueWork(() -> {
            ItemProperties.register(Music.CASSETTE.get(), 
                ResourceLocation.fromNamespaceAndPath(Music.MODID, "texture_index"), 
                (stack, level, entity, seed) -> (float)CassetteItem.getTextureIndex(stack));

            ItemProperties.register(Music.CASSETTE.get(), 
                ResourceLocation.fromNamespaceAndPath(Music.MODID, "visual_set"), 
                (stack, level, entity, seed) -> WalkmanConfig.getVisualSet() == WalkmanConfig.VisualSet.RETRO ? 1.0f : 0.0f);

            ItemProperties.register(Music.CASSETTE_PLAYER.get(), 
                ResourceLocation.fromNamespaceAndPath(Music.MODID, "visual_set"), 
                (stack, level, entity, seed) -> WalkmanConfig.getVisualSet() == WalkmanConfig.VisualSet.RETRO ? 1.0f : 0.0f);

            ItemProperties.register(Music.BLANK_CASSETTE.get(), 
                ResourceLocation.fromNamespaceAndPath(Music.MODID, "visual_set"), 
                (stack, level, entity, seed) -> WalkmanConfig.getVisualSet() == WalkmanConfig.VisualSet.RETRO ? 1.0f : 0.0f);
        });
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CASSETTE_PLAYER_MENU.get(), CassettePlayerScreen::new);
        event.register(ModMenus.CASSETTE_CASE_MENU.get(), net.walkman.cassette.CassetteCaseScreen::new);
    }
}
