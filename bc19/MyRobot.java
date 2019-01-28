package bc19;

public class MyRobot extends BCAbstractRobot {

    ///*** Directions ***///
    // Adjacent Squares
    public static Point[] adj_directions = {
        new Point(0, 1),
        new Point(1, 1),
        new Point(1, 0),
        new Point(1, -1),
        new Point(0, -1),
        new Point(-1, -1),
        new Point(-1, 0),
        new Point(-1, 1)
    };

    public static Point[] diag_directions = {
        new Point(1, 1),
        new Point(1, -1),
        new Point(-1, -1),
        new Point(-1, 1)
    };

    public static Point[] non_diag_directions = {
        new Point(0, 1),
        new Point(1, 0),
        new Point(0, -1),
        new Point(-1, 0)
    };

    // r^2 = 4
    public static Point[] four_directions = {
        new Point(0, 1),
        new Point(1, 1),
        new Point(1, 0),
        new Point(1, -1),
        new Point(0, -1),
        new Point(-1, -1),
        new Point(-1, 0),
        new Point(-1, 1),
        // extended directions
        new Point(0, 2),
        new Point(2, 0),
        new Point(0, -2),
        new Point(-2, 0)
    };

    // r^2 = 9
    public static Point[] nine_directions = {
        new Point(0, 1),
        new Point(1, 1),
        new Point(1, 0),
        new Point(1, -1),
        new Point(0, -1),
        new Point(-1, -1),
        new Point(-1, 0),
        new Point(-1, 1),
        // extended directions
        new Point(0, 2),
        new Point(2, 0),
        new Point(0, -2),
        new Point(-2, 0),
        // more extended
        new Point(0, 3),
        new Point(1, 2),
        new Point(2, 1),
        new Point(3, 0),
        new Point(2, -1),
        new Point(1, -2),
        new Point(0, -3),
        new Point(-1, -2),
        new Point(-2, -1),
        new Point(-3, 0),
        new Point(-2, 1),
        new Point(-1, 2),
        // others
        new Point(2, 2),
        new Point(2, -2),
        new Point(-2, 2),
        new Point(-2, -2)
    };

    // Attack troops pipeline
    public static int[] tiger_squad = {
        4,      // sniper
        4,
        4,
        4,
        5       // tank
    };

    ///*** Private Variables ***///
    Castle castle;
    Church church;
    Pilgrim pilgrim;
    Crusader crusader;
    Prophet prophet;
    Preacher preacher;
    int state, initial_move_count;
    Point guard_loc, home_castle, enemy_castle, home_base;
    Point target_loc;
    ResourceManager resData;
    RefData refdata;
    
    ///*** Helpers ***///
    public Management manager;
    public Comms radio;
    public CombatManager combat_manager;

    ///*** Main Code ***///
    public Action turn() {
        
        
        // Initialization
        if (me.turn == 1) {
            manager = new Management(this);
            radio = new Comms(this);
            combat_manager = new CombatManager(this);
            resData = new ResourceManager(manager.passable_map, manager.fuel_map, manager.karbo_map);
            refdata = new RefData();
            resData.pairClusters(me.x, me.y, manager.map_length, manager.vsymmetry, manager);

            if(me.unit == SPECS.CASTLE || me.unit == SPECS.CHURCH){
                home_base = new Point(me.x,me.y);
            }

            // for military units
            if (me.unit == SPECS.PROPHET || me.unit == SPECS.PREACHER || me.unit == SPECS.CRUSADER) {
                state = 0;
                initial_move_count = 0;
                guard_loc = null;
                home_castle = null;
                enemy_castle = null;
                home_base = null;
                target_loc = null;
                Robot base = combat_manager.baseCastleChurch();
                home_base = new Point(base.x, base.y);
                if (base != null && this.isRadioing(base)) {
                    if (base.unit == this.SPECS.CASTLE) {
                        home_castle = new Point(base.x, base.y);
                        home_base = home_castle;
                        enemy_castle = manager.oppPoint(base.x, base.y);
                        if (base.signal%16 == 8) {
                            state = 1;
                            initial_move_count = radio.decodeStepsToEnemy(base.signal);
                        } else if (base.signal%16 == 6) {
                            state = 2;
                            guard_loc = radio.decodeAssignGuard(base.signal);
                        } else if (base.signal%16 == 2) {
                            state = 3;
                            target_loc = radio.decodeTargetLocation(base.signal);
                        } else if (base.signal%16 == 9) {
                            state = 5;
                        }
                    } else { // home base is a church
                        home_base = new Point(base.x, base.y);
                        if (base.signal%16 == 2) {
                            state = 3;
                            target_loc = radio.decodeTargetLocation(base.signal);
                        } else if (base.signal%16 == 6) {
                            state = 2;
                            guard_loc = radio.decodeAssignGuard(base.signal);
                        }else if (base.signal%16 == 9) {
                            state = 5;
                        }
                    }
                }
            }

            switch (me.unit) {
                case 0: castle = new Castle(this);
                        break;
                case 1: church = new Church(this);
                        break;
                case 2: pilgrim = new Pilgrim(this);
                        break;
                case 3: crusader = new Crusader(this);
                        break;
                case 4: prophet = new Prophet(this);
                        break;
                case 5: preacher = new Preacher(this);
                        break;
            }
        }

        // Select Unit type and AI
        switch (me.unit) {
            case 0:     return castle.AI();
            case 1:     return church.AI();
            case 2:     return pilgrim.AI();
            case 3:     return crusader.AI();
            case 4:     return prophet.AI();
            case 5:     return preacher.AI();
            default:    return null;
        }

    }
}