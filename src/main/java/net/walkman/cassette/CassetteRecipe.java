package net.walkman.cassette;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.walkman.music.Music;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class CassetteRecipe extends CustomRecipe {
    public static final RecipeSerializer<CassetteRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(CassetteRecipe::new);

    static {
        System.out.println("[DEBUG_LOG] CassetteRecipe class loaded.");
    }

    private static final TagKey<Item> MUSIC_DISCS = TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("music_discs"));

    public CassetteRecipe(net.minecraft.world.item.crafting.CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {

        if (CassetteRegistry.getAll().isEmpty()) {
            CassetteRegistry.discoverDiscs();
        }

        ItemStack tape = ItemStack.EMPTY;
        ItemStack disc = ItemStack.EMPTY;
        int count = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            count++;

            boolean isTape = isTape(stack);
            boolean isDisc = isMusicDisc(stack);

            if (isTape) {
                if (!tape.isEmpty()) {
                    return false;
                }
                tape = stack;
            } else if (isDisc) {
                if (!disc.isEmpty()) {
                    return false;
                }
                disc = stack;
            } else {
                return false;
            }
        }

        boolean match = !tape.isEmpty() && !disc.isEmpty() && count == 2;
        if (match) {
            System.out.println("[DEBUG_LOG] Recipe Match SUCCESS with " + BuiltInRegistries.ITEM.getKey(tape.getItem()) + " and " + BuiltInRegistries.ITEM.getKey(disc.getItem()));
        }
        return match;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack disc = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && isMusicDisc(stack)) {
                disc = stack;
                break;
            }
        }

        if (disc.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(Music.CASSETTE.get());
        

        var playable = disc.get(DataComponents.JUKEBOX_PLAYABLE);
        if (playable == null) {
            playable = disc.getItem().components().get(DataComponents.JUKEBOX_PLAYABLE);
        }
        
        if (playable != null) {
            result.set(DataComponents.JUKEBOX_PLAYABLE, playable);
        }


        ResourceLocation discId = BuiltInRegistries.ITEM.getKey(disc.getItem());
        String name = disc.getHoverName().getString();
        

        if (name.startsWith("Music Disc - ")) {
            name = name.substring("Music Disc - ".length());
        }

        CassetteItem.setRecordableId(result, discId);
        CassetteItem.setDisplayName(result, name);

        return result;
    }

    private boolean isTape(ItemStack stack) {
        return stack.is(Music.BLANK_CASSETTE.get()) || stack.is(Music.CASSETTE.get());
    }

    private boolean isMusicDisc(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (isTape(stack)) return false;

        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);


        if (stack.is(MUSIC_DISCS)) return true;


        if (stack.has(DataComponents.JUKEBOX_PLAYABLE) || item.components().has(DataComponents.JUKEBOX_PLAYABLE)) return true;


        for (RecordableData data : CassetteRegistry.getAll().values()) {
            if (data.discItem() != null && data.discItem().equals(id)) return true;
        }


        String path = id.getPath().toLowerCase();
        if ((path.contains("music_disc") || path.contains("record") || path.contains("disc")) 
                && !id.getNamespace().equals(Music.MODID)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
