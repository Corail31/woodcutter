package ovh.corail.woodcutter.registry;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.poi.PointOfInterestType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.function.Predicate;

import static ovh.corail.woodcutter.WoodCutterMod.LOGGER;
import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModPointOfInterestTypes {
    private static final PointOfInterestType WOODSMITH = createPOI();

    public static void init() {
        if (WOODSMITH != null) {
            Registry.register(Registry.POINT_OF_INTEREST_TYPE, new Identifier(MOD_ID, "woodsmith"), WOODSMITH);
            LOGGER.info(String.format("%s: Register a new PointOfInterestType", MOD_ID));
        }
    }

    private static Set<BlockState> getAllStates() {
        ImmutableSet.Builder<BlockState> builder = ImmutableSet.builder();
        for (Block block : ModBlocks.WOODCUTTERS) {
            builder.addAll(block.getStateManager().getStates());
        }
        return builder.build();
    }

    private static final Predicate<PointOfInterestType> UNUSED = (pointOfInterestType) -> false;

    private static PointOfInterestType createPOI() {
        try {
            Constructor<?> CONSTRUCTOR = PointOfInterestType.class.getDeclaredConstructor(String.class, Set.class, int.class, Predicate.class, int.class);
            CONSTRUCTOR.setAccessible(true);
            return (PointOfInterestType) CONSTRUCTOR.newInstance("woodsmith", getAllStates(), 1, UNUSED, 1);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOGGER.info(String.format("%s: Impossible to instanciate a new PointOfInterestType", MOD_ID));
        }
        return null;
    }
}
