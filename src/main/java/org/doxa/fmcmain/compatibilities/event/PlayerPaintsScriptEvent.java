package org.doxa.fmcmain.compatibilities.event;

import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizen.objects.LocationTag;
import dev.wuason.unearthMechanic.events.PreApplyStageEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;

public class PlayerPaintsScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // UnearthMechanic player paints block
    //
    // @Regex ^on UnearthMechanic player paints block$
    //
    // @Triggers when a player modifies a block layer footprint using the UnearthMechanic paintbrush tool.
    //
    // @Location true
    //
    // @Context
    // <context.location> Returns the exact coordinate location of the block being painted as a LocationTag.
    //
    // @Plugin FmcMain, unearthMechanic
    //
    // @Player Always.
    //
    // @Group FmcMain
    //
    // -->

    public PlayerPaintsScriptEvent() {
    }

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("unearthmechanic player paints block");
    }

    @Override
    public boolean matches(ScriptPath path) {
        Block block = getBlock();
        if (block == null) {
            return false;
        }
        if (!runInCheck(path, block.getLocation())) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(getPlayer());
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("location")) {
            Block block = getBlock();
            if (block != null) {
                return new LocationTag(block.getLocation());
            }
        }
        return super.getContext(name);
    }

    @Override
    public void cancellationChanged() {
        if (cancelled && event != null) {
            event.setCancelled(true);
        }
        super.cancellationChanged();
    }

    public PreApplyStageEvent event;

    private final HashMap<String, Integer> slidingTickCache = new HashMap<>();
    private final HashMap<String, Boolean> cancellationCache = new HashMap<>();
    private final HashMap<UUID, Integer> playerTickCache = new HashMap<>();
    private final HashMap<UUID, Boolean> playerCancelCache = new HashMap<>();

    @EventHandler
    public void onPlayerPaints(PreApplyStageEvent event) {
        this.event = event;

        Player player = getPlayer();
        Block block = getBlock();

        if (player == null || block == null) {
            return;
        }

        int currentTick = Bukkit.getCurrentTick();
        UUID playerUUID = player.getUniqueId();
        String blockKey = playerUUID + ":" + block.getX() + "," + block.getY() + "," + block.getZ();

        if (playerTickCache.containsKey(playerUUID)) {
            int lastPlayerTick = playerTickCache.get(playerUUID);
            if (currentTick == lastPlayerTick) {
                Boolean wasPlayerCancelled = playerCancelCache.get(playerUUID);
                if (wasPlayerCancelled != null && wasPlayerCancelled) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (slidingTickCache.containsKey(blockKey)) {
            int lastPaintedTick = slidingTickCache.get(blockKey);
            if (currentTick == lastPaintedTick) {
                Boolean wasCancelled = cancellationCache.get(blockKey);
                if (wasCancelled != null && wasCancelled) {
                    event.setCancelled(true);
                }
                return;
            }
        }

        slidingTickCache.put(blockKey, currentTick);
        playerTickCache.put(playerUUID, currentTick);

        if (slidingTickCache.size() > 150) {
            slidingTickCache.entrySet().removeIf(entry -> (currentTick - entry.getValue()) > 20);
            cancellationCache.keySet().removeIf(key -> !slidingTickCache.containsKey(key));
        }
        if (playerTickCache.size() > 50) {
            playerTickCache.entrySet().removeIf(entry -> (currentTick - entry.getValue()) > 20);
            playerCancelCache.keySet().removeIf(uuid -> !playerTickCache.containsKey(uuid));
        }

        fire();

        cancellationCache.put(blockKey, this.cancelled);
        playerCancelCache.put(playerUUID, this.cancelled);
    }

    public Player getPlayer() {
        if (event == null) {
            return null;
        }
        try {
            for (Method method : event.getClass().getDeclaredMethods()) {
                if (method.getParameterCount() == 0) {
                    Class<?> returnType = method.getReturnType();
                    if (Player.class.isAssignableFrom(returnType) || returnType.getName().toLowerCase().contains("player")) {
                        method.setAccessible(true);
                        Object result = method.invoke(event);
                        if (result instanceof Player) {
                            return (Player) result;
                        }
                    }
                }
            }
            for (Field field : event.getClass().getDeclaredFields()) {
                if (Player.class.isAssignableFrom(field.getType()) || field.getType().getName().toLowerCase().contains("player")) {
                    field.setAccessible(true);
                    Object value = field.get(event);
                    if (value instanceof Player) {
                        return (Player) value;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public Block getBlock() {
        if (event == null) {
            return null;
        }
        try {
            for (Method method : event.getClass().getDeclaredMethods()) {
                if (method.getParameterCount() == 0) {
                    if (Block.class.isAssignableFrom(method.getReturnType())) {
                        method.setAccessible(true);
                        Object result = method.invoke(event);
                        if (result instanceof Block) {
                            return (Block) result;
                        }
                    } else if (org.bukkit.Location.class.isAssignableFrom(method.getReturnType())) {
                        method.setAccessible(true);
                        Object result = method.invoke(event);
                        if (result instanceof org.bukkit.Location) {
                            return ((org.bukkit.Location) result).getBlock();
                        }
                    }
                }
            }
            for (Field field : event.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(event);
                if (value instanceof Block) {
                    return (Block) value;
                } else if (value instanceof org.bukkit.Location) {
                    return ((org.bukkit.Location) value).getBlock();
                }
            }
        } catch (Exception ignored) {}

        Player player = getPlayer();
        if (player != null) {
            return player.getTargetBlockExact(5);
        }
        return null;
    }
}
