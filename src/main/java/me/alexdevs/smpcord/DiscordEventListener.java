package me.alexdevs.smpcord;

public class DiscordEventListener {
    private final SMPCord smpCord;

    public DiscordEventListener(SMPCord smpCord) {
        this.smpCord = smpCord;
    }

    /*@Override
    public void onMessageReceived(MessageReceivedEvent event) {
        var member = event.getMember();
        var message = event.getMessage();
        var channel = event.getGuildChannel().asStandardGuildMessageChannel();

        if (!channel.getId().equals(Config.channelId))
            return;

        if (member == null)
            return;

        var user = member.getUser();
        if (user.isBot())
            return;


        SMPCord.LOGGER.info(member.getUser().getEffectiveName() + "> " + message.getContentRaw());

        var text = Component.empty()
                .append(Component
                        .literal("D")
                        .withStyle(Style.EMPTY
                                .withColor(Colors.BLURPLE)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Message from the Discord server"))))
                )
                .append(Component.literal(" <"))
                .append(Utils.getMemberNameComponent(member));


        var raw = message.getContentRaw();

        var messageType = event.getMessage().getType();
        if (messageType == MessageType.INLINE_REPLY) {

            var reference = event.getMessage().getMessageReference();
            var refMessage = reference.getMessage();
            String refUserName;
            String refMention;
            var refUserColor = ChatFormatting.WHITE.getColor();
            if (refMessage.getMember() != null) {
                var refMember = refMessage.getMember();
                refUserName = refMember.getEffectiveName();
                refUserColor = refMember.getColorRaw();
                refMention = refMember.getAsMention();
            } else {
                refUserName = refMessage.getAuthor().getEffectiveName();
                refMention = refUserName;
                raw = refUserName + ": " + raw;
            }
            text = text
                    .append(Component.literal(" to ")
                            .withStyle(Style.EMPTY
                                    .withColor(ChatFormatting.GRAY)
                                    .withItalic(true)
                                    .withHoverEvent(new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT, Component.literal(refMessage.getContentDisplay())
                                    ))
                            )
                    )
                    .append(Component
                            .literal("@")
                            .withStyle(Style.EMPTY.withColor(Colors.BLURPLE))
                    )
                    .append(Utils.getMemberNameComponent(refUserName, TextColor.fromRgb(refUserColor), refMention));
        }

        text = text
                .append(Component.literal("> "))
                .append(Component.literal(message.getContentDisplay()));

        var attachments = message.getAttachments();
        if (!attachments.isEmpty()) {
            text = text.append(Component.literal(" "));
            for (var attachment : attachments) {
                text = text.append(Component
                                .literal("[" + attachment.getFileName() + "]")
                                .withStyle(Style.EMPTY
                                        .withColor(ChatFormatting.BLUE)
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open URL: " + attachment.getUrl())))
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()))))
                        .append(Component.literal(" "));
            }
        }

        smpCord.sendMessage(text, raw);
    }*/
}