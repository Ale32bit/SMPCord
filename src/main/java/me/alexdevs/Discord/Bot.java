package me.alexdevs.Discord;

import com.velocitypowered.api.proxy.ProxyServer;
import me.alexdevs.SMPCord;
import me.alexdevs.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.slf4j.Logger;

public class Bot {
    private final SMPCord smpCord;
    private final ProxyServer proxy;
    private final Logger logger;
    private final JDA jda;
    private final TextChannel channel;
    private Webhook webhook;

    public Bot(SMPCord smpCord) throws InterruptedException {
        this.smpCord = smpCord;
        var config = smpCord.getConfig();
        proxy = smpCord.getProxy();
        logger = smpCord.getLogger();

        var api = JDABuilder.createDefault(config.getToken(),
            GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS);
        jda = api.build();
        jda.setAutoReconnect(true);

        jda.addEventListener(new EventListener());
        jda.addEventListener(new LinkCommand());

        jda.awaitReady();
        channel = jda.getTextChannelById(config.getChannel());
        var webhooks = channel.retrieveWebhooks().complete();
        var webhook = webhooks.stream().filter(wh -> wh.getName().equals(config.getWebhookName())).findFirst();
        this.webhook = webhook.orElseGet(() -> channel.createWebhook(config.getWebhookName()).complete());

        var guild = channel.getGuild();
        guild.updateCommands()
            .addCommands(
                Commands.slash("link", "Join the whitelist")
                    .addOption(OptionType.STRING, "code", "The code to link your Discord account")
            ).queue();
    }

    public JDA getJda() {
        return jda;
    }

    public TextChannel getChannel() {
        return channel;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public class LinkCommand extends ListenerAdapter {
        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
            if (!event.getName().equals("link")) {
                return;
            }

            var code = event.getOption("code").getAsString();
            var userId = event.getUser().getId();

            var codes = smpCord.getWhitelistCodes();
            var whitelistConfig = smpCord.getWhitelistConfig();

            var whitelist = whitelistConfig.getWhitelist();
            if (whitelist.containsValue(userId)) {
                event.reply("You already linked your account!")
                    .setEphemeral(true).queue();
                return;
            }

            if (!codes.containsValue(code)) {
                event.reply("Could not find the code!")
                    .setEphemeral(true).queue();
                return;
            }

            var guild = event.getGuild();
            if (guild == null)
                return;

            var playerUuid = Utils.getKeyByValue(codes, code);
            var playerName = smpCord.usernamesCache.get(playerUuid);
            try {
                var member = event.getMember();
                member.modifyNickname(playerName).queue();
                var role = guild.getRoleById(smpCord.getConfig().getRoleId());
                if (role == null) {
                    logger.error("Role is null!");
                } else {
                    guild.addRoleToMember(member, role).queue();
                }
            } catch (Exception e) {
                logger.error(e.toString());
            }

            whitelist.put(playerUuid, userId);
            smpCord.saveList();

            event.reply("You are now linked to " + playerName)
                .setEphemeral(true).queue();
        }
    }

    public class EventListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            var member = event.getMember();
            var message = event.getMessage();
            var channel = event.getGuildChannel().asStandardGuildMessageChannel();
            var config = smpCord.getConfig();

            if (!channel.getId().equals(config.getChannel()))
                return;

            if (member == null)
                return;

            var user = member.getUser();
            if (user.isBot())
                return;

            var text = Component.empty()
                .append(Component
                    .text("D")
                    .color(TextColor.color(0x5865F2))
                    .hoverEvent(Component.text("Message from Discord")))
                .appendSpace()
                .append(Component.text("<"))
                .append(getMemberNameComponent(member))
                .append(Component.text(">"))
                .appendSpace();


            var raw = message.getContentRaw();

            var messageType = event.getMessage().getType();
            if (messageType == MessageType.INLINE_REPLY) {

                var reference = event.getMessage().getMessageReference();
                var refMessage = reference.getMessage();
                String refUserName;
                String refMention;
                var refUserColor = NamedTextColor.WHITE.value();
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
                    .append(Component
                        .text("@")
                        .color(NamedTextColor.BLUE)
                    )
                    .append(getMemberNameComponent(refUserName, refUserColor, refMention))
                    .append(Component
                        .text(":")
                        .color(NamedTextColor.GRAY)
                    )
                    .appendSpace();
            }

            text = text.append(Component.text(message.getContentDisplay()));

            var attachments = message.getAttachments();
            if (!attachments.isEmpty()) {
                text = text.appendSpace();
                for (var attachment : attachments) {
                    text = text.append(Component
                        .text("[" + attachment.getFileName() + "]")
                        .color(NamedTextColor.BLUE)
                        .hoverEvent(HoverEvent.showText(Component.text("Click to open URL: " + attachment.getUrl())))
                        .clickEvent(ClickEvent.openUrl(attachment.getUrl())))
                        .appendSpace();
                }
            }

            smpCord.sendMessage(text, raw);
        }

        private Component getMemberNameComponent(Member member) {
            return getMemberNameComponent(member.getEffectiveName(), member.getColorRaw(), member.getAsMention());
        }

        private Component getMemberNameComponent(String name, int color, String asMention) {
            return Component
                .text(name)
                .color(TextColor.color(color))
                .hoverEvent(HoverEvent.showText(Component.text("Click to mention")))
                .clickEvent(ClickEvent.suggestCommand(asMention + ": "));
        }

        private String getMemberName(Member member) {
            if (member == null) {
                return "Unknown User";
            }
            var name = member.getNickname();
            if (name == null)
                name = member.getUser().getName();
            return name;
        }
    }
}
