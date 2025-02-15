package atlantis.map.position;

import atlantis.Atlantis;
import atlantis.map.AChoke;
import atlantis.map.AMap;
import atlantis.map.ARegion;
import atlantis.map.Regions;
import atlantis.units.AUnit;
import bwapi.Point;
import bwapi.Position;
import bwapi.WalkPosition;

import java.util.Objects;

/**
 * Atlantis uses wrapper for BWAPI  classes which can't extended due to private constructors.
 * <br /><br />
 * I've decided to implement a solution which allows to use the .jar of BWMirror library, because from
 * my experience it turns out, that it's extremely tedious to upgrade Atlantis to use newer version of
 * BWMirror if you work with the source code rather than the .jar library release.
 * <br /><br />
 * <b>APosition</b> class contains numerous helper methods, but if you think some methods are missing
 * you can create them here or reference original Position class via p() method. 
 * <br /><br />
 * <b>Notice:</b> whenever possible, try to use APosition in place of Position.
 */
public class APosition extends Position implements HasPosition, Comparable<Point<Position>> {

//    private static final Map<Object, APosition> instances = new HashMap<>();
    
    private final Position p;
    
    // =========================================================

    public APosition(APosition position) {
        super(position.getX(), position.getY());
        this.p = position.p;
    }

    private APosition(HasPosition p) {
        super(p.position().getX(), p.position().getY());
        this.p = p.position();
    }

    public APosition(int pixelX, int pixelY) {
        super(pixelX, pixelY);
        this.p = new Position(pixelX, pixelY);
    }

    private APosition(Position p) {
        super(p.getX(), p.getY());
        this.p = p;
    }

    /**
     * <b>Notice:</b> whenever possible, use APosition instead of Position.
     *
     * Atlantis uses wrapper for bridge native classes.<br />
     * <b>APosition</b> class contains numerous helper methods, but if you think some methods are missing
     * you can create them here or reference original Position class via p() method.
     */
    public static APosition create(Object p) {
//        if (instances.containsKey(p)) {
//            return instances.get(p);
//        }
//        else {
            APosition position = null;

            if (p instanceof HasPosition) {
                position = new APosition(((HasPosition) p).position());
            } else if (p instanceof Position) {
                position = new APosition((Position) p);
            } else if (p instanceof WalkPosition) {
                position = new APosition(((WalkPosition) p).toPosition());
            } else {
                throw new RuntimeException("APosition::create invalid param " + p);
            }

//            instances.put(p, position);
            return position;
//        }
    }
    
    /**
     * <b>Notice:</b> whenever possible, use APosition instead of Position.
     *
     * Atlantis uses wrapper for bridge native classes.<br />
     * <b>APosition</b> class contains numerous helper methods, but if you think some methods are missing
     * you can create them here or reference original Position class via p() method.
     *
     * @return APosition object from (build) tile coordinates (32 pixels = 1 tile).
     */
    public static APosition create(int tileX, int tileY) {
        return new APosition(tileX * 32, tileY * 32);
    }

    public static APosition createFromPixels(int px, int py) {
        return new APosition(px, py);
    }

    // =========================================================

    /**
     * APosition class should be used always instead of Position when possible.
     */
    public Position rawP() {
        return p;
    }

    public APosition position() {
        return this;
    }

    @Override
    public int x() {
        return p.x;
    }

    @Override
    public int y() {
        return p.y;
    }

    /**
     * Returns distance from one position to other in build tiles. One build tile equals to 32 pixels. Usage
     * of build tiles instead of pixels is preferable, because it's easier to imagine distances if one knows
     * building dimensions.
     */
    public double distTo(Position position) {
        return PositionUtil.distanceTo(p, position);
    }

    public double distTo(APosition position) {
        return PositionUtil.distanceTo(p, position);
    }

    public double groundDistanceTo(HasPosition position) {
        return PositionUtil.groundDistanceTo(this, position.position());
    }

    public double distTo(AChoke choke) {
        return PositionUtil.distanceTo(p, choke);
    }

    /**
     * Returns distance from one position to other in build tiles. One build tile equals to 32 pixels. Usage
     * of build tiles instead of pixels is preferable, because it's easier to imagine distances if one knows
     * building dimensions.
     */
    public double distTo(AUnit unit) {
        return PositionUtil.distanceTo(p, unit);
    }

