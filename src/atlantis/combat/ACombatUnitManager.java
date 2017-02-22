package atlantis.combat;

import atlantis.AGame;
import atlantis.combat.micro.AAttackEnemyUnit;
import atlantis.combat.micro.AAvoidMeleeUnitsManager;
import atlantis.combat.micro.AbstractMicroManager;
import atlantis.combat.micro.terran.TerranMedic;
import atlantis.combat.micro.terran.TerranSiegeTankManager;
import atlantis.combat.micro.zerg.ZergOverlordManager;
import atlantis.combat.squad.Squad;
import atlantis.units.AUnit;
import atlantis.units.AUnitType;

/**
 *
 * @author Rafal Poniatowski <ravaelles@gmail.com>
 */
public class ACombatUnitManager extends AbstractMicroManager {

    protected static boolean update(AUnit unit) {
        
        // =========================================================
        // Don't INTERRUPT shooting units
        
        if (shouldNotDisturbUnit(unit)) {
            unit.setTooltip("#DontDisturb");
            return true;
        }

        // =========================================================
        // Handle some units in special way
        
        if (handledAsSpecialUnit(unit)) {
            unit.setTooltip(unit.getShortName());
            return true;
        }
        
        // =========================================================
        // Handle some units in semi-special way
        
        if (handledAsSemiSpecialUnit(unit)) {
            unit.setTooltip("Siege Tank");
            return true;
        }

        // =========================================================
        // Avoid melee units
        if (AAvoidMeleeUnitsManager.handleAvoidCloseMeleeUnits(unit)) {
            return true;
        }
        
        // =========================================================
        // If we couldn't beat nearby enemies, retreat
        if (handleUnfavorableOdds(unit)) {
            return true;
        }
        
        // =========================================================
        // Attack enemy units when in range (and choose the best target)
        if (AAttackEnemyUnit.handleAttackEnemyUnits(unit)) {
            return true;
        }

        // =========================================================
        // =========================================================
        // === If we're here, it means mission manager is allowed ==
        // === to take control over this unit, due to no action   ==
        // === needed on tactics level (proceed to strategy).     ==
        // =========================================================
        // =========================================================

        Squad squad = unit.getSquad();
        
        if (squad == null) {
            System.err.println("Unit " + unit + " has no squad assigned.");
            unit.setTooltip("Empty squad!");
            return false;
        }
        else {
            unit.setTooltip("Mission:" + squad.getMission().getName());
            return squad.getMission().update(unit);
        }
    }

    // =========================================================
    /**
     *
     */
    private static boolean shouldNotDisturbUnit(AUnit unit) {
        return (unit.isAttackFrame() || (!unit.type().isTank() && unit.isStartingAttack())) &&
                unit.getGroundWeaponCooldown() <= 0 && unit.getAirWeaponCooldown() <= 0;
//        return false;
//        return (unit.isAttackFrame() || unit.isStartingAttack()) &&
//                unit.getGroundWeaponCooldown() <= 0 && unit.getAirWeaponCooldown() <= 0;
//        return unit.isAttackFrame() || (!unit.type().isTank() && unit.isStartingAttack());
//        return (unit.isAttackFrame());
    }

    /**
     * There are some units that should have individual micro managers like Zerg Overlord. If unit is special
     * unit it will run proper micro managers here and return true, meaning no other managers should be used.
     * False will give command to standard Melee of Micro managers.
     * 
     * 
     */
    private static boolean handledAsSpecialUnit(AUnit unit) {
        
        // === Terran ========================================
        if (AGame.playsAsTerran()) {
            
            // MEDIC
            if (unit.isType(AUnitType.Terran_Medic)) {
                unit.setTooltip("Medic");
                return TerranMedic.update(unit);
            }
        }
        
        // === Zerg ========================================
        
        else if (AGame.playsAsZerg()) {
            
            // OVERLORD
            if (unit.getType().equals(AUnitType.Zerg_Overlord)) {
                ZergOverlordManager.update(unit);
                return true;
            } 
        } 
        
        // =========================================================
        
        return false;
    }

    /**
     * There are some units that should have additional micro manager actions like Siege Tank. If unit is 
     * semi-special it will run its micro managers after other managers have been executed.
     */
    private static boolean handledAsSemiSpecialUnit(AUnit unit) {
        if (unit.getType().isSiegeTank()) {
            boolean dontDoAnythingElse = TerranSiegeTankManager.update(unit);
            return dontDoAnythingElse;
        } else {
            return false;
        }
    }

}
