package org.doxa.fmcmain.compatibilities.condition;

import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;

public class CondIsCustomItem {

    public static void register() {
        // Syntax: <ItemTag.is_ce_item>
        ItemTag.tagProcessor.registerStaticTag(ElementTag.class, "is_ce_item", (attribute, object) -> {
            return new ElementTag(CraftEngineItems.isCustomItem(object.getItemStack()));
        });
    }
}
