package org.doxa.fmcmain.compatibilities.expression;

import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;

public class ExprItemCustomItemID {

    public static void register() {
        // <--[tag]
        // @attribute <ItemTag.ce_id>
        // @returns ElementTag
        // @description
        // Returns the CraftEngine ID of a custom item.
        // -->
        ItemTag.tagProcessor.registerStaticTag(ElementTag.class, "ce_id", (attribute, object) -> {
            net.momirealms.craftengine.core.util.Key key = CraftEngineItems.getCustomItemId(object.getItemStack());
            return key != null ? new ElementTag(key.asString()) : null;
        });
    }
}
