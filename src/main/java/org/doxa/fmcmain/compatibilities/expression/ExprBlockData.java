package org.doxa.fmcmain.compatibilities.expression;

import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import java.lang.reflect.Method;

public class ExprBlockData {

    public static void register() {
        // Defer by 1 tick to ensure Denizen has fully prepared all tag processor maps
        Bukkit.getScheduler().runTaskLater(org.doxa.fmcmain.FmcMain.instance, () -> {

            // <--[tag]
            // @Attribute <LocationTag.ce_block>
            // @Returns ElementTag
            // @Description
            // Returns the full CraftEngine block state string (e.g., lock_box[facing=west,waterlogged=true]).
            // Can be chained with .get[<key>] to extract a specific state value.
            // -->
            LocationTag.tagProcessor.registerTag(ElementTag.class, "ce_block", (attribute, object) -> {
                if (object == null) {
                    return null;
                }

                // Safely read the raw state metadata string representation
                String blockString = fetchCraftEngineBlockString(object.getBlock());
                if (blockString == null || blockString.isEmpty()) {
                    return null;
                }

                // Intercept the chained ".get[<key>]" parameter check dynamically
                if (attribute.startsWith("get") && attribute.hasParam()) {
                    String key = attribute.getParam();
                    attribute.fulfill(1); // Fulfill the "get" step in Denizen's execution queue

                    String val = extractValueFromStateString(blockString, key);
                    return val != null ? new ElementTag(val) : null;
                }

                // Return the full block name and state string representation if no sub-tag was chained
                return new ElementTag(blockString);
            });

        }, 1L);
    }

    /**
     * Extracts the property value matching the key from formats like "lock_box[facing=west,waterlogged=true]"
     */
    private static String extractValueFromStateString(String blockStr, String key) {
        String targetKey = key + "=";
        if (!blockStr.contains(targetKey)) {
            return null;
        }

        int startIndex = blockStr.indexOf(targetKey) + targetKey.length();
        int endIndex = blockStr.length();

        for (int i = startIndex; i < blockStr.length(); i++) {
            char c = blockStr.charAt(i);
            if (c == ',' || c == ']' || c == '}') {
                endIndex = i;
                break;
            }
        }

        return blockStr.substring(startIndex, endIndex).trim().toLowerCase();
    }

    /**
     * Safely reads the active block state from CraftEngine without executing any destructive commands.
     */
    private static String fetchCraftEngineBlockString(Block block) {
        try {
            // Target the official CraftEngineBlocks API class safely via reflection
            Class<?> apiClass = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineBlocks");

            // Loop specifically for the standard getter method (usually getCustomBlock or getBlockState)
            for (Method method : apiClass.getMethods()) {
                String name = method.getName().toLowerCase();
                // Ensure we only execute safe tracking/getter methods that accept a Block argument
                if ((name.contains("get") || name.contains("state")) && method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(Block.class)) {
                    method.setAccessible(true);
                    Object blockState = method.invoke(null, block);
                    if (blockState != null) {
                        String result = blockState.toString();
                        // Verify that it actually contains block data or standard state signatures
                        if (!result.equalsIgnoreCase("true") && !result.equalsIgnoreCase("false")) {
                            return result;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
