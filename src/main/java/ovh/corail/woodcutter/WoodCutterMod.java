package ovh.corail.woodcutter;

import net.fabricmc.api.ModInitializer;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModContainers;
import ovh.corail.woodcutter.registry.ModRecipeSerializers;

public class WoodCutterMod implements ModInitializer {
    public static final String MOD_ID = "corail_woodcutter";

    @Override
	public void onInitialize() {
        ModBlocks.init();
        ModRecipeSerializers.init();
        ModContainers.init();
    }
}
