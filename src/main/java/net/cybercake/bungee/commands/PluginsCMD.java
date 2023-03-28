package net.cybercake.bungee.commands;

import net.cybercake.bungee.Plugins;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.awt.*;
import java.text.Collator;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PluginsCMD extends Command implements TabExecutor {

    public PluginsCMD() {
        super("gplugins", "bungeecord.command.plugins", "gpl");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(Plugins.config.getBoolean("useAlternatePluginsMessage", false)) showAlternatePlugins(sender, args);
        else showPlugins(sender, args);
    }

    private void showAlternatePlugins(CommandSender sender, String[] args) {
        BaseComponent component = new TextComponent();

        // int values
        Integer MODULE = 1;
        Integer PLUGIN = 0;
        Map<String, Integer> pluginNames = new HashMap<>();
        for(Plugin plugin : ProxyServer.getInstance().getPluginManager().getPlugins()) {
            if(isPlugin(plugin)) pluginNames.put(plugin.getDescription().getName(), PLUGIN); // plugin
            else pluginNames.put(plugin.getDescription().getName(), MODULE); // module
        }

        Map<String, Integer> modules = pluginNames.entrySet().stream().filter((item) -> Objects.equals(item.getValue(), MODULE)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, Integer> plugins = pluginNames.entrySet().stream().filter((item) -> Objects.equals(item.getValue(), PLUGIN)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if(modules.values().size() > 0) { // if there is more than one module installed (almost always true)
            component.addExtra(alternatePluginDisplaySection(
                     "Modules: (" + modules.values().size() + "):", modules, new int[]{2, 136, 209}
            ));
        }
        if(plugins.values().size() > 0) { // if there is more than one plugin installed
            component.addExtra(alternatePluginDisplaySection(
                    (modules.values().size() > 0 ? "\n" : "") + "Bungeecord Plugins: (" + plugins.values().size() + "):", plugins, new int[]{237, 129, 6}
                    ));
        }

        sender.sendMessage(component);
    }

    private BaseComponent alternatePluginDisplaySection(String title, Map<String, Integer> plugins, int[] titleColor) {
        BaseComponent titleComponent = new TextComponent(title);
        titleComponent.setColor(ChatColor.of(new Color(titleColor[0], titleColor[1], titleColor[2])));
        BaseComponent displaySection = new TextComponent(titleComponent);
        int index = 0;
        for(String name : plugins.keySet()) {
            if(index == 0) displaySection.addExtra(new TextComponent("\n" + ChatColor.DARK_GRAY + "- "));
            Plugin plugin = ProxyServer.getInstance().getPluginManager().getPlugin(name);

            displaySection.addExtra(formatPlugin(plugin));
            displaySection.addExtra(new TextComponent(index == plugins.values().size()-1 ? "" : ChatColor.RESET + ", "));
            index++;
        }
        return displaySection;
    }

    private void showPlugins(CommandSender sender, String[] args) {
        BaseComponent component = new TextComponent();

        Collection<String> pluginNames = new TreeSet<>(Collator.getInstance());
        int number = 0;
        for(Plugin plugin : ProxyServer.getInstance().getPluginManager().getPlugins()) {
            if(args.length == 0 || args[0].equalsIgnoreCase("all") || !Arrays.asList("modules", "plugins").contains(args[0])) {
                pluginNames.add(plugin.getDescription().getName());
                number++;
            }else if(args[0].equalsIgnoreCase("modules") && !isPlugin(plugin)) {
                pluginNames.add(plugin.getDescription().getName());
                number++;
            }else if(args[0].equalsIgnoreCase("plugins") && isPlugin(plugin)) {
                pluginNames.add(plugin.getDescription().getName());
                number++;
            }
        }

        component.addExtra(new TextComponent("Proxy Plugins (" + number + "): "));
        int index=0;
        for(String name : pluginNames) {
            Plugin plugin = ProxyServer.getInstance().getPluginManager().getPlugin(name);

            component.addExtra(formatPlugin(plugin));
            component.addExtra(new TextComponent(index == number-1 ? "" : ChatColor.RESET + ", "));
            index++;
        }

        sender.sendMessage(component);
    }

    private boolean isPlugin(Plugin plugin) {
        return (plugin.getFile().getParent().contains("plugins"));
    }

    private BaseComponent formatPlugin(Plugin plugin) {
        BaseComponent pluginComponent = new TextComponent(ChatColor.GREEN + plugin.getDescription().getName());
        pluginComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
                ChatColor.WHITE + "Version: " + ChatColor.GREEN + plugin.getDescription().getVersion() +
                        (plugin.getDescription().getDescription() == null ? "" : "\n" + ChatColor.WHITE + "Description: " + ChatColor.GREEN + plugin.getDescription().getDescription()) +
                        (plugin.getDescription().getAuthor() == null ? "" : "\n" + ChatColor.WHITE + "Author: " + ChatColor.GREEN + plugin.getDescription().getAuthor()))));
        pluginComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/gversion " + plugin.getDescription().getName()));
        return pluginComponent;
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if(args.length < 2)
            return Plugins.getInstance().getTabCompletions(args[0], List.of("all", "modules", "plugins"));
        return new ArrayList<>();
    }

}
