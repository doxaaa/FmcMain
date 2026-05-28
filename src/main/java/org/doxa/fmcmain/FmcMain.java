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

        // --- PAINTER BRIDGE LOGIC ---
        // Register the script instance directly into Denizen's listener architecture pipeline.
        ScriptEvent.registerScriptEvent(new PlayerPaintsScriptEvent());

        // --- CE BRIDGE LOGIC ---
        // 1. Create the event instance
        EvtCraftEngineReload reloadEvent = new EvtCraftEngineReload();

        // 2. Register it so Denizen scripts can use 'on craftengine reload'
        ScriptEvent.registerScriptEvent(reloadEvent);

        // 3. Register it so the @EventHandler actually catches the CraftEngine event
        getServer().getPluginManager().registerEvents(reloadEvent, this);

        // 4. Register all your tags (IDs, items, etc)
        DenizenHook.register();

        getLogger().info("FmcMain successfully loaded combining PainterBridge & CEBridge!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FmcMain safely disabled.");
    }
}