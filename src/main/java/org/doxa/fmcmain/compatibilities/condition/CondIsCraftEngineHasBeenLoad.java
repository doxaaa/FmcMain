package org.doxa.fmcmain.compatibilities.condition;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.TagManager;
import org.doxa.fmcmain.compatibilities.event.EvtCraftEngineReload;

public class CondIsCraftEngineHasBeenLoad {

    public static void register() {
        // <--[tag]
        // @attribute <ce_is_loaded>
        // @returns ElementTag
        // @description
        // Returns true if CraftEngine has finished loading its resources.
        // -->
        TagManager.registerTagHandler(ObjectTag.class, "ce_is_loaded", (attribute) -> {
            return new ElementTag(EvtCraftEngineReload.hasBeenLoad());
        });
    }
}
