package atlantis.units;

import atlantis.combat.eval.ACombatEvaluator;
import atlantis.combat.missions.AFocusPoint;
import atlantis.combat.missions.Mission;
import atlantis.combat.missions.Missions;
import atlantis.combat.retreating.ARunningManager;
import atlantis.combat.squad.Squad;
import atlantis.combat.squad.CohesionAssurance;
import atlantis.debug.painter.APainter;
import atlantis.game.A;
import atlantis.game.AGame;
import atlantis.information.enemy.EnemyUnits;
import atlantis.information.enemy.UnitsArchive;
import atlantis.information.tech.ATech;
import atlantis.information.tech.SpellCoordinator;
import atlantis.map.position.APosition;
import atlantis.map.position.HasPosition;
import atlantis.map.position.PositionUtil;
import atlantis.map.scout.AScoutManager;
import atlantis.production.constructing.AConstructionManager;
import atlantis.production.constructing.ConstructionOrder;
import atlantis.production.constructing.ConstructionRequests;
import atlantis.terran.repair.ARepairAssignments;
import atlantis.units.actions.Action;
import atlantis.units.actions.Actions;
import atlantis.units.select.Count;
import atlantis.units.select.Select;
import atlantis.units.select.Selection;
import atlantis.util.We;
import atlantis.util.cache.Cache;
import atlantis.util.CappedList;
import atlantis.util.Vector;
import atlantis.util.Vectors;
import atlantis.util.log.Log;
import bwapi.*;
import tests.unit.FakeUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for bwapi Unit class that makes units much easier to use.<br /><br />
 * Atlantis uses wrappers for bwapi native classes which can't be extended.<br /><br />
 * <b>AUnit</b> class contains number of helper methods, but if you think some methods are missing you can
 * add them here.
 * <p>
 * Also you can always reference original Unit class via u() method, but please avoid it as code will be very
 * hard to migrate to another bridge. I've already used 3 of them in my career so far.
 */
//public class AUnit implements UnitInterface, Comparable<AUnit>, HasPosition, AUnitOrders {
public class AUnit implements Comparable<AUnit>, HasPosition, AUnitOrders {

    public static final int UPDATE_UNIT_POSITION_EVERY_FRAMES = 30;

    // Mapping of native unit IDs to AUnit objects
    private static final Map<Integer, AUnit> instances = new HashMap<>();

    // Cached distances to other units - reduces time on calculating unit1.distanceTo(unit2)
//    public static final ACachedValue<Double> unitDistancesCached = new ACachedValue<>();

    private Unit u;
    private Cache<Object> cache = new Cache<>();
    private Cache<Integer> cacheInt = new Cache<>();
    private Cache<Boolean> cacheBoolean = new Cache<>();
    protected AUnitType _lastType = null;
    private Log log = new Log(Log.UNIT_LOG_EXPIRE_AFTER_FRAMES, Log.UNIT_LOG_SIZE);
    private Action unitAction = Actions.INIT;
    //    private final AUnit _cachedNearestMeleeEnemy = null;
    public CappedList<Integer> _lastHitPoints = new CappedList<>(20);
    private int _lastActionReceived = 0;
    public int _lastAttackOrder;
    public int _lastAttackFrame;
    public int _lastCooldown;
    public int _lastFrameOfStartingAttack;
    public int _lastRetreat = -99;
    public int _lastStartedRunning;
    public int _lastStoppedRunning;
    public int _lastStartedAttack;
    public AUnit _lastTargetToAttack;
    public int _lastTargetToAttackAcquired;
    public TechType _lastTech;
    public APosition _lastTechPosition;
    public AUnit _lastTechUnit;
    public int _lastUnderAttack;
    public int _lastX;
    public int _lastY;

    // =========================================================

    /**
     * Atlantis uses wrapper for BWAPI classes.
     *
     * <b>AUnit</b> class contains numerous helper methods, but if you think some methods are missing you can
     * create missing method here and you can reference original Unit class via u() method.
     * <p>
     * The idea why we don't use inner Unit class is because if you change game bridge (JBWAPI, JNIBWAPI, BWMirror etc)
     * you need to change half of your codebase. I've done it 3 times already ;__:
     */
    public static AUnit createFrom(Unit u) {
        if (u == null) {
            throw new RuntimeException("AUnit constructor: unit is null");
        }

        AUnit unit;
        if (instances.containsKey(u.getID())) {
            unit = instances.get(u.getID());
//            if (unit != null && unit.isAlive()) {
            if (unit != null) {
                return unit;
            }
//            instances.remove(id());
        }

        unit = new AUnit(u);
        instances.put(unit.id(), unit);
        return unit;
    }

    public static AUnit getById(Unit u) {
        return createFrom(u);
    }

    // =========================================================
    // Only for tests
    protected AUnit() {
    }

    protected AUnit(FakeUnit unit) {
    }
    // =========================================================

    protected AUnit(Unit u) {
        this.u = u;

        // Cached type helpers
        refreshType();

        // Repair & Heal
        this._repairableMechanically = isBuilding() || isVehicle();
        this._healable = isInfantry() || isWorker();

        // Military building
        this._isMilitaryBuildingAntiGround = is(
            AUnitType.Terran_Bunker, AUnitType.Protoss_Photon_Cannon, AUnitType.Zerg_Sunken_Colony
        );
        this._isMilitaryBuildingAntiAir = is(
            AUnitType.Terran_Bunker, AUnitType.Terran_Missile_Turret,
            AUnitType.Protoss_Photon_Cannon, AUnitType.Zerg_Spore_Colony
        );
    }

    // =========================================================

    public static void forgetUnitEntirely(AUnit unit) {
        instances.remove(unit.id());
    }

    /**
     * Returns unit type from bridge OR if type is Unknown (behind fog of war) it will return last cached type.
     */
    public AUnitType type() {
        if (_lastType != null) {
            return _lastType;
        }

        return cacheType();
    }

    public UnitType bwapiType() {
        return u.getType();
    }

    public void refreshType() {
        cache.clear();
        cacheBoolean.clear();
        cacheInt.clear();
        cacheType();
    }

    protected AUnitType cacheType() {
        _lastType = AUnitType.from(u.getType());
        return _lastType;
    }

    @Override
    public APosition position() {
        return APosition.create(u.getPosition());
    }

    /**
     * <b>AVOID USAGE AS MUCH AS POSSIBLE</b> outside AUnit class. AUnit class should be used always in place
     * of Unit.
     */
    @Override
    public Unit u() {
        return u;
    }

