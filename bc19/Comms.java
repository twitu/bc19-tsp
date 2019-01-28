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
    public int assignGuard(Point p){
        return (p.x*1024 + p.y*16 + 6);
    }

    // LSB code: 7. Base under attack
    public int prophetMark(int id){
        return ((id - 1)*16 + 7);
    }

    // LSB code: 8. Move n steps towards enemy castle
    public int stepsToEnemy(int steps){
        return ((steps)*16 + 8);
    }

    // LSB code: 9. Tell crusader that you are a dummy(stack/lattice)
    public int latticeDefend() {
        return 9;
    }

    // LSB code: 10. Tell all units about attack
    public int yellowAlert(Point base) {
        return base.x*1024 + base.y*16 + 10;
    }

    // LSB code: 11. Tell all units about impending doom 
    // and to give their lives for king and country
    public int redAlert(Point base) {
        return base.x*1024 + base.y*16 + 11;
    }

    // LSB code: 12. Preacher rush to target location
    public int pantherStrike(Point target) {
        return target.x*1024 + target.y*16 + 12;
    }

    // LSB code 13
    public int yellowAlert(int base_id) {
        return 13*1024 + (base_id+64)*16 + 13;
    }

    //LSB code 15
    public int redAlert(int base_id) {
        return 15*1024 + (base_id+64)*16 +15;
    }

    // LSB code: 14. Preacher rush to target location
    public int snapGrid() {
        return 110;
    }


    ///*** Communication decoder functions ***///
    // Decode first location
    public Point decodes3(int signal){
        Point p = new Point(-1, -1);
        p.x = signal/1024;
        p.y = (signal % 1024)/16;
        return p;
    }

    // Decode LSB code: 8. Move n steps towards enemy castle
    public int decodeStepsToEnemy(int signal){
        return (signal-8)/16;
    }

    // Decode LSB code: 2. target location
    public Point decodeTargetLocation(int signal){
        return new Point(signal/1024, (signal % 1024)/16);
    }

    // Decode LSB code: 6. Base under attack
    public Point decodeAssignGuard(int signal){
        return new Point(signal/1024, (signal % 1024)/16);
    }

    // Decode LSB code: 12. Preacher alert
    public Point decodePantherStrike(int signal){
        return new Point(signal/1024, (signal % 1024)/16);
    }

    public int decodeYellowAlert(int signal) {
        if (signal%1024 == 13 && signal/1024 == 13) {
            return signal/1024%16;
        }
        return -1;
    }

    public int decodeRedAlert(int signal) {
        if (signal%1024 == 13 && signal/1024 == 13) {
            return signal/1024%16;
        }
        return -1;
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
    // LSB code: 3'b011. Rush and strike.
    public int death(int id) {
        return (id * 8 + 3);
    }

}