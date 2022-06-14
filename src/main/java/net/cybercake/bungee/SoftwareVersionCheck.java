package net.cybercake.bungee;

import com.google.gson.JsonObject;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class SoftwareVersionCheck {

    private static final Logger log = ProxyServer.getInstance().getLogger();

    // thanks stack overflow: https://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    // thanks stack overflow: https://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java
    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            JSONObject json;
            try {
                json = new JSONObject(jsonText);
            } catch (JSONException jsonException) {
                json = new JSONArray(jsonText).getJSONObject(0);
            }
            return json;
        }
    }

    public static int currentBuildFromType(Plugins.ServerType serverType) {
        return switch(serverType) {
            case BUNGEECORD, WATERFALL, HEXACORD -> Integer.parseInt(ProxyServer.getInstance().getVersion().split(":")[4]);
            case FLAMECORD -> -1;
            case UNSUPPORTED -> -3;
        };
    }

    public static int latestProtocolSoftware = -1;
    public static String latestVersionSoftware = "0.0.0";
    public static String downloadLink = "Failed to get download link!::an error occurred!";

    public static void check() {
        if(!ProxyServer.getInstance().getConfig().isOnlineMode()) {
            latestProtocolSoftware = -1;
            latestVersionSoftware = "server_offline_mode";
            downloadLink = "server_offline_mode";
            log.severe("Failed to check for updates because the server is set to offline mode!");
            return;
        }

        switch(Plugins.serverType) {
            case BUNGEECORD -> {
                try {
                    JSONObject json = readJsonFromUrl("https://ci.md-5.net/job/BungeeCord/api/json").getJSONObject("lastSuccessfulBuild");
                    latestProtocolSoftware = json.getInt("number");
                    latestVersionSoftware = "none:BungeeCord does not provide semantic versioning";
                    downloadLink = "ci.md-5.net/job/BungeeCord::https://ci.md-5.net/job/BungeeCord/";
                } catch (Exception exception) {
                    latestProtocolSoftware = -1;
                    latestVersionSoftware = "failed:" + exception;
                    log.severe("Failed to check for the latest version: " + ChatColor.DARK_GRAY + exception);
                }
            }
            case WATERFALL -> {
                try {
                    JSONArray json = readJsonFromUrl("https://papermc.io/api/v2/projects/waterfall/").getJSONArray("versions");
                    String latestMC = "1.19";
                    for(int i=0; i<1000; i++) {
                        if(json.isNull(i)) { latestMC = json.getString(i-1); break; }
                    }

                    json = readJsonFromUrl("https://papermc.io/api/v2/projects/waterfall/versions/" + latestMC).getJSONArray("builds");
                    int finalBuildNumber = 0;
                    for(int i=0; i<1000; i++) {
                        if(json.isNull(i)) { finalBuildNumber = i-1; break; }
                    }

                    latestProtocolSoftware = json.getInt(finalBuildNumber);
                    latestVersionSoftware = "none:Waterfall does not provide semantic versioning";
                    downloadLink = "https://papermc.io/downloads::https://papermc.io/downloads#Waterfall";
                } catch (Exception exception) {
                    latestProtocolSoftware = -1;
                    latestVersionSoftware = "failed:" + exception;
                    log.severe("Failed to check for the latest version: " + ChatColor.DARK_GRAY + exception);
                }
            }
            case HEXACORD -> {
                try {
                    JSONObject json = readJsonFromUrl("https://api.github.com/repos/HexagonMC/BungeeCord/releases");
                    latestProtocolSoftware = Integer.parseInt(json.getString("tag_name").replace("v", ""));
                    latestVersionSoftware = json.getString("tag_name");
                    downloadLink = "github.com/HexagonMC/BungeeCord::https://github.com/HexagonMC/BungeeCord/releases/latest";
                } catch (Exception exception) {
                    latestProtocolSoftware = -1;
                    latestVersionSoftware = "failed:" + exception;
                    log.severe("Failed to check for the latest version: " + ChatColor.DARK_GRAY + exception);
                }
            }
            case FLAMECORD -> downloadLink = "ci.2lstudios.dev/job/FlameCord::https://ci.2lstudios.dev/job/FlameCord/";
            case UNSUPPORTED -> {
                latestProtocolSoftware = -2;
                latestVersionSoftware = "UNSUPPORTED";
                downloadLink = "UNSUPPORTED";
            }
        }
    }

}