    /**
     * This method exists only to allow reference in UnitActions class.
     */
    @Override
    public AUnit unit() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (!(o instanceof AUnit)) return false;
        AUnit aUnit = (AUnit) o;
        return id() == aUnit.id();
    }

    @Override
    public int hashCode() {
        return id();
    }

    // =========================================================
    // =========================================================
    // =========================================================

    private Squad squad;
    private final ARunningManager runningManager = new ARunningManager(this);

    private boolean _repairableMechanically = false;
    private boolean _healable = false;
    private boolean _isMilitaryBuildingAntiGround = false;
    private boolean _isMilitaryBuildingAntiAir = false;

    // =========================================================
    // Important methods

    /**
     * Unit will move by given distance (in build tiles) from given position.
     */
    public boolean moveAwayFrom(HasPosition position, double moveDistance, String tooltip, Action action) {
        if (position == null || moveDistance < 0.01) {
            return false;
        }

        int dx = position.x() - x();
        int dy = position.y() - y();
        double vectorLength = Math.sqrt(dx * dx + dy * dy);
        double modifier = (moveDistance * 32) / vectorLength;
        dx = (int) (dx * modifier);
        dy = (int) (dy * modifier);

        APosition newPosition = new APosition(x() - dx, y() - dy).makeValid();

        if (
            runningManager().isPossibleAndReasonablePosition(this, newPosition)
                && move(newPosition, action, "Move away", false)
        ) {
            this.setTooltip(tooltip, false);
            return true;
        }

        APainter.paintLine(position, newPosition, Color.Teal);
        this.setTooltip("Cant move away", false);
        return move(newPosition, Actions.MOVE_ERROR, "Force move", false);
    }

    // =========================================================

    @Override
    public String toString() {
        return idWithHash() + " " + type().name();
    }

    @Override
    public int compareTo(AUnit otherUnit) {
        return Integer.compare(this.hashCode(), otherUnit.hashCode());
    }

    // =========================================================
    // Compare type methods

    public boolean isAlive() {
        return exists() && (hp() > 0 || !UnitsArchive.isDestroyed(id()));
    }

    public boolean isDead() {
        return !isAlive();
    }

    public boolean canBeHealed() {
        return _repairableMechanically || _healable;
    }

    public boolean isRepairableMechanically() {
        return _repairableMechanically;
    }

    public boolean isHealable() {
        return _healable;
    }

    /**
     * Returns true if given unit is OF TYPE BUILDING.
     */
    public boolean isBuilding() {
        return type().isBuilding() || type().isAddon();
    }

    public boolean isWorker() {
        return type().isWorker();
    }

    public boolean isBunker() {
        return type().equals(AUnitType.Terran_Bunker);
    }

    public boolean isBase() {
        return is(
            AUnitType.Terran_Command_Center, AUnitType.Protoss_Nexus,
            AUnitType.Zerg_Hatchery, AUnitType.Zerg_Lair, AUnitType.Zerg_Hive
        );
    }

    public boolean isInfantry() {
        return (boolean) cache.get(
            "isInfantry",
            -1,
            () -> type().isOrganic()
        );
    }

    public boolean isVehicle() {
        return type().isMechanical();
    }

    /**
     * Returns true if given unit is considered to be "ranged" unit (not melee).
     */
    public boolean isRanged() {
        return (boolean) cache.get(
            "isRanged",
            -1,
            () -> type().isRanged()
        );
    }

    /**
     * Returns true if given unit is considered to be "melee" unit (not ranged).
     */
    public boolean isMelee() {
        return (boolean) cache.get(
            "isMelee",
            -1,
            () -> type().isMelee()
        );
    }

    // =========================================================
    // Auxiliary methods
    public boolean ofType(AUnitType type) {
        return type().equals(type);
    }

    public boolean isFullyHealthy() {
        return hp() >= maxHp();
    }

    public int hpPercent() {
        return 100 * hp() / maxHp();
    }

    public boolean hpPercent(int minPercent) {
        return hpPercent() >= minPercent;
    }

    public double woundPercent() {
        return 100 - 100.0 * hp() / maxHp();
    }

    public boolean woundPercent(int minWoundPercent) {
        return woundPercent() >= minWoundPercent;
    }

    public boolean isWounded() {
        return hp() < maxHP();
    }

    public boolean isExists() {
        return u().exists();
    }

    public int shields() {
        return u().getShields();
    }

    public int maxShields() {
        return type().ut().maxShields();
    }

    public int maxHP() {
        return maxHp() + maxShields();
    }

    public int minesCount() {
        return u().getSpiderMineCount();
    }

    public String name() {
        return type().name();
    }

    public String nameWithId() {
        return type().name() + " #" + id();
    }

    public boolean isInWeaponRangeByGame(AUnit target) {
        return u.isInWeaponRange(target.u);
    }

    /**
     * Returns max shoot range (in build tiles) of this unit against land targets.
     */
    public int groundWeaponRange() {
        return cacheInt.get(
            "groundWeaponRange",
            60,
            () -> type().groundWeapon().maxRange() / 32
        );
    }

    /**
     * Returns max shoot range (in build tiles) of this unit against land targets.
     */
    public double groundWeaponMinRange() {
        return cacheInt.get(
            "getGroundWeaponMinRange",
            60,
            () -> type().groundWeapon().minRange() / 32
        );
    }

    /**
     * Returns max shoot range (in build tiles) of this unit against land targets.
     */
    public double airWeaponRange() {
        return cacheInt.get(
            "airWeaponRange",
            60,
            () -> type().airWeapon().maxRange() / 32
        );
    }

    /**
     * Returns max shoot range (in build tiles) of this unit against given <b>opponentUnit</b>.
     */
    public int weaponRangeAgainst(AUnit opponentUnit) {
        return opponentUnit.type().weaponRangeAgainst(this);
    }

    /**
     * Returns which unit of the same type this unit is. E.g. it can be first (0) Overlord or third (2)
     * Zergling. It compares IDs of units to return correct result.
     */
    public int getUnitIndexInBwapi() {
        int index = 0;
        for (AUnit otherUnit : Select.our().ofType(type()).list()) {
            if (otherUnit.id() < this.id()) {
                index++;
            }
        }
        return index;
    }

    // ===  Debugging / Painting methods ========================================

    private String tooltip;
//    private int tooltipStartInFrames;

    public AUnit setTooltipTactical(String tooltip) {
        return setTooltip(tooltip, false);
    }

    public AUnit setTooltip(String tooltip, boolean strategicLevel) {
        if (strategicLevel) {
            this.tooltip = tooltip;
        }
        this.tooltip = tooltip;
//        System.out.println(A.now() + " - " + this.tooltip);
        return this;
    }

    public AUnit setTooltip(String tooltip) {
        this.tooltip = tooltip;
//        System.out.println(A.now() + " - " + this.tooltip);
        return this;
    }

