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
    // LSB code: 0, 1. Tell unit to set its base id to given id. 1 for emergency
    public int baseAssignment(int id, boolean emergency) {
        if (emergency) {
            return (id * 16);
        } else {
            return (id * 16 + 1);
        }
    }

    // LSB code: 2. Specify a location to target
    public int targetLocation(int x, int y) {
        return (x*1024 + y*16 + 2);
    }

    // LSB code: 3. Used by church to assign a depot to a pilgrim
    public int assignDepot(Point p){
        return (p.x*1024 + p.y*16 + 3);
    }

    // LSB code: 4. Clear path to move @ dest P
    public int clearPath(Point p){
        return (p.x*1024 + p.y*16 + 4);
    }

    // LSB code: 5. Base under attack
    public int emergency(Point p){
        return (p.x*1024 + p.y*16 + 5);
    }
    
    // LSB code: 6. Base under attack
    public int assignGaurd(Point p){
        return (p.x*1024 + p.y*16 + 6);
    }

    // LSB code: 7. Base under attack
    public int prophetMark(int id){
        return ((id - 1)*16 + 7);
    }

    ///*** CastleTalk encoder functions ***///
    // LSB code: 3'b001. Own base id. I am here and okay
    public int baseID(int id) {
        return (id * 8 + 1);
    }

    // LSB code: 3'b010. Assigned a pilgrim to this base.
    public int baseAssigned(int id) {
        return (id * 8 + 2);
    }

}