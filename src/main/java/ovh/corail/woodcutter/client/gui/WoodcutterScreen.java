package ovh.corail.woodcutter.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractContainerScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ovh.corail.woodcutter.inventory.WoodcutterContainer;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;

import javax.annotation.Nonnull;
import java.util.List;

@Environment(EnvType.CLIENT)
public class WoodcutterScreen extends AbstractContainerScreen<WoodcutterContainer> {
    private static final Identifier BACKGROUND_TEXTURE = new Identifier("textures/gui/container/stonecutter.png");
    private float sliderProgress;
    private boolean isSliderClicked;
    private int recipeIndexOffset;
    private boolean hasInput;

    public WoodcutterScreen(WoodcutterContainer containerIn, PlayerInventory playerInv, Text title) {
        super(containerIn, playerInv, title);
        containerIn.setInventoryUpdateListener(this::onInventoryChange);
    }

    @SuppressWarnings("all")
    @Nonnull
    private MinecraftClient getMinecraft() {
        return this.minecraft;
    }

    @Override
    protected void onMouseClick(Slot slotIn, int slotId, int mouseButton, SlotActionType type) {
        if (type == SlotActionType.PICKUP && slotId == 0 && mouseButton == 0 && slotIn != null && slotIn.id == 0) {
            if (!this.playerInventory.getCursorStack().isEmpty() && !slotIn.getStack().isEmpty() && !Container.canStacksCombine(this.playerInventory.getCursorStack(), slotIn.getStack())) {
                this.sliderProgress = 0f;
                this.recipeIndexOffset = 0;
            }
        }
        super.onMouseClick(slotIn, slotId, mouseButton, type);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        super.render(mouseX, mouseY, partialTick);
        this.drawMouseoverTooltip(mouseX, mouseY);
    }

    @Override
    protected void drawForeground(int mouseX, int mouseY) {
        this.font.draw(this.title.asFormattedString(), 8f, 4f, 4210752);
        this.font.draw(this.playerInventory.getDisplayName().asFormattedString(), 8f, (float) (this.containerHeight - 94), 4210752);
    }

    @Override
    protected void drawBackground(float partialTicks, int mouseX, int mouseY) {
        this.renderBackground();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        getMinecraft().getTextureManager().bindTexture(BACKGROUND_TEXTURE);
        int i = this.x;
        int j = this.y;
        this.blit(i, j, 0, 0, this.containerWidth, this.containerHeight);
        int k = (int) (41.0F * this.sliderProgress);
        this.blit(i + 119, j + 15 + k, 176 + (this.canScroll() ? 0 : 12), 0, 12, 15);
        int l = this.x + 52;
        int i1 = this.y + 14;
        int j1 = this.recipeIndexOffset + 12;
        this.func_214141_a(mouseX, mouseY, l, i1, j1);
        this.func_214142_b(l, i1, j1);
    }

    private void func_214141_a(int mouseX, int mouseY, int p_214141_3_, int p_214141_4_, int p_214141_5_) {
        for (int i = this.recipeIndexOffset; i < p_214141_5_ && i < this.container.getRecipeListSize(); ++i) {
            int j = i - this.recipeIndexOffset;
            int k = p_214141_3_ + j % 4 * 16;
            int l = j / 4;
            int i1 = p_214141_4_ + l * 18 + 2;
            int j1 = this.containerHeight;
            if (i == this.container.getSelectedRecipe()) {
                j1 += 18;
            } else if (mouseX >= k && mouseY >= i1 && mouseX < k + 16 && mouseY < i1 + 18) {
                j1 += 36;
            }

            this.blit(k, i1 - 1, 0, j1, 16, 18);
        }
    }

    private void func_214142_b(int p_214142_1_, int p_214142_2_, int p_214142_3_) {
        List<WoodcuttingRecipe> list = this.container.getRecipeList();
        for (int i = this.recipeIndexOffset; i < p_214142_3_ && i < this.container.getRecipeListSize(); ++i) {
            int j = i - this.recipeIndexOffset;
            int k = p_214142_1_ + j % 4 * 16;
            int l = j / 4;
            int i1 = p_214142_2_ + l * 18 + 2;
            getMinecraft().getItemRenderer().renderGuiItem(list.get(i).getOutput(), k, i1);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonId) {
        this.isSliderClicked = false;
        if (this.hasInput) {
            int i = this.x + 52;
            int j = this.y + 14;
            int k = this.recipeIndexOffset + 12;

            for (int l = this.recipeIndexOffset; l < k; ++l) {
                int i1 = l - this.recipeIndexOffset;
                double d0 = mouseX - (double) (i + i1 % 4 * 16);
                double d1 = mouseY - (double) (j + i1 / 4 * 18);
                if (d0 >= 0d && d1 >= 0d && d0 < 16d && d1 < 18d && this.container.enchantItem(getMinecraft().player, l)) {
                    getMinecraft().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1f));
                    getMinecraft().interactionManager.clickButton(this.container.syncId, l);
                    return true;
                }
            }

            i = this.x + 119;
            j = this.y + 14;
            if (mouseX >= (double) i && mouseX < (double) (i + 12) && mouseY >= (double) j && mouseY < (double) (j + 54)) {
                this.isSliderClicked = true;
                this.sliderProgress = MathHelper.clamp((float) (mouseY - j - 7.5f) / 40f, 0f, 1f);
                this.recipeIndexOffset = (int) ((double) (this.sliderProgress * (float) getHiddenRows()) + 0.5d) * 4;
            }
        }
        return super.mouseClicked(mouseX, mouseY, buttonId);
    }

    @Override
    public boolean mouseDragged(double dragX, double dragY, int buttonId, double p_mouseDragged_6_, double p_mouseDragged_8_) {
        if (this.isSliderClicked && canScroll()) {
            int i = this.y + 14;
            int j = i + 54;
            this.sliderProgress = ((float) dragY - (float) i - 7.5f) / ((float) (j - i) - 15f);
            this.sliderProgress = MathHelper.clamp(this.sliderProgress, 0f, 1f);
            this.recipeIndexOffset = (int) ((double) (this.sliderProgress * (float) this.getHiddenRows()) + 0.5d) * 4;
            return true;
        } else {
            return super.mouseDragged(dragX, dragY, buttonId, p_mouseDragged_6_, p_mouseDragged_8_);
        }
    }

    @Override
    public boolean mouseScrolled(double p_mouseScrolled_1_, double p_mouseScrolled_3_, double p_mouseScrolled_5_) {
        if (canScroll()) {
            int i = getHiddenRows();
            this.sliderProgress = (float) ((double) this.sliderProgress - p_mouseScrolled_5_ / (double) i);
            this.sliderProgress = MathHelper.clamp(this.sliderProgress, 0.0F, 1.0F);
            this.recipeIndexOffset = (int) ((double) (this.sliderProgress * (float) i) + 0.5D) * 4;
        }

        return true;
    }

    private boolean canScroll() {
        return this.hasInput && this.container.getRecipeListSize() > 12;
    }

    private int getHiddenRows() {
        return (this.container.getRecipeListSize() + 4 - 1) / 4 - 3;
    }

    private void onInventoryChange() {
        this.hasInput = this.container.canCraft();
        if (!this.hasInput) {
            this.sliderProgress = 0f;
            this.recipeIndexOffset = 0;
        }
    }
}
