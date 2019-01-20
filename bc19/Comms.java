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
    }

    ///*** Communication encoder functions ***///
    // LSB code : 0, 1 . Tell unit to set its base ID to given ID. 1 for emergency
    public int baseAssignment(int ID, boolean emergency) {
        if (emergency) {
            return (ID * 16);
        } else {
            return (ID * 16 + 1);
        }
    }

    // LSB code :2 . Specify a location to target
    public int targetLocation(int x, int y) {
        return (x*1024 + y*16 + 2);
    }

    // LSB code :3 . Used by church to assign a depot to a pilgrim
    public int assignDepot(Point p){
        return (p.x*1024 + p.y*16 + 3);
    }

    // LSB code :4 . Clear path to move @ dest P
    public int clearPath(Point p){
        return (p.x*1024 + p.y*16 + 4);
    }

    // LSB code :5 . Base under attack
    public int emergency(Point p){
        return (p.x*1024 + p.y*16 + 5);
    }

}