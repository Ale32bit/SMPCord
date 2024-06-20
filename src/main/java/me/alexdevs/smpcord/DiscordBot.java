package me.alexdevs.smpcord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.internal.utils.JDALogger;

public class DiscordBot {
    private final SMPCord smpCord;
    private final TextChannel channel;
    private Webhook webhook;
    private JDA jda;

    public DiscordBot(SMPCord smpCord) throws InterruptedException {
        this.smpCord = smpCord;
        JDALogger.setFallbackLoggerEnabled(false);

        if(Config.botToken.isBlank()) {
            SMPCord.LOGGER.error("Bot token field is empty!");
            channel = null;
            return;
        }

        var api = JDABuilder.createDefault(Config.botToken,
                GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS);
        jda = api.build();
        jda.setAutoReconnect(true);

        jda.addEventListener(new DiscordEventListener(smpCord));

        jda.awaitReady();
        channel = jda.getTextChannelById(Config.channelId);
        if(channel == null) {
            SMPCord.LOGGER.error("Channel not found! Set an existing channel ID that I can see!");
            jda.awaitShutdown();
            return;
        }
        var webhooks = channel.retrieveWebhooks().complete();
        var webhook = webhooks.stream().filter(wh -> wh.getName().equals(Config.webhookName)).findFirst();
        this.webhook = webhook.orElseGet(() -> channel.createWebhook(Config.webhookName).complete());
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
}
