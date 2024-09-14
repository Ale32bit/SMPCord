package me.alexdevs.smpcord.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import me.alexdevs.smpcord.ChatComponents;
import me.alexdevs.smpcord.Colors;
import me.alexdevs.smpcord.Config;
import me.alexdevs.smpcord.SMPCord;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordEvents {
    private static final Pattern mentionPattern = Pattern.compile("(<@[!&]?\\d+>|<#\\d+>)");
    private static final Pattern integerPattern = Pattern.compile("\\d+");
    private final SMPCord smpCord;

    public DiscordEvents(SMPCord smpCord) {
        this.smpCord = smpCord;
    }

    public static List<String> splitMessage(String message) {
        List<String> parts = new ArrayList<>();
        Matcher matcher = mentionPattern.matcher(message);

        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                parts.add(message.substring(lastEnd, matcher.start()));
            }
            parts.add(matcher.group(1));
            lastEnd = matcher.end();
        }

        if (lastEnd < message.length()) {
            parts.add(message.substring(lastEnd));
        }

        return parts;
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

        int memberColor = ChatFormatting.WHITE.getColor();

        var nullableMemberColor = member.getColor().block();
        if (nullableMemberColor != null) {
            memberColor = nullableMemberColor.getRGB();
        }
        var memberComponent = ChatComponents.makeUser(member.getDisplayName(), member.getMention() + ": ", memberColor, Component.empty());
        Component messageHeaderComponent;

        if (message.getType() == Message.Type.REPLY && message.getReferencedMessage().isPresent()) {
            var referencedMessage = message.getReferencedMessage().get();
            Component referenceMemberComponent;
            var referenceMember = referencedMessage.getAuthorAsMember().block();
            if (referenceMember != null) {
                var referenceMemberColor = ChatFormatting.WHITE.getColor();
                var nullableReferenceMemberColor = member.getColor().block();
                if (nullableReferenceMemberColor != null) {
                    referenceMemberColor = nullableReferenceMemberColor.getRGB();
                }
                referenceMemberComponent = ChatComponents.makeUser(referenceMember.getDisplayName(), referenceMember.getMention() + ": ", referenceMemberColor, Component.empty());
            } else if (referencedMessage.getAuthor().isPresent()) {
                var referenceAuthor = referencedMessage.getAuthor().get();
                referenceMemberComponent = ChatComponents.makeUser(referenceAuthor.getUsername(), referenceAuthor.getMention() + ": ", ChatFormatting.WHITE.getColor(), Component.empty());
            } else {
                var referenceData = referencedMessage.getData();
                var referenceAuthor = referenceData.author();
                referenceMemberComponent = ChatComponents.makeUser(referenceAuthor.username(), referenceAuthor.username() + ": ", ChatFormatting.WHITE.getColor(), Component.empty());
            }

            messageHeaderComponent = ChatComponents.makeReplyHeader(memberComponent, referenceMemberComponent, Component.literal(referencedMessage.getContent()));
        } else {
            messageHeaderComponent = memberComponent;
        }

        var messageContent = message.getContent();
        var messageComponent = Component.empty();

        var splitContent = splitMessage(messageContent);
        var memberMentions = message.getMemberMentions();
        var roleMentions = message.getRoleMentions();
        for (var part : splitContent) {
            if (part.matches(mentionPattern.pattern())) {
                var matcher = integerPattern.matcher(part);
                if(matcher.find()) {
                    var snowflakeId = Snowflake.of(matcher.group());
                    if (part.startsWith("<@&")) { // Role mention
                        var mentionedRole = roleMentions.filter(p -> p.getId().equals(snowflakeId)).blockFirst();
                        if (mentionedRole != null) {
                            int color = mentionedRole.getColor().getRGB();
                            if(color == 0) {
                                color = 0x99aab5;
                            }
                            messageComponent.append(ChatComponents.makeUser(
                                    mentionedRole.getName(),
                                    mentionedRole.getMention() + ": ",
                                    color,
                                    ChatComponents.mentionIcon
                            ));
                        } else {
                            messageComponent.append(ChatComponents.makeUser(
                                    "unknown-role",
                                    String.format("<@&%s>: ", snowflakeId.asString()),
                                    ChatFormatting.WHITE.getColor(),
                                    ChatComponents.mentionIcon
                            ));
                        }
                    } else if (part.startsWith("<@") || part.startsWith("<@!")) { // Member mention
                        var mentionedOpt = memberMentions.stream().filter(p -> p.getId().equals(snowflakeId)).findFirst();
                        if (mentionedOpt.isPresent()) {
                            var mentioned = mentionedOpt.get();
                            messageComponent.append(ChatComponents.makeUser(
                                    mentioned.getDisplayName(),
                                    mentioned.getMention() + ": ",
                                    Colors.MENTION.getValue(),
                                    ChatComponents.mentionIcon
                            ));
                        } else {
                            messageComponent.append(ChatComponents.makeUser(
                                    "unknown-user",
                                    String.format("<@%s>: ", snowflakeId.asString()),
                                    Colors.MENTION.getValue(),
                                    ChatComponents.mentionIcon
                            ));
                        }
                    } else if (part.startsWith("<#")) { // Channel mention
                        var mentionedChannel = message.getClient().getChannelById(snowflakeId).block();
                        if (mentionedChannel != null
                                && (mentionedChannel.getType() == Channel.Type.GUILD_TEXT
                                || mentionedChannel.getType() == Channel.Type.GUILD_VOICE
                                || mentionedChannel.getType() == Channel.Type.GUILD_NEWS)) {

                            var guildChannel = (GuildChannel) mentionedChannel;
                            messageComponent.append(ChatComponents.makeUser(
                                    guildChannel.getName(),
                                    guildChannel.getMention() + ": ",
                                    Colors.MENTION.getValue(),
                                    ChatComponents.channelIcon
                            ));
                        } else {
                            messageComponent.append(ChatComponents.makeUser(
                                    "unknown",
                                    String.format("<#%s>: ", snowflakeId.asString()),
                                    Colors.MENTION.getValue(),
                                    ChatComponents.channelIcon
                            ));
                        }
                    }
                } else {
                    messageComponent.append(Component.literal(part));
                }

            } else {
                messageComponent.append(Component.literal(part));
            }
        }

        var attachments = message.getAttachments();
        if (!messageContent.isEmpty()) {
            messageComponent.append(ChatComponents.WHITESPACE);
        }
        for (var attachment : attachments) {
            messageComponent.append(ChatComponents.makeAttachment(attachment.getFilename(), attachment.getUrl()));
        }

        var outputComponent = ChatComponents.makeMessage(messageHeaderComponent, messageComponent);

        smpCord.sendMessage(outputComponent);
    }
}