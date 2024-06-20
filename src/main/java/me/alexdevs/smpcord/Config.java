package me.alexdevs.smpcord;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = SMPCord.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> BOT_TOKEN = BUILDER
            .comment("Bot token")
            .define("token", "");

    private static final ForgeConfigSpec.ConfigValue<String> CHANNEL_ID = BUILDER
            .comment("Channel ID")
            .define("channelId", "");

    private static final ForgeConfigSpec.ConfigValue<String> WEBHOOK_NAME = BUILDER
            .comment("Webhook name")
            .define("webhookName", "SMPCord");

    private static final ForgeConfigSpec.ConfigValue<String> ROLE_ID = BUILDER
            .comment("Role to give to verified members.")
            .define("roleId", "");

    private static final ForgeConfigSpec.ConfigValue<String> AVATAR_API_URL = BUILDER
            .comment("Avatar API URL\nUsed in messages sent by in-game players.\nPlaceholder: {{uuid}}: User UUID")
            .define("avatarApiUrl", "https://mc-heads.net/head/{{uuid}}");

    private static final ForgeConfigSpec.ConfigValue<String> AVATAR_API_THUMBNAIL_URL = BUILDER
            .comment("Avatar API URL\nUsed by login, logout and other similar events.\nPlaceholder: {{uuid}}: User UUID")
            .define("avatarApiThumbnailUrl", "https://mc-heads.net/head/{{uuid}}/16");

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String botToken;
    public static String channelId;
    public static String webhookName;
    public static String roleId;
    public static String avatarApiUrl;
    public static String avatarApiThumbnailUrl;


    private static boolean validateItemName(final Object obj)
    {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        botToken = BOT_TOKEN.get();
        channelId = CHANNEL_ID.get();
        webhookName = WEBHOOK_NAME.get();
        roleId = ROLE_ID.get();
        avatarApiUrl = AVATAR_API_URL.get();
        avatarApiThumbnailUrl = AVATAR_API_THUMBNAIL_URL.get();
    }
}
