package ovh.corail.woodcutter.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
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
        containerIn.setInventoryUpdateListener(this::onInventoryUpdate);
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
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        super.render(matrixStack, mouseX, mouseY, partialTick);
        renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
        this.font.func_243248_b(matrixStack, this.title, 8f, 4f, 4210752);
        this.font.func_243248_b(matrixStack, this.playerInventory.getDisplayName(), 8f, (float) (this.ySize - 94), 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        renderBackground(matrixStack);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        getMinecraft().getTextureManager().bindTexture(BACKGROUND_TEXTURE);
        int i = this.guiLeft;
        int j = this.guiTop;
        this.blit(matrixStack, i, j, 0, 0, this.xSize, this.ySize);
        int k = (int) (41f * this.sliderProgress);
        this.blit(matrixStack, i + 119, j + 15 + k, 176 + (this.canScroll() ? 0 : 12), 0, 12, 15);
        int l = this.guiLeft + 52;
        int i1 = this.guiTop + 14;
        int j1 = this.recipeIndexOffset + 12;
        renderRecipeBackground(matrixStack, mouseX, mouseY, l, i1, j1);
        renderRecipeIcons(matrixStack, l, i1, j1);
    }

    @Override
    protected void renderHoveredTooltip(MatrixStack matrixStack, int x, int y) {
        super.renderHoveredTooltip(matrixStack, x, y);
        if (this.hasInput) {
            int i = this.guiLeft + 52;
            int j = this.guiTop + 14;
            int k = this.recipeIndexOffset + 12;
            List<WoodcuttingRecipe> list = this.container.getRecipeList();
            for (int l = this.recipeIndexOffset; l < k && l < this.container.getRecipeListSize(); ++l) {
                int i1 = l - this.recipeIndexOffset;
                int j1 = i + i1 % 4 * 16;
                int k1 = j + i1 / 4 * 18 + 2;
                if (x >= j1 && x < j1 + 16 && y >= k1 && y < k1 + 18) {
                    this.renderTooltip(matrixStack, list.get(l).getRecipeOutput(), x, y);
                }
            }
        }
    }

    private void renderRecipeBackground(MatrixStack matrixStack, int mouseX, int mouseY, int x, int y, int scrollOffset) {
        for (int i = this.recipeIndexOffset; i < scrollOffset && i < this.container.getRecipeListSize(); ++i) {
            int j = i - this.recipeIndexOffset;
            int k = x + j % 4 * 16;
            int l = j / 4;
            int i1 = y + l * 18 + 2;
            int j1 = this.ySize;
            if (i == this.container.getSelectedRecipe()) {
                j1 += 18;
            } else if (mouseX >= k && mouseY >= i1 && mouseX < k + 16 && mouseY < i1 + 18) {
                j1 += 36;
            }
            this.blit(matrixStack, k, i1 - 1, 0, j1, 16, 18);
        }
    }

    private void renderRecipeIcons(MatrixStack matrixStack, int x, int y, int scrollOffset) {
        List<WoodcuttingRecipe> list = this.container.getRecipeList();
        for (int i = this.recipeIndexOffset; i < scrollOffset && i < this.container.getRecipeListSize(); ++i) {
            int j = i - this.recipeIndexOffset;
            int k = x + j % 4 * 16;
            int l = j / 4;
            int i1 = y + l * 18 + 2;
            getMinecraft().getItemRenderer().renderItemAndEffectIntoGUI(list.get(i).getRecipeOutput(), k, i1);
        }
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
    public boolean mouseDragged(double mouseX, double mouseY, int buttonId, double dragX, double dragY) {
        if (this.isSliderClicked && canScroll()) {
            int i = this.guiTop + 14;
            int j = i + 54;
            this.sliderProgress = ((float) mouseY - (float) i - 7.5f) / ((float) (j - i) - 15f);
            this.sliderProgress = MathHelper.clamp(this.sliderProgress, 0f, 1f);
            this.recipeIndexOffset = (int) ((double) (this.sliderProgress * (float) this.getHiddenRows()) + 0.5d) * 4;
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, buttonId, dragX, dragY);
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

    private void onInventoryUpdate() {
        this.hasInput = this.container.hasItemsinInputSlot();
        if (!this.hasInput) {
            this.sliderProgress = 0f;
            this.recipeIndexOffset = 0;
        }
    }
}
