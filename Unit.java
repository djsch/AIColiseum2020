package ratioplayer;

import aic2020.user.*;

import java.util.*;

/**
 * Abstract unit class, it is implemented by every unit type.
 */
public abstract class Unit {

    /**
     * Our UnitController.
     */
    UnitController uc;

    /**
     * Direction values.
     */
    protected ArrayList<Direction> directions;

    /**
     * My id.
     */
    int myID;

    // We specify a target number of each unit to build over the course
    // of the game. This is a rather primitive AI, but it is useful for testing.
    int targetFarms = 0;
    int targetLaboratories = 2;
    int targetBarracks = 2;
    int targetHospital = 0;
    int targetMarket = 0;
    int targetWorker = 3;
    int targetFumigator = 2;
    int targetInfecter = 2;
    int targetSoldier = 20;

    // Count of how many fumigators have been built this game. Used to assign
    // fumigatorIDs.
    int numFumigators = 0;

    // We attempt to build workers, soldiers, and fumigators in the given ratios.
    // For instance, here for every 2 workers we build, we expect to build 8
    // soldiers and 3 fumigators.
    int workerRatio = 2;
    int soldierRatio = 8;
    int fumigatorRatio = 3;

    int fumigatorRadius = 15;

    // Whether this unit should always stay within initialSafeRadius units
    // of the base.
    protected boolean stayInSafeRadius = false;
    // The radius around which designated units should stay around the base.
    protected int initialSafeRadius = 121;
    // Whether workers should stay within workerSafeRadius units of the base.
    protected boolean workerStayInSafeRadius = false;
    // The radius around which designated units should stay around the base.
    protected int workerSafeRadius = 25;

    // The radius around which the location of a zombie should be considered
    // 'near' a zombie.
    protected int zombieDangerRadius = 9;
    // The last round for which this unit has updated their infected locations.
    protected int lastRoundUpdatedInfected = 0;
    // The last round for which this unit has updated their fumigated locations.
    protected int lastRoundUpdatedFumigated = 0;

    // These variables are all for management of the shared array. They denote
    // the index of the array at which the relevant information is stored.
    final protected int FARM_INDEX = 0;
    final protected int LABORATORY_INDEX = 1;
    final protected int BARRACKS_INDEX = 2;
    final protected int HOSPITAL_INDEX = 3;
    final protected int MARKET_INDEX = 4;
    final protected int WORKER_INDEX = 5;
    final protected int FUMIGATOR_INDEX = 6;
    final protected int INFECTER_INDEX = 7;
    final protected int SOLDIER_INDEX = 8;

    final protected int CURRENT_BUILD_INDEX = 9;

    final protected int BASE_LOC_X = 10;
    final protected int BASE_LOC_Y = 11;

    final protected int PRIMARY_FARMER_BOOL = 12;

    final protected int PRIMARY_FARM_INIT = 13;
    final protected int PRIMARY_FARM_MIN_X = 14;
    final protected int PRIMARY_FARM_MAX_X = 15;
    final protected int PRIMARY_FARM_MIN_Y = 16;
    final protected int PRIMARY_FARM_MAX_Y = 17;

    final protected int WORKERS_BUILT = 18;
    final protected int SOLDIERS_BUILT = 19;
    final protected int FUMIGATORS_BUILT = 20;

    final protected int INFECTED_SQUARES_END = 21;
    final protected int FUMIGATED_SQUARES_END = 22;

    final protected int SAFE_RADIUS = 23;

    // We store arrays of squares which are known to be infected or fumigated
    // in the shared array, beginning at INFECTED_SQUARES_START and at
    // FUMIGATED_SQUARES_START. The ends of the arrays are at the values stored
    // in INFECTED_SQUARE_END and FUMIGATED_SQUARES_END. In this way units can
    // communicate between each other when squares are known to be infected,
    // ie if a unit sees a zombie, it knows that all squares around it are
    // infected, it can tell all other units that those square are infected by
    // adding them starting at INFECTED_SQUARES_START, and all units can know
    // that despite that zombie not being within their vision. Likewise, once a
    // fumigator has visited those squares, they are able to tell other units
    // that those squares are now safe, even though the other units are
    // not able to observe this for themselves.
    //
    // Squares are added in triplets in the form, (round, x, y), where round is
    // the current round number. In this way units can know if they have
    // already accounted for this information or now (they may have timed out
    // on a previous round).
    //
    // The base performs logic to remove obsolete data from this array each round.
    final protected int INFECTED_SQUARES_START = 70;
    final protected int FUMIGATED_SQUARES_START = 500000;

