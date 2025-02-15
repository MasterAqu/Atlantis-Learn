package atlantis.units;

import atlantis.map.position.APosition;

/**
 * Stores information about units in order to retrieve them when they are out of sight
 */
public class FoggedUnit extends AbstractFoggedUnit {

    // =========================================================

    public static AbstractFoggedUnit from(AUnit unit) {
        _lastAUnit = unit;

        AbstractFoggedUnit foggedUnit = (AbstractFoggedUnit) all.get(unit.id());
        if (foggedUnit != null) {
            return foggedUnit;
        }

        return new FoggedUnit(unit);
    }

    protected FoggedUnit(AUnit unit) {
        super(unit);
    }

//    protected FoggedUnit(AUnit unit) {
//        this._id = unit.id();
//        this.aUnit = unit;
//        this.update(unit);
//
//        all.put(unit.id(), this);
//    }

    // =========================================================

    @Override
    public APosition position() {
//        if (_lastAUnit != null && _lastAUnit.isVisibleUnitOnMap()) {
//            return _lastAUnit.position();
//        }
        return _position;
    }

    @Override
    public int id() {
        return _id;
    }

    @Override
    public int x() {
        return position().getX();
    }

    @Override
    public int y() {
        return position().getY();
    }

    @Override
    public boolean isDetected() {
        return true;
    }

    // =========================================================

    @Override
    public String toString() {
        return "FoggedUnit{#" + _id + " " + _lastType + " at " + _position + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        AUnit that = (AUnit) o;
//        return _position.distToLessThan(that.position(), 0.05);
        return _id == that.id();
    }

    @Override
    public int hashCode() {
        return _id;
    }

    @Override
    public int compareTo(AUnit o) {
        if (o == null) {
            return 1;
        }

        return o.compareTo(aUnit);
    }

}
