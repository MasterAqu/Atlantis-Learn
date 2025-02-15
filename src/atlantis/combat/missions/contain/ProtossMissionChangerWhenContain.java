package atlantis.combat.missions.contain;

import atlantis.combat.missions.MissionChanger;
import atlantis.combat.missions.Missions;
import atlantis.combat.retreating.RetreatManager;
import atlantis.game.A;
import atlantis.information.enemy.EnemyInfo;
import atlantis.information.generic.ArmyStrength;
import atlantis.information.strategy.GamePhase;

public class ProtossMissionChangerWhenContain extends MissionChangerWhenContain {

    public static void changeMissionIfNeeded() {
        if (shouldChangeMissionToDefend()) {
            MissionChanger.changeMissionTo(MissionChanger.defendOrSpartaMission());
        } else if (shouldChangeMissionToAttack()) {
            MissionChanger.changeMissionTo(Missions.ATTACK);
        }
    }

    // === DEFEND ==============================================

    public static boolean shouldChangeMissionToDefend() {
        if (ArmyStrength.weAreWeaker()) {
            if (RetreatManager.GLOBAL_RETREAT_COUNTER >= 2 && A.resourcesBalance() <= 300) {
                if (DEBUG) reason = "We are weaker (" + ArmyStrength.ourArmyRelativeStrength() + "%)";
                return true;
            }

            if (GamePhase.isEarlyGame()) {
                if (DEBUG) reason = "Eh, we are weaker (" + ArmyStrength.ourArmyRelativeStrength() + "%)";
                return true;
            }
        }

        if (defendAgainstMassZerglings()) {
            if (DEBUG) reason = "Mass zerglings";
            return true;
        }

        if (EnemyInfo.isEnemyNearAnyOurBase() && A.supplyUsed() <= 70) {
            if (DEBUG) reason = "Enemy near our building";
            return true;
        }

        if (A.resourcesBalance() <= -400 && A.supplyUsed() <= 130 && !GamePhase.isLateGame()) {
            if (DEBUG) reason = "Too many resources lost";
            return true;
        }

        return false;
    }

    // === ATTACK ==============================================

    private static boolean shouldChangeMissionToAttack() {
        if (A.supplyUsed() >= 194) {
            if (DEBUG) reason = "Supply blocked";
            return true;
        }

        if (ArmyStrength.weAreMuchStronger() && !EnemyInfo.hasDefensiveLandBuilding(true)) {
            if (DEBUG) reason = "Much stronger";
            return true;
        }

        if (A.resourcesBalance() >= 410 && ArmyStrength.weAreStronger()) {
            if (DEBUG) reason = "Resources balance good";
            return true;
        }

        return false;
    }

}
