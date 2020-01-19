package ovh.corail.woodcutter;

import net.fabricmc.api.ClientModInitializer;

public class ClientInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        //ScreenManager.registerFactory(ModContainers.WOODCUTTER, WoodcutterScreen::new);
        //ModBlocks.WOODCUTTERS.forEach(block -> RenderTypeLookup.setRenderLayer(block, RenderType.func_228643_e_()));
    }
}