    HashSet<MyLocation> infectedLocations;
    int failedMoveTowards = 0;

    // We hardcode the order which the first units which should be built
    // each game, before moving to a more dynamic approach.
    protected String[] buildOrder = {"worker", "worker", "barracks"};

    /**
     * Constructor
     */
    Unit(UnitController uc){
        this.uc = uc;
        this.myID = uc.getInfo().getID();
        directions = new ArrayList<Direction>();
        for (Direction dir : Direction.values()) {
            directions.add(dir);
        }
        infectedLocations = new HashSet<MyLocation>();
    }

    /**
     * Play method. It is implemented by each unit type.
     */
    abstract void play();

    // Randomize the order of the directions array so that behavior is non-
    // deterministic (ie, so that randomly walking units don't always run
    // directly north).
    protected void shuffleDirections() {
        for (int i = 0; i < directions.size() - 2; i++) {
            int rand = (int) (Math.random() * (directions.size() - i));
            rand += i;

            Direction tmp = directions.get(i);
            directions.set(i, directions.get(rand));
            directions.set(rand, tmp);
        }
    }

    // Make sure the initial location of the base is accessible by all units
    // in the shared array.
    public void setBaseLocation() {
        uc.write(BASE_LOC_X, uc.getLocation().x);
        uc.write(BASE_LOC_Y, uc.getLocation().y);
    }

    // Make sure the initial safe radius around the base is accessible by
    // all units.
    public void setInitialSafeRadius() {
        uc.write(SAFE_RADIUS, initialSafeRadius);
    }

    // Initialize the arrays used to store infected and fumigated squares
    // in the shared array.
    public void initializeInfectedSquares() {
        uc.write(INFECTED_SQUARES_START, -1);
        uc.write(FUMIGATED_SQUARES_START, -1);

        uc.write(INFECTED_SQUARES_END, INFECTED_SQUARES_START);
        uc.write(FUMIGATED_SQUARES_END, FUMIGATED_SQUARES_START);
    }

    // Return whether the given location is known, by this unit, to
    // be infected.
    protected boolean isKnownInfected(Location loc) {
        if (infectedLocations.contains(new MyLocation(loc.x, loc.y))) {
            return true;
        }
        return false;
    }

    // This location is now known to be not infected, so remove it from
    // the set of locations that this unit knows to be infected.
    protected void removeInfected(Location loc) {
        infectedLocations.remove(new MyLocation(loc.x, loc.y));
    }

    // Iterate through the shared array and find all locations that other
    // units have found to be infected, then update infectedLocations with
    // those locations.
    protected void updateInfected() {
        int index = INFECTED_SQUARES_START;
        int val = uc.read(index);
        while (val != -1) {
            if (val == -2) {
                // do nothing
            }
            else if (lastRoundUpdatedInfected <= val) {
                int x = uc.read(index+1);
                int y = uc.read(index+2);
                infectedLocations.add(new MyLocation(x, y));
            }
            index += 3;
            val = uc.read(index);
            if (index >= FUMIGATED_SQUARES_START)
                break;
        }
        lastRoundUpdatedInfected = uc.getRound();
    }

    // Iterate through the shared array and find all locations that other
    // units have found to be fumigated, then update infectedLocations to
    // remove those locations.
    protected void updateFumigated() {
        int index = FUMIGATED_SQUARES_START;
        int val = uc.read(index);
        while (val != -1) {
            if (val == -2) {
                // do nothing
            }
            else if (lastRoundUpdatedFumigated <= val) {
                int x = uc.read(index+1);
                int y = uc.read(index+2);
                infectedLocations.remove(new MyLocation(x, y));
            }
            index += 3;
            val = uc.read(index);
            if (index >= 1000000)
                break;
        }
        lastRoundUpdatedFumigated = uc.getRound();
    }

