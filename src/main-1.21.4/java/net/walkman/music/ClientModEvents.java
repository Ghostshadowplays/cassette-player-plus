package net.walkman.music;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CASSETTE_PLAYER_MENU.get(), CassettePlayerScreen::new);
        event.register(ModMenus.CASSETTE_CASE_MENU.get(), net.walkman.cassette.CassetteCaseScreen::new);
    }
}
