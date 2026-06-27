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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import java.lang.reflect.Method;

public class EvtCustomInteract extends BukkitScriptEvent implements Listener {

    public static EvtCustomInteract instance;
    public org.bukkit.event.Event currentEvent;

    public EvtCustomInteract() {
        instance = this;
        // FIX 1: Register both patterns to support directional and general clicks cleanly
        registerCouldMatcher("craftengine player <'left'/'right'> clicks <'block'/'furniture'>");
        registerCouldMatcher("craftengine player clicks <'block'/'furniture'>");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init() {
        String[] targetClassNames = {
                "net.momirealms.craftengine.bukkit.api.event.CustomBlockInteractEvent",
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
            fire();
        };

        for (String className : targetClassNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (org.bukkit.event.Event.class.isAssignableFrom(clazz)) {
                    Class<? extends org.bukkit.event.Event> eventClass = (Class<? extends org.bukkit.event.Event>) clazz;
                    org.bukkit.Bukkit.getServer().getPluginManager().registerEvent(
                            eventClass, this, EventPriority.NORMAL, executor, org.doxa.fmcmain.FmcMain.instance
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
    public boolean matches(ScriptPath path) {
        if (currentEvent == null) {
            return false;
        }

        // FIX 2: Dynamically shift indices depending on whether a direction was passed
        boolean hasDirection = !path.eventArgLowerAt(2).equals("clicks");
        int targetIndex = hasDirection ? 4 : 3;

        String targetArg = path.eventArgLowerAt(targetIndex);
        String eventSimpleName = currentEvent.getClass().getSimpleName().toLowerCase();

        if (targetArg.equals("furniture") && !eventSimpleName.contains("furniture")) {
            return false;
        }
        if (targetArg.equals("block") && eventSimpleName.contains("furniture")) {
            return false;
        }

        // FIX 3: If 'clicks' was used, we automatically skip directional checks to match BOTH clicks
        if (hasDirection) {
            String clickDirectionArg = path.eventArgLowerAt(2);
            String detectedActionType = getClickActionType();
            if (clickDirectionArg.equals("left") && !detectedActionType.equals("left")) {
                return false;
            }
            if (clickDirectionArg.equals("right") && !detectedActionType.equals("right")) {
                return false;
            }
        }

        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "location":
                Location loc = getTargetLocation();
                if (loc != null) {
                    return new LocationTag(loc);
                }
                break;
            case "id":
            case "material":
            case "furniture":
                Object assetObj = getAssetObject();
                if (assetObj != null) {
                    try {
                        Method idMethod = assetObj.getClass().getMethod("id");
                        idMethod.setAccessible(true);
                        return new ElementTag(idMethod.invoke(assetObj).toString());
                    } catch (Exception ignored) {
                        try {
                            Method getBlockMethod = assetObj.getClass().getMethod("getBlock");
                            getBlockMethod.setAccessible(true);
                            Object defObj = getBlockMethod.invoke(assetObj);
                            Method idMethod = defObj.getClass().getMethod("id");
                            idMethod.setAccessible(true);
                            return new ElementTag(idMethod.invoke(defObj).toString());
                        } catch (Exception ignored2) {}
                    }

                    String s = assetObj.toString();
                    if (s.contains("[")) return new ElementTag(s.substring(0, s.indexOf('[')));
                    if (s.contains("{")) return new ElementTag(s.substring(0, s.indexOf('{')));
                    return new ElementTag(s);
                }
                break;
            case "item_in_hand":
                Player p = getBukkitPlayerInstance();
                if (p != null) {
                    ItemStack mainHandItem = p.getInventory().getItemInMainHand();
                    return new ItemTag(mainHandItem);
                }
                break;
        }
        return super.getContext(name);
    }

    private Object getAssetObject() {
        if (currentEvent == null) {
            return null;
        }
        try {
            Method blockStateMethod = currentEvent.getClass().getMethod("blockState");
            blockStateMethod.setAccessible(true);
            return blockStateMethod.invoke(currentEvent);
        } catch (Exception e) {
            try {
                Method furnitureMethod = currentEvent.getClass().getMethod("furniture");
                furnitureMethod.setAccessible(true);
                return furnitureMethod.invoke(currentEvent);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String getClickActionType() {
        if (currentEvent == null) {
            return "right";
        }

        String eventClassName = currentEvent.getClass().getSimpleName();
        if (eventClassName.equals("FurnitureAttackEvent")) {
            return "left";
        }

        try {
            Method actionMethod = currentEvent.getClass().getMethod("action");
            actionMethod.setAccessible(true);
            Object actionValue = actionMethod.invoke(currentEvent);
            if (actionValue != null) {
                String actionString = actionValue.toString().toUpperCase();
                if (actionString.contains("LEFT") || actionString.contains("ATTACK")) {
                    return "left";
                }
                if (actionString.contains("RIGHT") || actionString.contains("INTERACT")) {
                    return "right";
                }
            }
        } catch (Exception ignored) {}
        return "right";
    }

    private Location getTargetLocation() {
        if (currentEvent == null) {
            return null;
        }
        try {
            Method locMethod = currentEvent.getClass().getMethod("location");
            locMethod.setAccessible(true);
            return (Location) locMethod.invoke(currentEvent);
        } catch (Exception ignored) {}
        return null;
    }

    private Player getBukkitPlayerInstance() {
        if (currentEvent == null) {
            return null;
        }
        try {
            Method playerMethod = currentEvent.getClass().getMethod("player");
            playerMethod.setAccessible(true);
            return (Player) playerMethod.invoke(currentEvent);
        } catch (Exception ignored) {
            try {
                Method getPlayerMethod = currentEvent.getClass().getMethod("getPlayer");
                getPlayerMethod.setAccessible(true);
                return (Player) getPlayerMethod.invoke(currentEvent);
            } catch (Exception ignored2) {}
        }
        return null;
    }
}
