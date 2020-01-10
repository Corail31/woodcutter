package ovh.corail.woodcutter.registry;

import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModRecipeTypes {
    public static final IRecipeType<WoodcuttingRecipe> WOODCUTTING = registerRecipeType("woodcutting");

    private static <T extends IRecipe<?>> IRecipeType<T> registerRecipeType(final String key) {
        return Registry.register(Registry.RECIPE_TYPE, new ResourceLocation(MOD_ID, key), new IRecipeType<T>() {
            public String toString() {
                return MOD_ID + ":" + key;
            }
        });
    }
}
