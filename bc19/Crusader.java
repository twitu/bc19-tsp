package bc19;

import java.util.ArrayList;

public class Crusader {

    // Map data
    ResourceManager resData;
    
    // Self references
    MyRobot robo;
    Robot me;
    Management manager;
    CombatManager combat_manager;
    Comms radio;

    // Private variables
    Point home_castle, enemy_castle;
    int status, initial_move_count;

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

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map, manager.fuel_map, manager.karbo_map);
        robo.log("Crusader: Map data acquired");

        // Am I a castle gaurd?
        status = 0;
        initial_move_count = 0;
        for (Point p: MyRobot.adj_directions) {
            if (manager.getRobotIdMap(me.x + p.x, me.y + p.y) > 0) {
                Robot bot = robo.getRobot(manager.getRobotIdMap(me.x + p.x, me.y + p.y));
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

        if (initial_move_count > 0) {
            initial_move_count--;
            Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), true, enemy_castle);
            if (manager.getRobotIdMap(next.x, next.y) > 0) {
                next = manager.findEmptyAdj(next, false);
            }

            return robo.move(next.x - me.x, next.y - me.y);
        }

        if (combat_manager.findSwarmed(robo)) {
            // move toward enemy castle one step if to many allies
            Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), true, enemy_castle);
            if (manager.getRobotIdMap(next.x, next.y) > 0) {
                next = manager.findEmptyAdj(next, false);
            }

            return robo.move(next.x - me.x, next.y - me.y);            
        }

        return null;
    }

}