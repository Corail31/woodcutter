package ovh.corail.woodcutter.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ConfigWoodcutter {
    public static class General {
        public final ForgeConfigSpec.ConfigValue<Boolean> openWoodcutterInInventory;

        General(ForgeConfigSpec.Builder builder) {
            builder.comment("Miscellaneous options").push("general");

            openWoodcutterInInventory = builder
                    .comment("Allows to open the woodcutter in inventory with right-click while sneaking [default:true]")
                    .translation(getTranslation("open_woodcutter_in_inventory"))
                    .define("open_woodcutter_in_inventory", true);

            builder.pop();
        }
    }

    private static String getTranslation(String name) {
        return MOD_ID + ".config." + name;
    }

    public static final ForgeConfigSpec GENERAL_SPEC;
    public static final General general;

    static {
        final Pair<General, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(General::new);
        GENERAL_SPEC = specPair.getRight();
        general = Objects.requireNonNull(specPair.getLeft());
    }
}