//    public AUnit addTooltip(String tooltip) {
//        this.tooltip = tooltip() + tooltip;
//        return this;
//    }

    public String tooltip() {
//        if (AGame.getTimeFrames() - tooltipStartInFrames > 30) {
//            String tooltipToReturn = this.tooltip;
//            this.tooltip = null;
//            return tooltipToReturn;
//        } else {
        return tooltip;
//        }
    }

    public void removeTooltip() {
        this.tooltip = null;
    }

    public boolean hasTooltip() {
        return tooltip != null && !tooltip.equals("");
    }

    // =========================================================
    // Very specific auxiliary methods

    /**
     * Returns true if given unit is one of buildings like Bunker, Photon Cannon etc. For more details, you
     * have to specify at least one <b>true</b> to the params.
     */
    public boolean isMilitaryBuilding(boolean canShootGround, boolean canShootAir) {
        if (!isBuilding()) {
            return false;
        }
        if (canShootGround && _isMilitaryBuildingAntiGround) {
            return true;
        }
        else return canShootAir && _isMilitaryBuildingAntiAir;
    }

    public boolean isGroundUnit() {
        return !type().isAir();
    }

    public boolean isAir() {
        return type().isAir();
    }

    public boolean isMine() {
        return type().equals(AUnitType.Terran_Vulture_Spider_Mine);
    }

    public boolean isLarvaOrEgg() {
        return type().equals(AUnitType.Zerg_Larva) || type().equals(AUnitType.Zerg_Egg);
    }

    public boolean isLarva() {
        return type().equals(AUnitType.Zerg_Larva);
    }

    public boolean isEgg() {
        return type().equals(AUnitType.Zerg_Egg);
    }

    /**
     * Not that we're racists, but buildings, spider mines and larvas aren't really units...
     */
    public boolean isRealUnit() {
        return type().isRealUnit();
    }

    public boolean isRealUnitOrBuilding() {
        return type().isRealUnitOrBuilding();
    }

    // =========================================================
    // Auxiliary

    public double distTo(AUnit otherUnit) {
        return PositionUtil.distanceTo(this, otherUnit);
    }

    public double distTo(Object o) {
        return PositionUtil.distanceTo(position(), o);
    }

    /**
     * Converts collection of <b>Unit</b> variables into collection of <b>AUnit</b> variables.
     */
    private static Object convertToAUnitCollection(Object collection) {
        if (collection instanceof Map) {
            Map<AUnit, Integer> result = new HashMap<>();
            for (Object key : ((Map) collection).keySet()) {
                Unit u = (Unit) key;
                AUnit unit = createFrom(u);
                result.put(unit, (Integer) ((Map) collection).get(u));
            }
            return result;
        }
        else if (collection instanceof List) {
            List<AUnit> result = new ArrayList<>();
            for (Object key : (List) collection) {
                Unit u = (Unit) key;
                AUnit unit = createFrom(u);
                result.add(unit);
            }
            return result;
        }
        else {
            throw new RuntimeException("I don't know how to convert collection of type: "
                + collection.toString());
        }
    }

    // =========================================================
    // RANGE and ATTACK methods

    /**
     * Returns true if this unit is capable of attacking <b>target</b>. For example Zerglings can't attack
     * flying targets and Corsairs can't attack ground targets.
     */
    public boolean canAttackTarget(AUnit target) {
        return canAttackTarget(target, true, true, false, 0);
    }

    public boolean canAttackTarget(
        AUnit target,
        boolean checkShootingRange
    ) {
        return canAttackTarget(target, checkShootingRange, true, false, 0);
    }

    public boolean canAttackTarget(
        AUnit target,
        boolean checkShootingRange,
        boolean checkVisibility
    ) {
        return canAttackTarget(target, checkShootingRange, checkVisibility, false, 0);
    }

    public boolean canAttackTarget(
        AUnit target,
        boolean checkShootingRange,
        boolean checkVisibility,
        boolean includeCooldown,
        double extraMargin
    ) {
        if (hasNoWeaponAtAll()) {
            return false;
        }

        if (target.isFoggedUnitWithUnknownPosition()) {
            return false;
        }

        // Target CLOAKED
        if (checkVisibility && target.effCloaked()) {
            return false;
        }

        // Target is GROUND unit
        if (target.isGroundUnit() && (!canAttackGroundUnits() || (includeCooldown && cooldownRemaining() >= 4))) {
            return false;
        }

        // Target is AIR unit
        if (target.isAir() && (!canAttackAirUnits() || (includeCooldown && cooldownRemaining() >= 4))) {
            return false;
        }

        // Shooting RANGE
        if (checkShootingRange && !hasWeaponRangeToAttack(target, extraMargin)) {
//            System.out.println("No range for " + target + " / " + A.dist(this, target));
            return false;
        }

        if (isRanged() && target.isUnderDarkSwarm()) {
            return false;
        }

        if (isBuilding() && isProtoss() && !isPowered()) {
            return false;
        }

        return true;
    }

    public boolean hasWeaponToAttackThisUnit(AUnit otherUnit) {
        // Enemy is GROUND unit
        if (otherUnit.isGroundUnit()) {
            return canAttackGroundUnits();
        }

        // Enemy is AIR unit
        else {
            return canAttackAirUnits();
        }
    }

    /**
     * Returns <b>true</b> if this unit can attack <b>targetUnit</b> in terms of both min and max range
     * conditions fulfilled.
     */
    public boolean hasWeaponRangeByGame(AUnit targetUnit) {
        return this.u.isInWeaponRange(targetUnit.u);
    }

    public boolean hasWeaponRangeToAttack(AUnit targetUnit, double extraMargin) {
        if (isBunker()) {
            return distToLessThan(targetUnit, 7);
        }

        if (!targetUnit.isDetected() || targetUnit.position() == null) {
            System.out.println("Undetected fix");
            return false;
        }

        WeaponType weaponAgainstThisUnit = weaponAgainst(targetUnit);
        if (weaponAgainstThisUnit == WeaponType.None) {
            return false;
        }

        double dist = distTo(targetUnit);
//        System.out.println("min = " + weaponAgainstThisUnit.minRange() / 32);
//        System.out.println("max = " + weaponAgainstThisUnit.maxRange() / 32);
        return (weaponAgainstThisUnit.minRange() / 32) <= dist
            && dist <= (weaponAgainstThisUnit.maxRange() / 32 + extraMargin);
    }

    /**
     * Returns weapon that would be used to attack given target. If no such weapon, then WeaponTypes.None will
     * be returned.
     */
    public WeaponType weaponAgainst(AUnit target) {
        if (target.isGroundUnit()) {
            return groundWeapon();
        }
        else {
            return airWeapon();
        }
    }

    public int damageAgainst(AUnit target) {
        WeaponType weapon = weaponAgainst(target);
        if (weapon == null) {
            return 0;
        }

        return weapon.damageAmount() * weapon.damageFactor();
    }

    public boolean distToLessThan(AUnit target, double maxDist) {
        if (target == null) {
            return false;
        }

        return distTo(target) <= maxDist;
    }

    public boolean distToMoreThan(AUnit target, double minDist) {
        if (target == null) {
            return false;
        }

        return distTo(target) >= minDist;
    }

    // === Getters ============================================= & setters

    /**
     * Returns true if given unit is currently (this frame) running from an enemy.
     */
    public boolean isRunning() {
//        return runningManager.isRunning() || (action() != null && action().isRunning());
        return runningManager.isRunning();
    }

    public boolean lastOrderMinFramesAgo(int minFramesAgo) {
        return AGame.now() - _lastActionReceived >= minFramesAgo;
    }

    /**
     * Returns battle squad object for military units or null for non military-units (or buildings).
     */
    public Squad squad() {
        if (squad == null && isOur()) {
//            NewUnitsToSquadsAssigner.possibleCombatUnitCreated(this);
//            A.printStackTrace("Should not be here");
        }

        return squad;
    }

    /**
     * Assign battle squad object for military units.
     */
    public void setSquad(Squad squad) {
        this.squad = squad;
    }

    /**
     * Returns AtlantisRunning object for this unit.
     */
    public ARunningManager runningManager() {
        return runningManager;
    }

    /**
     * Returns true if unit is in the middle of an attack and should not be interrupted otherwise
     * it would never shoot, just raise the weapon.
     */
//    public boolean isJustShooting() {
//        return DontInterruptStartedAttacks.shouldNotInterrupt(unit());
//    }

    /**
     * Returns the frames counter (time) when the unit had been issued any command.
     */
    public int lastUnitOrderTime() {
        return _lastActionReceived;
    }

    /**
     * Returns the frames counter (time) since the unit had been issued any command.
     */
    public int lastActionFramesAgo() {
        return AGame.now() - _lastActionReceived;
    }

    /**
     * Indicate that in this frame unit received some command (attack, move etc).
     */
    public AUnit setLastActionReceivedNow() {
        this._lastActionReceived = AGame.now();
        return this;
    }

    /**
     * Returns true if unit has anti-ground weapon.
     */
    public boolean canAttackGroundUnits() {
        return (boolean) cache.get(
            "canAttackGroundUnits",
            -1,
            () -> type().canAttackGround()
        );
    }

    /**
     * Returns true if unit has anti-air weapon.
     */
    public boolean canAttackAirUnits() {
        return (boolean) cache.get(
            "canAttackAirUnits",
            -1,
            () -> type().canAttackAir()
        );
    }

    public WeaponType airWeapon() {
        return type().airWeapon();
    }

    public WeaponType groundWeapon() {
        return type().groundWeapon();
    }

    /**
     * Returns number of frames unit has to wait between the shots.
     * E.g. for Dragoon this value will be always 30.
     */
    public int cooldownAbsolute() {
        if (canAttackGroundUnits()) {
            return groundWeapon().damageCooldown();
        }
        if (canAttackAirUnits()) {
            return airWeapon().damageCooldown();
        }
        return 0;
    }

    /**
     * Returns number of frames unit STILL has to wait before it can shoot again.
     * E.g. for Dragoon this value will vary between 0 and 30 inclusive.
     */
    public int cooldownRemaining() {
        if (canAttackGroundUnits()) {
            return groundWeaponCooldown();
        }
        if (canAttackAirUnits()) {
            return airWeaponCooldown();
        }
        return 0;
    }

    /**
     * Indicates that this unit should be running from given enemy unit.
     * If enemy parameter is null, it will try to determine the best run behavior.
     * If enemy is not null, it will try running straight from this unit.
     */
