package atlantis.constructing;

import atlantis.Atlantis;
import atlantis.AtlantisGame;
import atlantis.constructing.position.AbstractPositionFinder;
import atlantis.units.AUnit;
import atlantis.units.AUnitType;
import atlantis.util.PositionUtil;
import bwapi.Position;
import bwapi.TilePosition;

import bwapi.UnitType;

public class AtlantisBuilderManager {

    public static void update(AUnit builder) {
        if (builder == null) {
            System.err.println("builder null in ABM.update()");
            return;
        }

        // Don't disturb builder that are already constructing
        if (builder.isConstructing() || builder.isMorphing()) {
            return;
        }

        handleConstruction(builder);
    }

    // =========================================================
    
    private static void handleConstruction(AUnit builder) {
        ConstructionOrder constructionOrder = AtlantisConstructingManager.getConstructionOrderFor(builder);
        if (constructionOrder != null) {

            // Construction HASN'T STARTED YET, we're probably not even at the
            // required place
            if (constructionOrder.getStatus() == ConstructionOrderStatus.CONSTRUCTION_NOT_STARTED) {
                travelToConstruct(builder, constructionOrder);
            } // Construction is IN PROGRESS
            else if (constructionOrder.getStatus() == ConstructionOrderStatus.CONSTRUCTION_IN_PROGRESS) {
                // Do nothing
            } // Construction has FINISHED
            else if (constructionOrder.getStatus() == ConstructionOrderStatus.CONSTRUCTION_FINISHED) {
                // Do nothing
            }
        } else {
            System.err.println("constructionOrder null for " + builder);
        }
    }

    private static void travelToConstruct(AUnit builder, ConstructionOrder constructionOrder) {
        //TODO: check possible confusion with Position and TilePosition here
        Position buildPosition = constructionOrder.getPositionToBuild();
        AUnitType buildingType = constructionOrder.getBuildingType();

        if (builder == null) {
            throw new RuntimeException("Builder empty");
        }
        if (buildPosition == null) {
            System.err.println("buildPosition is null (travelToConstruct " + buildingType + ")");
            constructionOrder.cancel();
            return;
        }

        // Move builder to the build position
        //TODO: check possible confusion with Position and TilePosition here
        buildPosition = PositionUtil.translate(buildPosition, buildingType.getTileWidth() * 16, buildingType.getTileHeight() * 16);
        if (!builder.isMoving() && !builder.isConstructing() && PositionUtil.distanceTo(builder.getPosition(), buildPosition) > 0.15) {
            builder.move(buildPosition);
        } // AUnit is already at the build position, issue build order
        // If we can afford to construct this building exactly right now, issue build order which should
        // be immediate as unit is standing just right there
        else if (AtlantisGame.canAfford(buildingType.getMineralPrice(), buildingType.getGasPrice())) {
            if (!AbstractPositionFinder.canPhysicallyBuildHere(builder, buildingType, buildPosition)) {
                buildPosition = constructionOrder.findNewBuildPosition();
            }

            if (buildPosition != null && !builder.isConstructing()) {
                TilePosition buildTilePosition = buildPosition.toTilePosition();
                builder.build(buildingType, buildTilePosition);
//                if (Atlantis.getBwapi().canBuildHere(buildTilePosition, buildingType)) {
//                    System.out.println(buildTilePosition + " / " + buildingType);
//                    System.out.println(Atlantis.getBwapi().canBuildHere(buildTilePosition, buildingType));
//                    builder.build(buildingType, buildTilePosition);
//                }
//                else {
//                    System.err.println("canBuildHere for " + buildTilePosition + " / " + buildingType 
//                            + " returned false!");
//                }
            }
        }
    }

}
