package net.walkman.walkman;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.walkman.music.Music;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Music.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        CassettePlayerSoundHandler.clientTick();
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        while (KeyBindings.OPEN_CONFIG.consumeClick()) {
            mc.setScreen(new WalkmanConfigScreen());
        }

        while (KeyBindings.PLAY_PAUSE.consumeClick()) {
            boolean isPlaying = CassettePlayerSoundHandler.isPlaying(mc.player.getUUID());
            boolean nowPlaying = CassettePlayerSoundHandler.togglePlayback(mc.player.getUUID());
            if (nowPlaying) {
                mc.player.displayClientMessage(Component.translatable("message.walkman.playing").withStyle(ChatFormatting.GREEN), true);
            } else if (isPlaying) {
                mc.player.displayClientMessage(Component.translatable("message.walkman.paused").withStyle(ChatFormatting.YELLOW), true);
            }
        }

        while (KeyBindings.STOP.consumeClick()) {
            CassettePlayerSoundHandler.stopAllMusic();
            mc.player.displayClientMessage(Component.translatable("message.walkman.stopped").withStyle(ChatFormatting.RED), true);
        }

        while (KeyBindings.EJECT.consumeClick()) {
            if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr) {
                net.minecraft.core.BlockPos pos = bhr.getBlockPos();
                if (mc.level.getBlockState(pos).is(Music.BOOMBOX.get())) {
                    if (mc.gameMode != null) {
                        mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, bhr);
                    }
                }
            }
            if (CassettePlayerSoundHandler.isPlaying(mc.player.getUUID())) {
                CassettePlayerSoundHandler.stopMusic(mc.player.getUUID().toString());
                mc.player.displayClientMessage(Component.translatable("message.walkman.cassette_ejected").withStyle(ChatFormatting.YELLOW), true);
            }
        }

        while (KeyBindings.NEXT_TRACK.consumeClick()) {
            CassettePlayerSoundHandler.skipTrack(mc.player.getUUID());
            mc.player.displayClientMessage(Component.translatable("message.walkman.next_track").withStyle(ChatFormatting.AQUA), true);
        }

        while (KeyBindings.PREV_TRACK.consumeClick()) {
            CassettePlayerSoundHandler.previousTrack(mc.player.getUUID());
            mc.player.displayClientMessage(Component.translatable("message.walkman.prev_track").withStyle(ChatFormatting.AQUA), true);
        }
    }
}
