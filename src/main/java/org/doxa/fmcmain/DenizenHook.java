package org.doxa.fmcmain;

import org.doxa.fmcmain.compatibilities.condition.CondIsCraftEngineHasBeenLoad;
import org.doxa.fmcmain.compatibilities.condition.CondIsCustomItem;
import org.doxa.fmcmain.compatibilities.expression.ExprCustomItem;
import org.doxa.fmcmain.compatibilities.expression.ExprItemCustomItemID;

public class DenizenHook {

    public static void register() {
        // Register Global Tags (e.g., <ce_item[id]> and <ce_is_loaded>)
        ExprCustomItem.register();
        CondIsCraftEngineHasBeenLoad.register();

        // Register Object Properties (e.g., <item.is_ce_item> and <item.ce_id>)
        CondIsCustomItem.register();
        ExprItemCustomItemID.register();
    }
}