//    public boolean runFrom(HasPosition runFrom, double dist) {
//        return runningManager.runFrom(runFrom, dist);
//    }
//
//    public boolean runFrom() {
//        return runningManager.runFromCloseEnemies();
//    }

    /**
     * Returns <b>true</b> if this unit is supposed to "build" something. It will return true even if the unit
     * wasn't issued yet actual build order, but we've created ConstructionOrder and assigned it as a builder,
     * so it will return true.
     */
    public boolean isBuilder() {
        return AConstructionManager.isBuilder(this);
    }

    /**
     * If this unit is supposed to build something it will return ConstructionOrder object assigned to the
     * construction.
     */
    public ConstructionOrder constructionOrder() {
        return ConstructionRequests.constructionOrderFor(this);
    }

    /**
     * Returns true if this unit belongs to the enemy.
     */
    public boolean isEnemy() {
        if (u == null) {
            return true;
        }
//        if (isNeutral()) {
//            System.err.println("ooo");
//            return false;
//        }
        return AGame.getPlayerUs().isEnemy(player());
    }

    /**
     * Returns true if this unit belongs to us.
     */
    public boolean isOur() {
        if (u == null || player() == null) {
            return false;
        }

        return player().equals(AGame.getPlayerUs());
    }

    /**
     * Returns true if this unit is neutral (minerals, geysers, critters).
     */
    public boolean isNeutral() {
        return player().equals(AGame.getNeutralPlayer());
    }

    /**
     * Returns true if given building is able to build add-on like Terran Machine Shop.
     */
    public boolean canHaveAddon() {
        return type().canHaveAddon();
    }

    public int id() {
        return u.getID();
    }

    public String idWithHash() {
        return "#" + id();
    }

    public String typeWithHash() {
        return "#" + type();
    }

    // =========================================================
    // Method intermediates between BWMirror and Atlantis
    public Player player() {
        return u.getPlayer();
    }

    public int x() {
        return u.getX();
    }

    public int y() {
        return u.getY();
    }

    public boolean isCompleted() {
        return u.isCompleted();
    }

    public boolean exists() {
        return u.exists();
    }

    public boolean isConstructing() {
        return u.isConstructing();
    }

    public boolean hasAddon() {
        return u().getAddon() != null;
    }

    public int hp() {
        return u.getHitPoints() + shields();
    }

    public int maxHp() {
        return (int) cache.get(
            "maxHp",
            -1,
            () -> {
                int hp = type().maxHp() + maxShields();
                if (hp == 0 && !type().isSpell()) {
                    System.err.println("Max HP = 0 for");
                    System.err.println(this);
                }

                return hp > 0 ? hp : 1;
            }
        );
    }

    public boolean isResearching() {
        return u.isResearching();
    }

    public boolean isUpgradingSomething() {
        return u.isUpgrading();
    }

    public TechType whatIsResearching() {
        return u.getLastCommand().getTechType();
    }

    public UpgradeType whatIsUpgrading() {
        return u.getLastCommand().getUpgradeType();
    }

    public boolean isIdle() {
        return u.isIdle() || (u.getLastCommand() == null || u.getLastCommand().getType().equals(UnitCommandType.None));
    }

    public boolean isBusy() {
        return !isIdle();
    }

    private boolean ensnared() {
        return u.isEnsnared();
    }

    private boolean plagued() {
        return u.isPlagued();
    }

    /**
     * RETURNS TRUE IF UNIT IS VISIBLE ON MAP (NOT THAT UNIT IS NOT CLOAKED!).
     */
    public boolean isVisibleUnitOnMap() {
        return u != null && u.isVisible();
    }

    public boolean effVisible() {
        return isVisibleUnitOnMap() && (isDetected() || !effCloaked());
    }

    /**
     * Unit is effectvely cloaked and we can't attack it. Need to detect it first.
     */
    public boolean effCloaked() {
        return (!isDetected() || hp() == 0);

//        if ((!isCloaked() && !isBurrowed()) || ensnared() || plagued()) {
//            return false;
//        }

//        System.out.println(type() + " // " + isCloaked() + " // " + hp());

//        return true;
//        return hp() == 0;
//        return !unit().isDetected();
//        if (isOur()) {
//            return ;
//        }
//        effectivelyCloaked: Boolean = (
//                cloakedOrBurrowed
//                        && ! ensnared
//                        && ! plagued
//                        && (
//        if (isOurs) (
//                ! tile.enemyDetected
//                        && ! matchups.enemies.exists(_.orderTarget.contains(this))
//                        && ! With.bullets.all.exists(_.targetUnit.contains(this)))
//        else ! detected))
    }

    public boolean isDetected() {
        return u.isDetected() && hp() > 0;
    }

    public boolean notVisible() {
        return !u.isVisible();
    }

    public boolean isMiningOrExtractingGas() {
        return isGatheringMinerals() || isGatheringGas();
    }

    public boolean isGatheringMinerals() {
        return u.isGatheringMinerals();
    }

    public boolean isGatheringGas() {
        return u.isGatheringGas();
    }

    public boolean isCarryingMinerals() {
        return u.isCarryingMinerals();
    }

    public boolean isCarryingGas() {
        return u.isCarryingGas();
    }

    public boolean isCloaked() {
        return u.isCloaked() || u.isBurrowed();
    }

    public boolean isBurrowed() {
        if (u == null) {
            return true;
        }

        return u.isBurrowed();
    }

    public boolean isRepairing() {
        return u.isRepairing();
    }

    public int groundWeaponCooldown() {
        return u.getGroundWeaponCooldown();
    }

    public int airWeaponCooldown() {
        return u.getAirWeaponCooldown();
    }

    public boolean isAttackFrame() {
        return u.isAttackFrame();
    }

    public boolean isStartingAttack() {
        return u.isStartingAttack();
    }

    public boolean isStopped() {
        return u != null && (u.getLastCommand() == null || u.getLastCommand().getType() == UnitCommandType.Stop);
    }

    public boolean isStuck() {
        return u.isStuck();
    }

    public boolean isHoldingPosition() {
        return u.isHoldingPosition();
    }

    public boolean isPatrolling() {
        return u.isPatrolling();
    }

    public boolean isSieged() {
        return u.isSieged();
    }

    public boolean isUnsieged() {
        return !u.isSieged();
    }

    public boolean isUnderAttack(int inLastFrames) {
        // In-game solutions sucks ass badly
//        return u.isUnderAttack();

        if (_lastHitPoints.size() < inLastFrames) {
            return false;
        }

        return hp() < _lastHitPoints.get(inLastFrames - 1);
    }

    public boolean isUnderAttack() {
        // In-game solutions sucks ass badly
//        return u.isUnderAttack();

        return isUnderAttack(1);
    }

    public List<AUnitType> trainingQueue() {
        return (List<AUnitType>) AUnitType.convertToAUnitTypesCollection(u.getTrainingQueue());
    }

    public boolean isUpgrading() {
        return u.isUpgrading();
    }

    public List<AUnit> getLarva() {
        return (List<AUnit>) convertToAUnitCollection(u.getLarva());
    }

    public AUnit target() {
        if (u.getTarget() != null) {
            return AUnit.getById(u.getTarget());
        }

        return orderTarget();
    }

    public boolean hasTarget() {
        return u.getTarget() != null;
    }

    public boolean hasTargetPosition() {
        return u.getTargetPosition() != null;
    }

    public APosition targetPosition() {
        return APosition.create(u.getTargetPosition());
    }

    public AUnit orderTarget() {
        return u.getOrderTarget() != null ? AUnit.getById(u.getOrderTarget()) : null;
    }

    public AUnit buildUnit() {
        if (u == null) {
            return null;
        }
        return u.getBuildUnit() != null ? AUnit.getById(u.getBuildUnit()) : null;
    }

    public AUnitType buildType() {
        if (u == null) {
            return null;
        }
        return u.getBuildType() != null ? AUnitType.from(u.getBuildType()) : null;
    }

    public boolean isVulture() {
        return type().isVulture();
    }

    /**
     * Terran_SCV     - 4.92
     * Terran_Vulture - 6.4
     */
    public double maxSpeed() {
        return type().ut().topSpeed();
    }

    public boolean isTank() {
        return type().isTank();
    }

    public boolean isTankSieged() {
        return type().isTankSieged();
    }

    public boolean isTankUnsieged() {
        return type().isTankUnsieged();
    }

    public boolean isMorphing() {
        return u.isMorphing();
    }

    public boolean isMoving() {
        return u.isMoving();
    }

    public boolean isAttacking() {
        return u.isAttacking();
    }

    public boolean isAttackingOrMovingToAttack() {
        return hasValidTarget() && (
            isAttacking() || (action() != null && action().isAttacking())
        );
    }

    public boolean hasValidTarget() {
//        if (unit().isCombatUnit()) {
//            System.out.println(target() + " // alive=" + (target() != null ? target().isAlive() : 0));
//        }
        return target() != null && target().isAlive();
    }

    /**
     * Returns true for flying Terran building.
     */
    public boolean isLifted() {
        if (u == null) {
            return false;
        }
        return u.isLifted();
    }

    /**
     * Returns true if unit is inside bunker or dropship/shuttle.
     */
    public boolean isLoaded() {
        if (u == null) {
            return false;
        }
        return u.isLoaded();
    }

    public boolean isUnderDisruptionWeb() {
        if (u == null) {
            return false;
        }
        return u.isUnderDisruptionWeb();
    }

    public boolean isUnderDarkSwarm() {
        if (u == null) {
            return false;
        }
        return u.isUnderDarkSwarm();
    }

    public boolean isUnderStorm() {
        if (u == null) {
            return false;
        }
        return u.isUnderStorm();
    }

    public int getRemainingBuildTime() {
        return u().getRemainingBuildTime();
    }

    public int remainingResearchTime() {
        return u().getRemainingResearchTime();
    }

    public int remainingTrainTime() {
        return u().getRemainingTrainTime();
    }

    public int getTotalTrainTime() {
        return type().totalTrainTime();
    }

    public int remainingUpgradeTime() {
        return u().getRemainingUpgradeTime();
    }

    /**
     * Returns true if given position has land connection to given point.
     */
    public boolean hasPathTo(HasPosition point) {
        return u.hasPath(point.position());
    }

    public boolean hasPathTo(AUnit unit) {
        return u.hasPath(unit.position());
    }

    public boolean isTrainingAnyUnit() {
        return u.isTraining();
    }

    public boolean isBeingConstructed() {
        return u.isBeingConstructed();
    }

    public boolean isInterruptible() {
        return u.isInterruptible();
    }

    public UnitCommand getLastCommand() {
        return u.getLastCommand();
    }

    public boolean isCommand(UnitCommandType command) {
        return u.getLastCommand() != null && u.getLastCommand().getType().equals(command);
    }

    public Action action() {
        return unitAction;
    }

    // === Unit actions ========================================

    public boolean isUnitAction(Action constant) {
        return unitAction == constant;
    }

    public boolean isUnitActionAttack() {
        return unitAction == Actions.ATTACK_POSITION || unitAction == Actions.ATTACK_UNIT
            || unitAction == Actions.MOVE_ENGAGE;
    }

