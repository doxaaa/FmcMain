package org.doxa.fmcmain;

import com.denizenscript.denizencore.DenizenCore;
import org.doxa.fmcmain.compatibilities.command.CmdCeModifyBlock;
import org.doxa.fmcmain.compatibilities.condition.CondIsCraftEngineHasBeenLoad;
import org.doxa.fmcmain.compatibilities.condition.CondIsCustomItem;
import org.doxa.fmcmain.compatibilities.expression.ExprCustomItem;
import org.doxa.fmcmain.compatibilities.expression.ExprItemCustomItemID;
import org.doxa.fmcmain.compatibilities.expression.ExprPlayerListPlots;

public class DenizenHook {

    public static void register() {
        // Register Custom Command Pipeline
        DenizenCore.commandRegistry.registerCommand(CmdCeModifyBlock.class);

        // Register Global Tag Context Parsers
        ExprCustomItem.register();
        CondIsCraftEngineHasBeenLoad.register();
        ExprPlayerListPlots.register(); // Registered in exactly 1 single line

        // Register Object Configuration Properties
        CondIsCustomItem.register();
        ExprItemCustomItemID.register();
    }
}
