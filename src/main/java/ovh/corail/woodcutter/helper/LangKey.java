package ovh.corail.woodcutter.helper;

import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public enum LangKey {
    COMMAND_USAGE("command.usage"),
    DATAPACK_GENERATE_SUCCESS("message.datapack.generate.success"),
    DATAPACK_GENERATE_FAIL("message.datapack.generate.fail"),
    FILE_DELETE_FAIL("message.file.delete.fail"),
    FILE_WRITE_FAIL("message.file.write.fail"),
    FILE_COPY_FAIL("message.file.copy.fail"),
    FOLDER_CREATE_FAIL("message.folder.create.fail"),
    ZIP_CREATE_FAIL("message.zip.create.fail"),
    MCMETA_CREATE_FAIL("message.mcmeta.create.fail"),
    NO_VALID_RECIPE_FOR_MODID("message.no_valid_recipe_for_modid"),
    INVALID_MODID("message.invalid_modid"),
    DATAPACK_NOT_GENERATED("message.datapack.not_generated"),
    DATAPACK_APPLY_SUCCESS("message.datapack.apply.success"),
    DATAPACK_APPLY_FAIL("message.datapack.apply.fail"),
    DATAPACK_REMOVE_SUCCESS("message.datapack.remove.success"),
    DATAPACK_REMOVE_ABSENT("message.datapack.remove.absent"),
    DATAPACK_REMOVE_FAIL("message.datapack.remove.fail");

    private final String key;

    LangKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return MOD_ID + "." + this.key;
    }

    public Component getText(Object... args) {
        return new TranslatableComponent(this.getKey(), args);
    }

    public CommandRuntimeException asCommandException(Object... args) {
        return new CommandRuntimeException(getText(args));
    }
}