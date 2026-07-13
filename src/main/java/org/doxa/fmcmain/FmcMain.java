package org.doxa.fmcmain;

import org.bukkit.plugin.java.JavaPlugin;
import com.denizenscript.denizencore.events.ScriptEvent;
import org.doxa.fmcmain.compatibilities.event.PlayerPaintsScriptEvent;
import org.doxa.fmcmain.compatibilities.event.EvtCraftEngineReload;
import org.doxa.fmcmain.compatibilities.event.EvtCustomBlock;
import org.doxa.fmcmain.compatibilities.event.EvtCustomFurniture;
import org.doxa.fmcmain.compatibilities.event.EvtCustomBlockInteract;
import org.doxa.fmcmain.compatibilities.event.EvtCustomFurnitureInteract;

public final class FmcMain extends JavaPlugin {

    public static FmcMain instance;

    @Override
    public void onEnable() {
        instance = this;

        // Register Painter Bridge Event Tracks
        ScriptEvent.registerScriptEvent(new PlayerPaintsScriptEvent());

        // Exact matching lines for Reload Event Tracks
        EvtCraftEngineReload reloadEvent = new EvtCraftEngineReload();
        ScriptEvent.registerScriptEvent(reloadEvent);
        getServer().getPluginManager().registerEvents(reloadEvent, this);

        // --- SPECIFICATION: Registered inline in exactly 1 single line each ---

        // Register CraftEngine Custom Block Place/Break Events
        ScriptEvent.registerScriptEvent(new EvtCustomBlock());

        // Register CraftEngine Custom Furniture Place/Break Events
        ScriptEvent.registerScriptEvent(new EvtCustomFurniture());

        ScriptEvent.registerScriptEvent(new EvtCustomBlockInteract());
        ScriptEvent.registerScriptEvent(new EvtCustomFurnitureInteract());

        // Run Central Hook Registrations
        DenizenHook.register();

        getLogger().info("FmcMain successfully loaded combining PainterBridge & CEBridge!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FmcMain safely disabled.");
    }
}
