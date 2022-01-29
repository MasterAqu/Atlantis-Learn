package atlantis.protoss;

import atlantis.game.A;
import atlantis.units.AUnit;
import atlantis.units.select.Select;

public class ProtossShieldBattery {

    public static boolean update(AUnit shieldBattery) {
        if (shieldBattery.energy() >= 30) {
            shieldBattery.removeTooltip();
            for (AUnit unit : Select.ourRealUnits().inRadius(11, shieldBattery).list()) {
                if (unit.shields() + 12 < unit.maxShields() ) {
                    if (Select.enemyRealUnits().combatUnits().inRadius(7, unit).isNotEmpty()) {
                        return false;
                    }

                    if (shieldBattery.groundDist(unit) > 15) {
                        return false;
                    }
                    if (!shieldBattery.equals(unit.target()) || A.chance(2)) {
                        unit.doRightClickAndYesIKnowIShouldAvoidUsingIt(shieldBattery);
                    }
                    shieldBattery.setTooltipTactical("RECHARGE " + unit.name());
                    return true;
                }
            }
        }

        return false;
    }

}
