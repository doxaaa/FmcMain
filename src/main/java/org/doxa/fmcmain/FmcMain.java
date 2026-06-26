package org.doxa.fmcmain;

import org.bukkit.plugin.java.JavaPlugin;
import com.denizenscript.denizencore.events.ScriptEvent;
import org.doxa.fmcmain.compatibilities.event.PlayerPaintsScriptEvent;
import org.doxa.fmcmain.compatibilities.event.EvtCraftEngineReload;

public final class FmcMain extends JavaPlugin {

    public static FmcMain instance;

    @Override
    public void onEnable() {
        instance = this;

        // Register Painter Bridge Event Tracks
        ScriptEvent.registerScriptEvent(new PlayerPaintsScriptEvent());

        // Register CraftEngine Bridge Event Tracks
        EvtCraftEngineReload reloadEvent = new EvtCraftEngineReload();
        ScriptEvent.registerScriptEvent(reloadEvent);
        getServer().getPluginManager().registerEvents(reloadEvent, this);

        // Run Central Hook Registrations
        DenizenHook.register();

        getLogger().info("FmcMain successfully loaded combining PainterBridge & CEBridge!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FmcMain safely disabled.");
    }
}
