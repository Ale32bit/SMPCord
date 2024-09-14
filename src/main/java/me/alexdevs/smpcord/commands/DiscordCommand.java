package me.alexdevs.smpcord.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class DiscordCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var rootNode = Commands
                .literal("discord")
                .executes(context -> {
                    var source = context.getSource();
                    source.sendSuccess(() -> Component.literal("Hello!"), true);
                    return 0;
                });


        dispatcher.register(rootNode);
    }
}
