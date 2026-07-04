package org.doxa.fmcmain.compatibilities.expression;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Set;

public class ExprPlayerListPlots {

    public static void register() {
        // <--[tag]
        // @attribute <PlayerTag.list_plot_homes>
        // @returns ListTag
        // @description
        // Returns a list of players plot homes
        // -->
        PlayerTag.tagProcessor.registerTag(ListTag.class, "list_plot_homes", (attribute, player) -> {

            if (player.getOfflinePlayer() == null) {
                return new ListTag();
            }

            // Accesses the PlotSquared platform player manager mapping pipeline safely
            PlotPlayer<?> plotPlayer = PlotSquared.platform().playerManager()
                    .getPlayer(player.getOfflinePlayer().getUniqueId());

            ListTag plotHomesList = new ListTag();

            // FIXED: Removed the redundant null check wrapper to naturally eliminate the warning
            Set<Plot> playerPlots = plotPlayer.getPlots();

            if (playerPlots != null && !playerPlots.isEmpty()) {
                for (Plot plot : playerPlots) {
                    // Modern PlotSquared v6+ location lookup framework
                    com.plotsquared.core.location.Location plotLoc = plot.getHomeSynchronous();
                    if (plotLoc != null) {
                        // Dynamically instantiates a standard Bukkit location context
                        Location bukkitLoc = new Location(
                                Bukkit.getWorld(plotLoc.getWorldName()),
                                plotLoc.getX(),
                                plotLoc.getY(),
                                plotLoc.getZ(),
                                plotLoc.getYaw(),
                                plotLoc.getPitch()
                        );
                        // Adds the LocationTag seamlessly to the returned list
                        plotHomesList.addObject(new LocationTag(bukkitLoc));
                    }
                }
            }

            return plotHomesList;
        });
    }
}
