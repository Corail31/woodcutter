package ovh.corail.woodcutter.registry;

import com.google.common.collect.ImmutableSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import ovh.corail.woodcutter.helper.Helper;

import java.util.Set;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModPointOfInterestTypes {
    private static PoiType WOODSMITH = Helper.unsafeNullCast();

    static void onRegister(final RegisterEvent event) {
        WOODSMITH = new PoiType(getAllStates(), 1, 1);
        event.register(ForgeRegistries.Keys.POI_TYPES, new ResourceLocation(MOD_ID, "woodsmith"), () -> WOODSMITH);
    }

    private static Set<BlockState> getAllStates() {
        ImmutableSet.Builder<BlockState> builder = ImmutableSet.builder();
        for (Block block : ModBlocks.WOODCUTTERS) {
            builder.addAll(block.getStateDefinition().getPossibleStates());
        }
        return builder.build();
    }
}
