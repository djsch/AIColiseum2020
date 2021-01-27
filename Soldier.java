package ratioplayer;

import aic2020.user.*;
import java.util.*;

/**
 * Soldiers attempt to find a location (a post) where there is no friendly soldier
 * within their vision. Therefore soldiers will eventually create a network
 * covering the entire map. Once they have found such a location, they stay there,
 * unless an approaching zombie forces them to abandon their post. In such a case
 * they move towards their designated post whenever it is safe to do so.
 */
public class Soldier extends Unit {

    // Track locations that are already within soldierPostRadius distance
    // of a solider; hence they should not be considered when trying to find a post.
    public HashSet<MyLocation> alreadyCheckedLocations;

    Soldier(UnitController uc) {
        super(uc);

        stayInSafeRadius = true;
        alreadyCheckedLocations = new HashSet<MyLocation>();

        postX = -1;
        postY = -1;
    }

    // Soldiers attempt to find a 'post', which is a location where there is
    // no other soldier within this radius.
    int soldierPostRadius = 5;

    // Whether the soldier has found a post.
    boolean foundPost = false;

    // The location of this soldier's post, once it has found one.
    int postX;
    int postY;

    // If all possible adjacent locations have already been checked and
    // we have not yet found a post, we are stuck and need to clear our
    // alreadyCheckedLocations so we can travel in a new direction to
    // find a post.
    private void maybeClearAlreadyCheckedLocations() {
        for (Direction dir : directions) {
            Location loc = uc.getLocation().add(dir);
            if (hasUnitInDirection(dir))
                continue;
            if (!uc.isAccessible(loc))
                continue;
            if (!alreadyCheckedLocations.contains(new MyLocation(loc.x, loc.y)))
                return;
        }
        alreadyCheckedLocations.clear();
    }

    // Move randomly until it finds a post. Attacks every enemy it sees.
    void play() {
        // We always attack enemy units we can see.
        attack();

        // If we haven't found a post yet, move randomly to a location we
        // haven't checked yet. Then see if the new location is a valid post.
        if (!foundPost) {
            alreadyCheckedLocations.add(new MyLocation(uc.getLocation().x, uc.getLocation().y));
            maybeClearAlreadyCheckedLocations();
            moveRandomly(alreadyCheckedLocations);
            if (haveFoundPost()) {
                foundPost = true;
                postX = uc.getLocation().x;
                postY = uc.getLocation().y;
            }
        } else {
            // If a zombie is nearby, we need to abandon our post or risk
            // getting infected. We will return to our post later.
            Location maybeZombieLoc = isNearZombie(uc.getLocation());
            if (maybeZombieLoc != null) {
                moveRandomly();
            }
        }

        // If we aren't currently at our post (perhaps because a zombie forced
        // us to abandon it), try to move towards our post, if it is safe.
        if (foundPost) {
            Location curLoc = uc.getLocation();
            if (curLoc.x != postX || curLoc.y != postY) {
                moveTowardsLocation(new Location(postX, postY));
            }
        }
        // Update this unit with information from the shared array about squares
        // which are known to be infected or fumigated. Also update the shared
        // array with squares that this unit knows to be infected.
        updateInfected();
        updateFumigated();
        writeInfected();
    }

    // Check if the current location is a valid post for this soldier.
    boolean haveFoundPost(){
        // If we can see any soldiers within soldierPostRadius, return false.
        UnitInfo[] units = uc.senseUnits(soldierPostRadius, uc.getTeam());
        for (UnitInfo unit : units){
            if (unit.getType() == UnitType.SOLDIER) return false;
        }
        return true;
    }
}
