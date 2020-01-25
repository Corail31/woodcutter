package ovh.corail.woodcutter.registry;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.poi.PointOfInterestType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Set;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModPointOfInterestTypes {
    public static Constructor<?> CONSTRUCTOR = Arrays.stream(PointOfInterestType.class.getDeclaredConstructors()).filter(constructor -> constructor.getParameterCount() == 4).findFirst().orElse(null);
    static {
        if (CONSTRUCTOR == null) {
            throw new Error("Impossible to instanciate a new PointOfInterestType");
        }
        CONSTRUCTOR.setAccessible(true);
    }
    public static final PointOfInterestType WOODSMITH = createPOI();

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

    private static PointOfInterestType createPOI() {
        if (CONSTRUCTOR == null) {
            return null;
        }
        try {
            return (PointOfInterestType) CONSTRUCTOR.newInstance("woodsmith", getAllStates(), 1, 1);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }
}
