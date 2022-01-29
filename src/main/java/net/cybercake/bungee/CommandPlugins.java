package net.cybercake.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.text.Collator;
import java.util.*;

public class CommandPlugins extends Command implements TabExecutor {

    public CommandPlugins() {
        super("gplugins", "bungeecord.command.plugins", "gpl");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TextComponent component = new TextComponent();

        Collection<String> pluginNames = new TreeSet<>(Collator.getInstance());
        int number = 0;
        for(Plugin plugin : ProxyServer.getInstance().getPluginManager().getPlugins()) {
            if(args.length == 0 || args[0].equalsIgnoreCase("all") || !Arrays.asList("modules", "plugins").contains(args[0])) {
                pluginNames.add(plugin.getDescription().getName());
                number++;
            }else if(args[0].equalsIgnoreCase("modules") && plugin.getFile().getParent().contains("modules")) {
                pluginNames.add(plugin.getDescription().getName());
                number++;
            }else if(args[0].equalsIgnoreCase("plugins") && plugin.getFile().getParent().contains("plugins")) {
                pluginNames.add(plugin.getDescription().getName());
                number++;
            }
        }

        component.addExtra(new TextComponent("Proxy Plugins (" + number + "): "));
        int index=0;
        for(String name : pluginNames) {
            Plugin plugin = ProxyServer.getInstance().getPluginManager().getPlugin(name);

            TextComponent pluginComponent = new TextComponent(ChatColor.GREEN + plugin.getDescription().getName());
            pluginComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
                    ChatColor.WHITE + "Version: " + ChatColor.GREEN + plugin.getDescription().getVersion() +
                            (plugin.getDescription().getDescription() == null ? "" : "\n" + ChatColor.WHITE + "Description: " + ChatColor.GREEN + plugin.getDescription().getDescription()) +
                            (plugin.getDescription().getAuthor() == null ? "" : "\n" + ChatColor.WHITE + "Author: " + ChatColor.GREEN + plugin.getDescription().getAuthor()))));
            pluginComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/gversion " + plugin.getDescription().getName()));
            component.addExtra(pluginComponent);
            component.addExtra(new TextComponent(index == number-1 ? "" : ChatColor.RESET + ", "));
            index++;
        }

        sender.sendMessage(component);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if(args.length < 2) {
            if(Arrays.asList("all", "plugins", "modules").contains(args[0].toLowerCase(Locale.ROOT))) {
                return new ArrayList<>();
            }

            ArrayList<String> completions = new ArrayList<>();
            String toComplete = args[0].toLowerCase(Locale.ROOT);
            for(String msg : Arrays.asList("all", "plugins", "modules")) {
                if(msg.toLowerCase(Locale.ROOT).startsWith(toComplete)) completions.add(msg);
            }
            return completions;
        }
        return new ArrayList<>();
    }

}
