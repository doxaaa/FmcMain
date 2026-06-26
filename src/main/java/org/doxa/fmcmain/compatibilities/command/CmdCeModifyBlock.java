package org.doxa.fmcmain.compatibilities.command;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizen.objects.LocationTag;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CmdCeModifyBlock extends AbstractCommand {

    public CmdCeModifyBlock() {
        setName("cemodifyblock");
        setSyntax("cemodifyblock [<location>|...] [<id>[<state>=<val>,...]]");
        setRequiredArguments(2, 2);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("locations") && (arg.matchesArgumentList(LocationTag.class) || arg.matchesArgumentType(LocationTag.class))) {
                scriptEntry.addObject("locations", arg.asType(ListTag.class).filter(LocationTag.class, scriptEntry));
            }
            else if (!scriptEntry.hasObject("id")) {
                if (arg.hasPrefix()) {
                    scriptEntry.addObject("id", new ElementTag(arg.getPrefix() + ":" + arg.getValue()));
                } else {
                    scriptEntry.addObject("id", arg.asElement());
                }
            }
            else {
                arg.reportUnhandled();
            }
        }

        if (!scriptEntry.hasObject("locations")) {
            throw new InvalidArgumentsException("Missing location or location list argument!");
        }
        if (!scriptEntry.hasObject("id")) {
            throw new InvalidArgumentsException("Missing craftengine block ID/State argument!");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(final ScriptEntry scriptEntry) {
        List<LocationTag> locations = (List<LocationTag>) scriptEntry.getObject("locations");
        ElementTag idElement = scriptEntry.getObjectTag("id");

        if (locations == null || idElement == null) {
            return;
        }

        String rawInputId = idElement.asString();
        String blockId = rawInputId;
        Map<String, String> stateModifications = new HashMap<>();

        // Parse custom inline property changes: fmc:quest_matrix[facing=north]
        if (rawInputId.contains("[") && rawInputId.endsWith("]")) {
            int splitIdx = rawInputId.indexOf('[');
            blockId = rawInputId.substring(0, splitIdx);
            String statesRaw = rawInputId.substring(splitIdx + 1, rawInputId.length() - 1);
            for (String pair : statesRaw.split(",")) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2) {
                    stateModifications.put(parts[0].trim(), parts[1].trim());
                }
            }
        }

        // REMOVED FALLBACK NAMESPACE AUTO-INJECTION: Takes input strictly as typed

        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), locations, idElement);
        }

        try {
            Key craftKey = Key.from(blockId);
            BlockDefinition blockDef = CraftEngineBlocks.byId(craftKey);

            if (blockDef == null) {
                Debug.echoError(scriptEntry, "CraftEngine registry could not find block definition for ID: " + blockId);
                return;
            }

            ImmutableBlockState baseState = null;
            for (Method method : blockDef.getClass().getMethods()) {
                if (method.getName().equals("defaultState") && method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    Object result = method.invoke(blockDef);
                    if (result instanceof ImmutableBlockState) {
                        baseState = (ImmutableBlockState) result;
                        break;
                    }
                }
            }

            if (baseState == null) {
                Debug.echoError(scriptEntry, "Could not extract default block state for ID: " + blockId);
                return;
            }

            // Apply block property updates (like turning directions or toggling booleans)
            if (!stateModifications.isEmpty()) {
                try {
                    Method getPropertyMethod = blockDef.getClass().getMethod("getProperty", String.class);
                    Method withMethod = null;

                    for (Method sm : baseState.getClass().getMethods()) {
                        if (sm.getName().equals("with") && sm.getParameterCount() == 2) {
                            sm.setAccessible(true);
                            withMethod = sm;
                            break;
                        }
                    }

                    if (withMethod != null) {
                        for (Map.Entry<String, String> entry : stateModifications.entrySet()) {
                            Object propToken = getPropertyMethod.invoke(blockDef, entry.getKey());
                            if (propToken != null) {
                                Object valueObj = entry.getValue();
                                Class<?> propClazz = null;

                                for (Method pm : propToken.getClass().getMethods()) {
                                    if (pm.getParameterCount() == 0 && pm.getReturnType() == Class.class) {
                                        pm.setAccessible(true);
                                        propClazz = (Class<?>) pm.invoke(propToken);
                                        break;
                                    }
                                }

                                if (propClazz != null) {
                                    if (propClazz.isEnum()) {
                                        for (Object enumConstant : propClazz.getEnumConstants()) {
                                            if (enumConstant.toString().equalsIgnoreCase(entry.getValue())) {
                                                valueObj = enumConstant;
                                                break;
                                            }
                                        }
                                    } else if (propClazz == Boolean.class || propClazz == boolean.class) {
                                        valueObj = Boolean.valueOf(entry.getValue());
                                    } else if (propClazz == Integer.class || propClazz == int.class) {
                                        valueObj = Integer.valueOf(entry.getValue());
                                    }

                                    baseState = (ImmutableBlockState) withMethod.invoke(baseState, propToken, valueObj);
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    if (scriptEntry.dbCallShouldDebug()) {
                        Debug.echoError("State modification application failed: " + ex.getMessage());
                    }
                }
            }

            // Distribute block placement sequentially to all targeting world coordinates
            for (LocationTag locTag : locations) {
                if (locTag != null && locTag.getWorld() != null) {
                    Location loc = locTag.getBlock().getLocation();
                    CraftEngineBlocks.place(loc, baseState, false);
                }
            }

        } catch (Exception ex) {
            Debug.echoError(ex);
        }
    }
}
