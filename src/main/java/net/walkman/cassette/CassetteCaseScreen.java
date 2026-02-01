package net.walkman.cassette;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CassetteCaseScreen extends AbstractContainerScreen<CassetteCaseMenu> {
    private static final ResourceLocation GUI_TEXTURE = 
            ResourceLocation.withDefaultNamespace("textures/gui/container/shulker_box.png");

    public CassetteCaseScreen(CassetteCaseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176 + 25; // Extra space for blank cassette slot
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        
        graphics.pose().pushPose();
        graphics.pose().translate(180, 7, 0);
        graphics.pose().scale(0.5f, 0.5f, 1.0f);
        graphics.drawString(this.font, Component.literal("Blanks"), 0, 0, 4210752, false);
        graphics.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Draw main background
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, 176, this.imageHeight);
        
        // Draw extra slot background for blank cassettes
        graphics.fill(x + 176, y, x + 176 + 25, y + 40, 0xFFC6C6C6);
        graphics.renderOutline(x + 176, y, 25, 40, 0xFF8B8B8B);
        
        // Draw the slot itself at 180 (centered in the 25px extra space)
        // Relative x = 180, so blit at x + 179
        graphics.blit(GUI_TEXTURE, x + 179, y + 17, 7, 17, 18, 18); 
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
