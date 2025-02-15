package atlantis.units.select;

import atlantis.game.A;
import atlantis.map.position.APosition;
import atlantis.map.position.HasPosition;
import atlantis.terran.repair.ARepairAssignments;
import atlantis.units.AUnit;
import atlantis.units.AUnitType;
import atlantis.units.Units;
import atlantis.util.cache.CachePathKey;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Selection extends BaseSelection {

    protected Selection(Collection<? extends AUnit> unitsData, String initCachePath) {
        super(unitsData, initCachePath);
    }

    // =========================================================

    /**
     * Selects only units of given type(s).
     */
    public Selection ofType(AUnitType... types) {
        return cloneByRemovingIf(
            (unit -> !typeMatches(unit, types)),
            CachePathKey.get(types)
        );
    }

    /**
     * Returns whether the type in unit matches one in the haystack
     */
    private boolean typeMatches(AUnit unit, AUnitType... haystack) {
        for (AUnitType type : haystack) {
            if (
                type != null && unit.is(type)
                    || (unit != null && type != null && unit.is(AUnitType.Zerg_Egg) && type.equals(unit.buildType()))
            ) {
                return true;
            }
        }
        return false;
    }

    public Selection add(Selection otherSelection) {
        return cloneByAdding(otherSelection.data, null);
    }

    public Selection add(AUnit addUnit) {
        ArrayList<AUnit> list = new ArrayList<>();
        list.add(addUnit);

        return cloneByAdding(list, null);
    }

    /**
     * Returns all units that are closer than <b>maxDist</b> tiles from given <b>otherUnit</b>.
     */
    public Selection inRadius(double maxDist, AUnit unit) {
        return Select.cache.get(
            addToCachePath("inRadius:" + maxDist + ":" + unit.idWithHash()),
            0,
            () -> cloneByRemovingIf(
                (u -> u.distTo(unit) > maxDist),
                maxDist + ":" + unit.idWithHash()
            )
        );
    }

    /**
     * Returns all units that are closer than <b>maxDist</b> tiles from given <b>position</b>.
     */
    public Selection inRadius(double maxDist, HasPosition unitOrPosition) {
        return Select.cache.get(
            addToCachePath("inRadius:" + maxDist + ":" + unitOrPosition),
            0,
            () -> cloneByRemovingIf(
                (u -> u.distTo(unitOrPosition) > maxDist),
                maxDist + ":" + unitOrPosition
            )
        );
    }

    /**
     * Returns all units that are closer than <b>maxDist</b> tiles from given <b>position</b>.
     */
    public Selection maxGroundDist(double maxDist, AUnit unit) {
        if (unit.isAir()) {
            return this;
        }

        return Select.cache.get(
            addToCachePath("maxGroundDist:" + maxDist + ":" + unit.idWithHash()),
            0,
            () -> cloneByRemovingIf(
                (u -> u.groundDist(unit) > maxDist),
                ":" + unit.idWithHash()
            )
        );
    }

    /**
     * Returns whether the type in unit matches one in the haystack
     */
//    private boolean typeMatches(FoggedUnit unit, AUnitType... haystack) {
//        for (AUnitType type : haystack) {
//            if (unit.type().equals(type) || (unit.type().equals(AUnitType.Zerg_Egg) && unit.type().equals(type))) {
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * Selects only units of given type(s).
     */
    public int countOfType(AUnitType... types) {
        int total = 0;
        for (AUnit unit : data) {
            boolean typeMatches = false;
            for (AUnitType type : types) {
                if (unit.is(type) || (unit.is(AUnitType.Zerg_Egg) && type.equals(unit.buildType()))) {
                    typeMatches = true;
                    break;
                }
            }
            if (typeMatches) {
                total++;
            }
        }

        return total;
    }

    public Selection limit(int limit) {
        if (data.isEmpty()) {
            return this;
        }

        List<AUnit> newData = new ArrayList<>();
        int max = Math.min(limit, data.size());
        for (int i = 0; i < max; i++) {
            newData.add(data.get(i));
        }

        return new Selection(newData, null);
    }

    /**
     * Selects only those units which are VISIBLE ON MAP (not behind fog of war).
     */
    public Selection visible() {
        return cloneByRemovingIf(
            (unit -> !unit.isVisibleUnitOnMap()),
            "visible"
        );
    }

    /**
     * Selects only those units which are visible and not cloaked.
     */
    public Selection effVisible() {
        return cloneByRemovingIf(
            (unit -> !unit.isDetected() && unit.effCloaked()),
            "effVisible"
        );
    }

    /**
     * Selects only those units which are hidden, cloaked / burrowed. Not possible to be attacked.
     */
    public Selection effCloaked() {
        return cloneByRemovingIf(
            (unit -> !unit.effCloaked()),
            "effCloaked"
        );
    }

    public Selection detectors() {
        return cloneByRemovingIf(unit -> !unit.is(
            AUnitType.Protoss_Photon_Cannon,
            AUnitType.Protoss_Observer,
            AUnitType.Terran_Missile_Turret,
            AUnitType.Terran_Science_Vessel,
            AUnitType.Zerg_Overlord,
            AUnitType.Zerg_Spore_Colony
        ), "detectors");
    }

    public Selection tanksSieged() {
        return cloneByRemovingIf(
            (unit -> !unit.is(AUnitType.Terran_Siege_Tank_Siege_Mode)),
            "tanksSieged"
        );
    }

    public Selection tanks() {
        return cloneByRemovingIf(unit -> !unit.is(
            AUnitType.Terran_Siege_Tank_Siege_Mode,
            AUnitType.Terran_Siege_Tank_Tank_Mode
        ), "tanks");
    }

    public Selection groundUnits() {
        return cloneByRemovingIf(
            (unit -> !unit.isGroundUnit()),
            "groundUnits"
        );
    }

    public Selection nonBuildings() {
        return cloneByRemovingIf(
            (unit -> unit.isBuilding()),
            "nonBuildings"
        );
    }

    public Selection nonWorkers() {
        return cloneByRemovingIf(
            (unit -> unit.isWorker()),
            "nonWorkers"
        );
    }

    /**
     * Selects units that are gathering minerals.
     */
    public Selection gatheringMinerals(boolean onlyNotCarryingMinerals) {
        return cloneByRemovingIf(
            (unit -> !unit.isGatheringMinerals() || (onlyNotCarryingMinerals && unit.isCarryingMinerals())),
            "gatheringMinerals:" + A.trueFalse(onlyNotCarryingMinerals)
        );
    }

    public Selection notGathering() {
        return cloneByRemovingIf(
            (unit -> unit.isGatheringMinerals() || unit.isGatheringGas()), "notGathering"
        );
    }

    public Selection notGatheringGas() {
        return cloneByRemovingIf(
            (unit -> unit.isGatheringGas()), "notGatheringGas"
        );
    }

    /**
     * Selects units being infantry.
     */
    public Selection organic() {
        return cloneByRemovingIf(
            (unit -> !unit.type().isOrganic()), "organic"
        );
    }

    public Selection transports(boolean excludeOverlords) {
        return cloneByRemovingIf(
            (unit -> !unit.type().isTransport() || (excludeOverlords && !unit.type().isTransportExcludeOverlords())),
            ""
        );
    }

    /**
     * Selects bases only (including Lairs and Hives).
     */
    public Selection bases() {
        return cloneByRemovingIf(
            (unit -> !unit.type().isBase()), "bases"
        );
    }

    public Selection workers() {
        return cloneByRemovingIf(
            (unit -> !unit.isWorker()), "workers"
        );
    }

    public Selection realUnits() {
        return cloneByRemovingIf(
            (unit -> !unit.isRealUnit()), "realUnits"
        );
    }

    public Selection realUnitsButAllowBuildings() {
        return cloneByRemovingIf(
            (unit -> !unit.isRealUnitOrBuilding()), "realUnitsButAllowBuildings"
        );
    }

    public Selection air() {
        return cloneByRemovingIf(
            (unit -> !unit.isAir()), "air"
        );
    }

    /**
     * Selects melee units that is units which have attack range at most 1 tile.
     */
    public Selection melee() {
        return cloneByRemovingIf(
            (unit -> !unit.isMelee()), "melee"
        );
    }

    /**
     * Selects only units that do not currently have max hit points.
     */
    public Selection ranged() {
        return cloneByRemovingIf(unit -> !unit.isRanged(), "ranged");
    }

    public Selection wounded() {
        return cloneByRemovingIf(
            (unit -> !unit.isWounded()), "wounded"
        );
    }

    public Selection criticallyWounded() {
        return cloneByRemovingIf(
            (unit -> unit.hp() >= 20), "criticallyWounded");
    }

    public Selection terranInfantryWithoutMedics() {
        return cloneByRemovingIf(
            (unit -> !unit.isTerranInfantryWithoutMedics()), "terranInfantryWithoutMedics"
        );
    }

    /**
     * Selects only buildings.
     */
    public Selection buildings() {
        return cloneByRemovingIf(
            (unit -> !unit.type().isBuilding() && !unit.type().isAddon()), "buildings"
        );
    }

    /**
     * Fogged units may have no position if e.g. unit moved and we don't know where it is.
     */
    public Selection havingPosition() {
        return cloneByRemovingIf(u -> !u.hasPosition(), "havingPosition");
    }

    public Selection havingWeapon() {
        return cloneByRemovingIf(u -> !u.hasAnyWeapon(), "havingWeapon");
    }

    public int totalHp() {
        return data.stream()
            .map(AUnit::hp)
            .reduce(0, Integer::sum);
    }

    public Selection combatUnits() {
        return cloneByRemovingIf(
            (unit -> !unit.isCompleted() || !unit.type().isCombatUnit()), "combatUnits"
        );
    }

    /**
     * Selects military buildings like Photon Cannon, Bunker, Spore Colony, Sunken Colony
     */
    public Selection combatBuildings(boolean includeCreepColonies) {
        return cloneByRemovingIf(
            (unit -> includeCreepColonies ? !unit.type().isCombatBuildingOrCreepColony() : !unit.type().isCombatBuilding()),
            "combatBuildings:" + A.trueFalse(includeCreepColonies)
        );
    }

    public Selection onlyCompleted() {
        return cloneByRemovingIf(
            (unit -> !unit.isCompleted()), "onlyCompleted"
        );
    }

    /**
     * Selects only those Terran vehicles/buildings that can be repaired so it has to be:<br />
     * - mechanical<br />
     * - not 100% healthy<br />
     */
    public Selection repairable(boolean checkIfWounded) {
        return cloneByRemovingIf(
            (unit -> !unit.isCompleted() || !unit.type().isMechanical() || (checkIfWounded && !unit.isWounded())),
            "repairable:" + A.trueFalse(checkIfWounded)
        );
    }

    public boolean onlyMelee() {
        return units().onlyMelee();
    }

    public boolean onlyAir() {
        return units().onlyAir();
    }

    public Selection burrowed() {
        return cloneByRemovingIf(
            (unit -> !unit.isBurrowed()), "burrowed"
        );
    }

    public Selection loaded() {
        return cloneByRemovingIf(
            (unit -> !unit.isLoaded()), "loaded"
        );
    }

    public Selection unloaded() {
        return cloneByRemovingIf(
            (AUnit::isLoaded), "unloaded");
    }

    /**
     * Selects these units (makes sense only for workers) who aren't assigned to repair any other unit.
     */
    public Selection notRepairing() {
        return cloneByRemovingIf(
            (unit -> unit.isRepairing() || ARepairAssignments.isRepairerOfAnyKind(unit)), "notRepairing"
        );
    }

    /**
     * Selects these transport/bunker units which have still enough room inside.
     */
    public Selection havingSpaceFree(int spaceRequired) {
        return cloneByRemovingIf(
            (unit -> unit.spaceRemaining() < spaceRequired), "havingSpaceFree:" + spaceRequired);
    }

    public Selection havingEnergy(int minEnergy) {
        return cloneByRemovingIf(
            (unit -> !unit.energy(minEnergy)), "havingEnergy:" + minEnergy
        );
    }

    public Selection notHavingHp(int hp) {
        return cloneByRemovingIf(
            (unit -> unit.hpMoreThan(hp)), "notHavingHp:" + hp
        );
    }

    public Selection thatCanMove() {
        return cloneByRemovingIf(
            (unit -> !unit.isStasised() && !unit.isLockedDown()), "thatCanMove"
        );
    }

    /**
     * Selects these units (makes sense only for workers) who aren't assigned to construct anything.
     */
    public Selection notConstructing() {
        return cloneByRemovingIf(
            (unit -> unit.isConstructing() || unit.isMorphing() || unit.isBuilder()), "notConstructing"
        );
    }

    public Selection free() {
        return cloneByRemovingIf(
            (u -> u.isBusy() || u.isLifted()), "free"
        );
    }

    /**
     * Selects these units which are not scouts.
     */
    public Selection notScout() {
        return cloneByRemovingIf(
            (AUnit::isScout), "notScout");
    }

    /**
     * Selects these units which are not carrynig nor minerals, nor gas.
     */
    public Selection notCarrying() {
        return cloneByRemovingIf(
            (unit -> unit.isCarryingGas() || unit.isCarryingMinerals()), "notCarrying"
        );
    }

    public Selection excludeTypes(AUnitType... types) {
        return cloneByRemovingIf(
            (unit -> unit.is(types)), "excludeTypes:" + CachePathKey.get(types)
        );
    }

    public Selection excludeMedics() {
        return cloneByRemovingIf(AUnit::isMedic, "excludeMedics");
    }

    public Selection hasPathFrom(HasPosition fromPosition) {
        return cloneByRemovingIf(
            (unit -> !unit.hasPathTo(fromPosition)), "hasPathFrom:" + fromPosition
        );
    }

    public Selection canBeAttackedBy(AUnit attacker, double extraMargin) {
        return cloneByRemovingIf(target -> !attacker.canAttackTarget(
            target, true, true, false, extraMargin
        ), "canBeAttackedBy:" + attacker.idWithHash() + ":" + extraMargin);
    }

    /**
     * Selects only those units from current selection, which can be both <b>attacked by</b> given unit (e.g.
     * Zerglings can't attack Overlord) and are <b>in shot range</b> to the given <b>unit</b>.
     */
    public Selection canAttack(AUnit target, boolean checkShootingRange, boolean checkVisibility, double safetyMargin) {
        return cloneByRemovingIf(
            attacker -> !attacker.canAttackTarget(
                target, checkShootingRange, checkVisibility, false, safetyMargin
            ),
            "canAttack:" + target.idWithHash()
                + ":" + checkShootingRange
                + ":" + checkVisibility
                + ":" + safetyMargin
        );
    }

    public Selection canAttack(AUnit target, double safetyMargin) {
        return cloneByRemovingIf(attacker -> !attacker.canAttackTarget(
            target, true, true, false, safetyMargin
        ), "canAttack:" + target.idWithHash() + ":" + safetyMargin);
    }

    public Selection inShootRangeOf(AUnit attacker) {
        return canBeAttackedBy(attacker, 0);
    }

    public Selection inShootRangeOf(double shootingRangeBonus, AUnit attacker) {
        return canBeAttackedBy(attacker, shootingRangeBonus);
    }

    // =========================================================
    // Localization-related methods

    /**
     * Returns closest unit to given <b>position</b> from all units in the current selection.
     */
    public AUnit nearestTo(HasPosition position) {
        if (data.isEmpty() || position == null) {
            return null;
        }

        sortDataByDistanceTo(position, true);

        return data.isEmpty() ? null : (AUnit) data.get(0);
    }

    public AUnit mostDistantTo(HasPosition position) {
        if (data.isEmpty() || position == null) {
            return null;
        }

        sortDataByDistanceTo(position, false);

        return data.isEmpty() ? null : (AUnit) data.get(0);
    }

    /**
     * From all units currently in selection, returns closest unit to given <b>position</b>.
     */
    public AUnit nearestToOrNull(HasPosition position, double maxLength) {
        if (data.isEmpty() || position == null) {
            return null;
        }

//        Position position;
//        if (positionOrUnit instanceof APosition) {
//            position = (APosition) positionOrUnit;
//        } else if (positionOrUnit instanceof Position) {
//            position = (Position) positionOrUnit;
//        } else {
//            position = ((AUnit) positionOrUnit).getPosition();
//        }

        sortDataByDistanceTo(position, true);
        AUnit nearestUnit = (AUnit) data.get(0);

        if (nearestUnit != null && nearestUnit.distTo(position) < maxLength) {
            return nearestUnit;
        }
        else {
            return null;
        }
    }

    public AUnit mostWounded() {
        if (data.isEmpty()) {
            return null;
        }

        sortByHealth();

        return data.get(0);
    }

    // =========================================================
    // Special retrieve

    /**
     * Returns <b>true</b> if current selection contains at least one unit.
     */
    public boolean anyExists() {
        return !data.isEmpty();
    }

    /**
     * Returns first unit that matches previous conditions or null.
     */
    public AUnit first() {
        return data.isEmpty() ? null : (AUnit) data.get(0);
    }

    public AUnit second() {
        return data.size() < 2 ? null : (AUnit) data.get(1);
    }

    public AUnit randomWithSeed(int seed) {
        Random rand = new Random(seed);
        return data.isEmpty() ? null : (AUnit) data.get(rand.nextInt(data.size()));
    }

    /**
     * Returns first unit that matches previous conditions or null if no units match conditions.
     */
    public AUnit last() {
        return data.isEmpty() ? null : (AUnit) data.get(data.size() - 1);
    }

    /**
     * Returns random unit that matches previous conditions or null if no units matched all conditions.
     */
    public AUnit random() {
        return (AUnit) A.getRandomElement(data);
    }

    // === High-level of abstraction ===========================

    public AUnit lowestHealth() {
        int lowestHealth = 99999;
        AUnit lowestHealthUnit = null;

        for (Iterator it = data.iterator(); it.hasNext(); ) {
            AUnit unit = (AUnit) it.next();
            if (unit.hp() < lowestHealth) {
                lowestHealth = unit.hp();
                lowestHealthUnit = unit;
            }
        }

        return lowestHealthUnit;
    }

    public boolean areAllBusy() {
        for (Iterator<AUnit> it = (Iterator) data.iterator(); it.hasNext(); ) {
            AUnit unit = it.next();
            if (!unit.isBusy()) {
                return false;
            }
        }

        return true;
    }

    // === Operations on set of units ==========================

    /**
     * @return all units except for the given one
     */
    public Selection exclude(AUnit unitToExclude) {
//        if (unitToExclude != null) {
//            data.remove(unitToExclude);
//        }
        List<AUnit> newData = new ArrayList<>(data);
        newData.remove(unitToExclude);
        return new Selection(newData, null);
    }

    public Selection exclude(Collection unitsToExclude) {
        List<AUnit> newData = new ArrayList<>(data);
        newData.removeAll(unitsToExclude);
        return new Selection(newData, null);
    }

    /**
     * Reverse the order in which units are returned.
     */
    public Selection reverse() {
        Collections.reverse(data);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("Units (" + data.size() + "):\n");

        for (AUnit unit : data) {
            string.append("   - ").append(unit.type()).append(" (ID:").append(unit.id()).append(")\n");
        }

        return string.toString();
    }

    // =========================================================
    // Get results

    /**
     * Selects result as an iterable collection (list).
     */
    public List<AUnit> list() {
        return data;
    }

    /**
     * Returns result as an <b>Units</b> object, which contains multiple useful methods to handle set of
     * units.
     */
    public Units units() {
        return new Units(this.data);
    }

    /**
     * Returns number of units matching all previous conditions.
     */
    public int count() {
        return data.size();
    }

    public boolean atLeast(int min) {
        return data.size() >= min;
    }

    public boolean atMost(int max) {
        return data.size() <= max;
    }

    /**
     * Returns true if there're no units that fullfilled all previous conditions.
     */
    public boolean isEmpty() {
        return data.size() == 0;
    }

    public boolean empty() {
        return data.size() == 0;
    }

    public boolean isNotEmpty() {
        return data.size() > 0;
    }

    public boolean notEmpty() {
        return data.size() > 0;
    }

    /**
     * Returns number of units matching all previous conditions.
     */
    public int size() {
        return data.size();
    }

    public List<AUnit> sortDataByDistanceTo(final HasPosition position, final boolean nearestFirst) {
//        if (position == null) {
//            return null;
//        }

        Collections.sort(data, new Comparator<HasPosition>() {
            @Override
            public int compare(HasPosition p1, HasPosition p2) {
                if (!(p1 instanceof HasPosition)) {
                    throw new RuntimeException("Invalid comparison: " + p1);
                }
                if (!(p2 instanceof HasPosition)) {
                    throw new RuntimeException("Invalid comparison: " + p2);
                }

                double distance1 = p1.distTo(position);
                double distance2 = p2.distTo(position);

                return nearestFirst ? Double.compare(distance1, distance2) : Double.compare(distance2, distance1);
            }
        });

        return data;
    }

    public List<AUnit> sortDataByDistanceTo(final AUnit unit, final boolean nearestFirst) {
        Collections.sort(data, new Comparator<AUnit>() {
            @Override
            public int compare(AUnit p1, AUnit p2) {
                if (!(p1 instanceof HasPosition)) {
                    throw new RuntimeException("Invalid comparison: " + p1);
                }
                if (!(p2 instanceof HasPosition)) {
                    throw new RuntimeException("Invalid comparison: " + p2);
                }

                double distance1 = unit.distTo(p1);
                double distance2 = unit.distTo(p2);

                return nearestFirst ? Double.compare(distance1, distance2) : Double.compare(distance2, distance1);
            }
        });

        return data;
    }

    public Selection sortByHealth() {
        if (data.isEmpty()) {
            return new Selection(new ArrayList<>(), "");
        }

        data.sort(Comparator.comparingDouble(AUnit::hpPercent));

        return this;
    }

    public Selection sortByIdAsc() {
        if (data.isEmpty()) {
            return new Selection(new ArrayList<>(), "");
        }

        data.sort(Comparator.comparingInt(AUnit::id));

        return this;
    }

    public APosition center() {
        return units().average();
    }

    public Selection removeDuplicates() {
        List<AUnit> newData = data.stream().distinct().collect(Collectors.toList());
        return new Selection(newData, currentCachePath + ":removeDuplicates");
    }

    public Selection print() {
        return print(null);
    }

    public Selection print(String message) {
        System.out.println("=== " + (message != null ? message : currentCachePath) + " (" + size() + ") ===");
        for (AUnit unit : data) {
            System.out.println(unit.toString());
//            System.out.println(unit.toString() + " / " + unit.getClass());
        }
        System.out.println();
        return this;
    }
}
