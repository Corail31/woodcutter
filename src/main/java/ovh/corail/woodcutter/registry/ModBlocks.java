package ovh.corail.woodcutter.registry;

import com.google.common.reflect.Reflection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import ovh.corail.woodcutter.block.WoodcutterBlock;
import ovh.corail.woodcutter.compatibility.SupportMods;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.item.WoodcutterItem;

import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import static ovh.corail.woodcutter.WoodCutterMod.LOGGER;
import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlocks {
    public static final Set<Block> WOODCUTTERS = new HashSet<>();
    public static final Set<Item> WOODCUTTER_ITEMS = new HashSet<>();
    private static final Random RANDOM = new Random();
    private static ItemStack RANDOM_STACK = ItemStack.EMPTY;

    @SubscribeEvent
    public static void onRegisterBlocks(final RegisterEvent event) {
        if (!event.getRegistryKey().equals(ForgeRegistries.Keys.BLOCKS)) {
            return;
        }
        for (VanillaWoodVariant variant : VanillaWoodVariant.values()) {
            registerWoodcutter(event, variant.getSerializedName());
        }
        if (SupportMods.BIOMESOPLENTY.isLoaded()) {
            for (BOPWoodVariant variant : BOPWoodVariant.values()) {
                registerWoodcutter(event, variant.getSerializedName());
            }
        }
        if (SupportMods.TWILIGHT_FOREST.isLoaded()) {
            for (TFWoodVariant variant : TFWoodVariant.values()) {
                if (variant != TFWoodVariant.MANGROVE) {
                    registerWoodcutter(event, variant.getSerializedName());
                }
            }
        }
        if (SupportMods.TROPICRAFT.isLoaded()) {
            registerWoodcutter(event, SupportMods.TROPICRAFT.getSerializedName() + "_" + TropicraftVariant.PALM.getSerializedName());
            registerWoodcutter(event, SupportMods.TROPICRAFT.getSerializedName() + "_" + TropicraftVariant.MAHOGANY.getSerializedName());
        }
        if (SupportMods.BYG.isLoaded()) {
            if (SupportMods.EXTENSION_BYG.isLoaded()) {
                for (BYGWoodVariant variant : BYGWoodVariant.values()) {
                    registerWoodcutter(event, SupportMods.BYG.getSerializedName() + "_" + variant.getSerializedName());
                }
            } else {
                LOGGER.info("missing extension for \"Oh Biome You'll Go\" recipes");
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterBlockItems(final RegisterEvent event) {
        if (!event.getRegistryKey().equals(ForgeRegistries.Keys.ITEMS)) {
            return;
        }
        WOODCUTTERS.forEach(woodcutterBlock -> {
            Item woodcutter = new WoodcutterItem(woodcutterBlock);
            WOODCUTTER_ITEMS.add(woodcutter);
            event.register(ForgeRegistries.Keys.ITEMS, Helper.getRegistryRL(woodcutterBlock), () -> woodcutter);
        });
        //noinspection UnstableApiUsage
        Reflection.initialize(ModStats.class, ModRecipeTypes.class);
    }

    private static void registerWoodcutter(final RegisterEvent event, String name) {
        Block woodcutter = new WoodcutterBlock();
        WOODCUTTERS.add(woodcutter);
        event.register(ForgeRegistries.Keys.BLOCKS, new ResourceLocation(MOD_ID, name + "_woodcutter"), () -> woodcutter);
    }

    public static ItemStack createRandomStack() {
        if (RANDOM_STACK.isEmpty()) {
            RANDOM_STACK = new ItemStack(WOODCUTTERS.stream().skip(RANDOM.nextInt(WOODCUTTERS.size())).findFirst().orElse(Blocks.STONECUTTER));
        }
        return RANDOM_STACK;
    }

    public enum VanillaWoodVariant implements StringRepresentable {
        OAK, BIRCH, SPRUCE, JUNGLE, ACACIA, DARK_OAK, CRIMSON, WARPED, MANGROVE;
        private final String name;

        VanillaWoodVariant() {
            this.name = name().toLowerCase(Locale.US);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public enum BOPWoodVariant implements StringRepresentable {
        CHERRY, DEAD, FIR, HELLBARK, JACARANDA, MAGIC, MAHOGANY, PALM, REDWOOD, UMBRAN, WILLOW;
        private final String name;

        BOPWoodVariant() {
            this.name = name().toLowerCase(Locale.US);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public enum TFWoodVariant implements StringRepresentable {
        TWILIGHT_OAK, CANOPY, MANGROVE, DARK, TIME, TRANSFORMATION, MINING, SORTING;
        private final String name;

        TFWoodVariant() {
            this.name = name().toLowerCase(Locale.US);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public enum TropicraftVariant implements StringRepresentable {
        PALM, MAHOGANY, BAMBOO, THATCH;
        private final String name;

        TropicraftVariant() {
            this.name = name().toLowerCase(Locale.US);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public enum BYGWoodVariant implements StringRepresentable {
        ASPEN, BAOBAB, BLUE_ENCHANTED, CHERRY, CIKA, CYPRESS, EBONY, FIR, GREEN_ENCHANTED, HOLLY, JACARANDA, MAHOGANY, MANGROVE, MAPLE, PINE, RAINBOW_EUCALYPTUS, REDWOOD, SKYRIS, WILLOW, WITCH_HAZEL, ZELKOVA, SYTHIAN, EMBUR, PALM, LAMENT, BULBIS, NIGHTSHADE, ETHER, IMPARIUS;
        private final String name;

        BYGWoodVariant() {
            this.name = name().toLowerCase(Locale.US);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
