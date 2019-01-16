package bc19;

public class Castle {

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
    public Castle(MyRobot robo) {

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
        robo.log("Castle: Map data acquired");

    }

    // Bot AI
    public Action AI() {
        
        robo.log("I am at " + Integer.toString(me.x) + "," + Integer.toString(me.y));
        return null;

    }

}