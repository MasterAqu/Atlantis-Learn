package atlantis.information.strategy.response;

import atlantis.information.strategy.response.protoss.ProtossStrategyResponse;
import atlantis.information.strategy.response.terran.TerranStrategyResponse;
import atlantis.information.strategy.response.zerg.ZergStrategyResponse;
import atlantis.util.cache.Cache;
import atlantis.util.We;
import bwapi.Race;


public class AStrategyResponseFactory {

    private static Cache<AStrategyResponse> cache = new Cache<>();

    // =========================================================

    public static AStrategyResponse forOurRace() {
        return cache.get(
                "forOurRace",
                -1,
                () -> {
                    Race race = We.race();

                    if (race == Race.Protoss) {
                        return new ProtossStrategyResponse();
                    } else if (race == Race.Terran) {
                        return new TerranStrategyResponse();
                    } else if (race == Race.Zerg) {
                        return new ZergStrategyResponse();
                    }
                    return null;
                }
        );
    }

}
