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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.walkman.cassette.CassetteRegistry;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.CompoundTagArgument;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;


import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredBlock;

@Mod(Music.MODID)
public class Music {

    public static final String MODID = "cassette_player_plus";

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);


    public static final DeferredBlock<Block> BOOMBOX =
            BLOCKS.register("boombox",
                    () -> new BoomboxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<BoomboxBlockEntity>> BOOMBOX_BE =
            BLOCK_ENTITIES.register("boombox", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(BoomboxBlockEntity::new, BOOMBOX.get()).build(null));

    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> CASSETTE_TAPE_SOUND_EFFECT =
            SOUND_EVENTS.register("cassette_tape_sound_effect", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "cassette_tape_sound_effect")));

    public static final DeferredItem<BlockItem> BOOMBOX_ITEM =
            ITEMS.registerSimpleBlockItem("boombox", BOOMBOX);

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
                    .title(Component.translatable("itemGroup.cassette_player_plus.cassette_tab"))
                    .icon(() -> CASSETTE_PLAYER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(BOOMBOX_ITEM.get());
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

        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(Config::onLoad);

        // 2️⃣ Register registries
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
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
            Commands.literal("cassette_player_plus")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("config")
                    .then(Commands.literal("enableBoomboxDancing")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(context -> {
                                boolean value = BoolArgumentType.getBool(context, "value");
                                Config.setEnableBoomboxDancing(value);
                                context.getSource().sendSuccess(() -> Component.literal("Boombox dancing is now " + (value ? "enabled" : "disabled")), true);
                                return 1;
                            })
                        )
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> Component.literal("Boombox dancing is currently " + (Config.enableBoomboxDancing ? "enabled" : "disabled")), false);
                            return 1;
                        })
                    )
                )
        );

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
