package atlantis;

import atlantis.position.HasPosition;
import atlantis.units.AUnit;
import atlantis.units.AUnitType;
import atlantis.units.select.Select;

public class CameraManager {
    
    protected static int SCREEN_WIDTH = 640;
    protected static int SCREEN_HEIGHT = 480;
    protected static boolean focusCameraOnFirstCombatUnit = true;

    // =========================================================

    public static void update() {
        if (AGame.isUms()) {
            AUnit cameraUnit = centerCameraOnUnit();
            if (AGame.timeSeconds() <= 1 || CameraManager.isFocusCameraOnUnit()) {
                CameraManager.centerCameraOn(cameraUnit);
            }
        }
    }

    // =========================================================

    private static AUnit centerCameraOnUnit() {
//        return null;
        AUnit cameraUnit;

        cameraUnit = Select.ourOfType(AUnitType.Protoss_High_Templar).groundUnits().first();
        if (cameraUnit != null) {
//            if (GameSpeed.gameSpeed <= 0 && GameSpeed.frameSkip >= 1) {
//                GameSpeed.changeSpeedTo(2);
//            }
            return cameraUnit;
        }

        return Select.ourCombatUnits().groundUnits().first();
    }

    public static void centerCameraOn(HasPosition position) {
        if (position == null) {
            return;
        }

        Atlantis.game().setScreenPosition(
                position.position().translateByPixels(-SCREEN_WIDTH / 2, -SCREEN_HEIGHT * 3 / 7)
        );
    }

    public static void toggleFocusCameraOnFirstCombatUnit() {
        focusCameraOnFirstCombatUnit = !focusCameraOnFirstCombatUnit;
    }

    public static boolean isFocusCameraOnUnit() {
        return focusCameraOnFirstCombatUnit;
    }
}