//    public boolean isUnitActionMove() {
//        return unitAction.name().startsWith("MOVE_");
//    }

    public boolean isUnitActionRepair() {
        return unitAction == Actions.REPAIR || unitAction == Actions.MOVE_REPAIR;
    }

    public AUnit setAction(Action unitAction) {
        this.unitAction = unitAction;
        setLastActionReceivedNow();
        rememberSpecificUnitAction(unitAction);
        return this;
    }

    public AUnit setAction(Action unitAction, TechType tech, APosition usedAt) {
        this._lastTech = tech;
        this._lastTechPosition = usedAt;
        SpellCoordinator.newSpellAt(usedAt, tech);

        return setAction(unitAction);
    }

    public AUnit setAction(Action unitAction, TechType tech, AUnit usedOn) {
        this._lastTech = tech;
        this._lastTechUnit = usedOn;

        if (ATech.isOffensiveSpell(tech)) {
            SpellCoordinator.newSpellAt(usedOn.position(), tech);
        }

        return setAction(unitAction);
    }

    private void rememberSpecificUnitAction(Action unitAction) {
        if (unitAction == null) {
            return;
        }
        cacheInt.set(
            "_last" + unitAction.name(),
            -1,
            A.now()
        );
    }

    public boolean lastActionMoreThanAgo(int framesAgo) {
        if (unitAction == null) {
            System.err.println("unitAction A null for " + this);
            return true;
        }

        return lastActionAgo(unitAction) >= framesAgo;
    }

    public boolean lastActionLessThanAgo(int framesAgo) {
        return lastActionAgo(unitAction) <= framesAgo;
    }

    public boolean lastActionMoreThanAgo(int framesAgo, Action unitAction) {
        if (unitAction == null) {
            System.err.println("unitAction B null for " + this);
            return true;
        }

        return lastActionAgo(unitAction) >= framesAgo;
    }

    public boolean lastActionLessThanAgo(int framesAgo, Action unitAction) {
        if (unitAction == null) {
            return false;
        }

        return lastActionAgo(unitAction) <= framesAgo;
    }

    public int lastActionAgo(Action unitAction) {
        Integer time = cacheInt.get("_last" + unitAction.name());

//        if (!cacheInt.isEmpty()) {
//            cacheInt.print("lastActionAgo", true);
//        }

        if (time == null) {
            return 999;
        }
        return A.now() - time;
    }

    public int lastActionFrame(Action unitAction) {
        Integer time = cacheInt.get("_last" + unitAction.name());

        if (time == null) {
            return 0;
        }
        return time;
    }

    // =========================================================

//    public boolean shouldApplyAntiGlitch() {
////        return (isAttacking() || isAttackFrame());
//        return getLastUnitOrderWasFramesAgo() >= 40 || isMoving() && getLastUnitOrderWasFramesAgo() >= 10;
//    }

    public boolean noCooldown() {
        return groundWeaponCooldown() <= 0 || airWeaponCooldown() <= 0;
    }

    public boolean hasCooldown() {
        return groundWeaponCooldown() > 0 || airWeaponCooldown() > 0;
    }

    public int scarabCount() {
        return u().getScarabCount();
    }

    public boolean isRepairerOfAnyKind() {
        return ARepairAssignments.isRepairerOfAnyKind(this);
    }

    public boolean isScout() {
        return AScoutManager.isScout(this);
    }

    public int getSpaceProvided() {
        return type().ut().spaceProvided();
    }

    public int spaceRequired() {
        return type().ut().spaceRequired();
    }

    public int spaceRemaining() {
        return u().getSpaceRemaining();
    }

