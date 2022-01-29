package net.cybercake.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

public final class Plugins extends Plugin {

    public String latestVersion = "unknown";
    public int latestProtocol = 0;

    public String currentVersion = "unknown";
    public int currentProtocol = 0;

    public String latestDownloadLink = "https://github.com/CyberedCake/cmd_plugins/releases/latest";

    public Logger log;

    @Override
    public void onEnable() {
        log = ProxyServer.getInstance().getLogger();

        getProxy().getPluginManager().registerCommand(this, new CommandPlugins());
        getProxy().getPluginManager().registerCommand(this, new CommandVersion());

        currentVersion = this.getDescription().getVersion().split(" ")[0].replace("v", "");
        currentProtocol = Integer.parseInt(this.getDescription().getVersion().split(" ")[1].replace("p", ""));
        versionCheck();
    }

    public void versionCheck() {
        if(!ProxyServer.getInstance().getConfig().isOnlineMode()) {
            log.warning("Module " + this.getDescription().getName() + " version " + this.getDescription().getVersion() + " cannot check for updates because online mode is false!");
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
                log.severe("Failed version checking for " + this.getDescription().getName() + " version " + this.getDescription().getVersion() + "! " + ChatColor.DARK_GRAY + exception); return;
            }

            if(currentProtocol != latestProtocol && !latestVersion.startsWith("error:")) {
                log.warning("Module \"" + this.getDescription().getName() + "\" is outdated! The latest version is " + ChatColor.GREEN + latestVersion + ChatColor.YELLOW + ", your version is " + currentVersion + "!");
                if (latestProtocol - currentProtocol > 0) {
                    log.warning("Module \"" + this.getDescription().getName() + "\" is " + ChatColor.RED + (latestProtocol - currentProtocol) + " version(s) " + ChatColor.YELLOW + "behind!");
                    log.warning("You can download the latest version of module \"" + this.getDescription().getName() + "\" at " + ChatColor.LIGHT_PURPLE + latestDownloadLink + ChatColor.YELLOW + "!");
                }
            }
        });
    }

}
