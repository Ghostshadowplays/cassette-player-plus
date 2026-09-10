package net.walkman.walkman;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.walkman.music.Music;

public class WalkmanConfigScreen extends Screen {
    public WalkmanConfigScreen() {
        super(Component.literal("Walkman Configuration"));
    }

    @Override
    protected void init() {
        int x = this.width / 2;
        int y = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Modern Set"), b -> {
            WalkmanConfig.setVisualSet(WalkmanConfig.VisualSet.MODERN);
            this.onClose();
        }).bounds(x - 100, y - 30, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Retro Set"), b -> {
            WalkmanConfig.setVisualSet(WalkmanConfig.VisualSet.RETRO);
            this.onClose();
        }).bounds(x - 100, y - 5, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
            this.onClose();
        }).bounds(x - 100, y + 35, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        String current = "Current Set: " + WalkmanConfig.getVisualSet().getName();
        graphics.drawCenteredString(this.font, current, this.width / 2, 40, 0xAAAAAA);
        
        super.render(graphics, mouseX, mouseY, partialTicks);
    }
}
