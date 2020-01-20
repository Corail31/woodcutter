package ovh.corail.woodcutter.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.village.PointOfInterestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(PointOfInterestType.class)
public interface PointOfInterestTypeMixin {
    @Invoker
    public static PointOfInterestType createPointOfInterestType(String string, Set<BlockState> set, int i, int j) {
        throw new Error("Mixin failed to apply");
    }
}
