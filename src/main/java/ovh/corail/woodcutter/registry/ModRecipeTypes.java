package ovh.corail.woodcutter.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModRecipeTypes {
    public static final RecipeType<WoodcuttingRecipe> WOODCUTTING = registerRecipeType("woodcutting");

    private static <T extends Recipe<?>> RecipeType<T> registerRecipeType(final String key) {
        return Registry.register(Registry.RECIPE_TYPE, new ResourceLocation(MOD_ID, key), new RecipeType<T>() {
            public String toString() {
                return MOD_ID + ":" + key;
            }
        });
    }
}
