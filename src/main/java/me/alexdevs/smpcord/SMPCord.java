package me.alexdevs.smpcord;

import com.mojang.logging.LogUtils;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
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

    public void sendMessage(Component component, String rawMessage) {

        var players = server.getPlayerList();
        players.broadcastSystemMessage(component, false);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        try {
            discordBot = new DiscordBot(this);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void updatePlayerCount(int count) {
        discordBot.setPresence(ClientPresence.online(ClientActivity.playing(String.format("%d players online!", count))));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        discordBot.getWebhook().execute()
                .withEmbeds(EmbedCreateSpec.create()
                        .withDescription(":up: **Server started!**")
                        .withColor(Color.of(ChatFormatting.GREEN.getColor()))
                )
                .subscribe();

        updatePlayerCount(0);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        discordBot.getWebhook().execute()
                .withEmbeds(EmbedCreateSpec.create()
                        .withDescription(":electric_plug: **Server is stopping!**")
                        .withColor(Color.of(ChatFormatting.RED.getColor()))
                )
                .subscribe();

        discordBot.setPresence(ClientPresence.doNotDisturb(ClientActivity.playing("Stopping...")));
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        var username = event.getUsername();
        var player = event.getPlayer();
        var rawMessage = event.getRawText();
        var avatarUrl = Utils.getAvatarUrl(player);

        discordBot.getWebhook().execute()
                .withAvatarUrl(avatarUrl)
                .withUsername(username)
                .withContent(rawMessage)
                .withAllowedMentions(discord4j.rest.util.AllowedMentions.suppressEveryone())
                .subscribe();
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
        var avatarUrl = Utils.getAvatarThumbnailUrl(player);

        discordBot.getWebhook().execute()
                .withEmbeds(EmbedCreateSpec.create()
                        .withAuthor(EmbedCreateFields.Author.of(String.format("%s", stringMessage), null, avatarUrl))
                        .withColor(Color.of(ChatFormatting.GRAY.getColor())))
                .subscribe();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        var player = event.getEntity();
        var username = player.getName().getString();

        discordBot.getWebhook().execute()
                .withEmbeds(EmbedCreateSpec.create()
                        .withAuthor(EmbedCreateFields.Author.of(String.format("%s joined the server", username), null, Utils.getAvatarThumbnailUrl(player)))
                        .withColor(Color.of(ChatFormatting.GREEN.getColor())))
                .subscribe();

        var server = event.getEntity().getServer();
        if(server != null) {
            updatePlayerCount(event.getEntity().getServer().getPlayerCount());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        var player = event.getEntity();
        var username = player.getName().getString();

        discordBot.getWebhook().execute()
                .withEmbeds(EmbedCreateSpec.create()
                        .withAuthor(EmbedCreateFields.Author.of(String.format("%s left the server", username), null, Utils.getAvatarThumbnailUrl(player)))
                        .withColor(Color.of(ChatFormatting.RED.getColor())))
                .subscribe();

        var server = event.getEntity().getServer();
        if(server != null) {
            updatePlayerCount(event.getEntity().getServer().getPlayerCount() - 1);
        }
    }

    @SubscribeEvent
    public void onPlayerAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        var advancement = event.getAdvancement();
        if (advancement.getDisplay() == null || !advancement.getDisplay().shouldAnnounceChat())
            return;

        var player = event.getEntity();
        var username = player.getName().getString();

        var message = String.format("%s has made the advancement [%s]", username, advancement.getDisplay().getTitle().getString());

        discordBot.getWebhook().execute()
                .withEmbeds(EmbedCreateSpec.create()
                        .withAuthor(EmbedCreateFields.Author.of(message, null, Utils.getAvatarThumbnailUrl(player)))
                        .withColor(Color.of(ChatFormatting.GOLD.getColor())))
                .subscribe();
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
