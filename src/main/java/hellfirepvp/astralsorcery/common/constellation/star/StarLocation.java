package hellfirepvp.astralsorcery.common.constellation.star;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: StarLocation
 * Created by HellFirePvP
 * Date: 06.02.2016 01:57
 */
public class StarLocation {

    public final int x, y;

    public StarLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getDistanceToOrigin() {
        return x + y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StarLocation tuple = (StarLocation) o;
        return x == tuple.x && y == tuple.y;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }
}
