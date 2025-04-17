package me.alexdevs.smpcord;

import com.mojang.logging.LogUtils;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import me.alexdevs.smpcord.commands.DiscordCommand;
import me.alexdevs.smpcord.commands.LinkCommand;
import me.alexdevs.smpcord.discord.DiscordBot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.UUID;

@Mod(SMPCord.MODID)
public class SMPCord {
    public static final String MODID = "smpcord";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static SMPCord instance;
    public static SMPCord instance() {
        return instance;
    }
    public MinecraftServer server;
    private DiscordBot discordBot;
    private Links links;
    public final HashMap<String, UUID> pendingLinks = new HashMap<>();
    public final HashMap<UUID, String> usernameCache = new HashMap<>();

    public Links links() {
        return links;
    }

    public SMPCord(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;

        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public void sendMessage(Component component) {
        var players = server.getPlayerList();
        players.broadcastSystemMessage(component, false);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        try {
            links = Links.load();
            discordBot = new DiscordBot(this);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void updatePlayerCount(int count) {
        discordBot.setPresence(ClientPresence.online(ClientActivity.playing(String.format("with %d players!", count))));
    }

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        DiscordCommand.register(event.getDispatcher());
        LinkCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        discordBot.getWebhook().execute()
                .withEmbeds(EmbedCreateSpec.create()
                        .withDescription(":hourglass: **Server is starting...**")
                        .withColor(Color.of(ChatFormatting.YELLOW.getColor()))
                )
                .subscribe();

        discordBot.setPresence(ClientPresence.idle(ClientActivity.playing("Starting...")));

    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        discordBot.getWebhook().execute()
                .withEmbeds(EmbedCreateSpec.create()
                        .withDescription(":up: **Server started!**")
                        .withColor(Color.of(ChatFormatting.GREEN.getColor()))
                )
                .subscribe();

        this.server = event.getServer();

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
        var display = advancement.value().display();
        if (display.isEmpty() || !display.get().shouldAnnounceChat())
            return;

        var player = event.getEntity();
        var username = player.getName().getString();

        var message = String.format("%s has made the advancement [%s]", username, display.get().getTitle().getString());

        discordBot.getWebhook().execute()
                .withEmbeds(EmbedCreateSpec.create()
                        .withAuthor(EmbedCreateFields.Author.of(message, null, Utils.getAvatarThumbnailUrl(player)))
                        .withColor(Color.of(ChatFormatting.GOLD.getColor())))
                .subscribe();
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.warn("HEY! This is supposed to be ran on the server!");
        }
    }
}
