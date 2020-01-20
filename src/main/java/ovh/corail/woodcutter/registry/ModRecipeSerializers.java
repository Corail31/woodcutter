package ovh.corail.woodcutter.registry;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModRecipeSerializers {
    public static final RecipeSerializer<WoodcuttingRecipe> WOODCUTTING = new WoodcuttingRecipe.CustomSerializer<>(WoodcuttingRecipe::new);

    public static void init() {
        Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(MOD_ID, "woodcutting"), WOODCUTTING);
    }
}
