package atlantis.production.orders.production;

import atlantis.information.strategy.AStrategy;
import atlantis.production.ProductionOrder;
import atlantis.production.orders.build.CurrentBuildOrder;
import atlantis.units.AUnitType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Current production queue. Based on Build Order loaded from file, with some dynamic changes applied.
 */
public abstract class ProductionQueue {

    /**
     * Ordered list of next units we should build.
     * It gets rebuild whenever new unit is created.
     */
    protected static ArrayList<ProductionOrder> nextInQueue = new ArrayList<>();

    /**
     * Number of minerals reserved to produce some units/buildings.
     */
    protected static int mineralsNeeded = 0;

    /**
     * Number of gas reserved to produce some units/buildings.
     */
    protected static int gasNeeded = 0;

    // =========================================================

    public static int size() {
        return nextInQueue.size();
    }

    public static void addToQueue(int index, ProductionOrder productionOrder) {
        nextInQueue.add(index, productionOrder);
        sortQueueByAtSupply();

//        printQueue("Added to queue: " + productionOrder);
    }

    public static boolean isAtTheTopOfQueue(AUnitType type, int amongNTop) {
        for (int i = 0; i < amongNTop && i < nextInQueue.size(); i++) {
            if (type.equals(nextInQueue.get(i).unitType())) {
                return true;
            }
        }
        return false;
    }

    public static ProductionOrder nextOrderFor(AUnitType type, int amongNTop) {
        for (int i = 0; i < amongNTop && i < nextInQueue.size(); i++) {
            if (type.equals(nextInQueue.get(i).unitType())) {
                return nextInQueue.get(i);
            }
        }
        return null;
    }

    public static int countInQueue(AUnitType type, int amongNTop) {
        int count = 0;
        for (int i = 0; i < amongNTop && i < nextInQueue.size(); i++) {
            if (type.equals(nextInQueue.get(i).unitType())) {
                count++;
            }
        }
        return count;
    }

    // === Getters =============================================

    public static void useBuildOrderFrom(AStrategy strategy) {
        CurrentBuildOrder.set(strategy.buildOrder());
        ProductionQueueRebuilder.rebuildProductionQueueToExcludeProducedOrders();
    }

    /**
     * Number of minerals reserved to produce some units/buildings in the build order that according to it
     * should be produced right now (judging by the supply used).
     */
    public static int mineralsReserved() {
        return mineralsNeeded;
    }

    /**
     * Number of gas reserved to produce some units/buildings in the build order that according to it
     * should be produced right now (judging by the supply used).
     */
    public static int gasReserved() {
        return gasNeeded;
    }

    public static int countOrdersWithPriorityAtLeast(ProductionOrderPriority priority) {
        int total = 0;
        for (ProductionOrder order : nextInQueue) {
//            if (type.equals(order.getUnitOrBuilding())) {
            if (order.priority().compareTo(priority) >= 0) {
                total++;
            }
        }
        return total;
    }

    public static void printQueue(String message) {
        if (message == null) {
            message = "Currently in queue";
        }

        System.out.println("=== " + message + " (" + nextInQueue.size() + ") ===");
        for (ProductionOrder order : nextInQueue) {
            System.out.println(order);
        }
    }

    // =========================================================

    private static void sortQueueByAtSupply() {
//        nextInQueue.sort((o1, o2) -> o1.minSupply() < o2.minSupply());
        nextInQueue.sort(Comparator.comparing(ProductionOrder::minSupply));
    }
}
