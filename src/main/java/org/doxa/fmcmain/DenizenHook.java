package org.doxa.fmcmain;

import com.denizenscript.denizencore.DenizenCore;
import org.doxa.fmcmain.compatibilities.expression.ExprPlayerListPlots;

public class DenizenHook {

    public static void register() {
        ExprPlayerListPlots.register(); // Registered in exactly 1 single line
    }
}