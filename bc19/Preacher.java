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
    Point guard_loc;
    int initial_move_count;
    Point home_castle, enemy_castle;

    // Initialization
    public Preacher(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.radio = robo.radio;
        this.combat_manager = robo.combat_manager;
        this.initial_move_count = 0;
        this.home_castle = null;
        this.enemy_castle = null;

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map, manager.fuel_map, manager.karbo_map);
        refdata = new RefData();
        guard_loc = new Point(me.x, me.y);
        manager.updateData();
        robo.log("Preacher: Map data acquired");

        // Am I a castle gaurd?
        status = 0;
        for (Point p: MyRobot.adj_directions) {
            if (!manager.checkBounds(me.x + p.x, me.y + p.y)) continue;
            if (manager.vis_robot_map[me.y + p.y][me.x + p.x] > 0) {
                Robot bot = robo.getRobot(manager.vis_robot_map[me.y + p.y][me.x + p.x]);
                if (bot.unit == robo.SPECS.CASTLE && robo.isRadioing(bot)) {
                    home_castle = new Point(bot.x, bot.y);
                    enemy_castle = manager.oppPoint(bot.x, bot.y);
                    status = 1;
                    if (bot.signal%16 == 8) {
                        initial_move_count = bot.signal/16;
                    }
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
            Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), true, enemy_castle);
            if (manager.vis_robot_map[next.y][next.x] > 0) {
                next = manager.findEmptyAdj(next, false);
            }

            return robo.move(next.x - me.x, next.y - me.y);            
        }

        // If confused sit tight
        return null;
    }
}