package net.walkman.walkman;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.walkman.music.Music;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Music.MODID, value = Dist.CLIENT)
public class KeyBindings {
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.walkman.open_config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.walkman"
    );

    public static final KeyMapping PLAY_PAUSE = new KeyMapping(
            "key.walkman.play_pause",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.walkman"
    );

    public static final KeyMapping STOP = new KeyMapping(
            "key.walkman.stop",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.walkman"
    );

    public static final KeyMapping EJECT = new KeyMapping(
            "key.walkman.eject",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.walkman"
    );

    public static final KeyMapping NEXT_TRACK = new KeyMapping(
            "key.walkman.next_track",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_PERIOD,
            "key.categories.walkman"
    );

    public static final KeyMapping PREV_TRACK = new KeyMapping(
            "key.walkman.prev_track",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA,
            "key.categories.walkman"
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
        event.register(PLAY_PAUSE);
        event.register(STOP);
        event.register(EJECT);
        event.register(NEXT_TRACK);
        event.register(PREV_TRACK);
    }
}
