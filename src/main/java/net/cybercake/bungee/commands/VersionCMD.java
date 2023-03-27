package net.cybercake.bungee.commands;

import io.netty.bootstrap.ServerBootstrap;
import net.cybercake.bungee.Plugins;
import net.cybercake.bungee.SoftwareVersionCheck;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
                    if(onlineMode && Plugins.serverType != Plugins.ServerType.UNSUPPORTED)
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

                    sender.sendMessage(new TextComponent(ChatColor.GREEN + "Reloaded the configuration in " + ChatColor.YELLOW + (System.currentTimeMillis()-mss) + "ms" + ChatColor.GREEN + "!"));
                } catch (Exception exception) {
                    sender.sendMessage(new TextComponent(ChatColor.RED + "Failed to reload the configuration: " + ChatColor.DARK_GRAY + exception));
                }

                return;
            }else if(args[0].equalsIgnoreCase("--delcache")) {
                checkingVersion = -1L;
                SoftwareVersionCheck.latestProtocolSoftware = -1;
                SoftwareVersionCheck.latestVersionSoftware = "0.0.0";
                SoftwareVersionCheck.downloadLink = "Failed to get download link!::an error occurred!";
                sender.sendMessage(new TextComponent(
                        ChatColor.GREEN + "Deleted the cache for " + ChatColor.YELLOW + this.getClass().getCanonicalName() + ChatColor.GREEN + " and " + ChatColor.YELLOW + SoftwareVersionCheck.class.getCanonicalName() + ChatColor.GREEN + "!"
                ));

                return;
            }else if(args[0].startsWith("--plugininfo:")) {
                final String pluginString = args[0].substring("--plugininfo:".length());
                final Plugin plugin = ProxyServer.getInstance().getPluginManager().getPlugin(pluginString);
                if(plugin == null) {
                    sender.sendMessage(new TextComponent(ChatColor.RED + "Unknown plugin: " + ChatColor.DARK_GRAY + pluginString)); return;
                }
                showAlternatePluginVersion(sender, plugin);
                return;
            }else if(args[0].startsWith("--")) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Unknown flag: " + ChatColor.DARK_GRAY + args[0].substring(2)));
                return;
            }

            String pluginName = args[0];
            Plugin exactPlugin = ProxyServer.getInstance().getPluginManager().getPlugin(pluginName);
            if(exactPlugin != null) {
                if(Plugins.config.getBoolean("useAlternatePluginVersionMessage")) showAlternatePluginVersion(sender, exactPlugin);
                else showPlugin(sender, exactPlugin);
                return;
            }

            boolean found = false;
            pluginName = pluginName.toLowerCase(Locale.ROOT);
            for(Plugin plugin : ProxyServer.getInstance().getPluginManager().getPlugins()) {
                if(plugin.getDescription().getName().toLowerCase(Locale.ROOT).contains(pluginName)) {
                    if(Plugins.config.getBoolean("useAlternatePluginVersionMessage")) showAlternatePluginVersion(sender, plugin);
                    else showPlugin(sender, plugin);
                    found = true;
                }
            }

            if(!found) {
                sender.sendMessage(new TextComponent("This proxy server is not running any plugin by that name."));
                sender.sendMessage(new TextComponent("Use /gplugins to get a list of plugins."));
            }
        }
    }

    private BaseComponent getPluginInfo(String field, String value, boolean hover) {
        TextComponent returned = new TextComponent();
        TextComponent beginning = new TextComponent(ChatColor.GREEN + field + ChatColor.WHITE + ": ");
        TextComponent ending = new TextComponent((hover ? (ChatColor.DARK_GRAY + ChatColor.ITALIC.toString()) + "hover for " + field : ChatColor.YELLOW + value));
        if(hover)
            ending.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + value)));
        returned.addExtra(beginning);
        returned.addExtra(ending);
        return returned;
    }

    public void showAlternatePluginVersion(CommandSender sender, Plugin plugin) {
        final String separator = (ChatColor.BLUE + ChatColor.STRIKETHROUGH.toString() + " ").repeat(100);

        sender.sendMessage(new TextComponent(separator));
        sender.sendMessage(new TextComponent(
                (ChatColor.GREEN + ChatColor.BOLD.toString()) + plugin.getDescription().getName() + " " + ChatColor.DARK_GRAY + "(" + ChatColor.YELLOW + plugin.getDescription().getVersion() + ChatColor.DARK_GRAY + ")"
        ));
        sender.sendMessage(new TextComponent(" "));

        if(plugin.getDescription().getDescription() != null) sender.sendMessage(getPluginInfo("Description", plugin.getDescription().getDescription(), false));
        if(plugin.getDescription().getAuthor() != null) sender.sendMessage(getPluginInfo("Author", plugin.getDescription().getAuthor(), false));
        if(plugin.getDescription().getMain() != null) sender.sendMessage(getPluginInfo("Main Class", plugin.getDescription().getMain(), false));
        if(plugin.getDescription().getFile() != null)
            sender.sendMessage(getPluginInfo("File",
                    ChatColor.GREEN + "File Path" + ChatColor.WHITE + ": " + ChatColor.GOLD + plugin.getDescription().getFile().getPath() + "\n" +
                            ChatColor.GREEN + "File Absolute Path" + ChatColor.WHITE + ": " + ChatColor.GOLD + plugin.getDescription().getFile().getAbsolutePath() + "\n\n" +
                            ChatColor.GREEN + "Data Folder Path" + ChatColor.WHITE + ": " + ChatColor.GOLD + plugin.getDataFolder().getPath() + "\n" +
                            ChatColor.GREEN + "Data Folder Absolute Path" + ChatColor.WHITE + ": " + ChatColor.GOLD + plugin.getDataFolder().getAbsolutePath()
                    , true));
        if(plugin.getDescription().getDepends().size() != 0) sender.sendMessage(getPluginInfo("Depends",
                ChatColor.GREEN + "Amount of Depends" + ChatColor.WHITE + ": " + ChatColor.GOLD + plugin.getDescription().getDepends().size() + "\n" +
                        ChatColor.GREEN + "Depends" + ChatColor.WHITE + ": " + ChatColor.GOLD + String.join(", ", plugin.getDescription().getDepends())
                , true));
        if(plugin.getDescription().getSoftDepends().size() != 0) sender.sendMessage(getPluginInfo("Soft Depends",
                ChatColor.GREEN + "Amount of Soft Depends" + ChatColor.WHITE + ": " + ChatColor.GOLD + plugin.getDescription().getSoftDepends().size() + "\n" +
                        ChatColor.GREEN + "Soft Depends" + ChatColor.WHITE + ": " + ChatColor.GOLD + String.join(", ", plugin.getDescription().getSoftDepends())
                , true));
        if(plugin.getDescription().getLibraries().size() != 0) sender.sendMessage(getPluginInfo("Libraries",
                ChatColor.GREEN + "Libraries" + ChatColor.WHITE + ": " + ChatColor.GOLD + String.join(", ", plugin.getDescription().getLibraries())
                , true));
        if(plugin.getLogger() != null) sender.sendMessage(getPluginInfo("Logger",
                ChatColor.GREEN + "Logger Name" + ChatColor.WHITE + ": " + ChatColor.GOLD + plugin.getLogger().getName() + "\n" +
                        ChatColor.GREEN + "Logger Class" + ChatColor.WHITE + ": " + ChatColor.GOLD + plugin.getLogger().getClass().getCanonicalName() + "\n\n" +
                        ChatColor.GREEN + "Logger Parent Name" + ChatColor.WHITE + ": " + ChatColor.GOLD + plugin.getLogger().getParent().getName() + "\n" +
                        ChatColor.GREEN + "Logger Parent Class" + ChatColor.WHITE + ": " + ChatColor.GOLD + plugin.getLogger().getParent().getClass().getCanonicalName()
                , true));
        sender.sendMessage(new TextComponent(separator));
    }

    public void showVersion(CommandSender sender, String[] args) {
        boolean useAlternateVersionMessage = Plugins.config.getBoolean("useAlternateVersionMessage");
        ChatColor color;
        checkingVersion = System.currentTimeMillis();

        String addToComponent;
        TextComponent downloadLatest = null;
        TextComponent previousVersion = new TextComponent("\n" + ChatColor.GRAY + "" + ChatColor.ITALIC + "Previous version: " + Plugins.previousVersionWithMC);
        if(!ProxyServer.getInstance().getConfig().isOnlineMode()) {
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
            if(ProxyServer.getInstance().getPluginManager().getPlugin(args[0]) != null)
                return new ArrayList<>();

            List<String> plugins = new ArrayList<>(
                    ProxyServer.getInstance().getPluginManager().getPlugins()
                            .stream()
                            .map(item -> item.getDescription().getName())
                            .toList()
            );
            if(args[0].startsWith("--")) {
                if (args[0].startsWith("--plugininfo"))
                    return Plugins.getInstance().getTabCompletions(args[0], plugins
                            .stream()
                            .map(item -> "--plugininfo:" + item)
                            .toList()
                    );
                return Plugins.getInstance().getTabCompletions(args[0], List.of("--reload", "--delcache", "--plugininfo"));
            }
            return Plugins.getInstance().getTabCompletions(args[0], plugins);
        }
        return new ArrayList<>();
    }

}
