package me.alexdevs.smpcord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

public class Links {
    public static Path dataPath = FMLPaths.GAMEDIR.get().resolve("links.json");
    @Expose
    public final HashMap<UUID, String> players;

    public Links(HashMap<UUID, String> players) {
        this.players = players;
    }

    public Links() {
        players = new HashMap<>();
    }

    public static Links load() throws IOException {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        if(!dataPath.toFile().exists()) {
            return new Links();
        }

        var content = Files.readString(dataPath);
        return gson.fromJson(content, Links.class);
    }

    public void save() throws IOException {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        var content = gson.toJson(this);
        Files.writeString(dataPath, content);
    }
}
