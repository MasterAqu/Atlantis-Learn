package atlantis.map;

import atlantis.position.APosition;
import atlantis.position.HasPosition;
import bwta.Region;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ARegion implements HasPosition {

    private Region region;

    public static ARegion create(Region region) {
        ARegion aRegion = new ARegion();
        aRegion.region = region;

        return aRegion;
    }

    // =========================================================

    @Override
    public APosition position() {
        return APosition.create(region.getCenter());
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ARegion)) return false;
        ARegion aRegion = (ARegion) o;
        return region.equals(aRegion.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region);
    }

    // =========================================================

    public APosition getCenter() {
        return APosition.create(region.getCenter());
    }

    public List<AChoke> chokes() {
        return region.getChokepoints().stream().map(AChoke::create).collect(Collectors.toList());
    }

    public List<ABaseLocation> getBaseLocations() {
        return region.getBaseLocations().stream().map(ABaseLocation::create).collect(Collectors.toList());
    }

    public boolean isReachable(ARegion otherRegion) {
        return region.isReachable(otherRegion.region);
    }

    public List<ARegion> getReachableRegions() {
        return region.getReachableRegions().stream().map(ARegion::create).collect(Collectors.toList());
    }

}
