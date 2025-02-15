package atlantis.combat.micro.terran;

import atlantis.combat.missions.Missions;
import atlantis.combat.squad.Squad;
import atlantis.combat.squad.alpha.Alpha;
import atlantis.map.position.APosition;
import atlantis.map.position.HasPosition;
import atlantis.units.select.Have;

public class OffensiveTerranMissileTurrets extends TerranMissileTurretsForNonMain {

    public static boolean buildIfNeeded() {
        if (!Have.engBay()) {
            return false;
        }

        if (handleReinforceMissionContain()) {
            return true;
        }

        return false;
    }

    // =========================================================

    protected static boolean handleReinforceMissionAttack() {
        HasPosition squadCenter = Squad.alphaCenter();
        if (squadCenter == null) {
            return false;
        }

        return handleReinforcePosition(squadCenter, 14);
    }

    protected static boolean handleReinforceMissionContain() {
        APosition focusPoint = Missions.globalMission().focusPoint();
        if (focusPoint == null || !Have.main()) {
            return false;
        }

        if (Alpha.count() <= 12) {
            return false;
        }

        return handleReinforcePosition(containReinforcePoint(focusPoint), 9);
    }

    protected static HasPosition containReinforcePoint(APosition focusPoint) {
        APosition point = focusPoint;
//        FoggedUnit enemyBuilding = EnemyUnits.nearestEnemyBuilding();

        HasPosition alphaCenter = Squad.alphaCenter();
        if (alphaCenter != null) {
//            point = point.translateTilesTowards(-6, enemyBuilding);
//            AChoke choke = Chokes.nearestChoke(enemyBuilding);
//            if (choke != null) {
            point = point.translatePercentTowards(113, alphaCenter);
//            }
        }

        return point;
    }

}
