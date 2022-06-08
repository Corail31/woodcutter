package ovh.corail.woodcutter.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModRecipeTypes {
    public static RecipeType<WoodcuttingRecipe> WOODCUTTING = Helper.unsafeNullCast();

    @SubscribeEvent
    public static void registerRecipeType(final RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_TYPES)) {
            WOODCUTTING = new RecipeType<>() {
                public String toString() {
                    return MOD_ID + ":" + "woodcutting";
                }
            };
            event.register(ForgeRegistries.Keys.RECIPE_TYPES, new ResourceLocation(MOD_ID, "woodcutting"), () -> WOODCUTTING);
        }
    }
}
