package net.walkman.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.walkman.music.Music;
import net.walkman.cassette.CassetteRecipe;

public class ModItems {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = 
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Music.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CassetteRecipe>> CASSETTE_RECIPE = 
            RECIPE_SERIALIZERS.register("cassette_recipe", () -> CassetteRecipe.SERIALIZER);
}
