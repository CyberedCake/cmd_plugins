package net.cybercake.bungee;

import net.cybercake.bungee.commands.PluginsCMD;
import net.cybercake.bungee.commands.VersionCMD;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.JsonConfiguration;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.net.URL;
import java.text.Collator;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Plugins extends Plugin {

    private static Plugins instance;

    public String latestVersion = "unknown";
    public int latestProtocol = 0;

    public String currentVersion = "unknown";
    public int currentProtocol = 0;

    public String latestDownloadLink = "https://github.com/CyberedCake/cmd_plugins/releases/latest";

    public Logger log;

    // if you want to add more support for versions, create an issue in GitHub or make it yourself with a pull request :D
    public enum ServerType { BUNGEECORD, WATERFALL, HEXACORD, UNSUPPORTED; }

    public static ServerType serverType;
    public static int softwareBuildNumber;

    public static Configuration versionHistory;
    public static String currentVersionWithMC;
    public static String previousVersionWithMC;

    public static Configuration config;

    @Override
    public void onEnable() {
        instance = this;
        log = ProxyServer.getInstance().getLogger();

        getProxy().getPluginManager().registerCommand(this, new PluginsCMD());
        getProxy().getPluginManager().registerCommand(this, new VersionCMD(log));

        String name = ProxyServer.getInstance().getName();
        try {
            serverType = ServerType.valueOf(name.toUpperCase(Locale.ROOT));
            softwareBuildNumber = SoftwareVersionCheck.currentBuildFromType(serverType);
            log.info("Found server type to be " + name + " (serverType=" + serverType + ",buildNumber=" + softwareBuildNumber + ")");
        } catch (Exception exception) {
            serverType = ServerType.UNSUPPORTED;
            log.warning("Server type \"" + name + "\" not supported by cmd_plugins! Please use with caution: " + ChatColor.RED + exception);
        }

        // version history
        try {
            String gameVersion = ProxyServer.getInstance().getGameVersion();
            String minecraft = (gameVersion.split(", ").length > 1 ? gameVersion.split(", ")[gameVersion.split(", ").length-1] : gameVersion);
            currentVersionWithMC = "git-" + ProxyServer.getInstance().getName() + "-" + SoftwareVersionCheck.currentBuildFromType(serverType) + " (MC: " + minecraft + ")";

            File versionHistoryFile = new File(getDataFolder(), "version-history.json");
            if(!getDataFolder().exists())
                getDataFolder().mkdir();
            if(!versionHistoryFile.exists())
                versionHistoryFile.createNewFile();
            versionHistory = ConfigurationProvider.getProvider(JsonConfiguration.class).load(versionHistoryFile);

            if(versionHistory.getString("currentVersion").isBlank()) {
                versionHistory.set("currentVersion", currentVersionWithMC);
                versionHistory.set("versionHistory", new ArrayList<>());
            }

            if(!versionHistory.getString("currentVersion").equalsIgnoreCase(currentVersionWithMC)) {
                List<String> oldVersions = new ArrayList<>(versionHistory.getStringList("versionHistory"));
                oldVersions.add(versionHistory.getString("currentVersion"));
                versionHistory.set("versionHistory", oldVersions);
                versionHistory.set("currentVersion", currentVersionWithMC);
            }

            ConfigurationProvider.getProvider(JsonConfiguration.class).save(versionHistory, versionHistoryFile);

            try {
                previousVersionWithMC = versionHistory.getStringList("versionHistory").get(versionHistory.getStringList("versionHistory").size()-1);
            } catch (IndexOutOfBoundsException exception) {
                previousVersionWithMC = null;
            }
        } catch (IOException exception) {
            getLogger().severe("Failed to get/save version history! " + ChatColor.DARK_GRAY + exception);
        }

        // config
        try {
            reloadConfig();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to load the configuration!", exception);
        }

        currentVersion = this.getDescription().getVersion().split(" ")[0].replace("v", "");
        currentProtocol = Integer.parseInt(this.getDescription().getVersion().split(" ")[1].replace("p", ""));
        versionCheck();
    }

    public static void reloadConfig() throws IOException {
        File configFile = new File(Plugins.getInstance().getDataFolder(), "config.yml");
        if(!Plugins.getInstance().getDataFolder().exists())
            Plugins.getInstance().getDataFolder().mkdir();

        if(!configFile.exists()) {
            FileOutputStream outputStream = new FileOutputStream(configFile);
            InputStream inputStream = Plugins.getInstance().getResourceAsStream("config.yml");
            inputStream.transferTo(outputStream);
        }

        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
    }

    public static Plugins getInstance() { return instance; }

    public void versionCheck() {
        if(!ProxyServer.getInstance().getConfig().isOnlineMode()) {
            log.warning(this.getDescription().getName() + " version " + this.getDescription().getVersion() + " cannot check for updates because server is in offline mode!");
            return;
        }
        ProxyServer.getInstance().getScheduler().runAsync(this, () -> {
            try {
                URL url = new URL("https://raw.githubusercontent.com/CyberedCake/cmd_plugins/main/src/main/resources/bungee.yml");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

                String inputLine;
                while((inputLine = reader.readLine()) != null) {
                    if(inputLine.startsWith("version: '")) {
                        String line = inputLine.replace("version: '", "");
                        line = line.substring(0, line.length()-1);
                        latestVersion = line.split(" ")[0].replace("v", "");
                        latestProtocol = Integer.parseInt(line.split(" ")[1].replace("p", ""));
                    }
                }
            } catch (Exception exception) {
                log.severe(ChatColor.RED + "Failed version checking for " + this.getDescription().getName() + " version " + this.getDescription().getVersion() + "! " + ChatColor.DARK_GRAY + exception); return;
            }

            if(currentProtocol != latestProtocol && !latestVersion.startsWith("error:")) {
                log.warning(ChatColor.YELLOW + "\"" + this.getDescription().getName() + "\" is outdated! The latest version is " + ChatColor.GREEN + latestVersion + ChatColor.YELLOW + ", your version is " + ChatColor.GOLD + currentVersion + ChatColor.YELLOW + "!");
                if (latestProtocol - currentProtocol > 0) {
                    log.warning(ChatColor.YELLOW + "\"" + this.getDescription().getName() + "\" is " + ChatColor.RED + (latestProtocol - currentProtocol) + " version(s) " + ChatColor.YELLOW + "behind!");
                    log.warning(ChatColor.YELLOW + "You can download the latest version of \"" + this.getDescription().getName() + "\" at " + ChatColor.LIGHT_PURPLE + latestDownloadLink + ChatColor.YELLOW + "!");
                }
            }
        });
    }

    public Iterable<String> getTabCompletions(String argument, List<String> completions) {
        List<String> matching = completions.stream().filter(argument::equals).toList();
        if(matching.size() == 1 && matching.get(0).equalsIgnoreCase(argument)) return new ArrayList<>();
        Collection<String> returned = new TreeSet<>(Collator.getInstance(Locale.ROOT));
        returned.addAll(completions.stream().filter(item -> item.toLowerCase(Locale.ROOT).startsWith(argument.toLowerCase(Locale.ROOT))).toList());
        return returned;
    }

}
