package atlantis.combat.micro.managers;

import atlantis.combat.missions.Mission;
import atlantis.combat.missions.MissionUnitManager;
import atlantis.position.APosition;
import atlantis.units.AUnit;
import atlantis.units.actions.UnitActions;

public class AdvanceUnitsManager extends MissionUnitManager {

    private Mission mission;

    public boolean updateUnit(AUnit unit) {
        unit.setTooltip("#Advance");

        if (unit.distanceTo(mission.focusPoint()) > 6) {
            unit.move(mission.focusPoint(), UnitActions.MOVE_TO_ENGAGE, "#MA:Forward!");
            return true;
        }

        return false;
    }

    public static boolean moveToFocusPoint(AUnit unit, APosition focusPoint) {
//        Select<AUnit> nearbyAllies = Select.ourCombatUnits().inRadius(10, unit);
//        if (nearbyAllies.count() <= 4) {
//            unit.move(nearbyAllies.first().getPosition(), UnitActions.TOGETHER);
//            unit.setTooltip("#MA:Concentrate!");
//            return true;
//        }

        double distToFocusPoint = unit.distanceTo(focusPoint);

        if (distToFocusPoint >= 7) {
            unit.move(focusPoint, UnitActions.MOVE_TO_ENGAGE, "#Advance:Advance");
            return true;
        }
        else if (distToFocusPoint <= 4 && unit.moveAwayFrom(focusPoint, 2.5, "Together")) {
            unit.setTooltip("#Advance:Too close!");
            return true;
        }
        else if (distToFocusPoint <= 5) {
            if (unit.getOrderTarget() != null) {
                unit.holdPosition("Good");
            }
            return true;
        }

        return false;
    }

}