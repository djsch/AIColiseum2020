package ratioplayer;

import aic2020.user.*;

public class Worker extends Unit {
    private boolean isPrimaryWorker;
    private boolean isStopped;

    Worker(UnitController uc) {
        super(uc);

        // We designate a single 'primary worker' which is the first worker
        // which finds an appropriate spot for the first farm.
        isPrimaryWorker = false;

        // Whether this unit has stopped moving.
        isStopped = false;

        // Whether this worker should stay within the safe radius around the base.
        workerStayInSafeRadius = true;
    }

    // Set this worker as the primary worker, and write this into the shared
    // array so that no other worker becomes the primary worker.
    public void setIsPrimaryWorker() {
        if (uc.read(PRIMARY_FARMER_BOOL) == 0) {
            isPrimaryWorker = true;
            uc.write(PRIMARY_FARMER_BOOL, 1);
        }
    }

    // Check whether we have found a farm and should stop here for the
    // remainder of the game.
    private boolean shouldStop() {
        Location loc = uc.getLocation();
        for (Direction dir : directions) {
            if (dir == Direction.ZERO)
                continue;

            // if we can't sense the location, we shouldn't stop here
            if (!uc.canSenseLocation(loc.add(dir)))
                return false;

            // if we can't access the location, we shouldn't stop here
            if (!uc.isAccessible(loc.add(dir)))
                return false;

            // if there's a unit there, we shouldn't stop here
            if (uc.senseUnitAtLocation(loc.add(dir)) != null)
                return false;

            // check that there is a farm here
            if (uc.senseFarmAtLocation(loc.add(dir)) != null)
                return false;
        }

        isStopped = true;
        uc.write(PRIMARY_FARM_MIN_X, loc.x - 1);
        uc.write(PRIMARY_FARM_MAX_X, loc.x + 1);
        uc.write(PRIMARY_FARM_MIN_Y, loc.y - 1);
        uc.write(PRIMARY_FARM_MAX_Y, loc.y + 1);

        return true;
    }

    // The primary worker attempts to create a square around himself with a
    // single marker and 7 farms, so as to maximize income.
    private void playPrimaryWorker() {
        // Always gather and deposit food, if possible.
        if (uc.canGatherFood()) {
            uc.gatherFood();
        }
        if (uc.canDeposit()) {
            uc.deposit();
        }
        // If we have found an appropriate location for the first farm,
        // attempt to build a marker or farm.
        if (isStopped) {
            if (uc.canSpawn(UnitType.MARKET, Direction.SOUTH)) {

                uc.spawn(UnitType.MARKET, Direction.SOUTH);
            }
            for (Direction dir : directions) {
                if (dir == Direction.SOUTH)
                    continue;
                if (uc.canBuildFarm(dir)) {
                    uc.buildFarm(dir);
                }
            }
        }
        else {
            if (!shouldStop()) {
                moveRandomly();
            }
        }
    }

    void play() {
        // The primary worker has a different set of logic it follows.
        if (isPrimaryWorker) {
            playPrimaryWorker();
            return;
        }

        // Workers should move randomly, unless they are stopped (ie, they are
        // the primary worker and have found a farm).
        moveRandomly();

        // Always gather and deposit food, if able.
        if (uc.canGatherFood()) {
            uc.gatherFood();
        }
        if (uc.canDeposit()) {
            uc.deposit();
        }
        // Build a building, if appropriate.
        if (shouldCreateUnit(null, true)) {
            if (buildFarm()) {
                uc.write(FARM_INDEX, uc.read(FARM_INDEX) + 1);
                incrementBuildNum();
            }
        }
        if (shouldCreateUnit(UnitType.MARKET, false)) {
            if (spawn(UnitType.MARKET)) {
                uc.write(MARKET_INDEX, uc.read(MARKET_INDEX) + 1);
                incrementBuildNum();
            }
        }
        if (shouldCreateUnit(UnitType.BARRACKS, false)) {
            if (spawn(UnitType.BARRACKS)) {
                uc.write(BARRACKS_INDEX, uc.read(BARRACKS_INDEX) + 1);
                incrementBuildNum();
            }
        }
        if (shouldCreateUnit(UnitType.LABORATORY, false)) {
            if (spawn(UnitType.LABORATORY)) {
                uc.write(LABORATORY_INDEX, uc.read(LABORATORY_INDEX) + 1);
                incrementBuildNum();
            }
        }
        if (shouldCreateUnit(UnitType.HOSPITAL, false)) {
            if (spawn(UnitType.HOSPITAL)) {
                uc.write(HOSPITAL_INDEX, uc.read(HOSPITAL_INDEX) + 1);
                incrementBuildNum();
            }
        }
        // Update this unit with information from the shared array about squares
        // which are known to be infected or fumigated. Also update the shared
        // array with squares that this unit knows to be infected.
        updateInfected();
        updateFumigated();
        writeInfected();
    }


}