    // Check to see if there are any zombies or enemy fumigators in vision. If
    // there are, mark their locations, and all adjacent locations, as infected
    // in the shared array.
    protected void writeInfected() {
        // Returns enemies and zombies
        UnitInfo[] units = uc.senseUnits(uc.getTeam(), true, true);
        for (int i = 0; i < units.length; i++) {
            // If the unit is a zombie or enemy fumigator
            if (units[i].getTeam() == Team.ZOMBIE ||
                    (units[i].getTeam() == uc.getTeam().getOpponent() &&
                      units[i].getType() == UnitType.FUMIGATOR)) {
                Location enemyLoc = units[i].getLocation();
                for (Direction dir : directions) {
                    if (!isKnownInfected(enemyLoc.add(dir))) {
                        int endIndex = uc.read(INFECTED_SQUARES_END);

                        uc.write(endIndex, uc.getRound());
                        uc.write(endIndex+1, enemyLoc.add(dir).x);
                        uc.write(endIndex+2, enemyLoc.add(dir).y);
                        uc.write(endIndex+3, -1);
                        uc.write(INFECTED_SQUARES_END, endIndex+3);
                    }
                }
            }
        }
    }

    // Utility method to get the direction which best points from the current
    // unit's location towards the base.
    protected Direction getDirectionTowardsBase() {
        Location curLoc = uc.getLocation();
        Location baseLoc = new Location(uc.read(BASE_LOC_X), uc.read(BASE_LOC_Y));
        return curLoc.directionTo(baseLoc);
    }

    // Returns whether the next unit in the global build order is the
    // given UnitType.
    //
    // Unfortunately, Farm is not a UnitType, because it does not have a
    // team affiliation, so we need to special-case it here.
    // TODO: Investigate a better way to handle this.
    protected boolean shouldCreateUnitFromBuildOrder(UnitType type, boolean isFarm) {
        int buildNum = uc.read(CURRENT_BUILD_INDEX);
        if (isFarm) {
            if (buildOrder[buildNum].equals("farm"))
                return true;
            else
                return false;
        }
        else if (type == UnitType.LABORATORY) {
            if (buildOrder[buildNum].equals("laboratory"))
                return true;
            else
                return false;
        }
        else if (type == UnitType.HOSPITAL) {
            if (buildOrder[buildNum].equals("hospital"))
                return true;
            else
                return false;
        }
        else if (type == UnitType.BARRACKS) {
            if (buildOrder[buildNum].equals("barracks"))
                return true;
            else
                return false;
        }
        else if (type == UnitType.MARKET) {
            if (buildOrder[buildNum].equals("market"))
                return true;
            else
                return false;
        }
        else if (type == UnitType.ESSENTIAL_WORKER) {
            if (buildOrder[buildNum].equals("worker"))
                return true;
            else
                return false;
        }
        else if (type == UnitType.FUMIGATOR) {
            if (buildOrder[buildNum].equals("fumigator"))
                return true;
            else
                return false;
        }
        else if (type == UnitType.INFECTER) {
            if (buildOrder[buildNum].equals("infecter"))
                return true;
            else
                return false;
        }
        else if (type == UnitType.SOLDIER) {
            if (buildOrder[buildNum].equals("soldier"))
                return true;
            else
                return false;
        }
        return false;
    }

    // Returns whether we should build the given UnitType based upon the global
    // target numbers for each unit. For instance, if we have built 3 soldiers,
    // but targetSoldier is 5, this method will return true for UnitType soldier.
    //
    // Unfortunately, Farm is not a UnitType, because it does not have a
    // team affiliation, so we need to special-case it here.
    // TODO: Investigate a better way to handle this.
    protected boolean shouldCreateUnitFromTargetThresholds(UnitType type, boolean isFarm) {
        if (isFarm) {
            if (uc.read(FARM_INDEX) < targetFarms)
                return true;
            else
                return false;
        }
        else if (type == UnitType.LABORATORY) {
            if (uc.read(LABORATORY_INDEX) < targetLaboratories)
                return true;
            else
                return false;
        }
        else if (type == UnitType.HOSPITAL) {
            if (uc.read(HOSPITAL_INDEX) < targetHospital)
                return true;
            else
                return false;
        }
        else if (type == UnitType.BARRACKS) {
            int foo = uc.read(BARRACKS_INDEX);
            if (uc.read(BARRACKS_INDEX) < targetBarracks)
                return true;
            else
                return false;
        }
        else if (type == UnitType.MARKET) {
            if (uc.read(MARKET_INDEX) < targetMarket)
                return true;
            else
                return false;
        }
        else if (type == UnitType.ESSENTIAL_WORKER) {
            if (uc.read(WORKER_INDEX) < targetWorker)
                return true;
            else
                return false;
        }
        else if (type == UnitType.FUMIGATOR) {
            if (uc.read(FUMIGATOR_INDEX) < targetFumigator)
                return true;
            else
                return false;
        }
        else if (type == UnitType.INFECTER) {
            if (uc.read(INFECTER_INDEX) < targetInfecter)
                return true;
            else
                return false;
        }
        else if (type == UnitType.SOLDIER) {
            if (uc.read(SOLDIER_INDEX) < targetSoldier)
                return true;
            else
                return false;
        }
        return false;
    }

