package ovh.corail.woodcutter.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModRecipeSerializers {
    public static RecipeSerializer<WoodcuttingRecipe> WOODCUTTING = Helper.unsafeNullCast();


    static void onRegister(final RegisterEvent event) {
        WOODCUTTING = new SingleItemRecipe.Serializer<>(WoodcuttingRecipe::new) {};
        event.register(ForgeRegistries.Keys.RECIPE_SERIALIZERS, new ResourceLocation(MOD_ID, "woodcutting"), () -> WOODCUTTING);
    }
}
