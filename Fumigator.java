package ratioplayer;

import aic2020.user.*;

public class Fumigator extends Unit {
    // Some fumigators target a particular known infected location to fumigate.
    // If this fumigator has this behavior, store this location here.
    Location toFumigate = null;

    // Each fumigator gets a unique ID. We use this to determine what tactic
    // this fumigator should use (eg, spread out or target infected cells).
    int fumigatorID;

    Fumigator(UnitController uc){
        super(uc);

        stayInSafeRadius = true;
        fumigatorID = numFumigators;
        numFumigators++;
    }

    // Write all adjacent locations into the shared array as fumigated.
    protected void writeFumigated() {
        Location loc = uc.getLocation();
        for (Direction dir : directions) {
            if (isKnownInfected(loc.add(dir))) {
                // This last check is to try to get rid of that weird case
                // where a square wasn't cleaned. This might be computationally
                // expensive.
                if (isNearZombie(loc.add(dir)) == null) {
                    int endIndex = uc.read(FUMIGATED_SQUARES_END);

                    uc.write(endIndex, uc.getRound());
                    uc.write(endIndex + 1, loc.add(dir).x);
                    uc.write(endIndex + 2, loc.add(dir).y);
                    uc.write(endIndex + 3, -1);
                    uc.write(FUMIGATED_SQUARES_END, endIndex + 3);
                }
            }
        }
    }

    // If we've reached our target fumigate location, or if that location is
    // no longer infected (perhaps because another fumigator got there first),
    // we should find a new location to fumigate.
    protected void maybeResetToFumigate() {
        if (toFumigate == null)
            return;
        Location curLoc = uc.getLocation();
        if (curLoc.x == toFumigate.x && curLoc.y == toFumigate.y) {
            toFumigate = null;
            return;
        }
        if (!infectedLocations.contains(new MyLocation(toFumigate.x, toFumigate.y))) {
            toFumigate = null;
            return;
        }

    }

    // If there is an unclaimed infected location and we don't already have
    // a target location to fumigate, claim that location as our target.
    protected void maybeSelectToFumigate() {
        if (toFumigate != null)
            return;

        if (infectedLocations.size() <= fumigatorID)
            return;

        // TODO: This is just doing a linear search to find the next location
        // to target. Should consider using a different data structure for
        // infectedLocations so I can just go to infectedLocations[i] (or similar)
        int i = 0;
        for (MyLocation loc : infectedLocations) {
            if (i == fumigatorID) {
                toFumigate = new Location(loc.x, loc.y);
                break;
            }
            i++;
        }
    }

    // Fumigators attempt to spread out, so as to cover the greatest area possible.
    // Pick an allied fumigator within vision and attempt to move away from it.
    protected void moveFumigatorSpread() {
        /*Nothing to do if the unit can't move this turn*/
        if (!uc.canMove()) {
            return;
        }

        UnitInfo[] units = uc.senseUnits(uc.getTeam());
        for (int i = 0; i < units.length; i++) {
            if (units[i].getType() == UnitType.FUMIGATOR) {
                if (uc.getLocation().distanceSquared(units[i].getLocation()) <= fumigatorRadius) {
                    Direction dir = uc.getLocation().directionTo(units[i].getLocation()).opposite();

                    if (!uc.canMove(dir))
                        continue;

                    Location newLoc = uc.getLocation().add(dir);

                    // if stayInRadius and location is outside radius, don't move there
                    if (stayInSafeRadius) {
                        Location baseLoc = new Location(uc.read(BASE_LOC_X), uc.read(BASE_LOC_Y));
                        if (newLoc.distanceSquared(baseLoc) > uc.read(SAFE_RADIUS))
                            continue;
                    }

                    if (!couldMoveTo(newLoc))
                        continue;

                    uc.move(dir);
                    return;
                }
            }
        }

        // If we've haven't been able to move yet, just make a default move.
        moveRandomly();
        return;
    }

    // This strategy targets an infected square and moves towards it,
    // if possible.
    protected void playTarget() {
        maybeResetToFumigate();
        maybeSelectToFumigate();
        if (toFumigate != null) {
            if (!moveTowardsLocation(toFumigate)) {
                toFumigate = null;
            }
        } else {
            moveRandomly();
        }
    }

    // This strategy tries to move away from other fumigators in vision.
    protected void playSpread() {
        moveFumigatorSpread();
    }

    void play() {
        // One third of fumigators target infected squares; the rest spread out.
        if (fumigatorID % 3 == 0) {
            playTarget();
        }
        else {
            playSpread();
        }

        // Update this unit with information from the shared array about squares
        // which are known to be infected or fumigated. Also update the shared
        // array with squares that this unit knows to be infected or fumigated.
        updateInfected();
        updateFumigated();
        writeInfected();
        writeFumigated();
    }


}
