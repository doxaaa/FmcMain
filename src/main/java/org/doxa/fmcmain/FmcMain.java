package org.doxa.fmcmain;

import org.bukkit.plugin.java.JavaPlugin;
import com.denizenscript.denizencore.events.ScriptEvent;
import org.doxa.fmcmain.compatibilities.event.PlayerPaintsScriptEvent;
import org.doxa.fmcmain.compatibilities.event.EvtPlayerEntersPlot;
import org.doxa.fmcmain.compatibilities.event.EvtPlayerLeavesPlot;

public final class FmcMain extends JavaPlugin {

    public static FmcMain instance;

    @Override
    public void onEnable() {
        instance = this;

        // Register Painter Bridge Event Tracks
        ScriptEvent.registerScriptEvent(new PlayerPaintsScriptEvent());

        ScriptEvent.registerScriptEvent(new EvtPlayerEntersPlot());
        ScriptEvent.registerScriptEvent(new EvtPlayerLeavesPlot());

        // Run Central Hook Registrations
        DenizenHook.register();

        getLogger().info("FmcMain successfully loaded combining PainterBridge & CEBridge!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FmcMain safely disabled.");
    }
}