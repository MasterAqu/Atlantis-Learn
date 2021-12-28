package atlantis;

import atlantis.combat.squad.alpha.Alpha;
import atlantis.enemy.EnemyInformation;
import atlantis.env.Env;
import bwapi.Game;

import static atlantis.Atlantis.game;

public class GameSpeed {

    private static boolean isPaused = false; // On PauseBreak a pause mode can be enabled

    /**
     * Game speed. Lower is faster. 0 is fastest, 20 is about normal game speed.
     * In game you can use buttons -/+ to change the game speed.
     */
    public static int gameSpeed;

    /**
     * By skipping rendering of game frames, we can make the game much quicker, regardless of the game speed.
     * Value 3 means we render every 3th game frame, skipping 67% of total rendering.
     */
    public static int frameSkip;

//    private static final int NORMAL_GAME_SPEED = 10;
    private static final int NORMAL_GAME_SPEED = 0;
    private static final int NORMAL_FRAME_SKIP = 0;
//    private static final int NORMAL_GAME_SPEED = 0;
//    private static final int NORMAL_FRAME_SKIP = 500;

    private static final int DYNAMIC_SLOWDOWN_FRAME_SKIP = 0;
    private static final int DYNAMIC_SLOWDOWN_GAME_SPEED = 5;

    // DYNAMIC SLOWDOWN - game speed adjustment, fast initially, slow down when there's fighting - see AtlantisConfig
    private static boolean dynamicSlowdownIsAllowed = false;
    private static boolean dynamicSlowdownIsActive = false;

    public static boolean oneTimeSlowdownUsed = false;

    // Last time unit has died; when unit dies, game slows down
//    private static int dynamicSlowdown_lastTimeUnitDestroyed = 0;

    // =========================================================

    public static void init() {
        if (Env.isParamTweaker()) {
            Atlantis.game().setLocalSpeed(0);
            Atlantis.game().setFrameSkip(500);
            return;
        }

        GameSpeed.disallowToDynamicallySlowdownGameOnFirstFighting();
        Atlantis.game().setLocalSpeed(NORMAL_GAME_SPEED);
        Atlantis.game().setFrameSkip(NORMAL_FRAME_SKIP);
    }

    public static void checkIfNeedToSlowDown() {
//        if (true) { return; }

        if (!oneTimeSlowdownUsed && AGame.now() <= 1) {
            Atlantis.game().setGUI(frameSkip <= 30);
        }

        if (
                !oneTimeSlowdownUsed
                        && AGame.now() > 60
                        && gameSpeed == 0
                        && frameSkip >= 30
//                        && Count.ourCombatUnits() >= 5
//                        && Select.ourOfType(AUnitType.Terran_Science_Vessel).atLeast(1)
//                        && Select.enemyCombatUnits().atLeast(2)
//                        && Select.enemyCombatUnits().atLeast(2)
                        && EnemyInformation.discoveredEnemyBase()
                        && Alpha.get().getSquadScout() != null
                        && Alpha.get().getSquadScout().isWounded()
//                        && Missions.isGlobalMissionContain()
        ) {
            oneTimeSlowdownUsed = true;
            System.out.println("Slow down to " + currentSpeedAndFrameSkip());
            pauseGame();
            changeSpeedTo(0);
            changeFrameSkipTo(0);
//            CameraManager.centerCameraNowOnSquadCenter();
            unpauseGame();
        }
    }

    /**
     * Decreases game speed to the value specified in AtlantisConfig when action happens.
     */
    public static void allowToDynamicallySlowdownGameOnFirstFighting() {
        dynamicSlowdownIsAllowed = true;
        dynamicSlowdownIsActive = false;

        Atlantis.game().setLocalSpeed(NORMAL_GAME_SPEED);
        Atlantis.game().setFrameSkip(NORMAL_FRAME_SKIP);

        System.out.println("SLOWDOWN is allowed. Frame skip = " + DYNAMIC_SLOWDOWN_FRAME_SKIP);
    }

    public static void disallowToDynamicallySlowdownGameOnFirstFighting() {
        dynamicSlowdownIsAllowed = false;
    }

