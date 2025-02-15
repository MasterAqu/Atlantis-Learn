package atlantis.information.strategy;

import atlantis.combat.missions.MissionChanger;
import atlantis.combat.missions.Missions;
import atlantis.game.A;
import atlantis.information.enemy.EnemyFlags;
import atlantis.information.enemy.EnemyInfo;
import atlantis.information.decisions.OurStrategicBuildings;
import atlantis.information.enemy.EnemyUnits;
import atlantis.units.AUnit;
import atlantis.units.AUnitType;
import atlantis.units.AbstractFoggedUnit;
import atlantis.units.FakeFoggedUnit;
import atlantis.units.select.Count;
import tests.unit.FakeUnit;

public class EnemyUnitDiscoveredResponse {

    public static void updateEnemyUnitDiscovered(AUnit enemyUnit) {
        if (A.isUms()) {
            return;
        }

        // HIDDEN units and buildings to produce it
        handleHiddenUnitDetected(enemyUnit);

        // COMBAT buildings
        if (enemyUnit.type().isCombatBuildingOrCreepColony()) {
            if (GamePhase.isEarlyGame()) {
                EnemyInfo.startedWithCombatBuilding = true;
            }
//            if (Missions.isFirstMission()) {
//                MissionChanger.forceMissionContain();
////                CurrentBuildOrder.set(TerranStrategies.TERRAN_Mech.buildOrder());
//            }
        }

        if (GamePhase.isEarlyGame()) {
            if (enemyUnit.isZergling()) {
                if (EnemyUnits.count(AUnitType.Zerg_Zergling) >= 6) {
                    MissionChanger.forceMissionSparta("LotsOfZerglings");
                }
            }

            else if (enemyUnit.isZealot()) {
                if (Count.ourCombatUnits() <= 4) {
                    MissionChanger.forceMissionSparta("CautionWithZealot");
                }
            }
        }
    }

    // =========================================================

    private static void handleHiddenUnitDetected(AUnit enemyUnit) {
        if (!enemyUnit.isCloaked()
                && !enemyUnit.isLurker()
                && !enemyUnit.isDT() && !enemyUnit.is(AUnitType.Protoss_Templar_Archives)
        ) {
            return;
        }

        EnemyFlags.HAS_HIDDEN_COMBAT_UNIT = true;

//        if (enemyUnit.effVisible()) {
//            return;
//        }

//        if (enemyUnit.isType(AUnitType.Protoss_Dark_Templar, AUnitType.Zerg_Lurker)) {
        if (enemyUnit.is(AUnitType.Protoss_Dark_Templar)) {
            OurStrategicBuildings.setDetectorsNeeded(2);
//            ARequests.getInstance().requestDetectorQuick(
//                    Chokes.mainChoke().getCenter()
//            );
//            ARequests.getInstance().requestDetectorQuick(
//                    Chokes.natural(Select.mainBase().position()).getCenter()
//            );
        }
    }

}
