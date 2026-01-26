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
            ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");

    private static final ResourceLocation CASSETTE_PLAYER_LOGO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Music.MODID, "textures/gui/cassette_player.png");

    private VolumeSlider volumeSlider;

    public CassettePlayerScreen(CassettePlayerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        
        float currentVolume = this.menu.getVolume();


        this.volumeSlider = new VolumeSlider(this.leftPos + 38, this.topPos + 54, 100, 16, (double)currentVolume);
        this.addRenderableWidget(this.volumeSlider);


        this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            ItemStack cassette = this.menu.getSlot(0).getItem();
            if (!cassette.isEmpty()) {
                float vol = this.volumeSlider.getValue();
                CassettePlayerSoundHandler.playMusic(this.minecraft.player.getUUID(), cassette, vol);
            }
        }).bounds(this.leftPos + 110, this.topPos + 34, 22, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("■"), b -> {
            CassettePlayerSoundHandler.stopMusic(this.minecraft.player.getUUID());
        }).bounds(this.leftPos + 137, this.topPos + 34, 22, 20).build());
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
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int bgColor = 0xFFC6C6C6;


        graphics.fill(x + 60, y + 16, x + 116, y + 72, bgColor);


        graphics.blit(GUI_TEXTURE, x + 79, y + 34, 79, 34, 18, 18);
        

    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
