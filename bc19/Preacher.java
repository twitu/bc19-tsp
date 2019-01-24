package bc19;

import java.util.ArrayList;

public class Preacher {

    // Map data
    ResourceManager resData;
    
    // Self references
    MyRobot robo;
    Robot me;
    Management manager;
    CombatManager combat_manager;
    Comms radio;

    // Private Variables
    RefData refdata;
    int status;
    int initial_move_count;
    Point home_castle, enemy_castle;
    Point guard_loc;

    // Initialization
    public Preacher(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.radio = robo.radio;
        this.combat_manager = robo.combat_manager;
        this.home_castle = null;
        this.enemy_castle = null;
        this.guard_loc = null;

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map, manager.fuel_map, manager.karbo_map);
        refdata = new RefData();
        manager.updateData();
        robo.log("Preacher: Map data acquired");

        // Am I a castle gaurd?
        status = 0;
        initial_move_count = 0;
        Robot base = combat_manager.baseCastleChurch();
        if (base != null && robo.isRadioing(base)) {
            if (base.unit == robo.SPECS.CASTLE) {
                home_castle = new Point(base.x, base.y);
                enemy_castle = manager.oppPoint(base.x, base.y);
                status = 1;
                if (base.signal%16 == 8) {
                    initial_move_count = radio.decodeStepsToEnemy(base.signal);
                }
            } else {
                if (base.signal%16 == 2) {
                    guard_loc = radio.decodeTargetLocation(base.signal);
                }
            }
        }
    }

    // Bot AI
    public Action AI() {

        this.me = robo.me;
        manager.updateData();
        robo.log("I am at " + Integer.toString(me.x) + "," + Integer.toString(me.y));
        

        // Get enemy list in range, identify target and attack if valid
        Point target = combat_manager.preacherTarget();
        if (target != null) {
            return robo.attack(target.x - me.x, target.y - me.y);            
        }

        // if guard location is given move towards guard location
        if (guard_loc != null) {
            Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), true, guard_loc);
            return robo.move(next.x - me.x, next.y - me.y);
        }

        // move specified number of steps toward enemy castle
        if (initial_move_count > 0) {
            initial_move_count--;
            Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), true, enemy_castle);
            if (manager.vis_robot_map[next.y][next.x] > 0) {
                next = manager.findEmptyAdj(next, false);
            }

            return robo.move(next.x - me.x, next.y - me.y);
        }
        if (combat_manager.findSwarmed(robo)) {
            // move toward enemy castle one step if to many allies
            Point next = manager.findEmptyAdj(manager.me_location, false);
            return robo.move(next.x - me.x, next.y - me.y);            
        }

        // If confused sit tight
        return null;
    }
}