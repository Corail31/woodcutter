package ovh.corail.woodcutter;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import ovh.corail.woodcutter.registry.ModBlocks;

public class ClientInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        //ScreenManager.registerFactory(ModContainers.WOODCUTTER, WoodcutterScreen::new);
        ModBlocks.WOODCUTTERS.forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout()));
    }
}
