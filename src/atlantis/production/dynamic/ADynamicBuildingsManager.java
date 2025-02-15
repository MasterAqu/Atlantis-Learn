package atlantis.production.dynamic;

import atlantis.config.AtlantisConfig;
import atlantis.game.AGame;
import atlantis.information.strategy.AStrategy;
import atlantis.information.strategy.EnemyStrategy;
import atlantis.production.constructing.ConstructionRequests;
import atlantis.production.orders.build.AddToQueue;
import atlantis.units.AUnit;
import atlantis.units.AUnitType;
import atlantis.units.select.Count;
import atlantis.units.select.Select;
import atlantis.units.select.Selection;
import atlantis.util.Helpers;


public abstract class ADynamicBuildingsManager extends Helpers {

    public static void update() {
        
        // Check if we should automatically build new base, because we have shitload of minerals.
        if (AExpansionManager.shouldBuildNewBase()) {
            AExpansionManager.requestNewBase();
//            System.err.println("New base requested at " + A.seconds() + "s, minerals = " + AGame.minerals());
        }
        
        // If number of bases is bigger than gas buildings, it usually makes sense to build new gas extractor
        gasBuildingIfNeeded();
    }
    
    // =========================================================

    protected static AStrategy enemyStrategy() {
        return EnemyStrategy.get();
    }

    /**
     * Build Refineries/Assimilators/Extractors when it makes sense.
     */
    private static void gasBuildingIfNeeded() {
        if (AGame.supplyTotal() <= 20) {
            return;
        }

        if (AGame.everyNthGameFrame(37)) {
            return;
        }
        
        // =========================================================
        
        int numberOfBases = Select.ourBases().count();
        int numberOfGasBuildings = Select.ourWithUnfinished().ofType(AtlantisConfig.GAS_BUILDING).count();
        if (
            numberOfBases >= 2
            && numberOfBases > numberOfGasBuildings && !AGame.canAfford(0, 350)
            && ConstructionRequests.countNotStartedOfType(AtlantisConfig.GAS_BUILDING) == 0
            && hasABaseWithFreeGeyser()
        ) {
            AddToQueue.withTopPriority(AtlantisConfig.GAS_BUILDING);
        }
    }

    // =========================================================

    protected static boolean requestMoreIfAllBusy(AUnitType building, int freeMinerals, int freeGas) {
        if (AGame.canAffordWithReserved(freeMinerals, freeGas)) {
            Selection buildings = Select.ourOfType(building);

            if (buildings.areAllBusy()) {
                AddToQueue.withStandardPriority(building);
                return true;
            }
        }
        return false;
    }

    protected static void buildToHaveOne(int minSupply, AUnitType type) {
        if (AGame.supplyUsed() >= minSupply) {
            buildToHaveOne(type);
        }
    }

    protected static void buildToHaveOne(AUnitType type) {
        if (Count.WithPlanned(type) > 0) {
            return;
        }

        buildNow(type, true);
    }

    protected static void buildIfHaveMineralsAndGas(AUnitType type) {
        buildIfHaveMineralsAndGas(type, true, type.getMineralPrice(), type.getGasPrice());
    }

    protected static void buildIfCanAffordWithReserved(AUnitType type) {
        buildIfCanAffordWithReserved(type, true, type.getMineralPrice(), type.getGasPrice());
    }

    protected static void buildIfAllBusyButCanAfford(AUnitType type, int extraMinerals, int extraGas) {
        if (Select.ourOfType(type).areAllBusy()) {
            buildIfHaveMineralsAndGas(type, true, type.getMineralPrice() + extraMinerals, type.getGasPrice() + extraGas);
        }
    }

    protected static void buildIfHaveMineralsAndGas(AUnitType type, boolean onlyOneAtTime, int hasMinerals, int hasGas) {
        if (!AGame.canAfford(hasMinerals, hasGas)) {
            return;
        }

        buildNow(type, onlyOneAtTime);
    }

    protected static void buildIfCanAffordWithReserved(AUnitType type, boolean onlyOneAtTime, int hasMinerals, int hasGas) {
        if (!AGame.canAffordWithReserved(hasMinerals, hasGas)) {
            return;
        }

        buildNow(type, onlyOneAtTime);
    }

    protected static void buildNow(AUnitType type) {
        buildNow(type, false);
    }

    protected static void buildNow(AUnitType type, boolean onlyOneAtTime) {
        if (onlyOneAtTime && ConstructionRequests.hasRequestedConstructionOf(type)) {
            return;
        }

        if (!hasRequiredUnitFor(type)) {
            buildToHaveOne(type.getWhatIsRequired());
            return;
        }

        AddToQueue.withTopPriority(type);
    }

    // =========================================================

    public static boolean hasABaseWithFreeGeyser() {
        for (AUnit base : Select.ourBases().list()) {
            if (Select.geysers().inRadius(8, base).isNotEmpty()) {
                return true;
            }
        }

        return false;
    }

}
