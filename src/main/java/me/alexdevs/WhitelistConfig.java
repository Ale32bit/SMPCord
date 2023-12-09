package me.alexdevs;

import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class WhitelistConfig {
    @Expose
    private final HashMap<UUID, String> whitelist;
    public WhitelistConfig() {
        whitelist = new HashMap<>();
    }

    public HashMap<UUID, String> getWhitelist() {
        return whitelist;
    }
}
