package ovh.corail.woodcutter.registry;

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModRecipeTypes {
    public static final RecipeType<WoodcuttingRecipe> WOODCUTTING = registerRecipeType("woodcutting");

    private static <T extends Recipe<?>> RecipeType<T> registerRecipeType(final String key) {
        return Registry.register(Registry.RECIPE_TYPE, new Identifier(MOD_ID, key), new RecipeType<T>() {
            public String toString() {
                return MOD_ID + ":" + key;
            }
        });
    }
}
