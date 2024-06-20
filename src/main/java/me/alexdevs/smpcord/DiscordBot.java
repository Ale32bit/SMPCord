package me.alexdevs.smpcord;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.TextChannel;
import reactor.core.publisher.Mono;

public class DiscordBot {
    private final SMPCord smpCord;
    private final TextChannel channel;
    private Webhook webhook;
    private DiscordClient client;
    private GatewayDiscordClient gatewayClient;

    public DiscordBot(SMPCord smpCord) throws InterruptedException {
        this.smpCord = smpCord;

        if(Config.botToken.isBlank()) {
            SMPCord.LOGGER.error("Bot token field is empty!");
            channel = null;
            return;
        }

        SMPCord.LOGGER.info("Logging into Discord...");

        client = DiscordClient.create(Config.botToken);
        gatewayClient = client.login().block();

        final var self = gatewayClient.getSelf().block();
        SMPCord.LOGGER.info("Logged in as {}", self.getTag());

        channel = (TextChannel) gatewayClient.getChannelById(Snowflake.of(Config.channelId)).block();

        if(channel == null) {
            SMPCord.LOGGER.error("Channel not found! Set an existing channel ID that I can see!");
            return;
        }
        this.webhook = channel.getWebhooks().filter(wh -> wh.getName().equals(Config.webhookName)).singleOrEmpty().block();
        if(this.webhook == null) {
            this.webhook = channel.createWebhook(Config.webhookName).block();
        }
    }

    public TextChannel getChannel() {
        return channel;
    }

    public Webhook getWebhook() {
        return webhook;
    }
}
