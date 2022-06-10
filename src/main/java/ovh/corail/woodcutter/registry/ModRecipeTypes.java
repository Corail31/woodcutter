package ovh.corail.woodcutter.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModRecipeTypes {
    public static RecipeType<WoodcuttingRecipe> WOODCUTTING = Helper.unsafeNullCast();

    static void onRegister(final RegisterEvent event) {
        WOODCUTTING = new RecipeType<>() {
                public String toString() {
                    return MOD_ID + ":" + "woodcutting";
                }
            };
        event.register(ForgeRegistries.Keys.RECIPE_TYPES, new ResourceLocation(MOD_ID, "woodcutting"), () -> WOODCUTTING);
    }
}
