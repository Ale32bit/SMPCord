package me.alexdevs.smpcord;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.PatternPlaceholderParser;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class Placeholder {
    public static Component parse(String text) {
        return TextParserUtils.formatText(text);
    }

    public static Component parse(TextNode textNode, PlaceholderContext context, Map<String, Component> placeholders) {
        var predefinedNode = Placeholders.parseNodes(textNode, PatternPlaceholderParser.PREDEFINED_PLACEHOLDER_PATTERN, placeholders);
        return Placeholders.parseText(predefinedNode, context);
    }

    public static Component parse(Component text, PlaceholderContext context, Map<String, Component> placeholders) {
        return parse(TextNode.convert(text), context, placeholders);
    }

    public static Component parse(String text, PlaceholderContext context, Map<String, Component> placeholders) {
        return parse(parse(text), context, placeholders);
    }

    public static Component parse(String text, PlaceholderContext context) {
        return parse(parse(text), context, Map.of());
    }

    public static Component parse(String text, Map<String, Component> placeholders) {
        return Placeholders.parseText(parse(text), PatternPlaceholderParser.PREDEFINED_PLACEHOLDER_PATTERN, placeholders);
    }
}
