package atlantis.terran.repair;

import atlantis.game.GameLog;
import atlantis.units.AUnit;
import atlantis.units.select.Select;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ARepairAssignments {

    public static final int MODE_REPAIR_ONLY = 1;
    public static final int MODE_PROTECT = 2;

    private static final boolean addLogs = true;

    // Unit repairers
    protected static Map<AUnit, AUnit> repairersToUnit = new HashMap<>();
    protected static Map<AUnit, ArrayList<AUnit>> unitsToRepairers = new HashMap<>();
    protected static Map<AUnit, Integer> repairersToModes = new HashMap<>();

    // =========================================================

    public static boolean isProtector(AUnit repairer) {
        return repairersToModes.containsKey(repairer) && repairersToModes.get(repairer) == MODE_PROTECT;
    }

    public static AUnit getUnitToProtectFor(AUnit repairer) {
        return repairersToUnit.get(repairer);
    }

    public static AUnit getUnitToRepairFor(AUnit repairer) {
        return repairersToUnit.get(repairer);
    }

    public static AUnit getClosestRepairerAssignedTo(AUnit wounded) {
        if (
                unitsToRepairers.size() > MaxRepairers.MAX_REPAIRERS_AT_ONCE
                || unitsToRepairers.size() >= (0.5 * Select.ourWorkers().count())
        ) {
            return null;
        }

        if (unitsToRepairers.containsKey(wounded)) {
            ArrayList<AUnit> repairers = unitsToRepairers.get(wounded);
            return Select.from(repairers, "repairers").nearestTo(wounded);
//            if (repairers.size() >= 1) {
//                if (repairers.get(0).isAlive()) {
//                    return repairers.get(0);
//                }
//                else {
//                    removeRepairerOrProtector(repairers.get(0));
//                }
//            }
        }
        return null;
    }

    public static void removeRepairerOrProtector(AUnit repairer) {
//        if (repairer.target() != null && repairer.target().isAlive()) {
//            A.printStackTrace(A.now() + " REMOVED REPAIRER");
//        }

        AUnit unitToRepair = repairersToUnit.get(repairer);
        if (unitToRepair != null && unitsToRepairers.containsKey(unitToRepair)) {
            unitsToRepairers.get(unitToRepair).remove(repairer);
            repairer.stop("No longer repairer", true);
            if (addLogs) { GameLog.get().addMessage("No longer repairer of " + unitToRepair); }
        }
        repairersToUnit.remove(repairer);
        repairersToModes.remove(repairer);
    }

    public static void addProtector(AUnit protector, AUnit unit) {
        if (addLogs) { GameLog.get().addMessage("Added PROTECTOR of " + unit); }

        addRepairer(protector, unit);
        repairersToModes.put(protector, MODE_PROTECT);
    }

    public static void addRepairer(AUnit repairer, AUnit unitToRepair) {
        if (!repairer.isScv()) {
            throw new RuntimeException(repairer + "is not SCV in addRepairer!");
        }

        if (addLogs) { GameLog.get().addMessage("Added Repairer (" + repairer.idWithHash() + ") of " + unitToRepair); }

        repairersToUnit.put(repairer, unitToRepair);
        repairersToModes.put(repairer, MODE_REPAIR_ONLY);
        if (!unitsToRepairers.containsKey(unitToRepair)) {
            unitsToRepairers.put(unitToRepair, new ArrayList<>());
        }
        unitsToRepairers.get(unitToRepair).add(repairer);
    }

    public static int countProtectorsFor(AUnit unit) {
        if (!unitsToRepairers.containsKey(unit)) {
            return 0;
        }

        int protectors = 0;
        for (AUnit repairer : unitsToRepairers.get(unit)) {
            if (isProtector(repairer)) {
                protectors++;
            }
        }

        return protectors;
    }

    public static int countTotalProtectors() {
        int protectors = 0;
        for (int mode : repairersToModes.values()) {
            if (mode == MODE_PROTECT) {
                protectors++;
            }
        }

        return protectors;
    }

    public static int countRepairersForUnit(AUnit unit) {
        return unitsToRepairers.containsKey(unit) ? unitsToRepairers.get(unit).size() : 0;
    }

    public static boolean isRepairerOfAnyKind(AUnit worker) {
        return repairersToUnit.containsKey(worker);
    }

    public static List<AUnit> getProtectors() {
        ArrayList<AUnit> result = new ArrayList<>();
        for (AUnit repairer : repairersToUnit.keySet()) {
            if (isProtector(repairer)) {
                result.add(repairer);
            }
        }
        return result;
    }

    public static List<AUnit> getRepairers() {
        return new ArrayList<>(repairersToUnit.keySet());
    }

    public static int countTotalRepairers() {
        return repairersToUnit.keySet().size();
    }

    public static ArrayList<AUnit> getProtectorsFor(AUnit unit) {
        return unitsToRepairers.containsKey(unit) ? unitsToRepairers.get(unit) : new ArrayList<>();
    }
}
