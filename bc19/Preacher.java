package bc19;

import java.util.ArrayList;

public class Preacher {

    // Map data
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

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map, manager.fuel_map, manager.karbo_map);
        robo.log("Preacher: Map data acquired");
    }

    // Bot AI
    public Action AI() {

        this.me = robo.me;
        manager.updateData();
        return null;

    }

}