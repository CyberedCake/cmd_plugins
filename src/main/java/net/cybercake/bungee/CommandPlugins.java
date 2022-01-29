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

import java.util.ArrayList;

public class CommandPlugins extends Command implements TabExecutor {

    public CommandPlugins() {
        super("gplugins", "bungeecord.command.plugins", "gpl");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TextComponent component = new TextComponent();

        component.addExtra(new TextComponent("Proxy Plugins (" + ProxyServer.getInstance().getPluginManager().getPlugins().size() + "): "));
        int index=0;
        for(Plugin plugin : ProxyServer.getInstance().getPluginManager().getPlugins()) {
            TextComponent pluginComponent = new TextComponent(ChatColor.GREEN + plugin.getDescription().getName());
            pluginComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
                    ChatColor.WHITE + "Version: " + ChatColor.GREEN + plugin.getDescription().getVersion() +
                            (plugin.getDescription().getDescription() == null ? "" : "\n" + ChatColor.WHITE + "Description: " + ChatColor.GREEN + plugin.getDescription().getDescription() +
                                    (plugin.getDescription().getAuthor() == null ? "" : "\n" + ChatColor.WHITE + "Author: " + ChatColor.GREEN + plugin.getDescription().getAuthor())))));
            pluginComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/gversion " + plugin.getDescription().getName()));
            component.addExtra(pluginComponent);
            component.addExtra(new TextComponent(index == ProxyServer.getInstance().getPluginManager().getPlugins().size()-1 ? "" : ChatColor.RESET + ", "));
            index++;
        }

        sender.sendMessage(component);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

}
