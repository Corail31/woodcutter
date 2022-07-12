package ovh.corail.woodcutter;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ovh.corail.woodcutter.client.gui.WoodcutterScreen;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.registry.ModMenuTypes;
import ovh.corail.woodcutter.registry.ModRecipeTypes;

@Mod("corail_woodcutter")
public class WoodCutterMod {
    public static final String MOD_ID = "corail_woodcutter";
    public static final String MOD_NAME = "Corail Woodcutter";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public WoodCutterMod() {
        Helper.registerSharedConfig();
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        MenuScreens.register(ModMenuTypes.WOODCUTTER, WoodcutterScreen::new);
        event.enqueueWork(Helper::initItemModels);
    }

    private void onServerStarting(ServerStartingEvent event) {
        int count = event.getServer().overworld().getRecipeManager().byType(ModRecipeTypes.WOODCUTTING).size();
        LOGGER.info(count + " woodcutting recipes loaded");
    }
}
