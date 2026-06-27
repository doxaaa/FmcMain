package org.doxa.fmcmain.compatibilities.event;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockPlaceEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;

public class EvtCustomBlock extends BukkitScriptEvent implements Listener {

    public static EvtCustomBlock instance;
    public org.bukkit.event.Event currentEvent;
    private boolean isMutingVanillaMatch = false;

    public static final HashMap<UUID, Long> placementCooldownCache = new HashMap<>();

    public EvtCustomBlock() {
        instance = this;
        registerCouldMatcher("craftengine player <'places'/'breaks'> block");
    }

    @Override
    public void init() {
        org.bukkit.Bukkit.getServer().getPluginManager().registerEvents(this, org.doxa.fmcmain.FmcMain.instance);
    }

    @Override
    public void destroy() {
        org.bukkit.event.HandlerList.unregisterAll(this);
    }

    @Override
    public void cancellationChanged() {
        if (cancelled && currentEvent instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
        }
        super.cancellationChanged();
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        Player p = getBukkitPlayerInstance();
        return new BukkitScriptEntryData(p != null ? new PlayerTag(p) : null, null);
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (isMutingVanillaMatch) {
            return false;
        }

        String action = path.eventArgLowerAt(2);
        if (currentEvent instanceof CustomBlockPlaceEvent && !action.equals("places")) {
            return false;
        }
        if (currentEvent instanceof CustomBlockBreakEvent && !action.equals("breaks")) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "location":
                if (currentEvent instanceof CustomBlockPlaceEvent placeEvt) {
                    return new LocationTag(placeEvt.location());
                } else if (currentEvent instanceof CustomBlockBreakEvent breakEvt) {
                    return new LocationTag(breakEvt.location());
                }
                break;
            case "material":
                try {
                    Location loc = null;
                    if (currentEvent instanceof CustomBlockPlaceEvent placeEvt) {
                        loc = placeEvt.location();
                    } else if (currentEvent instanceof CustomBlockBreakEvent breakEvt) {
                        loc = breakEvt.location();
                    }

                    if (loc != null) {
                        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(loc.getBlock());
                        if (state != null) {
                            Object idObj = null;
                            for (Method m : state.getClass().getMethods()) {
                                if (m.getParameterCount() == 0) {
                                    String retName = m.getReturnType().getName();
                                    if (retName.contains("Key") || retName.contains("Definition") || retName.contains("BlockDefinition")) {
                                        m.setAccessible(true);
                                        Object res = m.invoke(state);
                                        if (res != null) {
                                            if (res.getClass().getName().contains("BlockDefinition")) {
                                                try {
                                                    Method idM = res.getClass().getMethod("id");
                                                    idM.setAccessible(true);
                                                    idObj = idM.invoke(res);
                                                } catch (Exception ignored) {}
                                            } else {
                                                idObj = res;
                                            }
                                            break;
                                        }
                                    }
                                }
                            }

                            if (idObj != null) {
                                return new ElementTag(idObj.toString());
                            }

                            String stateStr = state.toString();
                            if (stateStr.contains("[")) {
                                return new ElementTag(stateStr.substring(0, stateStr.indexOf('[')));
                            }
                            return new ElementTag(stateStr);
                        }
                    }
                } catch (Exception ignored) {}
                break;
            case "item_in_hand":
                Player playerEntity = getBukkitPlayerInstance();
                if (playerEntity != null) {
                    ItemStack handItem = playerEntity.getInventory().getItemInMainHand();
                    return new ItemTag(handItem);
                }
                break;
        }
        return super.getContext(name);
    }

    private Player getBukkitPlayerInstance() {
        if (currentEvent instanceof CustomBlockPlaceEvent placeEvt) {
            return placeEvt.player();
        } else if (currentEvent instanceof CustomBlockBreakEvent breakEvt) {
            return breakEvt.getPlayer();
        }
        return null;
    }

    @EventHandler
    public void onCustomBlockPlace(CustomBlockPlaceEvent event) {
        currentEvent = event;
        isMutingVanillaMatch = false;

        // FIX: Executed directly and stripped out the redundant null condition wrapper entirely
        placementCooldownCache.put(event.player().getUniqueId(), System.currentTimeMillis());

        fire();
    }

    @EventHandler
    public void onCustomBlockBreak(CustomBlockBreakEvent event) {
        currentEvent = event;
        isMutingVanillaMatch = false;
        fire();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVanillaPlaceMute(BlockPlaceEvent event) {
        try {
            if (CraftEngineBlocks.isCustomBlock(event.getBlock())) {
                currentEvent = event;
                isMutingVanillaMatch = true;
            }
        } catch (Exception ignored) {}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVanillaBreakMute(BlockBreakEvent event) {
        try {
            if (CraftEngineBlocks.isCustomBlock(event.getBlock())) {
                currentEvent = event;
                isMutingVanillaMatch = true;
            }
        } catch (Exception ignored) {}
    }
}
