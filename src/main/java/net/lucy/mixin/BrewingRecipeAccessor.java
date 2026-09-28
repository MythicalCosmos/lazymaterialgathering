package net.lucy.mixin;

import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.recipe.BrewingRecipeRegistry$Recipe")
public interface BrewingRecipeAccessor {

    @Accessor("input")
    Object lmg$getInput();

    @Accessor("ingredient")
    Ingredient lmg$getIngredient();

    @Accessor("output")
    Object lmg$getOutput();
}