package atlantis.units.select;

import atlantis.production.constructing.AConstructionRequests;
import atlantis.production.orders.ProductionQueue;
import atlantis.units.AUnitType;

/**
 * Quick auxiliary class for counting our units.
 */
public class Count {

    public static int ourCombatUnits() {
        return Select.ourCombatUnits().count();
    }

    public static int ofType(AUnitType type) {
        return Select.countOurOfType(type);
    }

    public static int ofTypeFree(AUnitType type) {
        return Select.ourOfType(type).free().count();
    }

    /**
     * Some buildings like Sunken Colony are morphed into from Creep Colony. When counting Creep Colonies, we
     * need to count sunkens as well.
     */
    public static int existingOrInProduction(AUnitType type) {
        return existing(type) + inProduction(type);
    }

    public static int existingOrInProductionOrInQueue(AUnitType type) {
        return existing(type) + inProductionOrInQueue(type);
    }

    public static int inProductionOrInQueue(AUnitType type) {
        return inProduction(type) + inQueue(type, 8);
    }

    private static int inQueue(AUnitType type, int amongNTop) {
        return ProductionQueue.countInQueue(type, amongNTop);
    }

    public static int inProduction(AUnitType type) {
        if (type.equals(AUnitType.Zerg_Sunken_Colony)) {
            return Select.ourUnfinished().ofType(AUnitType.Zerg_Creep_Colony).count()
                    + Select.ourUnfinished().ofType(AUnitType.Zerg_Sunken_Colony).count()
                    + AConstructionRequests.countNotStartedConstructionsOfType(AUnitType.Zerg_Creep_Colony)
                    + AConstructionRequests.countNotStartedConstructionsOfType(AUnitType.Zerg_Sunken_Colony);
        }
        else if (type.equals(AUnitType.Zerg_Spore_Colony)) {
            return Select.ourUnfinished().ofType(AUnitType.Zerg_Creep_Colony).count()
                    + Select.ourUnfinished().ofType(AUnitType.Zerg_Spore_Colony).count()
                    + AConstructionRequests.countNotStartedConstructionsOfType(AUnitType.Zerg_Creep_Colony)
                    + AConstructionRequests.countNotStartedConstructionsOfType(AUnitType.Zerg_Spore_Colony);
        }
//        if (type.equals(AUnitType.Zerg_Creep_Colony)) {
//            return Select.ourIncludingUnfinished().ofType(type).count()
//                    + Select.ourIncludingUnfinished().ofType(AUnitType.Zerg_Spore_Colony).count()
//                    + Select.ourIncludingUnfinished().ofType(AUnitType.Zerg_Sunken_Colony).count();
//        }
        else if (type.isPrimaryBase()) {
            return Select.ourIncludingUnfinished().bases().count()
                    + AConstructionRequests.countNotStartedConstructionsOfType(type)
                    + AConstructionRequests.countNotStartedConstructionsOfType(AUnitType.Zerg_Lair)
                    + AConstructionRequests.countNotStartedConstructionsOfType(AUnitType.Zerg_Hive);
        }
        else if (type.isBase() && !type.isPrimaryBase()) {
            return Select.ourUnfinished().ofType(type).count()
                    + AConstructionRequests.countNotStartedConstructionsOfType(type);
        }
        else {
            return Select.ourUnfinished().ofType(type).count()
                    + AConstructionRequests.countNotStartedConstructionsOfType(type);
        }
    }

    public static int existing(AUnitType type) {
//        if (type.equals(AUnitType.Zerg_Sunken_Colony)) {
//            return Select.countOurOfType(AUnitType.Zerg_Sunken_Colony);
////            return Select.countOurOfType(AUnitType.Zerg_Creep_Colony)
////                    + Select.countOurOfType(AUnitType.Zerg_Sunken_Colony);
//        }
        if (type.isPrimaryBase()) {
            return Select.ourOfType(AUnitType.Zerg_Hatchery, AUnitType.Zerg_Lair, AUnitType.Zerg_Hive).count();
        }
        else if (type.isBase() && !type.isPrimaryBase()) {
            return Select.countOurOfType(type);
        }
        else {
            return Select.countOurOfType(type);
        }
    }

//    private static int countExistingOrPlanned(AUnitType type) {
//        return Select.ourOfType(type).count() + ProductionQueue.countInQueue(type, 6);
//    }


    public static int ourOfTypeIncludingUnfinished(AUnitType type) {
        return Select.countOurOfTypeIncludingUnfinished(type);
    }

    public static int workers() {
        return Select.ourWorkers().count();
    }

    public static int dragoons() {
        return ofType(AUnitType.Protoss_Dragoon);
    }

    public static int zealots() {
        return ofType(AUnitType.Protoss_Zealot);
    }

    public static int pylons() {
        return Select.countOurOfType(AUnitType.Protoss_Pylon);
    }

    public static int bases() {
        return Select.ourBases().count();
    }

    public static int basesWithUnfinished() {
        return Select.ourIncludingUnfinished().bases().count();
    }

    public static int tanks() {
        return Select.countOurOfType(AUnitType.Terran_Siege_Tank_Siege_Mode)
                + Select.countOurOfType(AUnitType.Terran_Siege_Tank_Tank_Mode);
    }

    public static int vultures() {
        return Select.countOurOfType(AUnitType.Terran_Vulture);
    }
}
