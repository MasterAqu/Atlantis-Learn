package atlantis.production.dynamic;

import atlantis.config.AtlantisConfig;
import atlantis.game.A;
import atlantis.game.AGame;
import atlantis.map.Bases;
import atlantis.production.ProductionOrder;
import atlantis.production.constructing.ConstructionRequests;
import atlantis.production.orders.build.AddToQueue;
import atlantis.production.orders.production.ProductionQueue;
import atlantis.units.select.Count;
import atlantis.units.select.Select;
import atlantis.util.We;

public class AExpansionManager {

    public static boolean shouldBuildNewBase() {
//        if (true) return false;

        int bases = Count.bases();
        int basesInProduction = Count.inProductionOrInQueue(AtlantisConfig.BASE);

        if (bases == 0 && basesInProduction == 0) {
            return true;
        }

        if (bases >= 5 || basesInProduction >= 1) {
            return false;
        }

        if (Count.inQueue(AtlantisConfig.BASE, 6) > 0) {
            return false;
        }

//        if (ProductionQueue.size() >= 3) {
//            return false;
//        }

        if (
            bases <= 1
                && basesInProduction == 0
                && (
                (AGame.canAfford(370, 0))
                    || (A.seconds() >= 400 && Count.ourCombatUnits() >= 20)
                    || (A.seconds() >= 600 && Count.ourCombatUnits() >= 8)
                    || (A.seconds() >= 700)
            )
        ) {
            return true;
        }

        if (handleNoZergLarvas()) {
            return true;
        }

        if (Count.workers() <= 17 * (bases + basesInProduction)) {
            return false;
        }

        boolean hasPlentyOfMinerals = AGame.hasMinerals(580);
        int minMinerals = 100 + (AGame.isPlayingAsZerg() ? 268 : 356);

        // It makes sense to think about expansion only if we have a lot of minerals.
        if (!AGame.canAffordWithReserved(minMinerals, 0)) {
            return false;
        }

        // If we have lenty of minerals, then every new base is a hazard
        if (!AGame.canAffordWithReserved(minMinerals, 1200)) {
            return false;
        }

        int inConstruction = ProductionQueue.countInQueue(AtlantisConfig.BASE, 8);
        if (inConstruction >= 1) {
            return false;
        }

        // === Force decent army before 3rd base =========================================

        // Enforce too have a lot of tanks before expansion
        if (!hasPlentyOfMinerals && AGame.isPlayingAsTerran() && bases >= 2) {
            if (Select.ourTanks().count() <= 8) {
                return false;
            }
        }

        // === Check if we have almost as many bases as base locations; if so, exit ======

        if (bases >= Bases.baseLocations().size() - 2) {
            return false;
        }

        int numberOfUnfinishedBases = ConstructionRequests.countNotFinishedOfType(AtlantisConfig.BASE);

        boolean haveEnoughMinerals = AGame.hasMinerals(minMinerals);
//        boolean haveEnoughBases = bases >= 4 && AGame.isPlayingAsZerg() && Select.ourLarva().count() >= 2;
        boolean noBaseToConstruct = numberOfUnfinishedBases == 0;
        boolean allowExtraExpansion = AGame.hasMinerals(minMinerals + 200) && numberOfUnfinishedBases <= 1;

        return haveEnoughMinerals && (noBaseToConstruct || allowExtraExpansion);
    }

    private static boolean handleNoZergLarvas() {
        if (!We.zerg() || Count.larvas() > 0) {
            return false;
        }

        return AGame.canAffordWithReserved(270, 0);
    }

    public static void requestNewBase() {
        // ZERG case
        if (AGame.isPlayingAsZerg()) {
            AddToQueue.withHighPriority(AtlantisConfig.BASE, Select.naturalOrMain());
        }

        // TERRAN + PROTOSS
        else {
            ProductionOrder productionOrder = AddToQueue.withHighPriority(AtlantisConfig.BASE);
            if (Count.bases() <= 1) {
                productionOrder.setModifier("NATURAL");
            }
        }
    }
}
