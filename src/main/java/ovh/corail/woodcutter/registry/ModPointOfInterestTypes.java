package ovh.corail.woodcutter.registry;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ObjectHolder;

import java.util.Set;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@ObjectHolder(MOD_ID)
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModPointOfInterestTypes {
    public static final PointOfInterestType WOODSMITH = new PointOfInterestType("woodsmith", getAllStates(), 1, SoundEvents.ENTITY_VILLAGER_WORK_MASON, 1);

    @SubscribeEvent
    public static void onRegisterPointOfInterestTypes(final RegistryEvent.Register<PointOfInterestType> event) {
        event.getRegistry().register(WOODSMITH.setRegistryName(new ResourceLocation(MOD_ID, "woodsmith")));
    }

    private static Set<BlockState> getAllStates() {
        ImmutableSet.Builder<BlockState> builder = ImmutableSet.builder();
        for (Block block : ModBlocks.WOODCUTTERS) {
            builder.addAll(block.getStateContainer().getValidStates());
        }
        return builder.build();
    }
}
