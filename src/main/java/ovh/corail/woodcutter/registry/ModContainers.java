package ovh.corail.woodcutter.registry;

import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ObjectHolder;
import ovh.corail.woodcutter.inventory.WoodcutterContainer;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@ObjectHolder(MOD_ID)
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModContainers {
    public static final ContainerType<WoodcutterContainer> WOODCUTTER = new ContainerType<>(WoodcutterContainer::new);

    @SubscribeEvent
    public static void registerContainers(final RegistryEvent.Register<ContainerType<?>> event) {
        event.getRegistry().register(WOODCUTTER.setRegistryName(MOD_ID, "woodcutter"));
    }
}
