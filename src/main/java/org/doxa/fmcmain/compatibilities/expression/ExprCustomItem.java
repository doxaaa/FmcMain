package org.doxa.fmcmain.compatibilities.expression;

import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.TagManager;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
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

            CustomItem<ItemStack> customItem = CraftEngineItems.byId(Key.of(id));

            if (customItem == null) {
                return null;
            }

            ItemTag item = new ItemTag(customItem.buildItemStack(ItemBuildContext.empty()));

            return item.getObjectAttribute(attribute);
        });
    }
}
