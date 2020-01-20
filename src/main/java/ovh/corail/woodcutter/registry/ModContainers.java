package ovh.corail.woodcutter.registry;

import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.util.Identifier;
import ovh.corail.woodcutter.inventory.WoodcutterContainer;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModContainers {
    public static final Identifier WOODCUTTER_ID = new Identifier(MOD_ID, "woodcutter");

    public static void init() {
        ContainerProviderRegistry.INSTANCE.registerFactory(WOODCUTTER_ID, WoodcutterContainer::new);
    }
}
