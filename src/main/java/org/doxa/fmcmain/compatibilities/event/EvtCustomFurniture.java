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

public class EvtCustomFurniture extends BukkitScriptEvent implements Listener {

    public static EvtCustomFurniture instance;
    public org.bukkit.event.Event currentEvent;

    public EvtCustomFurniture() {
        instance = this;
        registerCouldMatcher("craftengine player <'places'/'breaks'> furniture");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init() {
        String[] targetClassNames = {
                "net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent",
                "net.momirealms.craftengine.bukkit.api.event.FurnitureBreakEvent"
        };

        EventExecutor executor = (listener, event) -> {
            String name = event.getClass().getSimpleName().toLowerCase();
            if (name.contains("furniture") && (name.contains("place") || name.contains("break"))) {
                currentEvent = event;
                fire();
            }
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
            } catch (Exception ignored) {
            }
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
        String action = path.eventArgLowerAt(2);
        String eventName = currentEvent.getClass().getSimpleName().toLowerCase();

        if (eventName.contains("place") && !action.equals("places")) {
            return false;
        }
        if (eventName.contains("break") && !action.equals("breaks")) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "location":
                Location loc = getFurnitureLocation();
                if (loc != null) {
                    return new LocationTag(loc);
                }
                break;
            // FIX: Context name mapped cleanly to 'furniture' instead of 'material'
            case "furniture":
                try {
                    if (currentEvent != null) {
                        Method furnitureMethod = currentEvent.getClass().getMethod("furniture");
                        furnitureMethod.setAccessible(true);
                        Object furnitureObj = furnitureMethod.invoke(currentEvent);

                        if (furnitureObj != null) {
                            Method idMethod = furnitureObj.getClass().getMethod("id");
                            idMethod.setAccessible(true);
                            Object idObj = idMethod.invoke(furnitureObj);

                            if (idObj != null) {
                                return new ElementTag(idObj.toString());
                            }
                        }
                    }
                } catch (Exception ex) {
                    try {
                        if (currentEvent != null) {
                            for (Method m : currentEvent.getClass().getMethods()) {
                                if (m.getParameterCount() == 0 && m.getName().toLowerCase().contains("furniture")) {
                                    m.setAccessible(true);
                                    Object furnObj = m.invoke(currentEvent);
                                    if (furnObj != null) {
                                        String str = furnObj.toString();
                                        if (str.contains("[")) return new ElementTag(str.substring(0, str.indexOf('[')));
                                        if (str.contains("{")) return new ElementTag(str.substring(0, str.indexOf('{')));
                                        return new ElementTag(str);
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
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

    private Location getFurnitureLocation() {
        if (currentEvent == null) {
            return null;
        }
        try {
            Method m = currentEvent.getClass().getMethod("location");
            m.setAccessible(true);
            return (Location) m.invoke(currentEvent);
        } catch (Exception ignored) {
            try {
                for (Method m : currentEvent.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType() == Location.class) {
                        m.setAccessible(true);
                        return (Location) m.invoke(currentEvent);
                    }
                }
            } catch (Exception ignored2) {}
        }
        return null;
    }

    private Player getBukkitPlayerInstance() {
        if (currentEvent == null) {
            return null;
        }
        try {
            Method m = currentEvent.getClass().getMethod("player");
            m.setAccessible(true);
            return (Player) m.invoke(currentEvent);
        } catch (Exception ignored) {
            try {
                for (Method m : currentEvent.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType() == Player.class) {
                        m.setAccessible(true);
                        return (Player) m.invoke(currentEvent);
                    }
                }
            } catch (Exception ignored2) {}
        }
        return null;
    }
}
