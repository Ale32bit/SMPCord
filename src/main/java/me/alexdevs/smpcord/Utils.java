package me.alexdevs.smpcord;

import net.dv8tion.jda.api.entities.Member;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.Player;

public class Utils {
    public static String getAvatarUrl(Player player) {
        var avatarApiUrl = Config.avatarApiUrl;
        return avatarApiUrl.replaceAll("\\{\\{uuid}}", player.getStringUUID());
    }

    public static String getAvatarThumbnailUrl(Player player) {
        var avatarApiUrl = Config.avatarApiThumbnailUrl;
        return avatarApiUrl.replaceAll("\\{\\{uuid}}", player.getStringUUID());
    }

    public static Component getMemberNameComponent(Member member) {
        return getMemberNameComponent(member.getEffectiveName(), TextColor.fromRgb(member.getColorRaw()), member.getAsMention());
    }

    public static Component getMemberNameComponent(String name, TextColor color, String asMention) {
        return Component
                .literal(name)
                .withStyle(Style.EMPTY
                        .withColor(color)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to mention")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, asMention + ": "))
                );
    }

    public static String getMemberName(Member member) {
        if (member == null) {
            return "Unknown User";
        }
        var name = member.getNickname();
        if (name == null)
            name = member.getUser().getName();
        return name;
    }
}
