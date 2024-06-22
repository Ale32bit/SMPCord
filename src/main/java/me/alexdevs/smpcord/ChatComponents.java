package me.alexdevs.smpcord;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;

import javax.annotation.Nullable;

public class ChatComponents {
    public static final Component discordMessagePrefix = Component
            .literal("D")
            .withStyle(Style.EMPTY
                    .withColor(0x5865F2) // Discord Blurple color
                    .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Message from the Discord server")
                    ))
                    .withClickEvent(new ClickEvent(
                            ClickEvent.Action.OPEN_URL,
                            Config.inviteLink
                    ))
            );

    public static final Component mentionIcon = Component
            .literal("@")
            .withStyle(Style.EMPTY
                    .withColor(0x5865F2));

    public static final Component channelIcon = Component
            .literal("#")
            .withStyle(Style.EMPTY
                    .withColor(0x5865F2));

    public static final Component WHITESPACE = Component.literal(" ");

    public static Component makeUser(String name, String suggest, int color, @Nullable Component prefix) {
        var comp =  Component.literal(name)
                .withStyle(Style.EMPTY
                        .withColor(color)
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click to mention")
                        ))
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.SUGGEST_COMMAND,
                                suggest
                        ))
                );

        if (prefix != null)
            return Component.empty()
                    .append(prefix)
                    .append(comp);

        return comp;
    }

    public static Component makeMessageHeader(Component content) {
        return Component.empty()
                .append(Component.literal("<"))
                .append(content)
                .append(">");
    }

    public static Component makeReplyHeader(Component user, Component referenceUser, Component referenceMessage) {
        return Component.empty()
                .append(user)
                .append(Component
                        .literal(" replied to ")
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.GRAY)
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component
                                                .literal("Message: ")
                                                .append(referenceMessage)
                                )))
                )
                .append(referenceUser);
    }

    public static Component makeMessage(Component headerContent, Component message) {
        return Component.empty()
                .append(discordMessagePrefix)
                .append(WHITESPACE)
                .append(makeMessageHeader(headerContent))
                .append(WHITESPACE)
                .append(message);
    }

    public static Component makeAttachment(String fileName, String url) {
        return Component
                .literal(String.format("[%s]", fileName))
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.BLUE)
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click to open attachment")
                        ))
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.OPEN_URL,
                                url
                        ))
                );
    }
}
