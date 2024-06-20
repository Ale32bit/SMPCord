package me.alexdevs.smpcord;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;

public class DiscordEvents {
    private final SMPCord smpCord;

    public DiscordEvents(SMPCord smpCord) {
        this.smpCord = smpCord;
    }

    public void onMessageCreate(MessageCreateEvent event) {
        if(event.getMember().isEmpty())
            return;
        var member = event.getMember().get();
        var message = event.getMessage();
        var channel = message.getChannel().block();
        if(channel == null)
            return;

        if (!channel.getId().equals(Snowflake.of(Config.channelId)))
            return;

        if (member.isBot())
            return;


        SMPCord.LOGGER.info(member.getDisplayName() + "> " + message.getContent());

        var text = Component.empty()
                .append(Component
                        .literal("D")
                        .withStyle(Style.EMPTY
                                .withColor(Colors.BLURPLE)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Message from the Discord server"))))
                )
                .append(Component.literal(" <"))
                .append(Utils.getMemberNameComponent(member));


        var raw = message.getContent();

        message.getType();
        var messageType = event.getMessage().getType();
        if (messageType == Message.Type.REPLY && message.getReferencedMessage().isPresent()) {

            var refMessage = message.getReferencedMessage().get();
            String refUserName;
            String refMention;
            var refUserColor = Color.of(ChatFormatting.WHITE.getColor());
            if (refMessage.getAuthor().isPresent()) {
                var refMember = refMessage.getAuthorAsMember().block();
                if(refMember != null) {
                    refUserName = refMember.getDisplayName();
                    refUserColor = refMember.getColor().block();
                    refMention = refMember.getMention();
                } else {
                    var author = refMessage.getAuthor().get();
                    refUserName = author.getUsername();
                    refMention = author.getMention();
                }

            } else {
                var data = refMessage.getData();
                var author = data.author();
                refUserName = author.username();
                refMention = refUserName;
                raw = refUserName + ": " + raw;
            }
            text = text
                    .append(Component.literal(" to ")
                            .withStyle(Style.EMPTY
                                    .withColor(ChatFormatting.GRAY)
                                    .withItalic(true)
                                    .withHoverEvent(new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT, Component.literal(refMessage.getContent())
                                    ))
                            )
                    )
                    .append(Component
                            .literal("@")
                            .withStyle(Style.EMPTY.withColor(Colors.BLURPLE))
                    )
                    .append(Utils.getMemberNameComponent(refUserName, TextColor.fromRgb(refUserColor.getRGB()), refMention));
        }

        text = text
                .append(Component.literal("> "))
                .append(Component.literal(message.getContent()));

        var attachments = message.getAttachments();
        if (!attachments.isEmpty()) {
            text = text.append(Component.literal(" "));
            for (var attachment : attachments) {
                text = text.append(Component
                                .literal("[" + attachment.getFilename() + "]")
                                .withStyle(Style.EMPTY
                                        .withColor(ChatFormatting.BLUE)
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open URL: " + attachment.getUrl())))
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()))))
                        .append(Component.literal(" "));
            }
        }

        smpCord.sendMessage(text, raw);
    }
}