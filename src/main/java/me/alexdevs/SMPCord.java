package me.alexdevs;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.*;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import me.alexdevs.Discord.Bot;
import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

@Plugin(id = "smpcord",
    name = "SMPCord", version = "0.2.0", url = "https://alexdevs.me",
    authors = {"AlexDevs"}, description = "Whitelist manager and Discord chat bridge for Devs.SMP();")
public class SMPCord {
    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("smpcord:events");
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private SMPCordConfig config;
    private Bot discordBot;

    private WhitelistConfig whitelistConfig;
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final HashMap<UUID, String> whitelistCodes = new HashMap<>();

    public final HashMap<UUID, String> usernamesCache = new HashMap<>();

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

        loadList();

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
        proxy.getChannelRegistrar().register(IDENTIFIER);
    }

    @Subscribe
    private void onProxyReload(ProxyReloadEvent event) {
        discordBot.getJda().shutdownNow();
        load();
    }

    public void loadList() {
        var dir = dataDirectory.toFile();
        var whitelistConfigPath = Path.of(dir.getAbsolutePath(), "players.json");
        var whitelistConfig = whitelistConfigPath.toFile();
        if (!dir.exists()) {
            dir.mkdir();
        }

        if (!whitelistConfig.exists()) {
            saveList();
        }

        try {
            var reader = new FileReader(whitelistConfigPath.toString());

            this.whitelistConfig = gson.fromJson(reader, WhitelistConfig.class);

            reader.close();
        } catch (IOException e) {
            logger.error("Could not read from file: " + e.getMessage());
        }
    }

    public void saveList() {
        var dir = dataDirectory.toFile();
        var whitelistConfigPath = Path.of(dir.getAbsolutePath(), "players.json");
        if (!dir.exists()) {
            dir.mkdir();
        }

        try {
            var writer = new FileWriter(whitelistConfigPath.toString());

            if (whitelistConfig == null)
                whitelistConfig = new WhitelistConfig();

            writer.write(gson.toJson(whitelistConfig));
            writer.close();
        } catch (IOException e) {
            logger.error("Could not write to file: " + e.getMessage());
        }
    }

    public void sendMessage(Component component, @Nullable String rawMessage) {
        proxy.sendMessage(component);
        if(rawMessage == null)
            return;

        // not working
        var sound = Sound.sound(Key.key("minecraft:block.note_block.bell"), Sound.Source.PLAYER, 1f, 1f);
        var players = proxy.getAllPlayers();
        rawMessage = rawMessage.toLowerCase();
        for(var player : players) {
            if(rawMessage.contains(player.getUsername().toLowerCase())) {
                player.playSound(sound, Sound.Emitter.self());
            }
        }
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

    public HashMap<UUID, String> getWhitelistCodes() {
        return whitelistCodes;
    }

    public WhitelistConfig getWhitelistConfig() {
        return whitelistConfig;
    }

    public class SMPCordListener {
        @Subscribe
        private void onLogin(LoginEvent event) {
            var player = event.getPlayer();
            var uuid = player.getUniqueId();

            var whitelist = whitelistConfig.getWhitelist();

            // player is whitelisted
            if (whitelist.containsKey(uuid))
                return;

            String code;
            if (!whitelistCodes.containsKey(uuid)) {
                do {
                    code = Utils.generateRandomCode();
                } while (whitelistCodes.containsValue(code));
                whitelistCodes.put(uuid, code);
            } else {
                code = whitelistCodes.get(uuid);
            }

            usernamesCache.put(uuid, player.getUsername());

            var command = String.format("/link %s", code);

            var text = Component.empty()
                .append(Component
                    .text("You are not whitelisted!")
                    .color(NamedTextColor.GOLD))
                .appendNewline()
                .append(Component
                    .text("Run the following command on Discord to join:"))
                .appendNewline()
                .append(Component
                    .text(command)
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.copyToClipboard(command))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy"))));

            player.disconnect(text);
        }


        @Subscribe(order = PostOrder.LATE)
        private void onServerConnected(ServerConnectedEvent event) {
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
            if(event.getLoginStatus() == DisconnectEvent.LoginStatus.CANCELLED_BY_PROXY
            || event.getLoginStatus() == DisconnectEvent.LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE)
                return;

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

        @Subscribe
        public void onPluginMessageFromBackend(PluginMessageEvent event) {
            if (!(event.getSource() instanceof ServerConnection)) {
                return;
            }
            var backend = (ServerConnection) event.getSource();
            // Ensure the identifier is what you expect before trying to handle the data
            if (event.getIdentifier() != IDENTIFIER) {
                return;
            }

            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            // handle packet data
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
