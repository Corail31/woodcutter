package ovh.corail.woodcutter;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ConfigWoodcutter {
    public static class Client {
        public final ForgeConfigSpec.ConfigValue<Boolean> stonecuttingSupportInJEI;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Personal Options that can be edited even on server").push("client");
            stonecuttingSupportInJEI = builder
                    .comment("Display the stonecutting recipes in a custom category in JEI - depending if you're on a server or singleplayer, this option requires to relog on the server or restart your client [false/true|default:true]")
                    .translation(getTranslation("stonecutting_support_in_jei"))
                    .define("stonecutting_support_in_jei", true);
        }
    }

    private static String getTranslation(String name) {
        return MOD_ID + ".config." + name;
    }

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ConfigWoodcutter.Client client;

    static {
        final Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = specPair.getRight();
        client = specPair.getLeft();
    }
}
