package bc19;

import java.util.LinkedList;

public class Prophet {

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
    int mark;
    int status;
    Point home_castle;
    Point enemy_castle;
    Point guard_loc;
    Point home_base;
    int initial_move_count;

    // Initialization
    public Prophet(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.combat_manager = robo.combat_manager;
        this.manager = robo.manager;
        this.radio = robo.radio;
        this.guard_loc = null;
        manager.updateData();

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map,manager.fuel_map, manager.karbo_map);
        refdata = new RefData();
        mark = -1;
        robo.log("Prophet: Map data acquired");

        // Am I a castle gaurd?
        status = 0;
        initial_move_count = 0;
        Robot base = combat_manager.baseCastleChurch();
        if (base != null && robo.isRadioing(base)) {
            if (base.unit == robo.SPECS.CASTLE) {
                home_castle = new Point(base.x, base.y);
                home_base = home_castle;
                enemy_castle = manager.oppPoint(base.x, base.y);
                status = 1;
                if (base.signal%16 == 8) {
                    initial_move_count = radio.decodeStepsToEnemy(base.signal);
                } else if (base.signal%16 == 6) {
                    robo.log("found guard loc");
                    guard_loc = radio.decodeTargetLocation(base.signal);
                }
            } else {
                home_base = new Point(base.x, base.y);
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
        
        // Check for currently marked target
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) continue;
            int dist = (me.x - bot.x)*(me.x - bot.x) + (me.y - bot.y)*(me.y - bot.y);
            if ((bot.id == mark) && dist<=64 && dist >= 16) {
                return robo.attack(bot.x - me.x, bot.y - me.y);
            }
        }

        // Marked target not in range? Listen to broadcast for nearby marks
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) continue;
            if (me.team == bot.team && bot.signal % 16 == 7) {
                mark = (((bot.signal - 7)/16) + 1);
                break;
            }
        }

        // Check for newly marked target
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) continue;
            int dist = (me.x - bot.x)*(me.x - bot.x) + (me.y - bot.y)*(me.y - bot.y);
            if ((bot.id == mark) && dist<=64 && dist >= 16) {
                return robo.attack(bot.x - me.x, bot.y - me.y);
            }
        }
        
        // Check for enemy bots and attack and mark if enemy in range
        Robot closest = null;
        int max_dist = Integer.MAX_VALUE;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) continue;
            int dist = (me.x - bot.x)*(me.x - bot.x) + (me.y - bot.y)*(me.y - bot.y);
            if (bot.team != me.team && refdata.in_attack_range(bot, me) && (dist >= 16) && (dist < max_dist)) {
                    max_dist = dist;
                    closest = bot;
            }
        }
        if (closest != null) {
            mark = closest.id;
            robo.signal(radio.prophetMark(mark), 4);
            return robo.attack(closest.x - me.x, closest.y - me.y);
        }

        // if guard location is given move towards guard location
        if (guard_loc != null) {
            robo.log("found my guard loc moving there");
            Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), true, true,  guard_loc);
            return robo.move(next.x - me.x, next.y - me.y);
        }

        // while initial moves move towards castle
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

        // Nothing to do
        return null;
    }
}