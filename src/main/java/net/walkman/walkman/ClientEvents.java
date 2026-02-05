package net.walkman.walkman;

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
        
        while (KeyBindings.OPEN_CONFIG.consumeClick()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new WalkmanConfigScreen());
        }
    }
}
