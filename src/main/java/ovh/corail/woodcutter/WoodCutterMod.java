package ovh.corail.woodcutter;

import com.google.common.reflect.Reflection;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fmlserverevents.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ovh.corail.woodcutter.client.gui.WoodcutterScreen;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.registry.ModContainers;
import ovh.corail.woodcutter.registry.ModRecipeTypes;
import ovh.corail.woodcutter.registry.ModStats;

@Mod("corail_woodcutter")
public class WoodCutterMod {
    public static final String MOD_ID = "corail_woodcutter";
    public static final String MOD_NAME = "Corail Woodcutter";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @SuppressWarnings("UnstableApiUsage")
    public WoodCutterMod() {
        Reflection.initialize(ModStats.class, ModRecipeTypes.class);
        Helper.registerSharedConfig();
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        MenuScreens.register(ModContainers.WOODCUTTER, WoodcutterScreen::new);
        event.enqueueWork(Helper::initItemModels);
    }

    private void onServerStarting(FMLServerStartingEvent event) {
        int count = event.getServer().overworld().getRecipeManager().byType(ModRecipeTypes.WOODCUTTING).size();
        LOGGER.info(count + " woodcutting recipes loaded");
    }
}
