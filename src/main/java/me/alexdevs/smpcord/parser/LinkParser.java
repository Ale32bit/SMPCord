package me.alexdevs.smpcord.parser;

import eu.pb4.placeholders.api.node.DirectTextNode;
import eu.pb4.placeholders.api.node.LiteralNode;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.parent.ParentNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import me.alexdevs.smpcord.Config;
import me.alexdevs.smpcord.Placeholder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class LinkParser implements NodeParser {
    public static final Pattern URL_REGEX = Pattern.compile("https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");

    @Override
    public TextNode[] parseNodes(TextNode node) {
        if (node instanceof LiteralNode literalNode) {
            var input = literalNode.value();
            var list = new ArrayList<TextNode>();
            var inputLength = input.length();

            var matcher = URL_REGEX.matcher(input);
            int pos = 0;

            while (matcher.find()) {
                if (inputLength <= matcher.start()) {
                    break;
                }

                String betweenText = input.substring(pos, matcher.start());

                if (!betweenText.isEmpty()) {
                    list.add(new LiteralNode(betweenText));
                }

                var link = matcher.group();

                var url = Component.literal(link);

                Map<String, Component> placeholders = Map.of(
                        "url", url,
                        "label", url
                );

                var display = Placeholder.parse(
                        Config.linkFormat,
                        placeholders
                );

                var hover = Placeholder.parse(
                        Config.linkHoverFormat,
                        placeholders
                );

                var text = Component.empty()
                        .append(display)
                        .setStyle(Style.EMPTY
                                .withHoverEvent(
                                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)
                                )
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link))
                        );

                list.add(new DirectTextNode(text));

                pos = matcher.end();
            }

            if (pos < inputLength) {
                var text = input.substring(pos, inputLength);
                if (!text.isEmpty()) {
                    list.add(new LiteralNode(text));
                }
            }

            return list.toArray(TextNode[]::new);
        } else if (node instanceof ParentNode parentNode) {
            var list = new ArrayList<TextNode>();

            for (var child : parentNode.getChildren()) {
                list.addAll(List.of(this.parseNodes(child)));
            }

            return new TextNode[]{
                    parentNode.copyWith(list.toArray(TextNode[]::new))
            };
        }

        return TextNode.array(node);
    }
}