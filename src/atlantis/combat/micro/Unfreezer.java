package atlantis.combat.micro;

import atlantis.debug.painter.APainter;
import atlantis.units.AUnit;
import atlantis.units.actions.Actions;
import atlantis.units.select.Select;
import bwapi.Color;

public class Unfreezer {

    /**
     * Some units can get FROZEN (stuck, unable to move/shoot). It's a known Starcraft bug.
     * This is my ugly way of fixing this.
     */
    public static boolean handleUnfreeze(AUnit unit) {
//        if (true) return false;

        if (unit.isRunning() && unit.lastActionFramesAgo() >= (AUnit.UPDATE_UNIT_POSITION_EVERY_FRAMES + 20)) {
            if (unit._lastX == unit.x() && unit._lastY == unit.y()) {
//                System.err.println("UNFREEZE #1!");
//                unit.setTooltip("UNFREEZE!");
                return unfreeze(unit);
            }
        }

//        if (
//                unit.lastUnderAttackLessThanAgo(5)
//                        && unit.getLastOrderFramesAgo() >= AUnit.UPDATE_UNIT_POSITION_EVERY_FRAMES
//                        && unit.cooldownRemaining() == 0
//        ) {
//            if (unit._lastX == unit.x() && unit._lastY == unit.y()) {
//                System.err.println("UNFREEZE #2!");
//                unit.setTooltip("UNFREEZE!");
//                return unfreeze(unit);
//            }
//        }

        return false;
    }

    // =========================================================

    public static boolean unfreeze(AUnit unit) {
        unit.runningManager().stopRunning();

//        CameraManager.centerCameraOn(unit);

        boolean paintingDisabled = APainter.isDisabled();
        if (paintingDisabled) {
            APainter.enablePainting();
        }
        APainter.paintCircleFilled(unit, 10, Color.Cyan);
        if (paintingDisabled) {
            APainter.disablePainting();
        }
//        GameSpeed.changeSpeedTo(70);
//        GameSpeed.pauseGame();

        if (Select.main() != null && unit.moveTactical(Select.main(), Actions.MOVE_UNFREEZE, "Unfreeze")) {
            return true;
        }

        if (unit.moveTactical(unit.translateByPixels(8, 0), Actions.MOVE_UNFREEZE, "Unfreeze")) {
            return true;
        }
        if (unit.moveTactical(unit.translateByPixels(-8, 0), Actions.MOVE_UNFREEZE, "Unfreeze")) {
            return true;
        }
        if (unit.moveTactical(unit.translateByPixels(0, 8), Actions.MOVE_UNFREEZE, "Unfreeze")) {
            return true;
        }
        if (unit.moveTactical(unit.translateByPixels(0, -8), Actions.MOVE_UNFREEZE, "Unfreeze")) {
            return true;
        }
//        } else {
//            unit.holdPosition("Unfreeze");
//            unit.stop("Unfreeze");
//            unit.holdPosition("Unfreeze");
//            unit.stop("Unfreeze");
//            unit.stop("Unfreeze");
//            unit.holdPosition("Unfreeze");
//        }

        return false;
    }

}
