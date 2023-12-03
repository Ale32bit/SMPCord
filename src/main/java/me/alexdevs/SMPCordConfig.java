package me.alexdevs;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.annotations.Expose;

import java.nio.file.Path;

public class SMPCordConfig {
    @Expose
    private String token;
    @Expose
    private String channel;
    @Expose
    private String webhookName;
    @Expose
    private String avatarApiUrl;
    @Expose
    private String avatarThumbnailApiUrl;

    public SMPCordConfig(String token, String channel, String webhookName, String avatarApiUrl, String avatarThumbnailApiUrl) {
        this.token = token;
        this.channel = channel;
        this.webhookName = webhookName;
        this.avatarApiUrl = avatarApiUrl;
        this.avatarThumbnailApiUrl = avatarThumbnailApiUrl;
    }

    public static SMPCordConfig read(Path path) {
        var defaultConfigLocation = SMPCordConfig.class
            .getClassLoader()
            .getResource("default-smpcord.toml");

        if (defaultConfigLocation == null)
            throw new RuntimeException("Default configuration file not found!");

        var config = CommentedFileConfig.builder(path)
            .defaultData(defaultConfigLocation)
            .autosave()
            .preserveInsertionOrder()
            .sync()
            .build();
        config.load();

        var token = config.getOrElse("token", "");
        var channel = config.getOrElse("channel", "");
        var webhookName = config.getOrElse("webhook-name", "SMPCord");
        var avatarApiUrl = config.getOrElse("avatar-api-url", "https://mc-heads.net/head/{{uuid}}");
        var avatarThumbnailApiUrl = config.getOrElse("avatar-thumbnail-api-url", "https://mc-heads.net/avatar/{{uuid}}/16");

        return new SMPCordConfig(token, channel, webhookName, avatarApiUrl, avatarThumbnailApiUrl);
    }

    public String getToken() {
        return token;
    }

    public String getChannel() {
        return channel;
    }

    public String getWebhookName() {
        return webhookName;
    }

    public String getAvatarApiUrl() {
        return avatarApiUrl;
    }

    public String getAvatarThumbnailApiUrl() {
        return avatarThumbnailApiUrl;
    }
}
