package ratioplayer;

import aic2020.user.*;

public class Barracks extends Unit {

    Barracks(UnitController uc){
        super(uc);
    }

    void play() {
        if (shouldCreateUnitRatio(UnitType.SOLDIER)) {
            if (spawn(UnitType.SOLDIER)) {
                uc.write(SOLDIER_INDEX, uc.read(SOLDIER_INDEX) + 1);
                uc.write(SOLDIERS_BUILT, uc.read(SOLDIERS_BUILT) + 1);
                incrementBuildNum();
            }
        }
    }
}
