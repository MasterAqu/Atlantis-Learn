package atlantis.combat.missions.defend;

import atlantis.combat.missions.AFocusPoint;
import atlantis.combat.missions.MoveToFocusPoint;
import atlantis.game.A;
import atlantis.units.AUnit;
import atlantis.units.actions.Actions;
import atlantis.units.select.Select;
import atlantis.util.Enemy;
import atlantis.util.We;

public class MoveToDefendFocusPoint extends MoveToFocusPoint {

    public boolean move(AUnit unit, AFocusPoint focusPoint) {
        this.unit = unit;
        this.focus = focusPoint;

//        if (holdOnPerpendicularLine()) {
//            return true;
//        }

        fromSide = focusPoint.fromSide();
        optimalDist = optimalDist(unit);
        unitToFocus = unit.distTo(focusPoint);
        unitToFromSide = focusPoint.fromSide() == null ? -1 : unit.distTo(focusPoint.fromSide());
        focusToFromSide = focusPoint.fromSide() == null ? -1 : focusPoint.distTo(focusPoint.fromSide());
//
//        if (tooFar() || tooClose()) {
//            return true;
//        }

        if (handleWrongSideOfFocus(unit, focusPoint) || tooCloseToFocusPoint() || joinSquad(unit) || advance()) {
            return true;
        }

        return false;
    }

    protected boolean advance() {
        focus = unit.mission().focusPoint();

        if (focus == null) {
//            System.err.println("Null focus point for " + unit + " in MoveToFocusPoint");
            System.err.println("unit.mission() = " + unit.mission());
            A.printStackTrace("Null focus point for " + unit + " in MoveToFocusPoint");
            return false;
        }

        unitToFocus = unit.distTo(focus);
        optimalDist = optimalDist(unit);

        if (unit.enemiesNear().inRadius(5, unit).notEmpty()) {
            unit.addLog("DontWithdraw");
            return false;
        }

        if (unitToFocus > (optimalDist + MARGIN)) {
            String dist = A.dist(unitToFocus);
            return unit.move(
                focus.translatePercentTowards(unit, 40),
                Actions.MOVE_FOCUS,
                "ToFocus" + dist,
                true
            );
        }

        return false;
    }

    private boolean joinSquad(AUnit unit) {
        if (unit.distToSquadCenter() >= 8 && unit.enemiesNear().isEmpty()) {
            unit.addLog("JoinSquad");
            return unit.move(unit.squadCenter(), Actions.MOVE_FORMATION, "JoinSquad", false);
        }
        return false;
    }

    // =========================================================

    public double optimalDist(AUnit unit) {
//        if (unit.isZealot()) {
//            private final double SPARTA_MODE_DIST_FROM_FOCUS = 0.55;
//            return SPARTA_MODE_DIST_FROM_FOCUS + letWorkersComeThroughBonus();
//        }

        double base = Enemy.protoss() ? 0.6 : 0.0;

        if (unit.isTerran()) {
            base += (unit.isTank() ? 3 : 0)
                + (unit.isMedic() ? -2.5 : 0)
                + (unit.isMarine() ? 2 : 0)
                + (Select.our().inRadius(2, unit).count() / 25.0);
        }

        return base
            + letWorkersComeThroughBonus()
            + rangedDistBonus();
    }

    private double letWorkersComeThroughBonus() {
        if (We.protoss() && A.seconds() >= 150) {
            return 0;
        }

        return unit.enemiesNear().combatUnits().isEmpty()
                && Select.ourWorkers().inRadius(7, unit).atLeast(1)
                ? 3 : 0;
    }

    private double rangedDistBonus() {
        if (unit.isDragoon()) {
            return 1.7;
        }

        return (unit.isRanged() ? 3 : 0);
    }

}
