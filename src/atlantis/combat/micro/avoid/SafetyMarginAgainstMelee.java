package atlantis.combat.micro.avoid;

import atlantis.units.AUnit;
import atlantis.units.AUnitType;
import atlantis.units.select.Select;

public class SafetyMarginAgainstMelee extends SafetyMargin {

//    public static double ENEMIES_NEARBY_FACTOR = 5.9;
    public static double ENEMIES_NEARBY_FACTOR = 0.3;
    public static double ENEMIES_NEARBY_MAX_DIST = 1.44;
    public static double INFANTRY_BASE_IF_MEDIC = 1.60;
    public static int INFANTRY_WOUND_IF_MEDIC = 19;
    public static double INFANTRY_BASE_IF_NO_MEDIC = 2.49;
    public static int INFANTRY_WOUND_IF_NO_MEDIC = 60;
//    public static double INFANTRY_BASE_IF_MEDIC = 0.64;
//    public static int INFANTRY_WOUND_IF_MEDIC = 20;
//    public static double INFANTRY_BASE_IF_NO_MEDIC = 2.02;
//    public static int INFANTRY_WOUND_IF_NO_MEDIC = 85;

    public static double calculate(AUnit defender, AUnit attacker) {
        double criticalDist;

        // Terran INFANTRY
        if (defender.isTerranInfantry()) {
            if (defender.hasMedicInRange()) {
                criticalDist = INFANTRY_BASE_IF_MEDIC
                        + woundedAgainstMeleeBonus(defender, attacker);
            } else {
                criticalDist = INFANTRY_BASE_IF_NO_MEDIC
                        + woundedAgainstMeleeBonus(defender, attacker)
                        + ourMovementBonus(defender) / 3
                        + enemyMovementBonus(defender, attacker) / 3;

                criticalDist += enemyUnitsNearbyBonus(defender) * ENEMIES_NEARBY_FACTOR;
            }
        }

        // VULTURE
        else if (defender.isVulture()) {
            criticalDist = 3.6;
        }

        // Standard unit
        else {
            criticalDist = baseForMelee(defender, attacker)
                    + enemyWeaponRange(defender, attacker)
                    + woundedAgainstMeleeBonus(defender, attacker)
                    + beastBonus(defender)
                    + ourUnitsNearbyBonus(defender)
                    + workerBonus(defender, attacker)
                    + ourMovementBonus(defender)
                    + quicknessBonus(defender, attacker)
                    + enemyMovementBonus(defender, attacker);
        }

        // 3.9 tiles (almost base width) should be enough as a minimum versus melee unit
        criticalDist = Math.min(criticalDist, 3.9);

        if (defender.isRanged() && attacker.isWorker()) {
            criticalDist = 2.3;
        }

        return attacker.distTo(defender) - criticalDist;
    }

    // =========================================================

    private static double baseForMelee(AUnit defender, AUnit attacker) {
        return attacker.isZealot() ? 0.5 : 0.7;
    }

    private static double enemyUnitsNearbyBonus(AUnit defender) {
        return Select.enemyCombatUnits().inRadius(ENEMIES_NEARBY_MAX_DIST, defender).count();
    }

    protected static double beastBonus(AUnit defender) {
        int beastNearby = Select.enemy()
                .ofType(
                        AUnitType.Protoss_Archon,
                        AUnitType.Protoss_Dark_Templar,
                        AUnitType.Zerg_Ultralisk
                )
                .inRadius(5, defender)
                .count();

        return beastNearby > 0 ? 1.6 : 0;
    }

    protected static double woundedAgainstMeleeBonus(AUnit defender, AUnit attacker) {
        if (defender.isTerranInfantry()) {
            if (defender.hasMedicInRange()) {
                return defender.woundPercent() / INFANTRY_WOUND_IF_MEDIC;
            } else {
                return defender.woundPercent() / INFANTRY_WOUND_IF_NO_MEDIC;
            }
        }

        if (defender.isAir()) {
            return defender.woundPercent() / 10;
        }

        boolean applyExtraModifier = defender.isTank();
        return (defender.woundPercent() * (applyExtraModifier ? 2 : 1)) / 32.0;
    }

}
