package ovh.corail.woodcutter.registry;

import com.google.common.collect.ImmutableSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModPointOfInterestTypes {
    private static final PoiType WOODSMITH = new PoiType("woodsmith", getAllStates(), 1, poi -> false, 1);

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onRegisterPointOfInterestTypes(final RegistryEvent.Register<PoiType> event) {
        event.getRegistry().register(WOODSMITH.setRegistryName(new ResourceLocation(MOD_ID, "woodsmith")));
    }

    private static Set<BlockState> getAllStates() {
        ImmutableSet.Builder<BlockState> builder = ImmutableSet.builder();
        for (Block block : ModBlocks.WOODCUTTERS) {
            builder.addAll(block.getStateDefinition().getPossibleStates());
        }
        return builder.build();
    }
}
