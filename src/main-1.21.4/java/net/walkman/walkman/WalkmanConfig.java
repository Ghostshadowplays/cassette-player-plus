package net.walkman.walkman;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.*;
import java.util.Properties;

@OnlyIn(Dist.CLIENT)
public class WalkmanConfig {
    private static VisualSet currentSet = VisualSet.MODERN;

    public enum VisualSet {
        RETRO("retro"),
        MODERN("modern");

        private final String name;
        VisualSet(String name) { this.name = name; }
        public String getName() { return name; }
        public static VisualSet fromName(String name) {
            for (VisualSet set : values()) {
                if (set.name.equalsIgnoreCase(name)) return set;
            }
            return MODERN;
        }
    }

    public static void load() {
        File configFile = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("walkman-client.properties").toFile();
        if (configFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
                String val = props.getProperty("visualSet", "modern");
                currentSet = VisualSet.fromName(val);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        File configFile = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("walkman-client.properties").toFile();
        Properties props = new Properties();
        props.setProperty("visualSet", currentSet.getName());
        if (configFile.getParentFile() != null) {
            configFile.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Walkman Client Config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static VisualSet getVisualSet() {
        return currentSet;
    }

    public static void setVisualSet(VisualSet set) {
        currentSet = set;
        save();
    }
}
