package ovh.corail.woodcutter.registry;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModRecipeSerializers {
    public static final RecipeSerializer<WoodcuttingRecipe> WOODCUTTING = new SingleItemRecipe.Serializer<>(WoodcuttingRecipe::new) {};

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onRegisterSerializers(final RegistryEvent.Register<RecipeSerializer<?>> event) {
        event.getRegistry().register(WOODCUTTING.setRegistryName(new ResourceLocation(MOD_ID, "woodcutting")));
    }
}
