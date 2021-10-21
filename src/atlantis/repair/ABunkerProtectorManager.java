package atlantis.repair;

import atlantis.units.AUnit;
import atlantis.units.actions.UnitActions;

public class ABunkerProtectorManager {

    public static boolean updateProtector(AUnit protector) {
        AUnit unit = ARepairAssignments.getUnitToProtectFor(protector);
        if (unit != null && unit.isAlive()) {

            // Bunker WOUNDED
            if (unit.hpPercent() < 100) {
                protector.repair(unit, "Protect " + unit.shortName());
                return true;
            }

            // Bunker fully HEALTHY
            else {
                double distanceToUnit = unit.distTo(protector);
                if (distanceToUnit > 1 && !protector.isMoving()) {
                    protector.move(unit.getPosition(), UnitActions.MOVE_TO_REPAIR, "Go to " + unit.shortName());
                    return true;
                }
                else {
                    protector.setTooltip("Protect " + unit.shortName());
                }
            }
        }
        else {
            protector.setTooltip("Null bunker");
            ARepairAssignments.removeRepairerOrProtector(protector);
            return true;
        }

        return ARepairerManager.handleIdleRepairer(protector);
    }
}
