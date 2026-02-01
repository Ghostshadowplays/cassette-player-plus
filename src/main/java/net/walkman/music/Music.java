package net.walkman.music;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.walkman.registry.ModDataLoaders;
import net.walkman.registry.ModMenus;
import net.walkman.registry.ModItems;
import net.walkman.cassette.CassetteItem;
import net.walkman.walkman.CassettePlayerItem;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.walkman.cassette.CassetteRegistry;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Music.MODID)
public class Music {

    public static final String MODID = "music";


    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);


    public static final DeferredItem<Item> BLANK_CASSETTE =
            ITEMS.registerSimpleItem("blank_cassette",
                    new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> CASSETTE_PLAYER =
            ITEMS.register("cassette_player",
                    () -> new CassettePlayerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CASSETTE =
            ITEMS.register("cassette",
                    () -> new CassetteItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CASSETTE_CASE =
            ITEMS.register("cassette_case",
                    () -> new net.walkman.cassette.CassetteCaseItem(new Item.Properties().stacksTo(1)));


    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CASSETTE_TAB =
            CREATIVE_TABS.register("cassette_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.music.cassette_tab"))
                    .icon(() -> CASSETTE_PLAYER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(CASSETTE_PLAYER.get());
                        output.accept(CASSETTE_CASE.get());
                        output.accept(BLANK_CASSETTE.get());


                        CassetteRegistry.getAll().forEach((id, data) -> {
                            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(CASSETTE.get());
                            CassetteItem.setRecordableId(stack, id);


                            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(data.discItem());
                            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                var discStack = new net.minecraft.world.item.ItemStack(item);
                                var playable = discStack.get(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE);
                                if (playable != null) {
                                    stack.set(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE, playable);
                                }
                            }
                            
                            // Always use the data's display name if available, even if no disc item
                            if (data.displayName() != null && !data.displayName().isEmpty()) {
                                CassetteItem.setDisplayName(stack, data.displayName());
                            }
                            
                            CassetteItem.assignRandomTexture(stack);

                            output.accept(stack);
                        });
                    })
                    .build());


    public Music(IEventBus modEventBus, ModContainer modContainer) {
        System.out.println("[DEBUG_LOG] Music mod constructor started!");

        // 2️⃣ Register registries
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModItems.RECIPE_SERIALIZERS.register(modEventBus);
        System.out.println("[DEBUG_LOG] Registries registered to mod bus.");

        // 3️⃣ Register reload listener (Forge bus)
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ModDataLoaders::onAddReloadListeners);

        // 4️⃣ Register commands
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // 5️⃣ Client setup
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            ClientModEvents.init(modEventBus);
        }

        // 6️⃣ Common setup
        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            System.out.println("[DEBUG_LOG] Common setup running");
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("givecassette")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("id", ResourceLocationArgument.id())
                    .suggests((context, builder) -> {
                        CassetteRegistry.getAll().keySet().forEach(id -> builder.suggest(id.toString()));
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        var source = context.getSource();
                        var id = ResourceLocationArgument.getId(context, "id");
                        var player = source.getPlayerOrException();
                        
                        if (CassetteRegistry.get(id) == null) {
                            source.sendFailure(Component.literal("Unknown cassette ID: " + id));
                            return 0;
                        }

                        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(CASSETTE.get());
                        CassetteItem.setRecordableId(stack, id);
                        
                        if (player.getInventory().add(stack)) {
                            CassetteItem.assignRandomTexture(stack);
                            source.sendSuccess(() -> Component.literal("Gave 1 cassette (" + id + ") to " + player.getScoreboardName()), true);
                            return 1;
                        } else {
                            source.sendFailure(Component.literal("Inventory full"));
                            return 0;
                        }
                    }))
        );
    }
}
