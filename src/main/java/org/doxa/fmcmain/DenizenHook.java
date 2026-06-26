package org.doxa.fmcmain;

import com.denizenscript.denizencore.DenizenCore;
import org.doxa.fmcmain.compatibilities.command.CmdCeModifyBlock;
import org.doxa.fmcmain.compatibilities.condition.CondIsCraftEngineHasBeenLoad;
import org.doxa.fmcmain.compatibilities.condition.CondIsCustomItem;
import org.doxa.fmcmain.compatibilities.expression.ExprCustomItem;
import org.doxa.fmcmain.compatibilities.expression.ExprItemCustomItemID;

public class DenizenHook {

    public static void register() {
        // Register Custom Command Pipeline
        DenizenCore.commandRegistry.registerCommand(CmdCeModifyBlock.class);

        // Register Global Tag Context Parsers
        ExprCustomItem.register();
        CondIsCraftEngineHasBeenLoad.register();

        // Register Object Configuration Properties
        CondIsCustomItem.register();
        ExprItemCustomItemID.register();
    }
}
