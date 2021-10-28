package atlantis.interrupt;

import atlantis.debug.APainter;
import atlantis.units.AUnit;
import atlantis.units.actions.UnitActions;
import atlantis.util.A;
import bwapi.Color;

public class DontDisturbInterrupt {

    public static boolean dontInterruptImportantActions(AUnit unit) {

        // Don't INTERRUPT shooting units
        if (DontInterruptStartedAttacks.shouldNotInterrupt(unit)) {
//            System.out.println(A.now() + " SHOOT");
            APainter.paintRectangle(unit.getPosition().translateByPixels(-5, 0), 10, 3, Color.Red);
            return true;
        }

        // Allow unit to load to shuttle
        if (allowUnitToLoadToTransport(unit)) {
            APainter.paintRectangle(unit.getPosition().translateByPixels(-5, 0), 10, 3, Color.Blue);
//            System.out.println(A.now() + " TRANSP");
            return true;
        }

        if (allowUnitToContinueRareRightClickActions(unit)) {
            return true;
        }

        return false;
    }

    // =========================================================

    private static boolean allowUnitToLoadToTransport(AUnit unit) {
        return !unit.type().isTransport() && unit.lastActionLessThanAgo(13, UnitActions.LOAD);
    }

    private static boolean allowUnitToContinueRareRightClickActions(AUnit unit) {
        return unit.lastActionLessThanAgo(6, UnitActions.RIGHT_CLICK);
    }

}
