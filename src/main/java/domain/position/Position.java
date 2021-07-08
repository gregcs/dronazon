package domain.position;

import settings.Config;
import java.util.Objects;
import domain.exceptions.*;

public class Position {

    public int x;
    public int y;

    public Position(int x, int y) throws InvalidPosition {
        if ((x < 0 || x >= Config.SMART_CITY_DIMENSION) || (y < 0 || y >= Config.SMART_CITY_DIMENSION))
            throw new InvalidPosition();

        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() { return "(" + x + "," + y + ")"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public static double distance(Position a, Position b){
        return Math.sqrt(Math.pow(b.x - a.x, 2) + Math.pow(b.y - a.y, 2));
    }

}
