package atlantis.combat.micro;

import atlantis.AtlantisGame;
import atlantis.combat.micro.terran.TerranMedic;
import atlantis.combat.micro.zerg.ZergOverlordManager;
import atlantis.debug.AtlantisPainter;
import atlantis.units.AUnit;
import atlantis.units.AUnitType;
import atlantis.units.Select;
import atlantis.units.actions.UnitActions;
import bwapi.Color;

/**
 * Default micro manager that will be used for all melee units.
 */
public class MicroManager extends AbstractMicroManager {

    @Override
    public boolean update(AUnit unit) {
//        AUnit mainBase = Select.mainBase();
//
//        System.out.println(unit.getShortName() + " range: " + unit.getWeaponRangeAgainst(mainBase));
//        boolean canAttack = unit.canAttackThisKindOfUnit(mainBase, false);
//        boolean hasRange = unit.hasRangeToAttack(mainBase, 0.1);
//        String dist = String.format("%.2f", unit.distanceTo(mainBase));
//        if (canAttack && hasRange) {
//            AtlantisPainter.paintCircle(unit, unit.getWeaponRangeAgainst(mainBase) * 32, Color.Green);
//        }
//        else {
//            AtlantisPainter.paintCircle(unit, unit.getWeaponRangeAgainst(mainBase) * 32, Color.Red);
//            AtlantisPainter.paintTextCentered(unit, "Dist: " + dist, Color.Red);
//        }
        
        if (canIssueOrderToUnit(unit)) {
            unit.setTooltip("Last: " + unit.getLastUnitActionWasFramesAgo());

            // SPECIAL UNIT TYPE action
            if (handleSpecialUnit(unit)) {
                return true;
            }

            // =========================================================
            // Avoid dying because of very low hit points
            // @FIX: Breaks handleUnfavorableOdds(), units tend to avoid fighting too much
            
//            if (handleLowHealthIfNeeded(unit)) {
//                return true;
//            }
            
            // =========================================================
            // Avoid melee units
            if (AtlantisAvoidMeleeUnitsManager.handleAvoidCloseMeleeUnits(unit)) {
                return true;
            }

            // =========================================================
            // Don't interrupt running
            
//            if (unit.isRunning()) {
//                unit.move(unit.getRunManager().getRunToPosition(), UnitActions.RUN_FROM_UNIT);
//                unit.setTooltip("Running");
//                return true;
//            }

            // =========================================================
            // Check chances to win the fight
            if (handleUnfavorableOdds(unit)) {
                return true;
            }

            // =========================================================
            // Don't spread too much
//            if (handleDontSpreadTooMuch(unit)) {
//                return true;
//            }

            // =========================================================
            // Attack enemy is possible
            if (AtlantisAttackEnemyUnit.handleAttackEnemyUnits(unit)) {
                return true;
            }

            // =========================================================
            // False (strategy): Did not use micro-manager, allow more generic mission behavior.
            // True (tactics): Micro-manager issued command, do not allow mission manager to take over control.
//            boolean canPropagateCommandToMissionManager = unit.getGroundWeaponCooldown() <= 0;
            boolean forbidMissionManagerControlOverThisUnit = !unit.isReadyToShoot();
            unit.setTooltip("-> mission:" + forbidMissionManagerControlOverThisUnit);
            return forbidMissionManagerControlOverThisUnit;
        } // =========================================================
        // Can't give orders to unit right now
        else {
            unit.setTooltip("ago: " + unit.getLastUnitActionWasFramesAgo());
            return true;
        }
    }

    // =========================================================
    /**
     * @return <b>true</b> if unit can be given order<br />
     * <b>false</b> if unit is in the shooting frame or does any other thing that mustn't be interrupted
     */
    private boolean canIssueOrderToUnit(AUnit unit) {
//        return true;
        if (unit.isRunning()) {
            return true;
        }
//        return !(unit.isAttackFrame() || unit.isStartingAttack()) || 
//                unit.getGroundWeaponCooldown() > 0 || unit.getAirWeaponCooldown() > 0;
        return !(unit.isAttackFrame()) || 
                unit.getGroundWeaponCooldown() > 0 || unit.getAirWeaponCooldown() > 0;
    }

    /**
     * There are special units like Terran Marines, Zerg Overlords that should be following different behavior
     * than standard combat units.
     */
    private boolean handleSpecialUnit(AUnit unit) {

        // ZERG
//        if (AtlantisGame.playsAsZerg()) {
//            if (unit.isType(AUnitType.Zerg_Overlord)) {
//                ZergOverlordManager.update(unit);
//                return true;
//            }
//        }

        // TERRAN
        if (AtlantisGame.playsAsTerran()) {
            if (unit.isType(AUnitType.Terran_Medic)) {
                unit.setTooltip("Medic");
                return TerranMedic.update(unit);
            }
        }

        return false;
    }

}
