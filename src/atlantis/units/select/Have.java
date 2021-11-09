package atlantis.units.select;

import atlantis.units.AUnitType;

public class Have {

    public static boolean armory() {
        return Count.ofType(AUnitType.Terran_Armory) > 0;
    }

    public static boolean base() {
        return Select.mainBase() != null;
    }
}
