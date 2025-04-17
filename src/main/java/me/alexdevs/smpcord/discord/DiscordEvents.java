package me.alexdevs.smpcord.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import eu.pb4.placeholders.api.parsers.NodeParser;
import me.alexdevs.smpcord.ChatComponents;
import me.alexdevs.smpcord.Config;
import me.alexdevs.smpcord.SMPCord;
import me.alexdevs.smpcord.parser.MarkdownParser;
import me.alexdevs.smpcord.parser.MentionNodeParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class DiscordEvents {
    private final HashMap<String, String> messageCache = new HashMap<>();
    private final SMPCord smpCord;

    public DiscordEvents(SMPCord smpCord) {
        this.smpCord = smpCord;
    }

    private boolean isActuallyEdited(String id, String content) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            SMPCord.LOGGER.error("sha256 no longer exists :(", e);
            return true;
        }

        var digest = new String(messageDigest.digest(content.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

        if (!messageCache.containsKey(id)) {
            messageCache.put(id, digest);
            return false;
        }

        if (!messageCache.get(id).equals(content)) {
            messageCache.put(id, content);
            return true;
        }

        return false;
    }

    public void onMessageCreate(MessageCreateEvent event) {
        var message = event.getMessage();
        var channel = message.getChannel().block();
        if (channel == null)
            return;

        if (!channel.getId().equals(Snowflake.of(Config.channelId)))
            return;

        if (event.getMember().isEmpty())
            return;

        var member = event.getMember().get();
        if (member.isBot())
            return;

        buildMessage(message, member, false);
    }

    public void onMessageEdit(MessageUpdateEvent event) {
        var message = event.getMessage().block();
        var channel = message.getChannel().block();
        if (channel == null)
            return;

        if (!channel.getId().equals(Snowflake.of(Config.channelId)))
            return;

        var memberOpt = message.getAuthorAsMember().blockOptional();
        if (memberOpt.isEmpty())
            return;

        var member = memberOpt.get();
        if (member.isBot())
            return;

        buildMessage(message, member, true);
    }

    public void buildMessage(Message message, Member member, boolean isEdited) {
        var isActuallyEdited = isActuallyEdited(message.getId().asString(), message.getContent());
        if (isEdited && !isActuallyEdited) {
            return;
        }
        isEdited = isActuallyEdited;

        if (isEdited) {
            //DiscordMessageEvents.MESSAGE_EDIT.invoker().onEdit(message, member);
        } else {
            //DiscordMessageEvents.MESSAGE_CREATE.invoker().onCreate(message, member);
        }

        int memberColor = NamedTextColor.WHITE.value();

        var nullableMemberColor = member.getColor().blockOptional();
        if (nullableMemberColor.isPresent() && nullableMemberColor.get().getRGB() != 0) {
            memberColor = nullableMemberColor.get().getRGB();
        }
        var memberComponent = ChatComponents.makeUser(member.getDisplayName(), member.getMention() + ": ", memberColor, Component.empty());
        Component replyComponent = null;

        if (message.getType() == Message.Type.REPLY && message.getReferencedMessage().isPresent()) {
            var referencedMessage = message.getReferencedMessage();
            Component referenceMemberComponent;
            var referenceMember = referencedMessage.get().getAuthorAsMember().blockOptional();
            if (referenceMember.isPresent()) {
                var referenceMemberColor = NamedTextColor.WHITE.value();
                var nullableReferenceMemberColor = referenceMember.get().getColor().blockOptional();
                if (nullableReferenceMemberColor.isPresent() && nullableReferenceMemberColor.get().getRGB() != 0) {
                    referenceMemberColor = nullableReferenceMemberColor.get().getRGB();
                }
                referenceMemberComponent = ChatComponents.makeUser(referenceMember.get().getDisplayName(), referenceMember.get().getMention() + ": ", referenceMemberColor, Component.empty());
            } else {
                //var referenceData = referencedMessage();
                var referenceAuthor = referencedMessage.get().getAuthor();
                if(referenceAuthor.isPresent()) {
                    var name = referenceAuthor.get().getGlobalName().orElse(referenceAuthor.get().getUsername());
                    referenceMemberComponent = ChatComponents.makeUser(name, referenceAuthor.get().getUsername() + ": ", NamedTextColor.WHITE.value(), Component.empty());
                } else {
                    referenceMemberComponent = ChatComponents.makeUser("Unknown", "<@invalid>" + ": ", NamedTextColor.WHITE.value(), Component.empty());
                }
            }

            replyComponent = ChatComponents.makeReplyHeader(referenceMemberComponent, Component.text(referencedMessage.get().getContent()));
        }

        var messageContent = message.getContent();
        Component messageComponent = Component.empty();

        var parser = NodeParser.merge(new MentionNodeParser(message), MarkdownParser.defaultParser);
        var mdContentVan = parser.parseNode(messageContent).toText();

        var json = net.minecraft.network.chat.Component.Serializer.toJson(mdContentVan, smpCord.server.registryAccess());
        var mdContent = JSONComponentSerializer.json().deserialize(json);

        messageComponent = messageComponent.append(mdContent);

        var attachments = message.getAttachments();
        if (!messageContent.isEmpty()) {
            messageComponent = messageComponent.appendSpace();
        }
        for (var attachment : attachments) {
            messageComponent = messageComponent.append(ChatComponents.makeAttachment(attachment.getFilename(), attachment.getUrl()));
            messageComponent = messageComponent.appendSpace();
        }

        var outputComponent = ChatComponents.makeMessage(memberComponent, replyComponent, messageComponent);

        if (isEdited) {
            outputComponent = outputComponent.append(Component.text("(edited)", NamedTextColor.GRAY));
        }

        smpCord.sendMessage(ChatComponents.toText(outputComponent));
    }

    public void onChatInputInteraction(ChatInputInteractionEvent event) {
        switch (event.getCommandName()) {
            case "link" -> onLinkCommand(event);
            case "list" -> onListCommand(event);
        }

    }

    private void onLinkCommand(ChatInputInteractionEvent event) {
        var code = event.getOption("code").get().getValue().get().asString();
        if (!smpCord.pendingLinks.containsKey(code)) {
            event.reply("Unknown code! Run `/link` in the server to get a code.").subscribe();
            return;
        }

        var uuid = smpCord.pendingLinks.get(code);

        var member = event.getInteraction().getMember().get();
        var userId = member.getId().asString();

        smpCord.links().players.put(uuid, userId);
        try {
            smpCord.links().save();
        } catch (IOException e) {
            SMPCord.LOGGER.error("Error saving links.json", e);
        }

        member.addRole(Snowflake.of(Config.roleId), "Automatic link").subscribe();

        event.reply("You have linked your Discord account!").subscribe();
    }

    private void onListCommand(ChatInputInteractionEvent event) {
        var list = smpCord.server.getPlayerNames();
        String players;
        if (list.length == 0) {
            players = "*There are no players online*";
        } else {
            players = "**Online players**: " + String.join(", ", list);
        }
        event.reply(players).withEphemeral(true).subscribe();
    }
}