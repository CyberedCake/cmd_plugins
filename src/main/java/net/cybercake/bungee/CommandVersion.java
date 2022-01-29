package net.cybercake.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.text.Collator;
import java.util.*;

public class CommandVersion extends Command implements TabExecutor {

    public CommandVersion() {
        super("gversion", "bungeecord.command.version", "gver", "gabout");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            sender.sendMessage(new TextComponent("This proxy server is running " + ProxyServer.getInstance().getName() + " version " + ProxyServer.getInstance().getVersion() + " (" + ProxyServer.getInstance().getGameVersion() + ") (Protocol Version: " + ProxyServer.getInstance().getProtocolVersion() + ")"));
        }else  {
            String pluginName = args[0];
            Plugin exactPlugin = ProxyServer.getInstance().getPluginManager().getPlugin(pluginName);
            if(exactPlugin != null) {
                showPlugin(sender, exactPlugin);
                return;
            }

            boolean found = false;
            pluginName = pluginName.toLowerCase(Locale.ROOT);
            for(Plugin plugin : ProxyServer.getInstance().getPluginManager().getPlugins()) {
                if(plugin.getDescription().getName().toLowerCase(Locale.ROOT).contains(pluginName)) {
                    showPlugin(sender, plugin);
                    found = true;
                }
            }

            if(!found) {
                sender.sendMessage(new TextComponent("This proxy server is not running any plugin by that name."));
                sender.sendMessage(new TextComponent("Use /gplugins to get a list of plugins."));
            }
        }
    }

    public void showPlugin(CommandSender sender, Plugin plugin) {
        PluginDescription desc = plugin.getDescription();
        sender.sendMessage(new TextComponent(ChatColor.GREEN + desc.getName() + ChatColor.WHITE + " version " + ChatColor.GREEN + desc.getVersion()));

        if(desc.getDescription() != null) {
            sender.sendMessage(new TextComponent(desc.getDescription()));
        }

        if(desc.getAuthor() != null) {
            sender.sendMessage(new TextComponent("Author: " + ChatColor.GREEN + desc.getAuthor()));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if(args.length < 2) {
            if(ProxyServer.getInstance().getPluginManager().getPlugin(args[0]) != null) {
                return new ArrayList<>();
            }

            Collection<String> completions = new TreeSet<>(Collator.getInstance());
            String toComplete = args[0].toLowerCase(Locale.ROOT);
            for(Plugin plugin : ProxyServer.getInstance().getPluginManager().getPlugins()) {
                if(plugin.getDescription().getName().toLowerCase(Locale.ROOT).startsWith(toComplete)) {
                    completions.add(plugin.getDescription().getName());
                }
            }
            return completions;
        }
        return new ArrayList<>();
    }

}
