package ovh.corail.woodcutter;

import com.google.common.reflect.Reflection;
import net.fabricmc.api.ModInitializer;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModContainers;
import ovh.corail.woodcutter.registry.ModPointOfInterestTypes;
import ovh.corail.woodcutter.registry.ModRecipeSerializers;
import ovh.corail.woodcutter.registry.ModRecipeTypes;
import ovh.corail.woodcutter.registry.ModTabs;
import ovh.corail.woodcutter.registry.ModTags;

public class WoodCutterMod implements ModInitializer {
    public static final String MOD_ID = "corail_woodcutter";

    @Override
    @SuppressWarnings("UnstableApiUsage")
	public void onInitialize() {
        Reflection.initialize(ModTabs.class, ModTags.class, ModRecipeTypes.class);
        ModRecipeSerializers.init();
        ModContainers.init();
        ModBlocks.init();
        ModPointOfInterestTypes.init();
    }
}
