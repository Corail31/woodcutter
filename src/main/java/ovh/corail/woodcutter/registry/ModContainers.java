package ovh.corail.woodcutter.registry;

import net.minecraft.container.ContainerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import ovh.corail.woodcutter.inventory.WoodcutterContainer;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModContainers {
    public static final ContainerType<WoodcutterContainer> WOODCUTTER = new ContainerType<>(WoodcutterContainer::new);

    public static void init() {
        Registry.register(Registry.CONTAINER, new Identifier(MOD_ID, "woodcutter"), WOODCUTTER);
    }
}
