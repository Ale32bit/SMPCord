package me.alexdevs.Discord;

import com.velocitypowered.api.proxy.ProxyServer;
import me.alexdevs.SMPCord;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.kyori.adventure.text.Component;
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

        jda.awaitReady();
        channel = jda.getTextChannelById(config.getChannel());
        var webhooks = channel.retrieveWebhooks().complete();
        var webhook = webhooks.stream().filter(wh -> wh.getName().equals(config.getWebhookName())).findFirst();
        this.webhook = webhook.orElseGet(() -> channel.createWebhook(config.getWebhookName()).complete());
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

    public class EventListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            var member = event.getMember();
            var message = event.getMessage();
            var channel = event.getGuildChannel().asStandardGuildMessageChannel();
            var config = smpCord.getConfig();

            if (!channel.getId().equals(config.getChannel()))
                return;

            if(member == null)
                return;

            var user = member.getUser();
            if (user.isBot())
                return;

            var text = Component.empty()
                .append(Component
                    .text("[")
                    .color(NamedTextColor.WHITE))
                .append(Component
                    .text("D")
                    .color(TextColor.color(0x5865F2)))
                .append(Component
                    .text("]")
                    .color(NamedTextColor.WHITE))
                .appendSpace()
                .append(Component
                    .text(getMemberName(member))
                    .color(TextColor.color(member.getColorRaw())))
                .append(Component.text(":"))
                .appendSpace()
                .append(Component.text(message.getContentRaw()));

            proxy.sendMessage(text);
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
