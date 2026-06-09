package org.doxa.fmcmain.compatibilities.expression;

import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.TagManager;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;

public class ExprCustomItem {

    public static void register() {
        // <--[tag]
        // @attribute <ce_item[<id>]>
        // @returns ItemTag
        // @description
        // Returns a CraftEngine item by its namespace ID.
        // -->
        TagManager.registerTagHandler(ObjectTag.class, "ce_item", (attribute) -> {
            if (!attribute.hasContext(1)) {
                return null;
            }

            String id = attribute.getContext(1);
            attribute.fulfill(1);

            BukkitItemDefinition customItem = CraftEngineItems.byId(Key.of(id));

            if (customItem == null) {
                return null;
            }

            ItemStack itemStack = customItem.buildBukkitItem(ItemBuildContext.empty());

            // 1. Instantiate the basic empty layout shell matching your item material type
            ItemTag item = new ItemTag(itemStack.getType());

            // 2. Map the untouched platform item directly to prevent JSON string modifications
            item.setItemStack(itemStack);

            // FIXED LINE: Directly pass the ItemTag itself back through the attribute tree.
            // Denizen's TagManager natively processes ItemTag as a valid ObjectTag.
            return item.getObjectAttribute(attribute);
        });
    }
}
