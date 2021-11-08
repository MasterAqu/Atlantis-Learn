package atlantis.combat.micro.avoid;

import atlantis.combat.eval.ACombatEvaluator;
import atlantis.units.AUnit;
import atlantis.units.AUnitType;

public class SafetyMarginAgainstRanged extends SafetyMargin {

    public static double calculate(AUnit attacker, AUnit defender) {
        double criticalDist = enemyWeaponRangeBonus(defender, attacker)
                + quicknessBonus(defender, attacker)
                + lurkerBonus(defender, attacker)
                + woundedBonus(defender)
                + transportBonus(defender)
                + ourUnitsNearbyBonus(defender)
                + ourMovementBonus(defender)
                + enemyMovementBonus(defender, attacker)
                + scoutBonus(defender, attacker)
                + combatEvalBonus(defender, attacker);

        return attacker.distTo(defender) - criticalDist;
    }

    // =========================================================

    private static double lurkerBonus(AUnit defender, AUnit attacker) {
        if (attacker.is(AUnitType.Zerg_Lurker) && attacker.effCloaked()) {
            return 3.6;
        }

        return 0;
    }

    private static double scoutBonus(AUnit defender, AUnit attacker) {
        return defender.isScout() ? (5 + defender.woundPercent() / 33) : 0;
    }

    private static double combatEvalBonus(AUnit defender, AUnit attacker) {
        if (!ACombatEvaluator.isSituationFavorable(defender)) {
            return -3;
        }

        return 0;
    }

}
