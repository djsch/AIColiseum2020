package ratioplayer;

import aic2020.user.*;

public class UnitPlayer {

    public void run(UnitController uc) {
        /* We instantiate myUnit to whatever type we are - note that we only create units of these types */
        Unit myUnit;
        if (uc.getType() == UnitType.BASE) myUnit = new Base(uc);
        else if (uc.getType() == UnitType.MARKET) myUnit = new Market(uc);
        else if (uc.getType() == UnitType.LABORATORY) myUnit = new Laboratory(uc);
        else if (uc.getType() == UnitType.HOSPITAL) myUnit = new Hospital(uc);
        else if (uc.getType() == UnitType.BARRACKS) myUnit = new Barracks(uc);
        else if (uc.getType() == UnitType.ESSENTIAL_WORKER) myUnit = new Worker(uc);
        else if (uc.getType() == UnitType.INFECTER) myUnit = new Infecter(uc);
        else if (uc.getType() == UnitType.FUMIGATOR) myUnit = new Fumigator(uc);
        else if (uc.getType() == UnitType.SOLDIER) myUnit = new Soldier(uc);
        else myUnit = null;

        // Set the coordinates for the Base once only.
        if (uc.getType() == UnitType.BASE) {
            myUnit.setBaseLocation();
            myUnit.setInitialSafeRadius();
            myUnit.initializeInfectedSquares();
        }

        if (uc.getType() == UnitType.ESSENTIAL_WORKER) {
            ((Worker) myUnit).setIsPrimaryWorker();
        }

        while (true) {
            myUnit.play();
            uc.yield(); // End of turn
        }

    }
}
