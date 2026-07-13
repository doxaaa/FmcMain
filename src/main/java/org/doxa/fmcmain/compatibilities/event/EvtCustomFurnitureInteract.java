package org.doxa.fmcmain.compatibilities.event;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import java.lang.reflect.Method;
import java.util.List;

public class EvtCustomFurnitureInteract extends BukkitScriptEvent implements Listener {

    public static EvtCustomFurnitureInteract instance;
    public org.bukkit.event.Event currentEvent;
    public ElementTag click_type;

    public EvtCustomFurnitureInteract() {
        instance = this;
        // REQUIRED SYNTAX: Updated precisely to use your custom prefix layout string
        registerCouldMatcher("ce_furniture player <'left'/'right'> clicks furniture");
        registerCouldMatcher("ce_furniture player clicks furniture");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init() {
        String[] targetClassNames = {
                "net.momirealms.craftengine.bukkit.api.event.FurnitureInteractEvent",
                "net.momirealms.craftengine.bukkit.api.event.FurnitureAttackEvent"
        };

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
                if (event.getClass().getSimpleName().equals("FurnitureAttackEvent")) {
                    click_type = new ElementTag("LEFT_CLICK_BLOCK");
                } else {
                    Method actionMethod = event.getClass().getMethod("action");
                    actionMethod.setAccessible(true);
                    Object actionValue = actionMethod.invoke(event);
                    click_type = new ElementTag(actionValue != null ? actionValue.toString() : "RIGHT_CLICK_BLOCK");
                }
            } catch (Exception e) {
                click_type = new ElementTag("RIGHT_CLICK_BLOCK");
            }
            fire();
        };

        for (String className : targetClassNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (org.bukkit.event.Event.class.isAssignableFrom(clazz)) {
                    org.bukkit.Bukkit.getServer().getPluginManager().registerEvent(
                            (Class<? extends org.bukkit.event.Event>) clazz, this, EventPriority.NORMAL, executor, org.doxa.fmcmain.FmcMain.instance
                    );
                }
            } catch (Exception ignored) {}
        }
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
        return path.eventLower.contains("clicks") && path.eventLower.contains("ce_furniture");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (currentEvent == null) {
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
        String customFurnitureId = getFurnitureIdStr().toLowerCase();
        String underscoredFurnitureId = customFurnitureId.replace(":", "_");

        boolean isMatchSuccessful = List.of("furniture", customFurnitureId, underscoredFurnitureId).contains(targetArg)
                || customFurnitureId.endsWith(":" + targetArg);

        if (!isMatchSuccessful) {
            return false;
        }

        return runInCheck(path, getTargetLocation());
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
            case "id":
            case "furniture":
                String idStr = getFurnitureIdStr();
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

    private String getFurnitureIdStr() {
        try {
            Object assetObj = getAssetObject();
            if (assetObj != null) {
                Method idMethod = assetObj.getClass().getMethod("id");
                idMethod.setAccessible(true);
                return idMethod.invoke(assetObj).toString();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private Object getAssetObject() {
        if (currentEvent == null) return null;
        try {
            Method furnitureMethod = currentEvent.getClass().getMethod("furniture");
            furnitureMethod.setAccessible(true);
            return furnitureMethod.invoke(currentEvent);
        } catch (Exception ignored) {
            try {
                for (Method m : currentEvent.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getName().toLowerCase().contains("furniture")) {
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
        if (currentEvent.getClass().getSimpleName().equals("FurnitureAttackEvent")) return "left";
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
