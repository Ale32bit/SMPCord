package me.alexdevs.smpcord;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = SMPCord.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<String> BOT_TOKEN = BUILDER
            .comment("Bot token")
            .define("token", "");

    private static final ModConfigSpec.ConfigValue<String> CHANNEL_ID = BUILDER
            .comment("Channel ID")
            .define("channelId", "");

    private static final ModConfigSpec.ConfigValue<String> WEBHOOK_NAME = BUILDER
            .comment("Webhook name")
            .define("webhookName", "SMPCord");

    private static final ModConfigSpec.ConfigValue<String> ROLE_ID = BUILDER
            .comment("Role to give to verified members.")
            .define("roleId", "");

    private static final ModConfigSpec.ConfigValue<String> AVATAR_API_URL = BUILDER
            .comment("Avatar API URL\nUsed in messages sent by in-game players.\nPlaceholder: {{uuid}}: User UUID")
            .define("avatarApiUrl", "https://mc-heads.net/head/{{uuid}}");

    private static final ModConfigSpec.ConfigValue<String> AVATAR_API_THUMBNAIL_URL = BUILDER
            .comment("Avatar API URL\nUsed by login, logout and other similar events.\nPlaceholder: {{uuid}}: User UUID")
            .define("avatarApiThumbnailUrl", "https://mc-heads.net/head/{{uuid}}/32");

    private static final ModConfigSpec.ConfigValue<String> INVITE_LINK = BUILDER
            .comment("Invite link\nUsed when clicking the D prefix in a message from Discord.")
            .define("inviteLink", "https://discord.gg/myinvite");

    public static final ModConfigSpec.ConfigValue<String> SERVER_AVATAR_URL = BUILDER
            .comment("URL of the avatar to use when a message is sent by the server")
            .define("serverAvatarUrl", "");

    public static final ModConfigSpec.ConfigValue<String> PREFIX = BUILDER
            .comment("Prefix of messages from Discord.")
            .define("prefix", "<#5865F2><hover:show_text:'This is a message from the Discord server'><click:open_url:'https://discord.gg/myserver'>D<reset>");


    public static final ModConfigSpec.ConfigValue<String> REPLY = BUILDER
            .comment("Format of the reply part of the message.")
            .define("reply", " <reference_username> <hover:show_text:'Message: <reference_message>'><gray>↵</gray>");

    public static final ModConfigSpec.ConfigValue<String> MESSAGE_FORMAT = BUILDER
            .comment("Final format of the message from Discord.")
            .define("messageFormat", "<prefix> <username><gray>:</gray><reply> <message>");

    public static final ModConfigSpec.ConfigValue<String> LINK_FORMAT = BUILDER
            .comment("Format of links.")
            .define("linkFormat", "<c:#8888ff><u>${label}</u></c>");

    public static final ModConfigSpec.ConfigValue<String> LINK_HOVER_FORMAT = BUILDER
            .comment("Format of links.")
            .define("linkHoverFormat", "${url}");


    static final ModConfigSpec SPEC = BUILDER.build();

    public static String botToken;
    public static String channelId;
    public static String webhookName;
    public static String roleId;
    public static String avatarApiUrl;
    public static String avatarApiThumbnailUrl;
    public static String inviteLink;
    public static String serverAvatarUrl;
    public static String prefix;
    public static String reply;
    public static String messageFormat;
    public static String linkFormat;
    public static String linkHoverFormat;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        botToken = BOT_TOKEN.get();
        channelId = CHANNEL_ID.get();
        webhookName = WEBHOOK_NAME.get();
        roleId = ROLE_ID.get();
        avatarApiUrl = AVATAR_API_URL.get();
        avatarApiThumbnailUrl = AVATAR_API_THUMBNAIL_URL.get();
        inviteLink = INVITE_LINK.get();
        serverAvatarUrl = SERVER_AVATAR_URL.get();
        prefix = PREFIX.get();
        reply = REPLY.get();
        messageFormat = MESSAGE_FORMAT.get();
        linkFormat = LINK_FORMAT.get();
        linkHoverFormat = LINK_HOVER_FORMAT.get();
    }
}
