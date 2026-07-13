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
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import java.lang.reflect.Method;
import java.util.List;

public class EvtCustomBlockInteract extends BukkitScriptEvent implements Listener {

    public static EvtCustomBlockInteract instance;
    public org.bukkit.event.Event currentEvent;
    public ElementTag click_type;

    public EvtCustomBlockInteract() {
        instance = this;
        registerCouldMatcher("ce_block player left clicks block");
        registerCouldMatcher("ce_block player right clicks block");
        registerCouldMatcher("ce_block player clicks block");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init() {
        String targetClassName = "net.momirealms.craftengine.bukkit.api.event.CustomBlockInteractEvent";
        EventExecutor executor = (listener, event) -> {
            try {
                Method handMethod = event.getClass().getMethod("hand");
                handMethod.setAccessible(true);
                Object handEnum = handMethod.invoke(event);
                if (handEnum != null && handEnum.toString().contains("OFF_HAND")) {
                    return;
                }
            } catch (Exception ignored) {}

            currentEvent = event;
            try {
                Method actionMethod = event.getClass().getMethod("action");
                actionMethod.setAccessible(true);
                Object actionValue = actionMethod.invoke(event);
                click_type = new ElementTag(actionValue != null ? actionValue.toString() : "RIGHT_CLICK_BLOCK");
            } catch (Exception e) {
                click_type = new ElementTag("RIGHT_CLICK_BLOCK");
            }
            fire();
        };

        try {
            Class<?> clazz = Class.forName(targetClassName);
            if (org.bukkit.event.Event.class.isAssignableFrom(clazz)) {
                org.bukkit.Bukkit.getServer().getPluginManager().registerEvent(
                        (Class<? extends org.bukkit.event.Event>) clazz, this, EventPriority.NORMAL, executor, org.doxa.fmcmain.FmcMain.instance
                );
            }
        } catch (Exception ignored) {}
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
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.contains("clicks") && path.eventLower.contains("ce_block") && !path.eventLower.contains("furniture");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (currentEvent == null) {
            return false;
        }

        Location targetLoc = getTargetLocation();
        if (targetLoc == null || !CraftEngineBlocks.isCustomBlock(targetLoc.getBlock())) {
            return false;
        }

        int clicksIndex = -1;
        for (int i = 0; i < path.eventArgsLower.length; i++) {
            if (path.eventArgsLower[i].equals("clicks")) {
                clicksIndex = i;
                break;
            }
        }

        if (clicksIndex == -1) {
            return false;
        }

        if (clicksIndex == 3) {
            String clickDirectionArg = path.eventArgLowerAt(2);
            String detectedActionType = getClickActionType();
            if (!clickDirectionArg.equalsIgnoreCase(detectedActionType)) {
                return false;
            }
        }

        String targetArg = path.eventArgLowerAt(clicksIndex + 1);
        String customBlockId = getBlockIdStr().toLowerCase();
        String underscoredBlockId = customBlockId.replace(":", "_");

        boolean isMatchSuccessful = List.of("block", customBlockId, underscoredBlockId).contains(targetArg)
                || customBlockId.endsWith(":" + targetArg);

        if (!isMatchSuccessful) {
            return false;
        }

        return runInCheck(path, targetLoc);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "click_type":
                return click_type;
            case "location":
                Location loc = getTargetLocation();
                if (loc != null) return new LocationTag(loc);
                break;
            case "material":
                String idStr = getBlockIdStr();
                if (!idStr.isEmpty()) {
                    return new ElementTag(idStr);
                }
                break;
            case "item_in_hand":
                Player p = getBukkitPlayerInstance();
                if (p != null) return new ItemTag(p.getInventory().getItemInMainHand());
                break;
        }
        return super.getContext(name);
    }

    private String getBlockIdStr() {
        try {
            Object state = getAssetObject();
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
                    return idObj.toString();
                }
                String stateStr = state.toString();
                if (stateStr.contains("[")) {
                    return stateStr.substring(0, stateStr.indexOf('['));
                }
                return stateStr;
            }
        } catch (Exception ignored) {}
        return "";
    }

    private Object getAssetObject() {
        if (currentEvent == null) return null;
        try {
            Method blockStateMethod = currentEvent.getClass().getMethod("blockState");
            blockStateMethod.setAccessible(true);
            return blockStateMethod.invoke(currentEvent);
        } catch (Exception ignored) {
            try {
                for (Method m : currentEvent.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getName().toLowerCase().contains("block")) {
                        m.setAccessible(true);
                        return m.invoke(currentEvent);
                    }
                }
            } catch (Exception ignored2) {}
        }
        return null;
    }

    private String getClickActionType() {
        if (currentEvent == null) return "right";
        try {
            Method actionMethod = currentEvent.getClass().getMethod("action");
            actionMethod.setAccessible(true);
            Object actionValue = actionMethod.invoke(currentEvent);
            if (actionValue != null && actionValue.toString().toUpperCase().contains("LEFT")) {
                return "left";
            }
        } catch (Exception ignored) {}
        return "right";
    }

    private Location getTargetLocation() {
        if (currentEvent == null) return null;
        try {
            Object stateObj = getAssetObject();
            if (stateObj != null) {
                Method locMethod = stateObj.getClass().getMethod("location");
                locMethod.setAccessible(true);
                return (Location) locMethod.invoke(stateObj);
            }
        } catch (Exception ignored) {
            try {
                Method fallbackLoc = currentEvent.getClass().getMethod("location");
                fallbackLoc.setAccessible(true);
                return (Location) fallbackLoc.invoke(currentEvent);
            } catch (Exception ignored2) {}
        }
        return null;
    }

    private Player getBukkitPlayerInstance() {
        if (currentEvent == null) return null;
        try {
            Method playerMethod = currentEvent.getClass().getMethod("player");
            playerMethod.setAccessible(true);
            return (Player) playerMethod.invoke(currentEvent);
        } catch (Exception ignored) {}
        return null;
    }
}