    // Determine whether we should build the given UnitType, depending on whether
    // we have exhausted the inital build order, or if we are now building
    // according to the target number of each unit.
    //
    // Unfortunately, Farm is not a UnitType, because it does not have a
    // team affiliation, so we need to special-case it here.
    // TODO: Investigate a better way to handle this.
    protected boolean shouldCreateUnit(UnitType type, boolean isFarm) {
        int buildNum = uc.read(CURRENT_BUILD_INDEX);
        if (buildNum < buildOrder.length) {
            return shouldCreateUnitFromBuildOrder(type, isFarm);
        }
        else {
            return shouldCreateUnitFromTargetThresholds(type, isFarm);
        }
    }

    // We create Soldiers, Fumigators, and Essential Workers in a ratio
    // specified by workerRatio, soldierRatio, and fumigatorRatio. This method
    // returns whether the given UnitType should be built in order to move us
    // closer to the ideal ratio.
    protected boolean shouldCreateUnitRatio(UnitType type) {
        // This method is only valid for the UnitType soldier, essential
        // worker, or fumigator.
        if (type != UnitType.SOLDIER && type != UnitType.ESSENTIAL_WORKER &&
                type != UnitType.FUMIGATOR) {
            return false;
        }
        double curWorkerRatio = uc.read(WORKERS_BUILT) / workerRatio;
        double curSoldierRatio = uc.read(SOLDIERS_BUILT) / soldierRatio;
        double curFumigatorRatio = uc.read(FUMIGATORS_BUILT) / fumigatorRatio;

        if (type == UnitType.SOLDIER && curSoldierRatio <= curWorkerRatio &&
            curSoldierRatio <= curFumigatorRatio) {
            return true;
        }
        if (type == UnitType.ESSENTIAL_WORKER && curWorkerRatio <= curSoldierRatio &&
                curWorkerRatio <= curFumigatorRatio) {
            return true;
        }
        if (type == UnitType.FUMIGATOR && curFumigatorRatio <= curWorkerRatio &&
                curFumigatorRatio <= curSoldierRatio) {
            return true;
        }
        return false;
    }

    // Increment the number of units we have constructed from the hardcoded
    // build order.
    protected void incrementBuildNum() {
        uc.write(CURRENT_BUILD_INDEX, uc.read(CURRENT_BUILD_INDEX) + 1);
    }

    /**
     * Generic attack method.
     */
    void attack(){

        /*Sense all enemies*/
        UnitInfo[] units = uc.senseUnits(uc.getTeam(), true, false);

        /*Attack any of them*/
        for (UnitInfo unit : units){
            if (uc.canAttack(unit.getLocation())) uc.attack(unit.getLocation());
        }
    }

    /**
     * Checks if a given location is adjacent to a non-structure unit.
     */
    boolean isAdjacentToAnotherUnit(Location loc){
        shuffleDirections();
        for (Direction dir : directions) {

            /*Location adjacent to loc following the given direction*/
            Location newLoc = loc.add(dir);

            /*If we can't sense it we assume there is no unit*/
            if (!uc.canSenseLocation(newLoc) || uc.isOutOfMap(newLoc)) continue;

            /*Unit at the new location*/
            UnitInfo unit = uc.senseUnitAtLocation(newLoc);

            /*If there is no unit or this is the unit, continue*/
            if (unit == null || unit.getID() == myID) continue;

            /*If it is not a structure => return true*/
            if (!unit.getType().isStructure()) return true;
        }

        return false;
    }

    protected boolean hasUnitInDirection(Direction dir) {

        /*Location adjacent to loc following the given direction*/
        Location newLoc = uc.getLocation().add(dir);

        /*If we can't sense it we assume there is no unit*/
        if (!uc.canSenseLocation(newLoc) || uc.isOutOfMap(newLoc)) return false;

        /*Unit at the new location*/
        UnitInfo unit = uc.senseUnitAtLocation(newLoc);

        /*If there is no unit or this is the unit, continue*/
        if (unit == null || unit.getID() == myID) return false;

        /*If it is not a structure => return true*/
        //if (!unit.getType().isStructure()) return true;

        return true;
    }

