package ovh.corail.woodcutter;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ovh.corail.woodcutter.helper.Helper;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod(MOD_ID)
public class WoodCutterMod {
    public static final String MOD_ID = "corail_woodcutter";
    public static final String MOD_NAME = "Corail Woodcutter";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public WoodCutterMod() {
        Helper.registerSharedConfig();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(Helper::initItemModels);
    }
}
