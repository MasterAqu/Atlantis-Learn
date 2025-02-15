package atlantis.production.dynamic.protoss;

import atlantis.game.A;
import atlantis.game.AGame;
import atlantis.information.decisions.Decisions;
import atlantis.information.tech.ATech;
import atlantis.production.dynamic.ADynamicTech;
import atlantis.units.select.Count;
import bwapi.TechType;
import bwapi.UpgradeType;

import static bwapi.UpgradeType.Singularity_Charge;


public class ProtossDynamicTech extends ADynamicTech {

    public static boolean update() {
        if (A.notNthGameFrame(71)) {
            return false;
        }

        if (Count.dragoons() >= 7) {
            return handleUpgrade(Singularity_Charge);
        }
        if (Count.zealots() >= 10) {
            return handleUpgrade(UpgradeType.Leg_Enhancements);
        }
        if (Count.ourCombatUnits() >= 16) {
            return handleUpgrade(UpgradeType.Protoss_Ground_Weapons);
        }
        else if (Count.ourCombatUnits() >= 25) {
            return handleUpgrade(UpgradeType.Protoss_Ground_Armor);
        }

        return false;
    }

}
