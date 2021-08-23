package ovh.corail.woodcutter.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ovh.corail.woodcutter.inventory.WoodcutterContainer;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModContainers {
    public static final MenuType<WoodcutterContainer> WOODCUTTER = new MenuType<>(WoodcutterContainer::new);

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void registerContainers(final RegistryEvent.Register<MenuType<?>> event) {
        event.getRegistry().register(WOODCUTTER.setRegistryName(MOD_ID, "woodcutter"));
    }
}
