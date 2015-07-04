package atlantis.constructing.position;

import jnibwapi.Position;
import jnibwapi.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import atlantis.Atlantis;
import atlantis.wrappers.SelectUnits;

public abstract class AbstractBuildPositionFinder {

	// =========================================================
	// Hi-level methods

	/**
	 * Returns true if game says it's possible to build given building at this position.
	 */
	protected static boolean canPhysicallyBuildHere(UnitType building, Position position) {
		return Atlantis.getBwapi().canBuildHere(position, building, false);
	}

	/**
	 * Returns true if any other building is too close to this building or if two buildings would overlap add-on place
	 * of another. Buildings can be stacked, but it needs to be done properly e.g. Supply Depots could be stacked.
	 */
	protected static boolean otherBuildingsTooClose(UnitType building, Position position) {
		// int veryCloseBuildings = 0;
		for (Unit otherBuilding : SelectUnits.ourBuildings().list()) {
			int status = areTwoBuildingsTooClose(otherBuilding, position, building);
			// veryCloseBuildings++;

			if (status >= STATUS_BUILDINGS_ADDON_COLLIDE) {
				return true;
			}
		}

		// No collisions detected
		return false;
	}

	private static final int STATUS_BUILDINGS_OK = 100;
	private static final int STATUS_BUILDINGS_STICK = 200;
	private static final int STATUS_BUILDINGS_ADDON_COLLIDE = 300;

	private static int areTwoBuildingsTooClose(Unit otherBuilding, Position position, UnitType building) {
		double edgeToEdgeDistance = getEdgeToEdgeDistanceBetween(otherBuilding, position, building);
		System.out.println("   --- Dist bitw " + otherBuilding.getType().getName() + " and " + building.getName()
				+ " is " + edgeToEdgeDistance);

		// If buildings are dangerously close
		if (edgeToEdgeDistance < 0.1) {

			// Allow stacking of depots
			if (building.isType(UnitTypes.Terran_Supply_Depot) && otherBuilding.isType(UnitTypes.Terran_Supply_Depot)) {
				return STATUS_BUILDINGS_STICK;
			}
		}

		return STATUS_BUILDINGS_OK;
	}

	// =========================================================
	// Lo-level methods

	/**
	 * Returns edge-to-edge distance (in build tiles) between one existing building and the other one not yet existing.
	 */
	protected static double getEdgeToEdgeDistanceBetween(Unit building, Position positionForNewBuilding,
			UnitType newBuildingType) {
		int targetRight = positionForNewBuilding.getPX() + newBuildingType.getDimensionRight();
		int targetLeft = positionForNewBuilding.getPX() - newBuildingType.getDimensionLeft();
		int targetTop = positionForNewBuilding.getPY() - newBuildingType.getDimensionUp();
		int targetBottom = positionForNewBuilding.getPY() + newBuildingType.getDimensionDown();

		int xDist = building.getLeftPixelBoundary() - (targetRight + 1);
		if (xDist < 0) {
			xDist = targetLeft - (building.getRightPixelBoundary() + 1);
			if (xDist < 0) {
				xDist = 0;
			}
		}
		int yDist = building.getTopPixelBoundary() - (targetBottom + 1);
		if (yDist < 0) {
			yDist = targetTop - (building.getBottomPixelBoundary() + 1);
			if (yDist < 0) {
				yDist = 0;
			}
		}
		return new Position(0, 0).distanceTo(new Position(xDist, yDist));
	}

}
