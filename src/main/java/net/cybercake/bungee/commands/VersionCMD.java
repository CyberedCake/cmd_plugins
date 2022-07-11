package net.cybercake.bungee.commands;

import net.cybercake.bungee.Plugins;
import net.cybercake.bungee.SoftwareVersionCheck;
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
import net.md_5.bungee.chat.BaseComponentSerializer;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.text.Collator;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class VersionCMD extends Command implements TabExecutor {

    public VersionCMD(Logger log) {
        super("gversion", "bungeecord.command.version", "gver", "gabout", "bungee");
        this.log = log;
    }

    public Logger log;

    public static long checkingVersion = 0L;

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            if(checkingVersion == 1L) {sender.sendMessage(new TextComponent(ChatColor.RED + "Already checking version, please wait!")); return; }

            boolean onlineMode = ProxyServer.getInstance().getConfig().isOnlineMode();
            ProxyServer.getInstance().getScheduler().runAsync(Plugins.getInstance(), () -> {
                if(System.currentTimeMillis()-checkingVersion > 600000) {
                    if(onlineMode && Plugins.serverType != Plugins.ServerType.UNSUPPORTED && Plugins.serverType != Plugins.ServerType.FLAMECORD)
                        ProxyServer.getInstance().getScheduler().schedule(Plugins.getInstance(), () -> sender.sendMessage(new TextComponent(ChatColor.ITALIC + "Checking version, please wait...")), 0L, TimeUnit.SECONDS);
                    checkingVersion = 1L;
                    SoftwareVersionCheck.check();
                }
                ProxyServer.getInstance().getScheduler().schedule(Plugins.getInstance(), () -> showVersion(sender, args), 0L, TimeUnit.SECONDS);
            });
        }else  {
            if(args[0].equalsIgnoreCase("--reload")) {
                long mss = System.currentTimeMillis();

                try {
                    Plugins.reloadConfig();

                    sender.sendMessage(new TextComponent(ChatColor.GREEN + "Reloaded the configuration in " + (System.currentTimeMillis()-mss) + "ms!"));
                } catch (Exception exception) {
                    sender.sendMessage(new TextComponent(ChatColor.RED + "Failed to reload the configuration: " + ChatColor.DARK_GRAY + exception));
                }

                return;
            }

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

    public void showVersion(CommandSender sender, String[] args) {
        boolean useAlternateVersionMessage = Plugins.config.getBoolean("useAlternateVersionMessage");
        ChatColor color = null;
        checkingVersion = System.currentTimeMillis();

        String addToComponent;
        TextComponent downloadLatest = null;
        TextComponent previousVersion = new TextComponent("\n" + ChatColor.GRAY + "" + ChatColor.ITALIC + "Previous version: " + Plugins.previousVersionWithMC);
        if (Plugins.serverType.equals(Plugins.ServerType.FLAMECORD)) {
            color = ChatColor.RED;

            addToComponent = color + "Cannot check latest version with FlameCord";

            downloadLatest = new TextComponent("\n" + ChatColor.YELLOW + "Download latest version at: ");

            TextComponent link = new TextComponent(ChatColor.GOLD + SoftwareVersionCheck.downloadLink.split("::")[0]);
            link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to open")));
            link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, SoftwareVersionCheck.downloadLink.split("::")[1]));
            downloadLatest.addExtra(link);
        }else if(!ProxyServer.getInstance().getConfig().isOnlineMode()) {
            color = ChatColor.RED;

            addToComponent = color + "Error obtaining version information (offline mode)";
            downloadLatest = new TextComponent("\n" + ChatColor.YELLOW + "Online mode is set to '" + ChatColor.RED + "false" +ChatColor.YELLOW + "' in " + ChatColor.GOLD + "config.yml");
        }else if(SoftwareVersionCheck.latestProtocolSoftware > Plugins.softwareBuildNumber) {
            color = ChatColor.YELLOW;

            addToComponent = color + "You are " + (SoftwareVersionCheck.latestProtocolSoftware - Plugins.softwareBuildNumber) + " version(s) behind";

            downloadLatest = new TextComponent("\n" + ChatColor.YELLOW + "Download the new version at: ");

            TextComponent link = new TextComponent(ChatColor.GOLD + SoftwareVersionCheck.downloadLink.split("::")[0]);
            link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to open")));
            link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, SoftwareVersionCheck.downloadLink.split("::")[1]));
            downloadLatest.addExtra(link);
        }else if(SoftwareVersionCheck.latestProtocolSoftware == Plugins.softwareBuildNumber) {
            color = ChatColor.GREEN;

            addToComponent = color + "You are running the latest version";
        }else if(SoftwareVersionCheck.latestVersionSoftware.equalsIgnoreCase("UNSUPPORTED")) {
            color = ChatColor.YELLOW;

            addToComponent = color + "Unknown version";
        }else{
            color = ChatColor.RED;

            addToComponent = color + "Error obtaining version information";
            log.severe("An error occurred checking for the latest version: " + ChatColor.DARK_GRAY + "unknown, current/latest: " + Plugins.softwareBuildNumber + "/" + SoftwareVersionCheck.latestProtocolSoftware);
        }

        TextComponent component =
                (useAlternateVersionMessage
                        ? new TextComponent(
                        ChatColor.GRAY + "Current: " + color + Plugins.currentVersionWithMC + "*\n" +
                                (Plugins.previousVersionWithMC == null ? "" : ChatColor.GRAY + "Previous: " + Plugins.previousVersionWithMC + "\n") +
                                color + "* " + addToComponent
                )
                        : new TextComponent(
                        "This proxy server is running " + ProxyServer.getInstance().getName() + " version " + Plugins.currentVersionWithMC + " (Protocol Version: " + ProxyServer.getInstance().getProtocolVersion() + ")" + "\n" + addToComponent
                )
                );
        if(Plugins.previousVersionWithMC != null && !useAlternateVersionMessage) component.addExtra(previousVersion);
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to copy to clipboard")));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ChatColor.stripColor(component.getText() + (downloadLatest != null ? downloadLatest.getText() : ""))));
        if(downloadLatest != null) component.addExtra(downloadLatest);

        sender.sendMessage(component);
    }

    public void showPlugin(CommandSender sender, Plugin plugin) {
        PluginDescription desc = plugin.getDescription();
        sender.sendMessage(new TextComponent(ChatColor.GREEN + desc.getName() + ChatColor.WHITE + " version " + ChatColor.GREEN + desc.getVersion()));

        if(desc.getDescription() != null)
            sender.sendMessage(new TextComponent(desc.getDescription()));

        if(desc.getAuthor() != null)
            sender.sendMessage(new TextComponent("Author: " + ChatColor.GREEN + desc.getAuthor()));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if(args.length < 2) {
            if(ProxyServer.getInstance().getPluginManager().getPlugin(args[0]) != null) {
                return new ArrayList<>();
            }

            if(args[0].startsWith("--")) return List.of("--reload");

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
