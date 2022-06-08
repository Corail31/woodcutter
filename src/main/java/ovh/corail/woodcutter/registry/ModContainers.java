package ovh.corail.woodcutter.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.inventory.WoodcutterContainer;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModContainers {
    public static MenuType<WoodcutterContainer> WOODCUTTER = Helper.unsafeNullCast();

    @SubscribeEvent
    public static void registerContainers(final RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.CONTAINER_TYPES)) {
            WOODCUTTER = new MenuType<>(WoodcutterContainer::new);
            event.register(ForgeRegistries.Keys.CONTAINER_TYPES, new ResourceLocation(MOD_ID, "woodcutter"), () -> WOODCUTTER);
        }
    }
}
