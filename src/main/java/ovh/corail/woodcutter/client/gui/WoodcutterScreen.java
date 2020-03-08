package ovh.corail.woodcutter.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.woodcutter.inventory.WoodcutterContainer;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class WoodcutterScreen extends ContainerScreen<WoodcutterContainer> {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("textures/gui/container/stonecutter.png");
    private float sliderProgress;
    private boolean isSliderClicked;
    private int recipeIndexOffset;
    private boolean hasInput;

    public WoodcutterScreen(WoodcutterContainer containerIn, PlayerInventory playerInv, ITextComponent title) {
        super(containerIn, playerInv, title);
        containerIn.setInventoryUpdateListener(this::func_214145_d);
    }

    @Override
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        if (type == ClickType.PICKUP && slotId == 0 && mouseButton == 0 && slotIn != null && slotIn.slotNumber == 0) {
            if (!this.playerInventory.getItemStack().isEmpty() && !slotIn.getStack().isEmpty() && !Container.areItemsAndTagsEqual(this.playerInventory.getItemStack(), slotIn.getStack())) {
                this.sliderProgress = 0f;
                this.recipeIndexOffset = 0;
            }
        }
        super.handleMouseClick(slotIn, slotId, mouseButton, type);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        super.render(mouseX, mouseY, partialTick);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.font.drawString(this.title.getFormattedText(), 8f, 4f, 4210752);
        this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8f, (float) (this.ySize - 94), 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.renderBackground();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        getMinecraft().getTextureManager().bindTexture(BACKGROUND_TEXTURE);
        int i = this.guiLeft;
        int j = this.guiTop;
        this.blit(i, j, 0, 0, this.xSize, this.ySize);
        int k = (int) (41.0F * this.sliderProgress);
        this.blit(i + 119, j + 15 + k, 176 + (this.canScroll() ? 0 : 12), 0, 12, 15);
        int l = this.guiLeft + 52;
        int i1 = this.guiTop + 14;
        int j1 = this.recipeIndexOffset + 12;
        this.renderRecipeBackground(mouseX, mouseY, l, i1, j1);
        this.renderRecipeIcons(l, i1, j1);
    }

    private void renderRecipeBackground(int mouseX, int mouseY, int x, int y, int scrollOffset) {
        for (int i = this.recipeIndexOffset; i < scrollOffset && i < this.container.getRecipeListSize(); ++i) {
            int j = i - this.recipeIndexOffset;
            int k = x + j % 4 * 16;
            int l = j / 4;
            int i1 = y + l * 18 + 2;
            int j1 = this.ySize;
            if (i == this.container.func_217073_e()) {
                j1 += 18;
            } else if (mouseX >= k && mouseY >= i1 && mouseX < k + 16 && mouseY < i1 + 18) {
                j1 += 36;
            }

            this.blit(k, i1 - 1, 0, j1, 16, 18);
        }
    }

    private void renderRecipeIcons(int x, int y, int scrollOffset) {
        //RenderHelper.func_227780_a_(); // TODO check this
        List<WoodcuttingRecipe> list = this.container.getRecipeList();

        for (int i = this.recipeIndexOffset; i < scrollOffset && i < this.container.getRecipeListSize(); ++i) {
            int j = i - this.recipeIndexOffset;
            int k = x + j % 4 * 16;
            int l = j / 4;
            int i1 = y + l * 18 + 2;
            getMinecraft().getItemRenderer().renderItemAndEffectIntoGUI(list.get(i).getRecipeOutput(), k, i1);
        }

        RenderHelper.disableStandardItemLighting();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonId) {
        this.isSliderClicked = false;
        if (this.hasInput) {
            int i = this.guiLeft + 52;
            int j = this.guiTop + 14;
            int k = this.recipeIndexOffset + 12;

            for (int l = this.recipeIndexOffset; l < k; ++l) {
                int i1 = l - this.recipeIndexOffset;
                double d0 = mouseX - (double) (i + i1 % 4 * 16);
                double d1 = mouseY - (double) (j + i1 / 4 * 18);
                if (d0 >= 0d && d1 >= 0d && d0 < 16d && d1 < 18d && this.container.enchantItem(getMinecraft().player, l)) {
                    Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1f));
                    getMinecraft().playerController.sendEnchantPacket((this.container).windowId, l);
                    return true;
                }
            }

            i = this.guiLeft + 119;
            j = this.guiTop + 14;
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
            int i = this.guiTop + 14;
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

    private void func_214145_d() {
        this.hasInput = this.container.func_217083_h();
        if (!this.hasInput) {
            this.sliderProgress = 0f;
            this.recipeIndexOffset = 0;
        }
    }
}
