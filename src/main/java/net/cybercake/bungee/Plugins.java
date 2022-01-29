package net.cybercake.bungee;

import net.md_5.bungee.api.plugin.Plugin;

public final class Plugins extends Plugin {

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(this, new CommandPlugins());
        getProxy().getPluginManager().registerCommand(this, new CommandVersion());
    }

}
