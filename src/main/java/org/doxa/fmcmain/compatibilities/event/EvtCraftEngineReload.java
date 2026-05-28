package org.doxa.fmcmain.compatibilities.event;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class EvtCraftEngineReload extends BukkitScriptEvent implements Listener {

    private static boolean hasBeenCalled = false;

    public static boolean hasBeenLoad() {
        return hasBeenCalled;
    }

    public EvtCraftEngineReload() {
        registerCouldMatcher("craftengine [first] load|reload");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (path.eventArgLowerAt(1).equals("first") && hasBeenCalled) {
            return false;
        }
        return super.matches(path);
    }

    @EventHandler
    public void onCraftEngineReload(CraftEngineReloadEvent event) {
        hasBeenCalled = true;
        fire(event);
    }
}
