package ratioplayer;

import aic2020.user.*;

public class Base extends Unit {
    Base(UnitController uc){
        super(uc);
    }

    // Last turn in which we built a fumigator.
    int fumigatorTurn = 0;

    // TODO: These methods which manipulate the shared array can behave
    // poorly if we run out of energy. We need some way to make sure we leave
    // the array in a sane state; perhaps by exiting gracefully when at
    // 99% energy, or by optimizing the usage of this array.

    // Delete any infected squares from the shared array that are two or
    // more rounds old.
    protected void deleteInfected() {
        int index = INFECTED_SQUARES_START;
        int val = uc.read(index);
        while (val != -1) {
            if (val == -2) {
                // do nothing
            } else if (val <= uc.getRound() - 2) {
                uc.write(index, -2);
                uc.write(index + 1, -2);
                uc.write(index + 2, -2);
            }
            index += 3;
            val = uc.read(index);
            if (index >= FUMIGATED_SQUARES_START - 3)
                break;
        }
    }


    protected void compressInfected() {
        int index = INFECTED_SQUARES_START;
        int val = uc.read(index);
        while (val != -1) {
            if (val != -2)
                break;
            index += 3;
            val = uc.read(index);
        }
        int initIndex = INFECTED_SQUARES_START;
        while (uc.read(index) != -1) {
            uc.write(initIndex, uc.read(index));
            uc.write(initIndex+1, uc.read(index+1));
            uc.write(initIndex+2, uc.read(index+2));

            index += 3;
            initIndex +=3;
            if (initIndex > 1000000)
                break;
        }
        uc.write(index, -1);
        uc.write(INFECTED_SQUARES_END, index);
    }

    // Delete any fumigated squares from the shared array that are two or
    // more rounds old.
    protected void deleteFumigated() {
        int index = FUMIGATED_SQUARES_START;
        int val = uc.read(index);
        while (val != -1) {
            if (val == -2) {
                // do nothing
            } else if (val <= uc.getRound() - 2) {
                uc.write(index, -2);
                uc.write(index + 1, -2);
                uc.write(index + 2, -2);
            }
            index += 3;
            val = uc.read(index);
            if (index >= 1000000)
                break;
        }
    }

    // Compress the fumigated squares portion of the shared array.
    protected void compressFumigated() {
        int index = FUMIGATED_SQUARES_START;
        int val = uc.read(index);
        while (val != -1) {
            if (val != -2)
                break;
            index += 3;
            val = uc.read(index);
        }
        int initIndex = FUMIGATED_SQUARES_START;
        while (uc.read(index) != -1) {
            uc.write(initIndex, uc.read(index));
            uc.write(initIndex+1, uc.read(index+1));
            uc.write(initIndex+2, uc.read(index+2));

            index += 3;
            initIndex +=3;
            if (initIndex > 1000000)
                break;
        }
        uc.write(index, -1);
        uc.write(FUMIGATED_SQUARES_END, index);
    }

    void play() {
        // Every 100 turns, we should build an additional fumigator.
        if ((uc.getRound() % 100) == 0) {
            targetFumigator++;
        }
        // Every 100 turns, we increase the 'safe' area around the base
        // by 3 units.
        if ((uc.getRound() % 100) == 0) {
            uc.write(SAFE_RADIUS, uc.read(SAFE_RADIUS) + 3);
        }
        // Create an Essential Worker, if it is appropriate to do so.
        if (shouldCreateUnitRatio(UnitType.ESSENTIAL_WORKER)) {
            if (spawn(UnitType.ESSENTIAL_WORKER)) {
                uc.write(WORKER_INDEX, uc.read(WORKER_INDEX) + 1);
                uc.write(WORKERS_BUILT, uc.read(WORKERS_BUILT) + 1);
                incrementBuildNum();
            }
        }
        // Create a Fumigator, if it is appropriate to do so.
        if (shouldCreateUnitRatio(UnitType.FUMIGATOR)) {
            if (spawn(UnitType.FUMIGATOR)) {
                uc.write(FUMIGATOR_INDEX, uc.read(FUMIGATOR_INDEX) + 1);
                uc.write(FUMIGATORS_BUILT, uc.read(FUMIGATORS_BUILT) + 1);
                incrementBuildNum();
            }
        }

        // As one of the units that must always exist, and has little
        // else it needs to calculate, we assign the Base responsibility
        // over maintaining the shared array.
        deleteInfected();
        compressInfected();
        deleteFumigated();
        compressFumigated();
    }
}
