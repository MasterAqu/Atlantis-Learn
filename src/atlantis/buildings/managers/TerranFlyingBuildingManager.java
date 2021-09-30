package atlantis.buildings.managers;

import atlantis.AGame;
import atlantis.combat.missions.Missions;
import atlantis.position.APosition;
import atlantis.units.AUnit;
import atlantis.units.AUnitType;
import atlantis.units.Select;
import atlantis.units.actions.UnitActions;
import java.util.ArrayList;

/**
 *
 * @author Rafal Poniatowski <ravaelles@gmail.com>
 */
public class TerranFlyingBuildingManager {

    private static ArrayList<AUnit> flyingBuildings = new ArrayList<>();
    
    // =========================================================
    
    public static void update() {
        if (AGame.isPlayingAsTerran() && !AGame.isUmtMode()) {
            if (shouldHaveAFlyingBuilding()) {
                liftABuildingAndFlyAmongStars();
            }

            updateIfBuildingNeedsToBeLifted();
            
            for (AUnit flyingBuilding : flyingBuildings) {
                updateFlyingBuilding(flyingBuilding);
            }
        }
    }

    // =========================================================

    private static void updateIfBuildingNeedsToBeLifted() {
        for (AUnit building : Select.ourBuildings().listUnits()) {
            if (building.isUnderAttack() && !building.isLifted() && building.getHPPercent() < 24) {
                building.lift();
            }
        }
    }

    private static boolean updateFlyingBuilding(AUnit flyingBuilding) {
        
        // Define focus point for current mission
        APosition focusPoint = Missions.globalMission().focusPoint();
        
        // Move towards focus point if needed
        if (focusPoint != null) {
            double distToFocusPoint = focusPoint.distanceTo(flyingBuilding);
            
            if (distToFocusPoint > 2) {
                flyingBuilding.move(focusPoint, UnitActions.MOVE);
            }
            
            if (distToFocusPoint > 5) {
                return true;
            }
        }
        
        // Fly away from nearest tank if it's too close
        AUnit tankTooNear = Select.ourTanks().nearestToOrNull(flyingBuilding, 4.2);
        if (tankTooNear != null) {
            return flyingBuilding.moveAwayFrom(tankTooNear.getPosition(), 0.6);
        }
        
        return false;
    }
    
    // =========================================================

    private static boolean shouldHaveAFlyingBuilding() {
        if (!flyingBuildings.isEmpty()) {
            return false;
        }
        
        if (Select.ourTanks().count() < 1) {
            return false;
        }
        
        return true;
    }

    private static void liftABuildingAndFlyAmongStars() {
        AUnit flying = Select.ourOfType(AUnitType.Terran_Barracks, AUnitType.Terran_Engineering_Bay).idle().first();
        if (flying != null) {
            flying.lift();
            flyingBuildings.add(flying);
        }
    }
    
    // =========================================================

    public static boolean isFlyingBuilding(AUnit unit) {
        return unit.getType().isBuilding() && flyingBuildings.contains(unit);
    }
    
}
