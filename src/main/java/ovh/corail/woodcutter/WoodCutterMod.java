package ovh.corail.woodcutter;

import net.fabricmc.api.ModInitializer;

public class WoodCutterMod implements ModInitializer {
    public static final String MOD_ID = "corail_woodcutter";

    @Override
	public void onInitialize() {
        //ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigWoodcutter.CLIENT_SPEC);
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }
}