    public static void activateDynamicSlowdown() {
        dynamicSlowdownIsActive = true;

        Atlantis.game().setLocalSpeed(DYNAMIC_SLOWDOWN_GAME_SPEED);
        Atlantis.game().setFrameSkip(DYNAMIC_SLOWDOWN_FRAME_SKIP);

        System.out.println("Activated SLOWDOWN");
    }

    public static void deactivateDynamicSlowdown() {
        dynamicSlowdownIsActive = false;

        Atlantis.game().setLocalSpeed(NORMAL_GAME_SPEED);
        Atlantis.game().setFrameSkip(NORMAL_FRAME_SKIP);

        System.out.println("Disabled SLOWDOWN");
    }

    // =========================================================

    public static boolean isDynamicSlowdownAllowed() {
        return dynamicSlowdownIsAllowed;
    }

    public static boolean isDynamicSlowdownActive() {
        return dynamicSlowdownIsActive;
    }

    /**
     * Changes game speed. 0 - fastest 1 - very quick 20 - around default
     */
    public static void changeSpeedTo(int speed) {
        dynamicSlowdownIsActive = false;

        if (speed < 0) {
            speed = 0;
            changeFrameSkipTo(frameSkip - 5);
        }
        if (speed > 0) {
            changeFrameSkipTo(0);
        }

//        GameSpeed.pauseGame();

//        try { TimeUnit.MILLISECONDS.sleep(10); } catch (InterruptedException e) {}

        gameSpeed = speed;

//        try { TimeUnit.MILLISECONDS.sleep(10); } catch (InterruptedException e) {}

//        try {
//            game().setLocalSpeed(AtlantisConfig.GAME_SPEED);
//            Thread.sleep(40);
//            game().setLocalSpeed(AtlantisConfig.GAME_SPEED);
//            Thread.sleep(40);
//        } catch (InterruptedException ex) {
//            // Ignore
//        }
//        try { TimeUnit.MILLISECONDS.sleep(10); } catch (InterruptedException e) {}
        AGame.sendMessage("/speed " + gameSpeed);
        game().setLocalSpeed(gameSpeed);

        // Turn off GUI for huge game speeds to make it even quicker
        game().setGUI(frameSkip <= 150);

//        GameSpeed.unpauseGame();
//        String speedString = AtlantisConfig.GAME_SPEED + (AtlantisConfig.GAME_SPEED == 0 ? " (Max)" : "");
//        sendMessage("Game speed: " + speedString);
    }

    /**
     * Changes game speed by given ammount of units. Total game speed: 0 - fastest 1 - very quick 20 - around
     * default
     */
    public static void changeSpeedBy(int deltaSpeed) {
//        int speed = gameSpeed + deltaSpeed;
        int speed;

        if (deltaSpeed < 0) {
            if (gameSpeed > 1) {
                speed = 1;
            } else {
                speed = 0;
            }
        }
        else {
            if (gameSpeed == 0) {
                speed = 1;
            } else {
                speed = gameSpeed + deltaSpeed;
            }
        }

//        if (deltaSpeed > 0) {
//            frameSkip += 10;
//        } else {
//            frameSkip /= 2;
//        }
//        game().setFrameSkip(frameSkip);

        if (game() != null) {
            changeSpeedTo(speed);
        }
        else {
            System.err.println("Can't change game speed, bwapi is null.");
        }
    }

    /**
     * Change game rendering frame skipping - speeds up game considerably.
     */
    public static void changeFrameSkipTo(int newFrameSkip) {
        if (frameSkip <= 1) {
            frameSkip = 0;
        }

        Game game = game();
        if (game != null) {
            frameSkip = newFrameSkip;
            game.setFrameSkip(frameSkip);
            game.setFrameSkip(frameSkip);
        }
        else {
            System.err.println("Can't change game speed, bwapi is null.");
        }
    }

    public static void pauseGame() {
        isPaused = true;
    }

    public static void unpauseGame() {
        isPaused = false;
    }

    /**
     * Enable/disable pause.
     */
    public static void pauseModeToggle() {
        isPaused = !isPaused;
    }

    /**
     * Returns true if game is paused.
     */
    public static boolean isPaused() {
        return isPaused;
    }

    private static String currentSpeedAndFrameSkip() {
        return "speed = " + gameSpeed + ", frame skip = " + frameSkip;
    }
}