//    public AUnit getCachedNearestMeleeEnemy() {
//        return _cachedNearestMeleeEnemy;
//    }

//    public void setCachedNearestMeleeEnemy(AUnit _cachedNearestMeleeEnemy) {
//        this._cachedNearestMeleeEnemy = _cachedNearestMeleeEnemy;
//    }

    public boolean lastStartedAttackMoreThanAgo(int framesAgo) {
        return A.ago(_lastStartedAttack) >= framesAgo;
    }

    public boolean lastStartedAttackLessThanAgo(int framesAgo) {
        return A.ago(_lastStartedAttack) <= framesAgo;
    }

    public boolean lastUnderAttackLessThanAgo(int framesAgo) {
        return A.ago(_lastUnderAttack) <= framesAgo;
    }

    public boolean lastUnderAttackMoreThanAgo(int framesAgo) {
        return A.ago(_lastUnderAttack) >= framesAgo;
    }

    public boolean lastAttackFrameMoreThanAgo(int framesAgo) {
        return A.ago(_lastAttackFrame) >= framesAgo;
    }

    public boolean lastAttackFrameLessThanAgo(int framesAgo) {
        return A.ago(_lastAttackFrame) <= framesAgo;
    }

    public int lastUnderAttackAgo() {
        return A.ago(_lastUnderAttack);
    }

    public boolean lastAttackOrderLessThanAgo(int framesAgo) {
        return A.ago(_lastAttackOrder) <= framesAgo;
    }

    public boolean lastAttackOrderMoreThanAgo(int framesAgo) {
        return A.ago(_lastAttackOrder) <= framesAgo;
    }

    public int lastAttackFrameAgo() {
        return A.ago(_lastAttackFrame);
    }

    public boolean lastFrameOfStartingAttackMoreThanAgo(int framesAgo) {
        return A.ago(_lastFrameOfStartingAttack) >= framesAgo;
    }

    public boolean lastFrameOfStartingAttackLessThanAgo(int framesAgo) {
        return A.ago(_lastFrameOfStartingAttack) <= framesAgo;
    }

    public int lastFrameOfStartingAttackAgo() {
        return A.ago(_lastFrameOfStartingAttack);
    }

    public int lastStartedAttackAgo() {
        return A.ago(_lastStartedAttack);
    }

    public int lastRetreatedAgo() {
        return A.ago(_lastRetreat);
    }

    public int lastStartedRunningAgo() {
        return A.ago(_lastStartedRunning);
    }

    public boolean lastStartedRunningMoreThanAgo(int framesAgo) {
        return A.ago(_lastStartedRunning) >= framesAgo;
    }

    public boolean lastStartedRunningLessThanAgo(int framesAgo) {
        return A.ago(_lastStartedRunning) <= framesAgo;
    }

    public boolean lastStoppedRunningLessThanAgo(int framesAgo) {
        return A.ago(_lastStoppedRunning) <= framesAgo;
    }

    public boolean lastStoppedRunningMoreThanAgo(int framesAgo) {
        return A.ago(_lastStoppedRunning) >= framesAgo;
    }

    public boolean hasNotMovedInAWhile() {
        return x() == _lastX && y() == _lastY;
    }

    public boolean isQuick() {
        return maxSpeed() >= 5.8;
    }

    public boolean isAccelerating() {
        return u().isAccelerating();
    }

    public boolean isBraking() {
        return u().isBraking();
    }

    public double getAngle() {
        return u().getAngle();
    }

    public boolean isFacingItsTarget() {
        return target() != null && target().isOtherUnitFacingThisUnit(this);
    }

    public boolean isOtherUnitFacingThisUnit(AUnit otherUnit) {
        if (otherUnit.hasNoU()) {
            return false;
        }

        Vector positionDifference = Vectors.fromPositionsBetween(this, otherUnit);
        Vector otherUnitLookingVector = Vectors.vectorFromAngle(otherUnit.getAngle(), positionDifference.length());

//        if (isFirstCombatUnit()) {
//            System.out.println("### ARE PARALLEL = " + (positionDifference.isParallelTo(otherUnitLookingVector)));
//            System.out.println(positionDifference + " // " + positionDifference.toAngle());
//            System.out.println(otherUnitLookingVector + " // " + otherUnitLookingVector.toAngle());
//        }

        return positionDifference.isParallelTo(otherUnitLookingVector);
    }

    /**
     * Inner BWAPI unit object, if null it means unit is not visible.
     */
    public boolean hasNoU() {
        return u == null;
    }

    public boolean isFirstCombatUnit() {
        return id() == Select.ourCombatUnits().first().id();
    }

    public Mission micro() {
        return squad().mission();
    }

    public int squadSize() {
        return squad().size();
    }

    public HasPosition squadCenter() {
        if (!hasSquad()) {
            return null;
        }
        return squad().center();
    }

    public int energy() {
        if (u == null) {
            System.out.println(this.getClass());
            System.out.println(this);
            System.out.println(u);
            System.out.println("---------");
        }
        return u.getEnergy();
    }

    public boolean energy(int min) {
        return energy() >= min;
    }

    /**
     * If anotherUnit is null it returns FALSE.
     * Returns TRUE if anotherUnit is the same unit as this unit (and it's alive and not null).
     */
    public boolean is(AUnit isTheSameAliveNotNullUnit) {
        return isTheSameAliveNotNullUnit != null && isTheSameAliveNotNullUnit.isAlive() && !this.equals(isTheSameAliveNotNullUnit);
    }

    public int cooldownPercent() {
        if (cooldownRemaining() <= 0 || cooldownAbsolute() == 0) {
            return 100;
        }

        return 100 * cooldownRemaining() / (cooldownAbsolute() + 1);
    }

    /**
     * Current mission object for this unit's squad.
     */
    public Mission mission() {
        return squad != null ? squad.mission() : null;
    }

    public boolean isQuickerOrSameSpeedAs(Units enemies) {
        return enemies.stream().noneMatch(u -> u.maxSpeed() > this.maxSpeed());
    }

    public boolean isQuickerOrSameSpeedAs(AUnit enemy) {
        return enemy.maxSpeed() < this.maxSpeed();
    }

    public boolean isSlowerThan(Units enemies) {
        return enemies.stream().anyMatch(u -> u.maxSpeed() > this.maxSpeed());
    }

    public boolean hasBiggerRangeThan(AUnit enemy) {
        if (isGroundUnit()) {
            return groundWeaponRange() > enemy.groundWeaponRange();
        }
        return airWeaponRange() > enemy.airWeaponRange();
    }

    public boolean hasBiggerRangeThan(Units enemies) {
        if (isGroundUnit()) {
            return enemies.stream().noneMatch(u -> u.groundWeaponRange() > this.groundWeaponRange());
        }
        else {
            return enemies.stream().noneMatch(u -> u.groundWeaponRange() > this.airWeaponRange());
        }
    }

    public boolean hasNothingInQueue() {
        return trainingQueue().size() <= 1;
    }

    public boolean canCloak() {
        return type().isCloakable() && !isCloaked();
    }

    public boolean is(AUnitType type) {
        return cacheBoolean.get(
//                "isType:" + type.id(),
            "isType:" + type.fullName(),
            -1,
            () -> type().is(type)
        );
    }

    public boolean is(AUnitType... types) {
        return type().is(types);
    }

    public boolean isTargetedBy(AUnit attacker) {
        return this.equals(attacker.target());
    }

    public boolean isArchon() {
        return is(AUnitType.Protoss_Archon);
    }

    public boolean isUltralisk() {
        return is(AUnitType.Zerg_Ultralisk);
    }

    public List<AUnit> loadedUnits() {
        List<AUnit> loaded = new ArrayList<>();
        for (Unit unit : u.getLoadedUnits()) {
            loaded.add(AUnit.getById(unit));
        }
        return loaded;
    }

    public int lastTechUsedAgo() {
        return lastActionAgo(Actions.USING_TECH);
    }

    public TechType lastTechUsed() {
        return _lastTech;
    }

    public APosition lastTechPosition() {
        return _lastTechPosition;
    }

    public AUnit lastTechUnit() {
        return _lastTechUnit;
    }

    public boolean hasCargo() {
        return u.getLoadedUnits().size() > 0;
    }

    public boolean hasFreeSpaceFor(AUnit passenger) {
        return spaceRemaining() >= passenger.spaceRequired();
    }

    public boolean hasAnyWeapon() {
        return !hasNoWeaponAtAll();
    }

    public boolean hasNoWeaponAtAll() {
        return cacheBoolean.get(
            "hasNoWeaponAtAll",
            -1,
            () -> !isBunker() && type().hasNoWeaponAtAll()
        );

//        if (type().isReaver() && scarabCount() == 0) {
//            return true;
//        }
    }

    public boolean recentlyAcquiredTargetToAttack() {
        if (target() == null) {
            return false;
        }

        int targetAcquiredAgo = lastTargetToAttackAcquiredAgo();

        return target().isAlive()
            && (
            (targetAcquiredAgo <= 70 && unit().woundPercent() <= 5 && !lastUnderAttackMoreThanAgo(30 * 6))
                || targetAcquiredAgo <= cooldownAbsolute() / 1.1
        );

//        return target().isAlive()
//                && (
//                    targetAcquiredAgo <= 4
////                    (targetAcquiredAgo <= 45 && unit().woundPercent() <= 5 && !lastUnderAttackMoreThanAgo(30 * 10))
////                    || targetAcquiredAgo <= cooldownAbsolute() / 1.1
//                );
    }

    public int lastTargetToAttackAcquiredAgo() {
        return A.ago(_lastTargetToAttackAcquired);
    }

    public boolean isAirUnitAntiAir() {
        return type().isAirUnitAntiAir();
    }

    public boolean isSquadScout() {
        if (squad() == null || A.isUms() || Count.ourCombatUnits() <= 7) {
            return false;
        }

        return equals(squad().getSquadScout()) && Missions.CONTAIN.equals(squadMission());
    }

    private Mission squadMission() {
        if (squad() == null) {
            return null;
        }

        return squad.mission();
    }

    public boolean isNotAttackableByRangedDueToSpell() {
        return isUnderDarkSwarm();
    }

    public boolean isStimmed() {
        return u.isStimmed();
    }

    public boolean isStasised() {
        return u.isStasised();
    }

    public boolean isLockedDown() {
        return u.isLockedDown();
    }

    public boolean isDefenseMatrixed() {
//        System.out.println(u.getDefenseMatrixTimer() + " // " + u.getDefenseMatrixPoints() + " // " + u.isDefenseMatrixed());
        return u.isDefenseMatrixed();
    }

    public int stimTimer() {
        return u.getStimTimer();
    }

    public double combatEvalAbsolute() {
        return ACombatEvaluator.absoluteEvaluation(this);
    }

    public double combatEvalRelative() {
        return ACombatEvaluator.relativeAdvantage(this);
    }

    public boolean isMedic() {
        return type().isMedic();
    }

    public boolean isTerranInfantry() {
        return type().isTerranInfantry();
    }

    public boolean isTerranInfantryWithoutMedics() {
        return type().isTerranInfantryWithoutMedics();
    }

    public boolean isLurker() {
        return type().isLurker();
    }

    public boolean hpLessThan(int min) {
        return hp() < min;
    }

    public boolean hpMoreThan(int max) {
        return hp() > max;
    }

    public boolean isSunken() {
        return type().isSunken();
    }

    // Approximate unit width (in tiles).
    public double size() {
        return (type().dimensionLeft() + type().dimensionRight() + 2) / 64.0;
    }

    public boolean isMarine() {
        return type().isMarine();
    }

    public boolean isFirebat() {
        return type().isFirebat();
    }

    public boolean isRepairable() {
        return (boolean) cache.get(
            "isRepairable",
            -1,
            () -> type().isMechanical() || isBuilding()
        );
    }

    public int totalCost() {
        return type().totalCost();
    }

    public AUnit loadedInto() {
        return (AUnit) cache.get(
            "loadedInto",
            10,
            () -> {
                if (!isLoaded()) {
                    return null;
                }

                for (AUnit transport : Select.ourOfType(
                    AUnitType.Terran_Bunker,
                    AUnitType.Terran_Dropship,
                    AUnitType.Protoss_Shuttle,
                    AUnitType.Zerg_Overlord
                ).list()) {
                    if (transport.hasCargo()) {
                        if (transport.loadedUnits().contains(this)) {
                            return transport;
                        }
                    }
                }

                throw new RuntimeException("Cant find loaded into");
            }
        );
    }

    public boolean isCombatBuilding() {
        return type().isCombatBuilding();
    }

    public boolean isMutalisk() {
        return type().isMutalisk();
    }

    public boolean isZealot() {
        return type().isZealot();
    }

    public boolean isZergling() {
        return type().isZergling();
    }

    public boolean isMissileTurret() {
        return type().isMissileTurret();
    }

    public boolean isScv() {
        return type().isScv();
    }

    public boolean isScienceVessel() {
        return type().isScienceVessel();
    }

    public boolean isCombatUnit() {
        return type().isCombatUnit();
    }

    public Selection enemiesNear() {
        return ((Selection) cache.get(
            "enemiesNear",
            3,
            () -> {
                if (unit().isOur()) {
                    return EnemyUnits.discovered()
                        .realUnitsButAllowBuildings()
                        .inRadius(15, this)
                        .exclude(this);
                }
                else if (unit().isEnemy()) {
//                        System.out.println("------- for enemy -------- ");
//                        System.out.println(Select.ourRealUnits().size());
//                        System.out.println(Select.ourRealUnits().inRadius(15, this).size());
                    return Select.ourRealUnits()
                        .inRadius(15, this)
                        .exclude(this);
                }
                else {
                    System.err.println("This is weird, should not be here");
                    System.err.println("This = " + this);
                    A.printStackTrace("This is weird, should not be here");
                    return Select.from(new Units());
                }
            }
        ));
    }

    public int enemiesNearInRadius(double maxDist) {
        return enemiesNear().inRadius(maxDist, this).count();
//        return ((Selection) cache.get(
//            "enemiesNearInRadius:" + maxDist,
//            5,
//            () -> enemiesNear().inRadius(maxDist, this)
//        ));
    }

    public int meleeEnemiesNearCount() {
        return cacheInt.get(
            "meleeEnemiesNearCount",
            3,
            () -> enemiesNear().melee().inRadius(2.7, this).size()
        );
    }

    public int friendsNearCount() {
        return cacheInt.get(
            "friendsNearCount",
            3,
            () -> friendsNear().size()
        );
    }

    public Selection friendsNear() {
        return ((Selection) cache.get(
            "friendsNear",
            5,
            () -> {
                if (unit().isOur()) {
                    return Select.ourRealUnits()
                        .inRadius(15, this)
                        .exclude(this);
                }
                else if (unit().isEnemy()) {
                    return EnemyUnits.discovered()
                        .inRadius(15, this)
                        .exclude(this);
                }
                else {
                    return Select.from(new Units());
                }
            }
        ));
    }

    public Selection friendsNearInRadiusSelect(double radius) {
        return friendsNear().inRadius(radius, this);
    }

    public int friendsNearInRadius(double radius) {
        return friendsNear().inRadius(radius, this).count();
    }

    public Selection allUnitsNear() {
        return ((Selection) cache.get(
            "allUnitsNear",
            5,
            () -> Select.all()
                .inRadius(15, this)
                .exclude(this)
        ));
    }

    public boolean hasMedicInRange() {
        return cacheBoolean.get(
            "hasMedicInRange",
            2,
            () -> Select.ourOfType(AUnitType.Terran_Medic).inRadius(2.1, this).notEmpty()
        );
    }

    public boolean isProtoss() {
        return type().isProtoss();
    }

    public boolean isTerran() {
        return type().isTerran();
    }

    public boolean isZerg() {
        return type().isZerg();
    }

    public boolean isPowered() {
        return u.isPowered();
    }

    private boolean isSpell() {
        return type().isSpell();
    }

    public boolean medicInHealRange() {
        return cacheBoolean.get(
            "medicInHealRange",
            5,
            () -> Select.ourOfType(AUnitType.Terran_Medic)
                .inRadius(2, this)
                .havingEnergy(12)
                .atLeast(1)
        );
    }

    public boolean isOverlord() {
        return cacheBoolean.get(
            "isOverlord",
            -1,
            () -> is(AUnitType.Zerg_Overlord)
        );
    }

    public boolean isDragoon() {
        return cacheBoolean.get(
            "isDragoon",
            -1,
            () -> is(AUnitType.Protoss_Dragoon)
        );
    }

    public boolean isHydralisk() {
        return cacheBoolean.get(
            "isHydralisk",
            -1,
            () -> is(AUnitType.Zerg_Hydralisk)
        );
    }

    public boolean isDT() {
        return cacheBoolean.get(
            "isDT",
            -1,
            () -> is(AUnitType.Protoss_Dark_Templar)
        );
    }

    public boolean isObserver() {
        return cacheBoolean.get(
            "isObserver",
            -1,
            () -> is(AUnitType.Protoss_Observer)
        );
    }

    public int meleeEnemiesNearCount(double maxDistToEnemy) {
        return cacheInt.get(
            "meleeEnemiesNear:" + maxDistToEnemy,
            2,
            () -> enemiesNear().melee().inRadius(maxDistToEnemy, this).size()
        );
    }

    public boolean isFoggedUnitWithUnknownPosition() {
        return this instanceof AbstractFoggedUnit && position() == null;
    }

    public boolean isHealthy() {
        return !isWounded();
    }

    public APosition makeLandable() {
        int maxRadius = 1;
        int currentRadius = 0;
        while (currentRadius <= maxRadius) {
            for (int dtx = -currentRadius; dtx <= currentRadius; dtx++) {
                for (int dty = -currentRadius; dty <= currentRadius; dty++) {
                    if (
                        dtx == -currentRadius || dtx == currentRadius
                            || dty == -currentRadius || dty == currentRadius
                    ) {
                        APosition position = this.translateByTiles(dtx, dty);
                        if (u.canLand(position.toTilePosition())) {
                            return position;
                        }
                    }
                }
            }

            currentRadius++;
        }

        return null;
    }

    public boolean shieldDamageAtMost(int maxDamage) {
        if (!We.protoss()) {
            return false;
        }

        return shields() + maxDamage >= maxShields();
    }

    public boolean shieldDamageAtLeast(int minDamage) {
        if (!We.protoss()) {
            return false;
        }

        return shields() + minDamage <= maxShields();
    }

    public boolean targetPositionAtLeastAway(double minTiles) {
        return targetPosition() != null && targetPosition().distToMoreThan(this, minTiles);
    }

