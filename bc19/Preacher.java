package bc19;

public class Preacher {

    // Map data
    boolean[][] passable_map;
    boolean[][] fuel_map;
    boolean[][] karbo_map;
    ResourceManager resData;
    
    // Self references
    MyRobot robo;
    Robot me;
    Management manager;
    Comms radio;

    // Initialization
    public Preacher(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.radio = robo.radio;

        // Get raw map data
        passable_map = robo.getPassableMap();
        fuel_map = robo.getFuelMap();
        karbo_map = robo.getKarboniteMap();

        // Process and store depot clusters
        resData = new ResourceManager(fuel_map, karbo_map);
        robo.log("Preacher: Map data acquired");

    }

    // Bot AI
    public Action AI() {
        this.me = robo.me;
        
        robo.log("I am at " + Integer.toString(me.x) + "," + Integer.toString(me.y));
        return null;

    }

}