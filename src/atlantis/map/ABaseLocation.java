package atlantis.map;

import atlantis.position.APosition;
import atlantis.position.HasPosition;
import bwta.BaseLocation;

public class ABaseLocation implements HasPosition {

    private BaseLocation baseLocation;

    public static ABaseLocation create(BaseLocation baseLocation) {
        ABaseLocation aBaseLocation = new ABaseLocation();
        aBaseLocation.baseLocation = baseLocation;

        return aBaseLocation;
    }


    // =========================================================

    @Override
    public APosition position() {
        return APosition.create(baseLocation.getPosition());
    }

    @Override
    public int x() {
        return position().getX();
    }

    @Override
    public int y() {
        return position().getY();
    }

    // =========================================================

    public boolean isIsland() {
        return baseLocation.isIsland();
    }

    public boolean isMineralOnly() {
        return baseLocation.isMineralOnly();
    }

    public boolean isStartLocation() {
        return baseLocation.isStartLocation();
    }


}
