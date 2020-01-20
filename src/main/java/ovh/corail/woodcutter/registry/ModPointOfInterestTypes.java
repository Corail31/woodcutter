package ovh.corail.woodcutter.registry;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.PointOfInterestType;
import ovh.corail.woodcutter.mixin.PointOfInterestTypeMixin;

import java.util.Set;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModPointOfInterestTypes {
    public static final PointOfInterestType WOODSMITH = PointOfInterestTypeMixin.createPointOfInterestType("woodsmith", getAllStates(), 1, 1);

    public static void init() {
        Registry.register(Registry.POINT_OF_INTEREST_TYPE, new Identifier(MOD_ID, "woodsmith"), WOODSMITH);
    }

    private static Set<BlockState> getAllStates() {
        ImmutableSet.Builder<BlockState> builder = ImmutableSet.builder();
        for (Block block : ModBlocks.WOODCUTTERS) {
            builder.addAll(block.getStateManager().getStates());
        }
        return builder.build();
    }
}
