package net.lucy.mixin;

import net.minecraft.recipe.BrewingRecipeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(BrewingRecipeRegistry.class)
public interface BrewingRecipeRegistryAccessor {

    @Accessor("POTION_RECIPES")
    static List<?> getPotionRecipes() {
        throw new AssertionError();
    }

    @Accessor("ITEM_RECIPES")
    static List<?> getItemRecipes() {
        throw new AssertionError();
    }
}