    // We specify a location for a single farm to harvest from in the shared
    // array. Return whether the given Location is within radius 1 of that
    // initial farm.
    protected boolean isPrimaryFarmLoc(Location loc) {
        if (uc.read(PRIMARY_FARM_INIT) == 0)
            return false;

        int farmXMin = uc.read(PRIMARY_FARM_MIN_X);
        int farmXMax = uc.read(PRIMARY_FARM_MAX_X);
        int farmYMin = uc.read(PRIMARY_FARM_MIN_Y);
        int farmYMax = uc.read(PRIMARY_FARM_MAX_Y);

        if (farmXMin <= loc.x && farmXMax >= loc.x &&
            farmYMin <= loc.y && farmYMax >= loc.y) {
            return true;
        }
        return false;
    }

    /**
     * Generic spawn method. It tries to spawn one unit of a given type in every direction (but only one in total).
     * Also, the new unit should not be adjacent to any other. It returns true iff it successfully creates the given type.
     */
    boolean spawn(UnitType type) { return spawn(type, false); }
    boolean spawn(UnitType type, boolean canBuildAdj){
        try {
            shuffleDirections();
        }
        catch (Exception e) {
            int foo = 4;
        }
        for (Direction dir : directions) {

            /*Check for adjacency with a unit*/
            if (!canBuildAdj) {
            }

            if (isPrimaryFarmLoc(uc.getLocation().add(dir)))
                continue;

            /*try spawning*/
            if (uc.canSpawn(type, dir)) {
                // Don't spawn on top of a farm
                FarmInfo info = uc.senseFarmAtLocation(uc.getLocation().add(dir));
                if (info != null)
                    continue;

                uc.spawn(type, dir);
                return true;
            }
        }
        return false;
    }

    boolean buildFarm() {
        shuffleDirections();
        for (Direction dir : directions){

            /*Check for adjacency with a unit*/
            if (isAdjacentToAnotherUnit(uc.getLocation().add(dir))) continue;

            /*try spawning*/
            if (uc.canBuildFarm(dir)){
                uc.buildFarm(dir);
                return true;
            }
        }
        return false;
    }

    // It says 'zombie' but really it means 'zombie or enemy fumigator'
    // returns the Location of the zombie/enemy that is threatening,
    // or null if nothing.
    protected Location isNearZombie(Location loc) {
        UnitInfo[] units = uc.senseUnits(uc.getTeam(), true, true);
        for (int i = 0; i < units.length; i++) {
            // if the unit is a zombie or enemy fumigator
            if (units[i].getTeam() == Team.ZOMBIE ||
                    (units[i].getTeam() == uc.getTeam().getOpponent() &&
                            units[i].getType() == UnitType.FUMIGATOR)) {
                if (loc.distanceSquared(units[i].getLocation()) <= zombieDangerRadius)
                    return units[i].getLocation();
            }
        }
        return null;
    }

    // Move method which attempts to move the unit directly away from the base.
    protected void moveAwayFromBase() {
        Direction towards = getDirectionTowardsBase();
        Direction away = towards.opposite();
        if (uc.canMove(away)) {
            Location newLoc = uc.getLocation().add(away);
            if (!couldMoveTo(newLoc))
                return;

            uc.move(away);
            return;
        }
    }

    // Randomly choose a direction for this unit to move, biased towards
    // the base. For instance, in the sample diagram below, we move towards
    // the base with probability weight 5, in a direction perpendicular
    // towards the base with probability weight 3, and away from the base with
    // probability weight 1.
    //
    //   2        3       4
    //            ^
    //   1  <-  UNIT  ->  5    BASE
    //            V
    //   2        3       4
    protected void moveBiasTowardsBase() {
        if (!uc.canMove()) return;

        ArrayList<Direction> arr = new ArrayList<Direction>();
        Direction towards = getDirectionTowardsBase();

        // TODO: Create global constants for these weights
        for (int i = 0; i < 10; i++) {
            arr.add(towards);
        }
        for (int i = 0; i < 6; i++) {
            arr.add(towards.rotateLeft());
            arr.add(towards.rotateRight());
        }
        for (int i = 0; i < 3; i++) {
            arr.add(towards.rotateLeft().rotateLeft());
            arr.add(towards.rotateRight().rotateRight());
        }
        for (int i = 0; i < 2; i++) {
            arr.add(towards.rotateLeft().rotateLeft().rotateLeft());
            arr.add(towards.rotateRight().rotateRight().rotateRight());
        }
        for (int i = 0; i < 1; i++) {
            arr.add(towards.opposite());
        }

        // TODO: refactor this so that if a randomly chosen direction is invalid,
        // we don't attempt to move there again.
        for (int i = 0; i < 10; i++) {
            int r = (int) (Math.random() * arr.size());
            Direction dir = Direction.values()[r];

            if (uc.canMove(dir)) {
                Location newLoc = uc.getLocation().add(dir);
                if (!couldMoveTo(newLoc))
                    continue;

                uc.move(dir);
                return;
            }
        }
    }

