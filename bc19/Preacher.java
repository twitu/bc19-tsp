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
    int state;
    int initial_move_count;
    Point home_castle, enemy_castle,home_base;
    Point guard_loc, target_loc;
    int guard_loc_count;

    // Initialization
    public Preacher(MyRobot robo) {
        
        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.home_base = robo.home_base;
        this.home_castle = robo.home_castle;
        this.enemy_castle = robo.enemy_castle;
        this.combat_manager = robo.combat_manager;
        this.radio = robo.radio;
        this.guard_loc = robo.guard_loc;
        this.target_loc = robo.target_loc;
        this.guard_loc_count = 0;
        this.initial_move_count = robo.initial_move_count;
        this.state = robo.state;

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map, manager.fuel_map, manager.karbo_map);
        refdata = new RefData();
        manager.updateData();
        robo.log("Preacher: Map data acquired");
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

        // move initial number of moves
        if(state == 1){
            // while initial moves move towards enemy castle
            if (initial_move_count > 0) {
                initial_move_count--;
                Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, true, enemy_castle);
                return robo.move(next.x - me.x, next.y - me.y);
            }
            state = 0;
        }

        if (state == 3) {
            // move towards target location
            Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, true, target_loc);
            if (next.equals(target_loc)) {
                state = 0;
            }
            return robo.move(next.x - me.x, next.y - me.y);
        }

        if(state == 2){
            // find number of steps to reach guard location
            robo.log("found my guard loc moving there");
            guard_loc_count = manager.numberOfMoves(manager.me_location, guard_loc, MyRobot.adj_directions);
            state = 4;
        }

        if (state == 4) {
            // move calculated number of steps towards destination
            if (guard_loc_count > 0) {
                if (--guard_loc_count == 0) {
                    state = 0;
                }
                Point next = manager.findNextStep(me.x, me.y, MyRobot.adj_directions, true, guard_loc);
                return robo.move(next.x - me.x, next.y - me.y);
            }
        }

        // current swarm is hard coded to 6
        if (state == 0) {
            Point next = combat_manager.findSwarmedMove(home_base);
            if (next != null) return robo.move(next.x - me.x, next.y - me.y);
        }

        // If confused sit tight
        return null;
    }
}