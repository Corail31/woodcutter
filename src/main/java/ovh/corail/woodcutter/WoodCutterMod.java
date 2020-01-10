package ovh.corail.woodcutter;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ovh.corail.woodcutter.client.gui.WoodcutterScreen;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModContainers;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod(MOD_ID)
public class WoodCutterMod {
    public static final String MOD_ID = "corail_woodcutter";

    public WoodCutterMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigWoodcutter.CLIENT_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        ScreenManager.registerFactory(ModContainers.WOODCUTTER, WoodcutterScreen::new);
        ModBlocks.WOODCUTTERS.forEach(block -> RenderTypeLookup.setRenderLayer(block, RenderType.func_228643_e_()));
    }
}
