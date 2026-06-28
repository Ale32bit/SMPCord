package me.alexdevs.smpcord.mixin;

import me.alexdevs.smpcord.event.SystemChatEvent;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.commands.SayCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(PlayerList.class)
public class SayEventMixin {
    // Lnet/minecraft/server/players/PlayerList;broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V

    @Inject(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void bridge$injectEvent(PlayerChatMessage message, Predicate<ServerPlayer> shouldFilterMessageTo, ServerPlayer sender, ChatType.Bound boundChatType, CallbackInfo ci) {
        var boundKey = boundChatType.chatType().getKey();
        if (boundKey == ChatType.SAY_COMMAND || boundKey == ChatType.EMOTE_COMMAND) {
            var ev = NeoForge.EVENT_BUS.post(new SystemChatEvent(sender, message.signedContent(), boundChatType));

            if (ev.isCanceled()) {
                ci.cancel();
            }
        }
    }
}
