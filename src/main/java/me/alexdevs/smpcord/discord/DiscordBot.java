package me.alexdevs.smpcord.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.presence.ClientPresence;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.interaction.GuildCommandRegistrar;
import me.alexdevs.smpcord.Config;
import me.alexdevs.smpcord.SMPCord;
import reactor.core.publisher.Mono;

import java.util.List;

public class DiscordBot {
    private final SMPCord smpCord;
    private TextChannel channel;
    private Webhook webhook;
    private GatewayDiscordClient client;
    private DiscordEvents events;

    public TextChannel getChannel() {
        return channel;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public DiscordBot(SMPCord smpCord) throws InterruptedException {
        this.smpCord = smpCord;
        events = new DiscordEvents(smpCord);

        if (Config.botToken.isBlank()) {
            SMPCord.LOGGER.error("Bot token field is empty!");
            channel = null;
            return;
        }

        SMPCord.LOGGER.info("Logging into Discord...");

        var clientMono = DiscordClientBuilder.create(Config.botToken)
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.of(
                        Intent.MESSAGE_CONTENT,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_MEMBERS
                ))
                .login();

        clientMono.flatMap(client -> {
            client.on(ReadyEvent.class).subscribe(event -> {
                this.client = client;
                final var self = event.getSelf();
                SMPCord.LOGGER.info("Logged in as {}", self.getTag());

                channel = (TextChannel) client.getChannelById(Snowflake.of(Config.channelId)).block();

                if (channel == null) {
                    SMPCord.LOGGER.error("Channel not found! Set an existing channel ID that I can see!");
                    return;
                }
                this.webhook = channel.getWebhooks().filter(wh -> wh.getName().get().equals(Config.webhookName)).singleOrEmpty().block();
                if (this.webhook == null) {
                    this.webhook = channel.createWebhook(Config.webhookName).block();
                }

                var guild = channel.getGuild().block();

                var linkCommand = ApplicationCommandRequest.builder()
                        .name("link")
                        .description("Link your Minecraft profile with Discord.")
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("code")
                                .description("Linking code")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .build();

                var listCommand = ApplicationCommandRequest.builder()
                        .name("list")
                        .description("Get online players.")
                        .build();

                GuildCommandRegistrar.create(client.getRestClient(), List.of(linkCommand, listCommand))
                        .registerCommands(guild.getId())
                        .doOnError(e -> SMPCord.LOGGER.error("Error registering guild commands", e))
                        .onErrorResume(e -> Mono.empty())
                        .blockLast();
            });

            client.on(MessageCreateEvent.class).subscribe(event -> {
                try {
                    events.onMessageCreate(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            client.on(MessageUpdateEvent.class).subscribe(event -> {
                try {
                    events.onMessageEdit(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            client.on(ChatInputInteractionEvent.class).subscribe(event -> {
                try {
                    events.onChatInputInteraction(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return Mono.empty();
        }).block();
        clientMono.subscribe();
    }

    public void setPresence(ClientPresence presence) {
        client.updatePresence(presence).subscribe();
    }
}
