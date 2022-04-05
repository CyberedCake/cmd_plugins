package net.cybercake.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Logger;

public final class Plugins extends Plugin {

    public String latestVersion = "unknown";
    public int latestProtocol = 0;

    public String currentVersion = "unknown";
    public int currentProtocol = 0;

    public String latestDownloadLink = "https://github.com/CyberedCake/cmd_plugins/releases/latest";

    public Logger log;

    // if you want to add more support for versions, create an issue in GitHub or make it yourself with a pull request :D
    public enum ServerType { BUNGEECORD, WATERFALL, FLAMECORD, HEXACORD, UNSUPPORTED; }

    public static ServerType serverType;
    public static int softwareBuildNumber;

    @Override
    public void onEnable() {
        log = ProxyServer.getInstance().getLogger();

        getProxy().getPluginManager().registerCommand(this, new CommandPlugins());
        getProxy().getPluginManager().registerCommand(this, new CommandVersion(log));

        String name = ProxyServer.getInstance().getName();
        try {
            serverType = ServerType.valueOf(name.toUpperCase(Locale.ROOT));
            softwareBuildNumber = SoftwareVersionCheck.currentBuildFromType(serverType);
            log.info("Found server type to be " + name + " (serverType=" + serverType + ",buildNumber=" + softwareBuildNumber + ")");
        } catch (Exception exception) {
            serverType = ServerType.UNSUPPORTED;
            log.warning("Server type \"" + name + "\" not supported by cmd_plugins! Please use with caution: " + ChatColor.RED + exception);
        }

        currentVersion = this.getDescription().getVersion().split(" ")[0].replace("v", "");
        currentProtocol = Integer.parseInt(this.getDescription().getVersion().split(" ")[1].replace("p", ""));
        versionCheck();
    }

    public void versionCheck() {
        if(!ProxyServer.getInstance().getConfig().isOnlineMode()) {
            log.warning("Module " + this.getDescription().getName() + " version " + this.getDescription().getVersion() + " cannot check for updates because server is in offline mode!");
            return;
        }
        ProxyServer.getInstance().getScheduler().runAsync(this, () -> {
            try {
                URL url = new URL("https://raw.githubusercontent.com/CyberedCake/cmd_plugins/main/version.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

                String inputLine;
                while((inputLine = reader.readLine()) != null) {
                    if(inputLine.startsWith("latestVersion=")) {
                        latestVersion = inputLine.replace("latestVersion=", "");
                    }else if(inputLine.startsWith("latestProtocol=")) {
                        latestProtocol = Integer.parseInt(inputLine.replace("latestProtocol=", ""));
                    }
                }
            } catch (Exception exception) {
                log.severe(ChatColor.RED + "Failed version checking for " + this.getDescription().getName() + " version " + this.getDescription().getVersion() + "! " + ChatColor.DARK_GRAY + exception); return;
            }

            if(currentProtocol != latestProtocol && !latestVersion.startsWith("error:")) {
                log.warning(ChatColor.YELLOW + "Module \"" + this.getDescription().getName() + "\" is outdated! The latest version is " + ChatColor.GREEN + latestVersion + ChatColor.YELLOW + ", your version is " + ChatColor.GOLD + currentVersion + ChatColor.YELLOW + "!");
                if (latestProtocol - currentProtocol > 0) {
                    log.warning(ChatColor.YELLOW + "Module \"" + this.getDescription().getName() + "\" is " + ChatColor.RED + (latestProtocol - currentProtocol) + " version(s) " + ChatColor.YELLOW + "behind!");
                    log.warning(ChatColor.YELLOW + "You can download the latest version of module \"" + this.getDescription().getName() + "\" at " + ChatColor.LIGHT_PURPLE + latestDownloadLink + ChatColor.YELLOW + "!");
                }
            }
        });
    }

}
