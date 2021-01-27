package ratioplayer;

import aic2020.user.*;
import java.lang.*;

/**
 * Implement a wrapper for the Location class which overrides equals()
 * and hashCode(), so that we can store MyLocation objects in a
 * HashTable.
 */
public class MyLocation {

    public int x;
    public int y;

    /**
     * Inherited constructor.
     */
    MyLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MyLocation)) {
            return false;
        }
        MyLocation foo = ((MyLocation) o);
        if (this.x == foo.x && this.y == foo.y)
            return true;
        return false;
    }

    @Override
    public int hashCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("x");
        sb.append(this.x);
        sb.append("y");
        sb.append(this.y);
        return sb.toString().hashCode();
    }
}
