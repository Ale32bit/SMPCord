package me.alexdevs;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.*;
import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.alexdevs.Discord.Bot;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(id = "smpcord",
    name = "SMPCord", version = "0.1.0-SNAPSHOT", url = "https://alexdevs.me",
    authors = {"AlexDevs"}, description = "Discord bridge for Devs.SMP();")
public class SMPCord {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private SMPCordConfig config;
    private Bot discordBot;

    @Inject
    public SMPCord(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    private void load() {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Path configPath = dataDirectory.resolve("SMPCord.toml");
        config = SMPCordConfig.read(configPath);

        try {
            discordBot = new Bot(this);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Subscribe
    private void onProxyInitialize(ProxyInitializeEvent event) {
        load();

        proxy.getEventManager().register(this, new SMPCordListener());
    }

    @Subscribe
    private void onProxyReload(ProxyReloadEvent event) {
        discordBot.getJda().shutdownNow();
        load();
    }

    public SMPCordConfig getConfig() {
        return config;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public class SMPCordListener {
        @Subscribe(order = PostOrder.LATE)
        private void onPostLogin(PostLoginEvent event) {
            var player = event.getPlayer();
            var username = player.getUsername();

            var embed = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(String.format("%s joined the server", username), getAvatarThumbnailUrl(player), null))
                .setColor(NamedTextColor.GREEN.value())
                .build();

            var webhook = new WebhookMessageBuilder()
                .addEmbeds(embed)
                .build();

            sendWebhook(webhook);
        }

        @Subscribe
        private void onDisconnect(DisconnectEvent event) {
            var player = event.getPlayer();
            var username = player.getUsername();

            var embed = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(String.format("%s left the server", username), getAvatarThumbnailUrl(player), null))
                .setColor(NamedTextColor.RED.value())
                .build();

            var webhook = new WebhookMessageBuilder()
                .addEmbeds(embed)
                .build();

            sendWebhook(webhook);
        }

        @Subscribe(order = PostOrder.EARLY)
        private void onPlayerChat(PlayerChatEvent event) {
            var player = event.getPlayer();
            var message = event.getMessage();
            var avatarUrl = getAvatarUrl(player);

            var webhookMessage = new WebhookMessageBuilder()
                .setUsername(player.getUsername())
                .setContent(message)
                .setAvatarUrl(avatarUrl)
                .setAllowedMentions(new AllowedMentions()
                    .withParseUsers(true))
                .build();

            sendWebhook(webhookMessage);
        }
        public void sendWebhook(WebhookMessage message) {
            var webhook = discordBot.getWebhook();
            try (var client = JDAWebhookClient.from(webhook)) {
                client.send(message);
            }
        }

        private String getAvatarUrl(Player player) {
            var avatarApiUrl = config.getAvatarApiUrl();
            return avatarApiUrl.replaceAll("\\{\\{uuid\\}\\}", player.getUniqueId().toString());
        }

        private String getAvatarThumbnailUrl(Player player) {
            var avatarApiUrl = config.getAvatarThumbnailApiUrl();
            return avatarApiUrl.replaceAll("\\{\\{uuid\\}\\}", player.getUniqueId().toString());
        }
    }
}
