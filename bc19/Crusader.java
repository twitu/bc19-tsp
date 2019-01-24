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
    int status, initial_move_count;
    Point guard_loc;
    int mark;

    // Initialization
    public Crusader(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.home_castle = null;
        this.enemy_castle = null;
        this.combat_manager = combat_manager;
        this.radio = robo.radio;
        this.guard_loc = null;
        this.refdata = new RefData();

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map, manager.fuel_map, manager.karbo_map);
        robo.log("Crusader: Map data acquired");

        // Am I a castle gaurd?
        status = 0;
        initial_move_count = 0;
        Robot base = combat_manager.baseCastleChurch();
        home_base = new Point(base.x, base.y);
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

        // variables for iterators
        Robot closest;

        // Check for enemy in attack range and attack
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) continue;
            if (refdata.in_attack_range(bot, me)) return robo.attack(bot.x - me.x, bot.y - me.y);
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
            Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), false, false, new Point(closest.x, closest.y));
            return robo.move(next.x - me.x, next.y - me.y);
        }

        // Check for enemies in range that me has to defend and escape
        closest = null;
        int max_dist = Integer.MAX_VALUE;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) continue;
            if (refdata.in_attack_range(me, bot)) {
                int dist = (bot.x - me.x)*(bot.x - me.x) + (bot.y - me.y)*(bot.y - me.y);
                if (dist < max_dist) {
                    max_dist = dist;
                    closest = bot;
                }                
            }
        }

        if (closest != null) {
            Point next = manager.findFarthestMove(manager.me_location, new Point(closest.x, closest.y), MyRobot.four_directions);
            return robo.move(next.x - me.x, next.y - me.y);
        }

        // if guard location is given move towards guard location
        if (guard_loc != null) {
            Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), true, true,  guard_loc);
            return robo.move(next.x - me.x, next.y - me.y);
        }

        if (initial_move_count > 0) {
            initial_move_count--;
            Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), true, true,  enemy_castle);
            if (manager.getRobotIdMap(next.x, next.y) > 0) {
                next = manager.findEmptyAdj(next, false);
            }

            return robo.move(next.x - me.x, next.y - me.y);
        }

        // current swarm is hard coded to 6
        Point next = combat_manager.findSwarmedMove(home_base);
        if (next != null) return robo.move(next.x - me.x, next.y - me.y);

        return null;
    }

}