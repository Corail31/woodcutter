package ovh.corail.woodcutter;

import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ovh.corail.woodcutter.client.gui.WoodcutterScreen;
import ovh.corail.woodcutter.registry.ModContainers;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod(MOD_ID)
public class WoodCutterMod {
    public static final String MOD_ID = "corail_woodcutter";

    public WoodCutterMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        ScreenManager.registerFactory(ModContainers.WOODCUTTER, WoodcutterScreen::new);
    }
}