    // Move in a random direction, obeying some common sense directives
    // (ie don't step next to a zombie). This is a default action for most
    // units; ideally all units will do something more intelligent, but
    // if units don't have a directive yet or can't fulfill that directive,
    // they may default to moving randomly.
    void moveRandomly(){ moveRandomly(null); }
    void moveRandomly(HashSet<MyLocation> alreadyCheckedLocations){

        /*Nothing to do if the unit can't move this turn*/
        if (!uc.canMove()) {
            return;
        }

        shuffleDirections();
        for (Direction dir : directions) {
            if (!uc.canMove(dir))
                continue;

            // If we've already checked this location, don't move there.
            if (alreadyCheckedLocations != null) {
                if (alreadyCheckedLocations.contains(new MyLocation(uc.getLocation().add(dir).x, uc.getLocation().add(dir).y)))
                    continue;
            }

            // if stayInRadius and location is outside radius, don't move there
            if (stayInSafeRadius) {
                Location newLoc = uc.getLocation().add(dir);
                Location baseLoc = new Location(uc.read(BASE_LOC_X), uc.read(BASE_LOC_Y));
                if (newLoc.distanceSquared(baseLoc) > uc.read(SAFE_RADIUS))
                    continue;
            }

            Location newLoc = uc.getLocation().add(dir);
            if (workerStayInSafeRadius) {
                Location baseLoc = new Location(uc.read(BASE_LOC_X), uc.read(BASE_LOC_Y));
                if (newLoc.distanceSquared(baseLoc) > workerSafeRadius)
                    continue;
            }

            if (!couldMoveTo(newLoc))
                continue;

            uc.move(dir);
            return;
        }
    }

    // Moves towards the given location. Returns false if we can't move towards
    // that location.
    protected boolean moveTowardsLocation(Location loc) {
        // If we can't move yet, we don't want to say we can't
        // get there, because we still could.
        if (!uc.canMove()) {
            return true;
        }

        // If we've failed to move towards the location 5 times; just
        // accept that we won't get there and give up.
        if (failedMoveTowards >= 5) {
            moveRandomly();
            failedMoveTowards = 0;
            return false;
        }

        Direction dirTo = uc.getLocation().directionTo(loc);
        Location newLoc = uc.getLocation().add(dirTo);

        // if stayInRadius and location is outside radius, we can't go there,
        // so move randomly instead and return false.
        if (stayInSafeRadius) {
            Location baseLoc = new Location(uc.read(BASE_LOC_X), uc.read(BASE_LOC_Y));
            if (newLoc.distanceSquared(baseLoc) > uc.read(SAFE_RADIUS)) {
                moveRandomly();
                failedMoveTowards = 0;
                return false;
            }
        }

        // If we would need to get close to a zombie to get there,
        // don't go there, move randomly instead, but don't delete
        // our target.
        if (isNearZombie(newLoc) != null) {
            moveRandomly();
            failedMoveTowards++;
            return true;
        }

        // Don't move to a known infected square
        if (isKnownInfected(newLoc)) {
            moveRandomly();
            failedMoveTowards++;
            return false;
        }

        if (!uc.canMove(dirTo)) {
            moveRandomly();
            failedMoveTowards = 0;
            return false;
        }

        uc.move(dirTo);
        return true;
    }

    // Check for a variety of common reasons we wouldn't want to move
    // to a location.
    protected boolean couldMoveTo(Location loc) {
        // Don't move into a strongly infected spot.
        if (uc.isStronglyInfected(loc))
            return false;

        // Don't move too close to a zombie.
        if (isNearZombie(loc) != null)
            return false;

        // Don't move to a known infected square.
        if (isKnownInfected(loc))
            return false;

        // Otherwise, this location is valid.
        return true;
    }
}
