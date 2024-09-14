package me.alexdevs.smpcord.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.alexdevs.smpcord.SMPCord;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.UUID;

public class LinkCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var builder = LiteralArgumentBuilder
                .<CommandSourceStack>literal("link")
                .executes(ctx -> {
                    var source = ctx.getSource();
                    var uuid = source.getPlayer().getUUID();
                    var pendingLinks = SMPCord.instance().pendingLinks;
                    String code;
                    if (pendingLinks.containsValue(uuid)) {
                        code = keyOf(uuid);

                    } else {
                        do {
                            code = randomCode();
                        } while (pendingLinks.containsKey(code));
                        pendingLinks.put(code, uuid);
                    }

                    SMPCord.instance().usernameCache.put(uuid, source.getPlayer().getName().getString());

                    // ??? java moment
                    final String finalCode = code;
                    source.sendSuccess(() -> Component
                            .literal("Link your Discord account by running the following command on the Discord server: ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.literal("/link " + finalCode)
                                    .setStyle(Style.EMPTY
                                            .withColor(ChatFormatting.BLUE)
                                            .withUnderlined(true)
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy command")))
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "/link " + finalCode))
                                    )
                            ), false);

                    return 0;
                });

        dispatcher.register(builder);
    }

    public static String keyOf(UUID uuid) {
        for (var entry : SMPCord.instance().pendingLinks.entrySet()) {
            if (entry.getValue().equals(uuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static String randomCode() {
        var code = (int) Math.floor(Math.random() * 1e6);
        return String.format("%06d", code);
    }
}
