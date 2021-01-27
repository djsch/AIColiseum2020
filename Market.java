package ratioplayer;

import aic2020.user.*;

public class Market extends Unit {
    Market(UnitController uc){
        super(uc);
    }

    // Attempt to spawn fumigators and essential workers, if appropriate.
    void play() {
        if (shouldCreateUnit(UnitType.FUMIGATOR, false)) {
            if (spawn(UnitType.FUMIGATOR)) {
                uc.write(FUMIGATOR_INDEX, uc.read(FUMIGATOR_INDEX) + 1);
                incrementBuildNum();
            }
        }
        if (shouldCreateUnit(UnitType.ESSENTIAL_WORKER, false)) {
            if (spawn(UnitType.ESSENTIAL_WORKER)) {
                uc.write(WORKER_INDEX, uc.read(WORKER_INDEX) + 1);
                incrementBuildNum();
            }
        }
    }

}
