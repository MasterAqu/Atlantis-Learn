package atlantis.combat.missions.contain;

import atlantis.combat.missions.MissionChanger;
import atlantis.combat.missions.Missions;
import atlantis.combat.retreating.RetreatManager;
import atlantis.game.A;
import atlantis.information.enemy.EnemyInformation;
import atlantis.information.generic.ArmyStrength;
import atlantis.units.select.Count;

public class ProtossMissionChangerWhenContain extends MissionChangerWhenContain {

    public static void changeMissionIfNeeded() {
        if (shouldChangeMissionToDefend()) {
            MissionChanger.changeMissionTo(Missions.DEFEND);
        } else if (shouldChangeMissionToAttack()) {
            MissionChanger.changeMissionTo(Missions.ATTACK);
        }
    }

    // === DEFEND ==============================================

    private static boolean shouldChangeMissionToDefend() {
        if (EnemyInformation.isEnemyNearAnyOurBuilding() && A.supplyUsed() <= 60) {
            return true;
        }

        if (ArmyStrength.weAreWeaker() && RetreatManager.GLOBAL_RETREAT_COUNTER >= 2 && A.resourcesBalance() <= 300) {
            return true;
        }

        if (A.resourcesBalance() <= -400) {
            return true;
        }

        return false;

//        if (Missions.isFirstMission()) {
//            return false;
//        }
//
//        int ourCombatUnits = Count.ourCombatUnits();
//
//        if (ourCombatUnits <= 10 && EnemyInformation.hasDefensiveLandBuilding(true)) {
//            return true;
//        }
//
//        return false;
//
//        return ourCombatUnits <= 6;
    }

    // === ATTACK ==============================================

    private static boolean shouldChangeMissionToAttack() {
//        if (AGame.killsLossesResourceBalance() <= 100) {
//            return false;
//        }

        if (ArmyStrength.weAreMuchStronger() && !EnemyInformation.hasDefensiveLandBuilding(true)) {
            return true;
        }

        if (A.resourcesBalance() >= 410 && ArmyStrength.weAreStronger()) {
            return true;
        }

        if (A.supplyUsed() >= 190 || Count.ourCombatUnits() >= 35) {
            return true;
        }

        return false;
//        if (AGame.timeSeconds() <= 400 && AGame.killsLossesResourceBalance() >= 900) {
//            return true;
//        }
//
//        return AGame.timeSeconds() <= 700 && AGame.killsLossesResourceBalance() >= 1600;
    }

}
