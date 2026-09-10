package net.walkman.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.walkman.music.Music;
import net.walkman.walkman.CassettePlayerMenu;
import net.walkman.cassette.CassetteCaseMenu;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = 
            DeferredRegister.create(Registries.MENU, Music.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CassettePlayerMenu>> CASSETTE_PLAYER_MENU =
            MENUS.register("cassette_player_menu", 
                    () -> IMenuTypeExtension.create(CassettePlayerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CassetteCaseMenu>> CASSETTE_CASE_MENU =
            MENUS.register("cassette_case_menu",
                    () -> IMenuTypeExtension.create(CassetteCaseMenu::new));
}
