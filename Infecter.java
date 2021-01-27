package ratioplayer;

import aic2020.user.*;

// Default implementation given with the project. Unaltered by me.
public class Infecter extends Unit {

    Infecter(UnitController uc){
        super(uc);
    }

    /**
     * Tells if the soldier should stop moving.
     */
    boolean stop = false;

    /**
     * It moves randomly until it is isolated. It attacks every enemy it sees.
     */
    void play(){
        moveRandomly();
        attack();
    }

    /**
     * If there is no other soldier at a distance of 18 or less it should stop forever.
     */
    boolean shouldStop(){

        /*Sense teammates*/
        UnitInfo[] units = uc.senseUnits(18, uc.getTeam());

        /*Check for nearby soldiers - note that global senses do not sense this unit*/
        for (UnitInfo unit : units){
            if (unit.getType() == UnitType.SOLDIER) return false;
        }

        return true;
    }


}
