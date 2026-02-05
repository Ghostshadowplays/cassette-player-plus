package net.walkman.walkman;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.walkman.music.Music;

public class CassettePlayerScreen extends AbstractContainerScreen<CassettePlayerMenu> {
    private static final ResourceLocation GUI_TEXTURE = 
            ResourceLocation.withDefaultNamespace("textures/gui/container/hopper.png");

    private static final ResourceLocation CASSETTE_PLAYER_LOGO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Music.MODID, "textures/gui/retro_player.png");

    private VolumeSlider volumeSlider;
    private Button repeatButton;

    public CassettePlayerScreen(CassettePlayerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 133;
        this.titleLabelY = 6;
        this.inventoryLabelY = 40;
    }

    @Override
    protected void init() {
        super.init();
        
        float currentVolume = this.menu.getVolume();


        // Volume Slider - moved higher to avoid overlapping inventory (which starts at y=51)
        this.volumeSlider = new VolumeSlider(this.leftPos + 38, this.topPos + 6, 100, 14, (double)currentVolume);
        this.addRenderableWidget(this.volumeSlider);


        // Previous Button
        this.addRenderableWidget(Button.builder(Component.literal("⏮"), b -> {
            CassettePlayerSoundHandler.previousTrack(this.minecraft.player.getUUID());
        }).bounds(this.leftPos + 10, this.topPos + 20, 22, 20).build());

        // Play Button
        this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            ItemStack cassette = this.menu.getSlot(0).getItem();
            if (!cassette.isEmpty()) {
                float vol = this.volumeSlider.getValue();
                CassettePlayerSoundHandler.playMusic(this.minecraft.player.getUUID(), cassette, vol);
            }
        }).bounds(this.leftPos + 34, this.topPos + 20, 22, 20).build());

        // Stop Button
        this.addRenderableWidget(Button.builder(Component.literal("■"), b -> {
            CassettePlayerSoundHandler.stopMusic(this.minecraft.player.getUUID());
        }).bounds(this.leftPos + 120, this.topPos + 20, 22, 20).build());

        // Next Button
        this.addRenderableWidget(Button.builder(Component.literal("⏭"), b -> {
            CassettePlayerSoundHandler.skipTrack(this.minecraft.player.getUUID());
        }).bounds(this.leftPos + 144, this.topPos + 20, 22, 20).build());
        
        // Repeat Button
        this.repeatButton = Button.builder(Component.literal(this.menu.isRepeat() ? "🔁" : "➡"), b -> {
            boolean nextRepeat = !this.menu.isRepeat();
            this.menu.setRepeat(nextRepeat);
            CassettePlayerSoundHandler.setRepeat(this.minecraft.player.getUUID(), nextRepeat);
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 101);
            updateRepeatButtonText();
        }).bounds(this.leftPos + 58, this.topPos + 23, 14, 14).build();
        this.addRenderableWidget(this.repeatButton);
    }

    private void updateRepeatButtonText() {
        if (this.repeatButton != null) {
            this.repeatButton.setMessage(Component.literal(this.menu.isRepeat() ? "🔁" : "➡"));
        }
    }

    private class VolumeSlider extends AbstractSliderButton {
        public VolumeSlider(int x, int y, int width, int height, double pValue) {
            super(x, y, width, height, Component.empty(), pValue);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("Vol: " + (int)(this.value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            float vol = (float)this.value;
            CassettePlayerSoundHandler.setVolume(minecraft.player.getUUID(), vol);

            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, (int)(vol * 100));
        }

        public float getValue() {
            return (float)this.value;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Do not render title or inventory label
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        int bgColor = 0xFFC6C6C6;
        // Draw the background fill FIRST
        graphics.fill(x + 5, y + 5, x + 171, y + 130, bgColor);

        // Then draw the texture over it
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Cover up the 4 extra slots of the hopper texture (they are at x+44, x+62, x+98, x+116)
        // Leaving only the slot at x+80 (the center one)
        graphics.fill(x + 44, y + 20, x + 44 + 18, y + 20 + 18, bgColor); // Slot 1
        graphics.fill(x + 62, y + 20, x + 62 + 18, y + 20 + 18, bgColor); // Slot 2
        graphics.fill(x + 98, y + 20, x + 98 + 18, y + 20 + 18, bgColor); // Slot 4
        graphics.fill(x + 116, y + 20, x + 116 + 18, y + 20 + 18, bgColor); // Slot 5

        // Redraw the cassette slot (center one) to ensure it's visible. 
        graphics.blit(GUI_TEXTURE, x + 79, y + 20, 79, 19, 18, 18);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
