
package atlantis.production.dynamic.protoss;

import atlantis.game.A;
import atlantis.game.AGame;
import atlantis.information.decisions.Decisions;
import atlantis.information.enemy.EnemyFlags;
import atlantis.information.generic.ArmyStrength;
import atlantis.information.generic.ProtossArmyComposition;
import atlantis.information.strategy.EnemyStrategy;
import atlantis.information.strategy.GamePhase;
import atlantis.production.AbstractDynamicUnits;
import atlantis.production.orders.build.AddToQueue;
import atlantis.production.orders.build.BuildOrderSettings;
import atlantis.units.AUnit;
import atlantis.units.AUnitType;
import atlantis.units.select.Count;
import atlantis.units.select.Have;
import atlantis.units.select.Select;

import java.util.List;


public class ProtossDynamicUnitsManager extends AbstractDynamicUnits {

    public static void update() {
        if (AGame.notNthGameFrame(3)) {
            return ;
        }

        scarabs();
        observers();
        arbiters();
        corsairs();
        shuttles();
        reavers();

        dragoons();
        zealots();
    }

    // =========================================================

    private static void shuttles() {
        if (
                Have.no(AUnitType.Protoss_Robotics_Facility)
                || Count.ofType(AUnitType.Protoss_Reaver) > Count.ofType(AUnitType.Protoss_Shuttle)
        ) {
            return;
        }

        buildToHave(AUnitType.Protoss_Shuttle, 1);
    }

    private static void observers() {
        if (Have.no(AUnitType.Protoss_Observatory)) {
            if (EnemyFlags.HAS_HIDDEN_COMBAT_UNIT) {
                AddToQueue.withTopPriority(AUnitType.Protoss_Observatory);
            }
            return;
        }

        int limit = Math.max(
                1 + (EnemyFlags.HAS_HIDDEN_COMBAT_UNIT ? 2 : 0),
                A.supplyTotal() / 42
        );
        buildToHave(AUnitType.Protoss_Observer, limit);
    }

    private static void corsairs() {
        if (Have.no(AUnitType.Protoss_Stargate)) {
            return;
        }

        buildToHave(AUnitType.Protoss_Corsair, 4);
    }

    private static void reavers() {
        if (Have.no(AUnitType.Protoss_Robotics_Facility) || Have.no(AUnitType.Protoss_Robotics_Support_Bay)) {
            return;
        }

        int maxReavers = Decisions.isEnemyGoingAirAndWeAreNotPreparedEnough() ? 0 : 5;

        buildToHave(AUnitType.Protoss_Reaver, maxReavers);
    }

    private static void dragoons() {
        if (Have.no(AUnitType.Protoss_Gateway)) {
            return;
        }

        if (GamePhase.isEarlyGame() && EnemyStrategy.get().isRushOrCheese() && Count.zealots() < minZealotsInRush()) {
            if (!A.hasMinerals(225)) {
                return;
            }
        }

//        if (ProtossArmyComposition.zealotsToDragoonsRatioTooLow()) {
//            return;
//        }

        trainIfPossible(AUnitType.Protoss_Dragoon);
    }

    private static int minZealotsInRush() {
        return 2;
    }

    private static void zealots() {
        if (Have.no(AUnitType.Protoss_Gateway)) {
            return;
        }

        if (A.hasGas(50) && !A.hasMinerals(225) && Count.dragoons() <= 1 && Count.zealots() >= 1) {
            return;
        }

        if (!AGame.canAffordWithReserved(125, 0)) {
            return;
        }

        if (BuildOrderSettings.autoProduceZealots()) {
            trainIfPossible(AUnitType.Protoss_Zealot);
            return;
        }

        if (ProtossArmyComposition.zealotsToDragoonsRatioTooLow()) {
            trainIfPossible(AUnitType.Protoss_Zealot);
            return;
        }

        if (
                GamePhase.isEarlyGame()
                        && EnemyStrategy.get().isRushOrCheese()
                        && Count.existingOrInProductionOrInQueue(AUnitType.Protoss_Zealot) <= minZealotsInRush()
        ) {
            trainIfPossible(AUnitType.Protoss_Zealot);
            return;
        }

        if (AGame.isEnemyZerg() && Count.ofType(AUnitType.Protoss_Zealot) <= 0) {
            trainIfPossible(AUnitType.Protoss_Zealot);
            return;
        }
    }

    private static void scarabs() {
        List<AUnit> reavers = Select.ourOfType(AUnitType.Protoss_Reaver).list();
        for (AUnit reaver : reavers) {
            if (reaver.scarabCount() <= 3 && !reaver.isTrainingAnyUnit()) {
                reaver.train(AUnitType.Protoss_Scarab);
            }
        }
    }

    private static void arbiters() {
        if (Count.ofType(AUnitType.Protoss_Arbiter_Tribunal) == 0) {
            return;
        }

        trainNowIfHaveWhatsRequired(AUnitType.Protoss_Arbiter, true);
    }

}