    /**
     * Paint it on screen for testing.
     */
//    default void paintIt(int widthTiles, int heightTiles) {
//        paintIt(widthTiles, heightTiles, Color.Red);
//    }

    /**
     * Paint it on screen for testing.
     */
//    default void paintIt(int widthTiles, int heightTiles, Color color) {
//        APainter.paintRectangle(this, widthTiles, heightTiles, color);
//    }

    // =========================================================

    /**
     * Ensures that position's [x,y] are valid map coordinates.
     */
//    @Override
    public APosition makeValid() {
//        p = p.makeValid();

        boolean somethingChanged = false;
        int px = p.getX();
        int py = p.getY();

        if (px <= 1) {
            px = 1;
            somethingChanged = true;
        }
        else if (px >= 32 * AMap.getMapWidthInTiles()) {
            px = 32 * AMap.getMapWidthInTiles() - 1;
            somethingChanged = true;
        }

        if (py < 1) {
            py = 1;
            somethingChanged = true;
        }
        else if (py >= 32 * AMap.getMapHeightInTiles()) {
            py = 32 * AMap.getMapHeightInTiles() - 1;
            somethingChanged = true;
        }

        if (somethingChanged) {
//            p = new HasPosition(px, py);
            return new APosition(px, py);
        }
        else {
            return this;
        }
    }

    public APosition makeValidFarFromBounds() {
        return makeValidFarFromBounds(PIXELS_TO_MAP_BOUNDARIES_CONSIDERED_CLOSE);
    }

    /**
     * Ensures that position's [x,y] are valid map coordinates and are "quite" far from map boundaries.
     */
    public APosition makeValidFarFromBounds(int maxPixelsAwayFromEdges) {
        boolean somethingChanged = false;
        int px = p.getX();
        int py = p.getY();

        // =========================================================

        if (px < maxPixelsAwayFromEdges) {
            px = maxPixelsAwayFromEdges;
            somethingChanged = true;
        }
        else if (px > (32 * AMap.getMapWidthInTiles() - maxPixelsAwayFromEdges)) {
            px = 32 * AMap.getMapWidthInTiles() - maxPixelsAwayFromEdges;
            somethingChanged = true;
        }

        if (py < maxPixelsAwayFromEdges) {
            py = maxPixelsAwayFromEdges;
            somethingChanged = true;
        }
        else if (py > (32 * AMap.getMapHeightInTiles() - maxPixelsAwayFromEdges)) {
            py = 32 * AMap.getMapHeightInTiles() - maxPixelsAwayFromEdges;
            somethingChanged = true;
        }

        // =========================================================

        if (somethingChanged) {
            return new APosition(px, py);
        }
        else {
            return this;
        }
    }

    // =========================================================

    @Override
    public String toString() {
        return "(" + tx() + ", " + ty() + ")";
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.p);
        return hash;
    }
    
//    public int compareTo(Position o) {
    @Override
    public int compareTo(Point o) {
        int compare = Integer.compare(getX(), o.getX());
        if (compare == 0) {
            compare = Integer.compare(getY(), o.getY());
        }
        return compare;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
//        if (!(obj instanceof APosition) && !(obj instanceof Position)) {
//            return false;
//        }
        if (!(obj instanceof HasPosition)) {
            return false;
        }
        
        int otherX = ((HasPosition) obj).x();
        int otherY = ((HasPosition) obj).y();
        return this.getX() == otherX && this.getY() == otherY;
    }
    
    /**
     * Returns true if given position has land connection to given point.
     */
    public boolean hasPathTo(APosition point) {
        return Atlantis.game().hasPath(this, point);
    }

    public boolean isCloseToMapBounds() {
        int px = p.getX();
        int py = p.getY();
        
        if (px < PIXELS_TO_MAP_BOUNDARIES_CONSIDERED_CLOSE) {
            return true;
        }
        else if (px >= (32 * AMap.getMapWidthInTiles() - PIXELS_TO_MAP_BOUNDARIES_CONSIDERED_CLOSE)) {
            return true;
        }
        
        if (py < PIXELS_TO_MAP_BOUNDARIES_CONSIDERED_CLOSE) {
            return true;
        }
        else return py >= (32 * AMap.getMapHeightInTiles() - PIXELS_TO_MAP_BOUNDARIES_CONSIDERED_CLOSE);
    }    

    /**
     * Return region object for this position.
     */
    public ARegion region() {
        return Regions.getRegion(this);
    }

}
