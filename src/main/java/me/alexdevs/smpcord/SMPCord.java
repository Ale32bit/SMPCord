package me.alexdevs.smpcord;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.*;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import javax.annotation.Nullable;

@Mod(SMPCord.MODID)
public class SMPCord {
    public static final String MODID = "smpcord";
    public static final Logger LOGGER = LogUtils.getLogger();
    public MinecraftServer server;
    private DiscordBot discordBot;

    public SMPCord() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public void sendWebhook(WebhookMessage message) {
        var webhook = discordBot.getWebhook();
        try (var client = JDAWebhookClient.from(webhook)) {
            client.send(message);
        }
    }

    public void sendMessage(Component component, String rawMessage) {

        var players = server.getPlayerList();
        players.broadcastSystemMessage(component, false);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        try {
            discordBot = new DiscordBot(this);

            var embed = new WebhookEmbedBuilder()
                    .setAuthor(new WebhookEmbed.EmbedAuthor("Server is starting...", null, null))
                    .setColor(ChatFormatting.YELLOW.getColor())
                    .build();

            var webhookMessage = new WebhookMessageBuilder()
                    .addEmbeds(embed)
                    .build();

            sendWebhook(webhookMessage);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        var embed = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor("Server started!", null, null))
                .setColor(ChatFormatting.GREEN.getColor())
                .build();

        var webhookMessage = new WebhookMessageBuilder()
                .addEmbeds(embed)
                .build();

        sendWebhook(webhookMessage);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        var embed = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor("Server is stopping!", null, null))
                .setColor(ChatFormatting.RED.getColor())
                .build();

        var webhookMessage = new WebhookMessageBuilder()
                .addEmbeds(embed)
                .build();

        sendWebhook(webhookMessage);
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        var username = event.getUsername();
        var player = event.getPlayer();
        var rawMessage = event.getRawText();

        var avatarUrl = Utils.getAvatarUrl(player);
        var webhookMessage = new WebhookMessageBuilder()
                .setUsername(username)
                .setContent(rawMessage)
                .setAvatarUrl(avatarUrl)
                .setAllowedMentions(new AllowedMentions()
                        .withParseUsers(true))
                .build();

        sendWebhook(webhookMessage);
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        var entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        var source = event.getSource();
        var deathMessage = source.getLocalizedDeathMessage(entity);
        var stringMessage = deathMessage.getString();
        var embed = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(String.format("%s", stringMessage),
                        Utils.getAvatarThumbnailUrl(player), null))
                .setColor(ChatFormatting.GRAY.getColor())
                .build();

        var webhookMessage = new WebhookMessageBuilder()
                .addEmbeds(embed)
                .build();

        sendWebhook(webhookMessage);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        var player = event.getEntity();
        var username = player.getName().getString();

        var embed = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(String.format("%s joined the server", username),
                        Utils.getAvatarThumbnailUrl(player), null))
                .setColor(ChatFormatting.GREEN.getColor())
                .build();

        var webhook = new WebhookMessageBuilder()
                .addEmbeds(embed)
                .build();

        sendWebhook(webhook);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        var player = event.getEntity();
        var username = player.getName().getString();

        var embed = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(String.format("%s left the server", username),
                        Utils.getAvatarThumbnailUrl(player), null))
                .setColor(ChatFormatting.RED.getColor())
                .build();

        var webhook = new WebhookMessageBuilder()
                .addEmbeds(embed)
                .build();

        sendWebhook(webhook);
    }

    @SubscribeEvent
    public void onPlayerAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        var advancement = event.getAdvancement();
        if(advancement.getDisplay() == null || !advancement.getDisplay().shouldAnnounceChat())
            return;

        var player = event.getEntity();
        var username = player.getName().getString();

        var message = String.format("%s has made the advancement [%s]", username, advancement.getDisplay().getTitle().getString());

        var embed = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(message,
                        Utils.getAvatarThumbnailUrl(player), null))
                .setColor(ChatFormatting.GOLD.getColor())
                .build();

        var webhook = new WebhookMessageBuilder()
                .addEmbeds(embed)
                .build();

        sendWebhook(webhook);
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.warn("HEY! This is supposed to be ran on the server!");
        }
    }
}
