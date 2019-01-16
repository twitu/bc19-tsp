package bc19;

public class Comms {

    ///*** Private Variables and Initialization ***///
    // Identification and map
    MyRobot robo;
    Robot me;
    boolean[][] passable_map;
    boolean[][] fuel_map;
    boolean[][] karbo_map;

    // Other Variables

    public Comms(MyRobot robo) {
        this.robo = robo;
        this.me = robo.me;
        passable_map = robo.getPassableMap();
        fuel_map = robo.getFuelMap();
        karbo_map = robo.getKarboniteMap();
    }

    ///*** Communication encoder functions ***///
    

}