package ovh.corail.woodcutter;

import net.fabricmc.api.ModInitializer;
import ovh.corail.woodcutter.registry.ModBlocks;

public class WoodCutterMod implements ModInitializer {
    public static final String MOD_ID = "corail_woodcutter";

    @Override
	public void onInitialize() {
        ModBlocks.init();
    }
}
