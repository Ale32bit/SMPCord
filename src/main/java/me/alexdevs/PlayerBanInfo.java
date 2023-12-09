package me.alexdevs;

import com.google.gson.annotations.Expose;
import com.velocitypowered.api.proxy.Player;

import java.util.Optional;
import java.util.UUID;

public class PlayerBanInfo {
    @Expose
    private final UUID uuid;
    @Expose
    private final UUID bannedByUuid;


    public PlayerBanInfo(UUID uuid, UUID banInvoker) {
        this.uuid = uuid;
        this.bannedByUuid = banInvoker;
    }

    public PlayerBanInfo(Player player, Player banInvoker) {
        this.uuid = player.getUniqueId();
        this.bannedByUuid = banInvoker.getUniqueId();
    }
}
