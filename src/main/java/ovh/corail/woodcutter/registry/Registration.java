package ovh.corail.woodcutter.registry;

import com.google.common.reflect.Reflection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import static net.minecraftforge.registries.ForgeRegistries.Keys.*;
import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;
import static ovh.corail.woodcutter.WoodCutterMod.MOD_NAME;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Registration {
    public static final ResourceLocation TAB_ID = new ResourceLocation(MOD_ID);

    @SubscribeEvent
    public static void onRegistration(final RegisterEvent event) {
        if (event.getRegistryKey().equals(BLOCKS)) {
            ModBlocks.onRegisterBlocks(event);
        } else if (event.getRegistryKey().equals(ITEMS)) {
            ModBlocks.onRegisterBlockItems(event);
            Reflection.initialize(ModStats.class);
        } else if (event.getRegistryKey().equals(ForgeRegistries.Keys.MENU_TYPES)) {
            ModMenuTypes.onRegister(event);
        } else if (event.getRegistryKey().equals(ForgeRegistries.Keys.POI_TYPES)) {
            ModPointOfInterestTypes.onRegister(event);
        } else if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_SERIALIZERS)) {
            ModRecipeSerializers.onRegister(event);
        } else if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_TYPES)) {
            ModRecipeTypes.onRegister(event);
        }
    }

    @SubscribeEvent
    public static void onCreativeTab(CreativeModeTabEvent.Register event) {
        event.registerCreativeModeTab(TAB_ID, builder -> builder.title(Component.literal(MOD_NAME)).icon(ModBlocks::createRandomStack).displayItems((featureFlagSet, toAdd, flag) -> ModBlocks.WOODCUTTER_ITEMS.forEach(toAdd::accept)).build());
    }
}
