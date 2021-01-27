package ratioplayer;

import aic2020.user.*;

public class Laboratory extends Unit {
    Laboratory(UnitController uc){
        super(uc);
    }

    // Attempt to spawn an infecter, if appropriate.
    void play() {
        if (shouldCreateUnit(UnitType.INFECTER, false)) {
            if (spawn(UnitType.INFECTER)) {
                uc.write(INFECTER_INDEX, uc.read(INFECTER_INDEX) + 1);
                incrementBuildNum();
            }
        }
    }

}
