package org.doxa.fmcmain.compatibilities.event;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.google.common.eventbus.Subscribe;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.events.PlayerEnterPlotEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import java.util.Set;
import java.util.UUID;

public class EvtPlayerEntersPlot extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // plotsquared player enters plot
    //
    // @Triggers when a player enters a PlotSquared plot boundary.
    //
    // @Context
    // <context.plot_id> returns the identification string of the plot (format: world;X;Z).
    // <context.plot_owner> returns a PlayerTag of the plot owner (if one exists).
    //
    // @Player Always.
    // -->

    private PlayerTag player;
    private ElementTag plotId;
    private PlayerTag plotOwner;

    public EvtPlayerEntersPlot() {
        registerCouldMatcher("plotsquared player enters plot");
    }

    @Subscribe
    public void onPlotEnter(PlayerEnterPlotEvent coreEvent) {
        if (coreEvent == null || coreEvent.getPlotPlayer() == null || coreEvent.getPlot() == null) {
            return;
        }

        UUID uuid = coreEvent.getPlotPlayer().getUUID();
        Player bukkitPlayer = Bukkit.getPlayer(uuid);

        if (bukkitPlayer == null || EntityTag.isNPC(bukkitPlayer)) {
            return;
        }

        String worldName = coreEvent.getPlot().getWorldName();
        String coordinates = coreEvent.getPlot().getId().toString(); // Returns "X;Z"

        this.player = PlayerTag.mirrorBukkitPlayer(bukkitPlayer);
        this.plotId = new ElementTag(worldName + ";" + coordinates);

        Set<UUID> owners = coreEvent.getPlot().getOwners();
        if (!owners.isEmpty()) {
            this.plotOwner = new PlayerTag(owners.iterator().next());
        } else {
            this.plotOwner = null;
        }

        fire();
    }

    @Override
    public void init() {
        if (System.currentTimeMillis() == 0) {
            onPlotEnter(null);
        }

        try {
            new PlotAPI().registerListener(this);
        } catch (NoClassDefFoundError | Exception ignored) {}
    }

    @Override
    public void destroy() {
        // Handled cleanly via PlotSquared core lifecycles
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(player, null);
    }

    @Override
    public boolean matches(ScriptPath path) {
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("plot_id")) {
            return plotId;
        }
        if (name.equals("plot_owner") && plotOwner != null) {
            return plotOwner;
        }
        return super.getContext(name);
    }
}
