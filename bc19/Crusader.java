package bc19;

import java.util.ArrayList;
import java.util.HashSet;

public class Crusader {

    // Map data
    ResourceManager resData;
    
    // Self references
    MyRobot robo;
    Robot me;
    Management manager;
    CombatManager combat_manager;
    Comms radio;
    RefData refdata;

    // Private variables
    Point home_castle, enemy_castle,home_base;
    Point target_loc;
    int state, initial_move_count;
    Point guard_loc;
    int mark, guard_loc_count;
    ArrayList<Point> edge = new ArrayList<>();

    // Initialization
    public Crusader(MyRobot robo) {

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
        this.refdata = robo.refdata;
        this.resData = robo.resData;

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map, manager.fuel_map, manager.karbo_map);
        robo.log("Crusader: Initialization state " + Integer.toString(state));

        // Analyze map symmetry and get default direction
        if (manager.vsymmetry) {
            if (manager.map_length - me.y > me.y) {
                for (int i = 0; i < manager.map_length; i++) {
                    edge.add(new Point(i, 0));
                }
            } else {
                for (int i = 0; i < manager.map_length; i++) {
                    edge.add(new Point(i, manager.map_length - 1));
                }
            }
        } else {
            if (manager.map_length - me.x > me.x) {
                for (int i = 0; i < manager.map_length; i++) {
                    edge.add(new Point(0, i));
                }
            } else {
                for (int i = 0; i < manager.map_length; i++) {
                    edge.add(new Point(manager.map_length - 1, i));
                }
            }
        }

    }

    // Bot AI
    public Action AI() {
        
        this.me = robo.me;
        manager.updateData();

        // variables for iterators
        Robot closest;

        // Check for enemy in attack range and attack
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot) || me.team == bot.team) continue;
            if (refdata.inAttackRange(bot, me)) return robo.attack(bot.x - me.x, bot.y - me.y);
        }

        // Listen to broadcast for nearby marks
        HashSet<Integer> marks = new HashSet<>();
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) continue;
            if (me.team == bot.team && bot.signal % 16 == 7) {
                mark = (((bot.signal - 7) /16) + 1);
                marks.add(mark);
            }
        }

        // Check visible range for marked target
        closest = null;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) continue;
            int max_dist = Integer.MAX_VALUE;
            if (marks.contains(bot.id)) {
                int dist = (bot.x - me.x)*(bot.x - me.x) + (bot.y - me.y)*(bot.y - me.y);
                if (dist < max_dist) {
                    max_dist = dist;
                    closest = bot;
                }
            }
        }
        
        if (closest != null) {
            Point next = manager.findNextStep(me.x, me.y, MyRobot.nine_directions, false, true, new Point(closest.x, closest.y));
            return robo.move(next.x - me.x, next.y - me.y);
        }

        // Check for enemies in range that me has to defend and escape
        closest = null;
        int max_dist = Integer.MAX_VALUE;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot) || me.team == bot.team) continue;
            if (refdata.inAttackRange(me, bot)) {
                int dist = (bot.x - me.x)*(bot.x - me.x) + (bot.y - me.y)*(bot.y - me.y);
                if (dist < max_dist) {
                    max_dist = dist;
                    closest = bot;
                }                
            }
        }

        if(state == 1) {
            // while initial moves move towards enemy castle
            if (initial_move_count > 0) {
                initial_move_count--;
                Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, true, true, enemy_castle);
                return robo.move(next.x - me.x, next.y - me.y);
            }
            state = 0;
        }

        if (state == 3) {
            // move towards target location
            Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, true, true, target_loc);
            return robo.move(next.x - me.x, next.y - me.y);
        }

        if (state == 2) {
            // find number of steps to reach guard location
            robo.log("guard location is x" + Integer.toString(guard_loc.x) + " y " + Integer.toString(guard_loc.y));
            guard_loc_count = manager.numberOfMoves(manager.me_location, guard_loc, MyRobot.adj_directions);
            state = 4;
        }

        if (state == 4) {
            // move calculated number of steps towards destination
            if (guard_loc_count > 0) {
                if (--guard_loc_count == 0) {
                    state = 0;
                }
                Point next = combat_manager.stepToGuardPoint(guard_loc, true, MyRobot.adj_directions);
                robo.log("Next move to guard point x" + Integer.toString(next.x) + " y " + Integer.toString(next.y));
                return robo.move(next.x - me.x, next.y - me.y);
            }
        }

        // current swarm is hard coded to 6
        if (state == 0) {
            Point next = combat_manager.findSwarmedMove(home_base);
            if (next != null) return robo.move(next.x - me.x, next.y - me.y);
        }
        
        // populate the map
        if (state == 5) {
            Point next = manager.coulombRepel();
            if (next != null) {
                return robo.move(next.x - me.x, next.y - me.y);
            }
        }

        // current swarm is hard coded to 6
        Point next = combat_manager.findSwarmedMove(home_base);
        if (next != null) return robo.move(next.x - me.x, next.y - me.y);

        return null;
    }

}