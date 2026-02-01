package net.walkman.cassette;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.walkman.music.Music;

public class CassetteRecipe extends CustomRecipe {
    public static final RecipeSerializer<CassetteRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(CassetteRecipe::new);

    static {
        System.out.println("[DEBUG_LOG] CassetteRecipe class loaded.");
    }

    private static final TagKey<Item> MUSIC_DISCS = TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("music_discs"));

    public CassetteRecipe(net.minecraft.world.item.crafting.CraftingBookCategory category) {
        super(category);
    }

    private boolean isMixTape(ItemStack stack) {
        if (!stack.is(Music.CASSETTE.get())) return false;
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            return tag.getBoolean("IsMixTape");
        }
        return false;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!CassetteRegistry.hasDiscoveryAttempted()) {
            CassetteRegistry.discoverDiscs();
        }

        int count = 0;
        int cassetteCount = 0;
        ItemStack disc = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            count++;

            if (isTape(stack)) {
                if (isMixTape(stack)) return false; // Don't allow mix tapes as input for further mix tapes
                cassetteCount++;
            } else if (isMusicDisc(stack)) {
                if (!disc.isEmpty()) return false;
                disc = stack;
            } else {
                return false;
            }
        }

        // 1. Original recipe: 1 tape + 1 disc
        if (cassetteCount == 1 && !disc.isEmpty() && count == 2) {
            return true;
        }

        // 2. New recipe: 2-5 cassettes -> Mix Tape
        if (cassetteCount >= 2 && cassetteCount <= 5 && disc.isEmpty() && count == cassetteCount) {
            return true;
        }

        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack disc = ItemStack.EMPTY;
        java.util.List<ItemStack> cassettes = new java.util.ArrayList<>();

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (isMusicDisc(stack)) {
                disc = stack;
            } else if (isTape(stack)) {
                cassettes.add(stack);
            }
        }

        if (cassettes.size() == 1 && !disc.isEmpty()) {
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
            
            CassetteItem.setRecordableId(result, discId);
            CassetteItem.setDisplayName(result, name);

            return result;
        } else if (cassettes.size() >= 2 && cassettes.size() <= 5 && disc.isEmpty()) {
            // Mix Tape logic
            ItemStack result = new ItemStack(Music.CASSETTE.get());
            
            java.util.List<String> names = new java.util.ArrayList<>();
            net.minecraft.nbt.ListTag tracks = new net.minecraft.nbt.ListTag();
            
            for (ItemStack cassette : cassettes) {
                ResourceLocation id = CassetteItem.getRecordableId(cassette);
                if (id != null) {
                    CompoundTag trackTag = new CompoundTag();
                    trackTag.putString("id", id.toString());
                    
                    var playableComponent = cassette.get(DataComponents.JUKEBOX_PLAYABLE);
                    if (playableComponent == null) {
                        playableComponent = cassette.getItem().components().get(DataComponents.JUKEBOX_PLAYABLE);
                    }
                    
                    if (playableComponent != null) {
                        try {
                            Object holder = playableComponent.song();
                            ResourceLocation songId = null;
                            // Use reflection to get Song ID, same as in CassetteRegistry
                            try {
                                java.lang.reflect.Method unwrapKeyMethod = holder.getClass().getMethod("unwrapKey");
                                java.util.Optional<?> optionalKey = (java.util.Optional<?>) unwrapKeyMethod.invoke(holder);
                                if (optionalKey.isPresent()) {
                                    Object keyObj = optionalKey.get();
                                    java.lang.reflect.Method locationMethod = keyObj.getClass().getMethod("location");
                                    songId = (ResourceLocation) locationMethod.invoke(keyObj);
                                }
                            } catch (Exception ignored) {
                                try {
                                    java.lang.reflect.Method keyMethod = holder.getClass().getMethod("key");
                                    Object keyObj = keyMethod.invoke(holder);
                                    java.lang.reflect.Method locationMethod = keyObj.getClass().getMethod("location");
                                    songId = (ResourceLocation) locationMethod.invoke(keyObj);
                                } catch (Exception ignored2) {
                                    try {
                                        java.lang.reflect.Method unwrapMethod = holder.getClass().getMethod("unwrap");
                                        com.mojang.datafixers.util.Either<?, ?> either = (com.mojang.datafixers.util.Either<?, ?>) unwrapMethod.invoke(holder);
                                        java.util.Optional<?> left = (java.util.Optional<?>) either.left();
                                        if (left.isPresent()) {
                                            Object keyObj = left.get();
                                            java.lang.reflect.Method locationMethod = keyObj.getClass().getMethod("location");
                                            songId = (ResourceLocation) locationMethod.invoke(keyObj);
                                        }
                                    } catch (Exception ignored3) {}
                                }
                            }
                            
                            if (songId != null) {
                                trackTag.putString("song", songId.toString());
                                // Try to resolve actual sound ID using registries provided in assemble
                                try {
                                    var songRegistry = registries.lookupOrThrow(Registries.JUKEBOX_SONG);
                                    var songHolder = songRegistry.get(ResourceKey.create(Registries.JUKEBOX_SONG, songId));
                                    if (songHolder.isPresent()) {
                                        var song = songHolder.get().value();
                                        ResourceLocation soundId = song.soundEvent().value().getLocation();
                                        if (soundId != null) {
                                            trackTag.putString("sound", soundId.toString());
                                        }
                                    }
                                } catch (Exception e) {}
                            }
                        } catch (Exception ignored) {}
                    }
                    
                    tracks.add(trackTag);
                    
                    if (names.size() < 3) {
                        RecordableData data = CassetteRegistry.get(id);
                        String trackName;
                        if (data != null) {
                            trackName = data.displayName();
                        } else {
                            // Fallback to item name if possible
                            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                trackName = item.getName(new ItemStack(item)).getString();
                            } else {
                                trackName = id.getPath();
                            }
                        }
                        
                        if (trackName.startsWith("Music Disc - ")) {
                            trackName = trackName.substring("Music Disc - ".length());
                        } else if (trackName.startsWith("Music Disc ")) {
                            trackName = trackName.substring("Music Disc ".length());
                        }
                        names.add(trackName);
                    }

                    if (!result.has(DataComponents.JUKEBOX_PLAYABLE)) {
                        if (playableComponent != null) {
                            result.set(DataComponents.JUKEBOX_PLAYABLE, playableComponent);
                            CassetteItem.setRecordableId(result, id);
                        }
                    }
                }
            }

            String combinedName = String.join(", ", names);
            if (names.size() < cassettes.size()) {
                combinedName += "...";
            }
            
            CassetteItem.setDisplayName(result, "Mix: " + combinedName);
            CassetteItem.assignRandomTexture(result);
            
            result.update(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY,
                customData -> customData.update(tag -> {
                    tag.putBoolean("IsMixTape", true);
                    tag.put("Tracks", tracks);
                }));

            return result;
        }

        return ItemStack.EMPTY;
    }

    private boolean isTape(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Music.BLANK_CASSETTE.get() || item == Music.CASSETTE.get();
    }

    private boolean isMusicDisc(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        
        // 1. Check if it's our mod's cassette (don't treat it as a disc)
        if (item == Music.BLANK_CASSETTE.get() || item == Music.CASSETTE.get()) return false;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);

        // 2. Check JukeboxPlayable component (Primary method for 1.21.1)
        if (stack.has(DataComponents.JUKEBOX_PLAYABLE) || item.components().has(DataComponents.JUKEBOX_PLAYABLE)) {
            return true;
        }

        // 3. Check music_discs tag
        if (stack.is(MUSIC_DISCS)) {
            return true;
        }

        // 4. Check our registry
        for (RecordableData data : CassetteRegistry.getAll().values()) {
            if (data.discItem() != null && data.discItem().equals(id)) {
                return true;
            }
        }

        // 5. Fallback path check
        String path = id.getPath().toLowerCase();
        if ((path.contains("music_disc") || path.contains("record") || path.contains("disc") || path.contains("disk")) 
                && !path.contains("fragment") && !path.contains("part")
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
