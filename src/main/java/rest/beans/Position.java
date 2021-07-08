package rest.beans;

import settings.Config;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Random;

@XmlRootElement
public class Position {

    private int x;
    private int y;

    public Position(){}

    public Position(int x, int y){
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY(){
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public static Position generateRandomPosition(){
        Random random = new Random();
        Position position = new Position();
        position.x = random.nextInt(Config.SMART_CITY_DIMENSION);
        position.y = random.nextInt(Config.SMART_CITY_DIMENSION);
        return position;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