//    public void paintInfo(String text) {
//        APainter.paintTextCentered(this, text, Color.White);
//    }
//
//    public void paintInfo(String text, Color color, double dty) {
//        APainter.paintTextCentered(this, text, color, 0, dty / 32.0);
//    }

    public void addLog(String message) {
        if (!log.lastMessageWas(message)) {
            log.addMessage(message);
        }
    }

    public Log log() {
        return log;
    }

    public int friendlyZealotsNearCount(double maxDist) {
        return friendsNear().ofType(AUnitType.Protoss_Zealot).inRadius(maxDist, this).count();
    }

    public int enemyZealotsNearCount(double maxDist) {
        return enemiesNear().ofType(AUnitType.Protoss_Zealot).inRadius(maxDist, this).count();
    }

    public boolean isNearEnemyBuilding() {
        AUnit nearestEnemyBuilding = enemiesNear().buildings().nearestTo(this);

        if (nearestEnemyBuilding != null) {
            return nearestEnemyBuilding.distToLessThan(this, 9);
        }

        return false;
    }

    public boolean isMissionSparta() {
        if (mission() == null) {
            return false;
        }
        return mission().equals(Missions.SPARTA);
    }

    public boolean isMissionDefend() {
        if (mission() == null) {
            return false;
        }
        return mission().equals(Missions.DEFEND);
    }

    public boolean isMissionAttack() {
        if (mission() == null) {
            return false;
        }
        return mission().isMissionAttack();
    }

    public boolean isMissionContain() {
        if (mission() == null) {
            return false;
        }
        return mission().isMissionContain();
    }

    public boolean isReaver() {
        return type().isReaver();
    }

    public boolean recentlyMoved() {
        return action().isMoving() && lastActionLessThanAgo(40);
    }

    public boolean recentlyMoved(int framesAgo) {
        return action().isMoving() && lastActionLessThanAgo(framesAgo);
    }

    public boolean idIsOdd() {
        return id() % 2 > 0;
    }

    public AUnit nearestEnemy() {
        return (AUnit) cache.get(
            "nearestEnemy",
            2,
            () -> enemiesNear().canAttack(this, 5).nearestTo(this)
        );
    }

    public double nearestEnemyDist() {
        AUnit nearestEnemy = nearestEnemy();

        if (nearestEnemy != null) {
            return distTo(nearestEnemy);
        }

        return 999;
    }

    public Selection friendsInRadius(double radius) {
        return friendsNear().inRadius(radius, this);
    }

    public int friendsInRadiusCount(double radius) {
        return friendsNear().inRadius(radius, this).count();
    }

    public double distToSquadCenter() {
        if (squad == null || squad.center() == null) {
            return 0;
        }

        return (double) cache.get(
            "distToSquadCenter",
            5,
            () -> squad.center().distTo(this)
        );
    }

    public boolean hasSquad() {
        return squad != null;
    }

    public double squadRadius() {
        if (this.squad == null) {
            return 988;
        }
        return CohesionAssurance.squadMaxRadius(this.squad);
    }

    public boolean outsideSquadRadius() {
        return (boolean) cache.get(
            "outsideSquadRadius",
            2,
            () -> distToSquadCenter() > squadRadius()
        );
    }

    public boolean isProtector() {
        return ARepairAssignments.isProtector(this);
    }

    public boolean kitingUnit() {
        return isDragoon() || isVulture();
    }

    public double distToFocusPoint() {
        Mission mission = mission();
        if (mission == null) {
            return 0;
        }

        AFocusPoint focusPoint = mission.focusPoint();
        if (focusPoint == null) {
            return 0;
        }

        return focusPoint.distTo(this);
    }

    public boolean lastPositioningActionMoreThanAgo(int minFramesAgo) {
        return lastActionMoreThanAgo(minFramesAgo, Actions.MOVE_FORMATION)
            && lastActionMoreThanAgo(minFramesAgo, Actions.MOVE_ENGAGE);
    }

}
