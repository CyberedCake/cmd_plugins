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
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.text.Collator;
import java.util.*;
import java.util.logging.Logger;

public class CommandVersion extends Command implements TabExecutor {

    public CommandVersion(Logger log) {
        super("gversion", "bungeecord.command.version", "gver", "gabout");
        this.log = log;
    }

    public Logger log;

    public static long checkingVersion = 0L;

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            if(checkingVersion == 1L) {sender.sendMessage(new TextComponent(ChatColor.RED + "Already checking version, please wait!")); return; }

            if(System.currentTimeMillis()-checkingVersion > 600000) {
                if(ProxyServer.getInstance().getConfig().isOnlineMode() && Plugins.serverType != Plugins.ServerType.UNSUPPORTED && Plugins.serverType != Plugins.ServerType.FLAMECORD) sender.sendMessage(new TextComponent(ChatColor.ITALIC + "Checking version, please wait..."));
                checkingVersion = 1L;
                SoftwareVersionCheck.check();
            }

            checkingVersion = System.currentTimeMillis();

            String addToComponent;
            TextComponent downloadLatest = null;
            if (Plugins.serverType.equals(Plugins.ServerType.FLAMECORD)) {
                addToComponent = ChatColor.RED + "Cannot check latest version with FlameCord";

                downloadLatest = new TextComponent("\n" + ChatColor.YELLOW + "Download latest version at: ");

                TextComponent link = new TextComponent(ChatColor.GOLD + SoftwareVersionCheck.downloadLink.split("::")[0]);
                link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to open")));
                link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, SoftwareVersionCheck.downloadLink.split("::")[1]));
                downloadLatest.addExtra(link);
            }else if(!ProxyServer.getInstance().getConfig().isOnlineMode()) {
                addToComponent = ChatColor.RED + "Error obtaining version information (offline mode)";
                downloadLatest = new TextComponent("\n" + ChatColor.YELLOW + "Online mode is set to '" + ChatColor.RED + "false" +ChatColor.YELLOW + "' in " + ChatColor.GOLD + "config.yml");
            }else if(SoftwareVersionCheck.latestProtocolSoftware > Plugins.softwareBuildNumber) {
                addToComponent = ChatColor.YELLOW + "You are " + (SoftwareVersionCheck.latestProtocolSoftware - Plugins.softwareBuildNumber) + " version(s) behind";

                downloadLatest = new TextComponent("\n" + ChatColor.YELLOW + "Download the new version at: ");

                TextComponent link = new TextComponent(ChatColor.GOLD + SoftwareVersionCheck.downloadLink.split("::")[0]);
                link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to open")));
                link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, SoftwareVersionCheck.downloadLink.split("::")[1]));
                downloadLatest.addExtra(link);
            }else if(SoftwareVersionCheck.latestProtocolSoftware == Plugins.softwareBuildNumber) {
                addToComponent = ChatColor.GREEN + "You are running the latest version";
            }else if(SoftwareVersionCheck.latestVersionSoftware.equalsIgnoreCase("UNSUPPORTED")) {
                addToComponent = ChatColor.YELLOW + "Unknown version";
            }else{
                addToComponent = ChatColor.RED + "Error obtaining version information";
                log.severe("An error occurred checking for the latest version: " + ChatColor.DARK_GRAY + "unknown, current/latest: " + Plugins.softwareBuildNumber + "/" + SoftwareVersionCheck.latestProtocolSoftware);
            }

            String gameVersion = ProxyServer.getInstance().getGameVersion();
            TextComponent component = new TextComponent("This proxy server is running " + ProxyServer.getInstance().getName() + " version " + ProxyServer.getInstance().getVersion() + " (Game Version: " + (gameVersion.split(", ").length > 1 ? gameVersion.split(", ")[gameVersion.split(", ").length-1] : gameVersion) + ") (Protocol Version: " + ProxyServer.getInstance().getProtocolVersion() + ")" + "\n" + addToComponent);
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to copy to clipboard")));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ChatColor.stripColor(component.getText() + (downloadLatest != null ? downloadLatest.getText() : ""))));

            if(downloadLatest != null) component.addExtra(downloadLatest);

            sender.sendMessage(component);
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
