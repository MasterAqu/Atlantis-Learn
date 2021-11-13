package atlantis.combat.missions;

import atlantis.combat.micro.managers.AdvanceUnitsManager;
import atlantis.combat.squad.ASquadCohesionManager;
import atlantis.combat.squad.SquadScout;
import atlantis.map.Bases;
import atlantis.position.APosition;
import atlantis.units.AUnit;
import atlantis.util.A;

public class MissionContain extends Mission {

    protected MissionContain() {
        super("Contain");
        focusPointManager = new MissionContainFocusPoint();
    }

    @Override
    public boolean update(AUnit unit) {
        APosition focusPoint = focusPoint();
        unit.setTooltip("#Contain(" + (focusPoint != null ? A.digit(focusPoint.distTo(unit)) : null) + ")");

        // =========================================================

//        if (handleUnitSafety(unit, true, true)) {
//            return true;
//        }

        if (SquadScout.handle(unit)) {
            return true;
        }

        if (ASquadCohesionManager.handle(unit)) {
            return true;
        }

        // Focus point is well known
        return focusPoint != null && AdvanceUnitsManager.moveToFocusPoint(unit, focusPoint);

        // =========================================================
    }

    @Override
    public boolean allowsToAttackEnemyUnit(AUnit unit, AUnit enemy) {
        APosition focusPoint = focusPoint();

        if (enemy.hasWeaponRange(unit, 0.8) || unit.hasWeaponRange(enemy, 0.8)) {
//        if (enemy.distTo(unit) <= 6.1 || unit.hasWeaponRange(enemy, 0.8)) {
            return true;
        }

        // Only attack enemies near squad center
        if (enemy.distTo(unit.squad().median()) <= 8) {
            return true;
        }

        // Allow to defend base
        APosition natural = Bases.natural();
        if (natural != null && enemy.distTo(natural) <= 35) {
            return true;
        }

        return false;
    }
}