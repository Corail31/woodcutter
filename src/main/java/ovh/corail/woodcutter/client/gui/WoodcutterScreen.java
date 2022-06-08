package ovh.corail.woodcutter.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.woodcutter.inventory.WoodcutterContainer;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class WoodcutterScreen extends AbstractContainerScreen<WoodcutterContainer> {
    private static final ResourceLocation BG_LOCATION = new ResourceLocation("textures/gui/container/stonecutter.png");
    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;
    private boolean displayRecipes;

    public WoodcutterScreen(WoodcutterContainer containerIn, Inventory playerInv, Component title) {
        super(containerIn, playerInv, title);
        containerIn.registerUpdateListener(this::containerChanged);
        --this.titleLabelY;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTick) {
        super.render(matrixStack, mouseX, mouseY, partialTick);
        renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTicks, int mouseX, int mouseY) {
        renderBackground(poseStack);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, BG_LOCATION);
        int i = this.leftPos;
        int j = this.topPos;
        this.blit(poseStack, i, j, 0, 0, this.imageWidth, this.imageHeight);
        int k = (int) (41f * this.scrollOffs);
        this.blit(poseStack, i + 119, j + 15 + k, 176 + (this.isScrollBarActive() ? 0 : 12), 0, 12, 15);
        int l = this.leftPos + 52;
        int i1 = this.topPos + 14;
        int j1 = this.startIndex + 12;
        renderButtons(poseStack, mouseX, mouseY, l, i1, j1);
        renderRecipes(l, i1, j1);
    }

    @Override
    protected void renderTooltip(PoseStack poseStack, int x, int y) {
        super.renderTooltip(poseStack, x, y);
        if (this.displayRecipes) {
            int i = this.leftPos + 52;
            int j = this.topPos + 14;
            int k = this.startIndex + 12;
            List<WoodcuttingRecipe> list = this.menu.getRecipes();
            for (int l = this.startIndex; l < k && l < this.menu.getNumRecipes(); ++l) {
                int i1 = l - this.startIndex;
                int j1 = i + i1 % 4 * 16;
                int k1 = j + i1 / 4 * 18 + 2;
                if (x >= j1 && x < j1 + 16 && y >= k1 && y < k1 + 18) {
                    this.renderTooltip(poseStack, list.get(l).getResultItem(), x, y);
                }
            }
        }
    }

    private void renderButtons(PoseStack poseStack, int mouseX, int mouseY, int x, int y, int scrollOffset) {
        for (int i = this.startIndex; i < scrollOffset && i < this.menu.getNumRecipes(); ++i) {
            int j = i - this.startIndex;
            int k = x + j % 4 * 16;
            int l = j / 4;
            int i1 = y + l * 18 + 2;
            int j1 = this.imageHeight;
            if (i == this.menu.getSelectedRecipeIndex()) {
                j1 += 18;
            } else if (mouseX >= k && mouseY >= i1 && mouseX < k + 16 && mouseY < i1 + 18) {
                j1 += 36;
            }
            this.blit(poseStack, k, i1 - 1, 0, j1, 16, 18);
        }
    }

    private void renderRecipes(int x, int y, int scrollOffset) {
        List<WoodcuttingRecipe> list = this.menu.getRecipes();
        for (int i = this.startIndex; i < scrollOffset && i < this.menu.getNumRecipes(); ++i) {
            int j = i - this.startIndex;
            int k = x + j % 4 * 16;
            int l = j / 4;
            int i1 = y + l * 18 + 2;
            getMinecraft().getItemRenderer().renderAndDecorateItem(list.get(i).getResultItem(), k, i1);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonId) {
        this.scrolling = false;
        if (this.displayRecipes) {
            int i = this.leftPos + 52;
            int j = this.topPos + 14;
            int k = this.startIndex + 12;
            for (int l = this.startIndex; l < k; ++l) {
                int i1 = l - this.startIndex;
                double d0 = mouseX - (double) (i + i1 % 4 * 16);
                double d1 = mouseY - (double) (j + i1 / 4 * 18);
                if (d0 >= 0d && d1 >= 0d && d0 < 16d && d1 < 18d && this.menu.clickMenuButton(this.minecraft.player, l)) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1f));
                    getMinecraft().gameMode.handleInventoryButtonClick((this.menu).containerId, l);
                    return true;
                }
            }
            i = this.leftPos + 119;
            j = this.topPos + 14;
            if (mouseX >= (double) i && mouseX < (double) (i + 12) && mouseY >= (double) j && mouseY < (double) (j + 54)) {
                this.scrolling = true;
                // allows to click on the slide bar to change the position (without drag)
                this.scrollOffs = Mth.clamp((float) (mouseY - j - 7.5f) / 40f, 0f, 1f);
                this.startIndex = (int) ((double) (this.scrollOffs * (float) getOffscreenRows()) + 0.5d) * 4;
            }
        }
        return super.mouseClicked(mouseX, mouseY, buttonId);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int buttonId, double dragX, double dragY) {
        if (this.scrolling && isScrollBarActive()) {
            int i = this.topPos + 14;
            int j = i + 54;
            this.scrollOffs = ((float) mouseY - (float) i - 7.5f) / ((float) (j - i) - 15f);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0f, 1f);
            this.startIndex = (int) ((double) (this.scrollOffs * (float) this.getOffscreenRows()) + 0.5d) * 4;
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, buttonId, dragX, dragY);
        }
    }

    @Override
    public boolean mouseScrolled(double p_mouseScrolled_1_, double p_mouseScrolled_3_, double p_mouseScrolled_5_) {
        if (isScrollBarActive()) {
            int i = getOffscreenRows();
            float f = (float) p_mouseScrolled_5_ / (float) i;
            this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0f, 1f);
            this.startIndex = (int) ((double) (this.scrollOffs * (float) i) + 0.5d) * 4;
        }
        return true;
    }

    private boolean isScrollBarActive() {
        return this.displayRecipes && this.menu.getNumRecipes() > 12;
    }

    private int getOffscreenRows() {
        return (this.menu.getNumRecipes() + 4 - 1) / 4 - 3;
    }

    private void containerChanged() {
        this.displayRecipes = this.menu.hasInputItem();
        if (!this.displayRecipes) {
            this.scrollOffs = 0f;
            this.startIndex = 0;
        }
    }

    @Override
    protected void slotClicked(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        // reset the slider bar if 2 items are switched from the input slot
        if (type == ClickType.PICKUP && slotId == 0 && mouseButton == 0 && slotIn != null && slotIn.index == 0) {
            if (!this.menu.getCarried().isEmpty() && !slotIn.getItem().isEmpty() && (this.menu.getCarried().getItem() != slotIn.getItem().getItem() || !ItemStack.tagMatches(this.menu.getCarried(), slotIn.getItem()))) {
                this.scrollOffs = 0f;
                this.startIndex = 0;
            }
        }
        super.slotClicked(slotIn, slotId, mouseButton, type);
    }
}
