package me.alexdevs.smpcord.event;

import net.minecraft.network.chat.ChatType;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import javax.annotation.Nullable;

public class SystemChatEvent extends Event implements ICancellableEvent {
    private final @Nullable ServerPlayer player;
    private final String message;
    private final ChatType.Bound boundChatType;

    public SystemChatEvent(ServerPlayer player, String message, ChatType.Bound boundChatType) {
        this.player = player;
        this.message = message;
        this.boundChatType = boundChatType;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getMessage() {
        return message;
    }

    public ChatType.Bound getBoundChatType() {
        return boundChatType;
    }
}