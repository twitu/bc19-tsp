package bc19;

import java.util.LinkedList;

public class Prophet {

    // Map data
    ResourceManager resData;
    boolean type; // church type false, castle type = true
    Point home_castle, enemy_castle, home_church;
    
    // Self references
    MyRobot robo;
    Robot me;
    Management manager;
    CombatManager combat;
    Comms radio;
    int status;
    Point dest;

    // Initialization
    public Prophet(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.radio = robo.radio;
        this.combat = new CombatManager();
        this.dest = null;
        manager.updateData();
        status = 0;

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map,manager.fuel_map, manager.karbo_map);
        robo.log("Prophet: Map data acquired");

        // find nearby church or castle
        for (Point p: MyRobot.adj_directions) {
            Robot bot;
            if (manager.vis_robot_map[p.x + me.x][p.y + me.y] > 0) {
                bot = robo.getRobot(manager.vis_robot_map[p.x + me.x][p.y + me.y]);
                if (bot.unit == robo.SPECS.CASTLE) {
                    home_castle = new Point(bot.x, bot.y);
                    enemy_castle = manager.oppPoint(bot.x, bot.y);
                    type = true;
                    if (robo.isRadioing(bot) && bot.signal%16 == 2) {
                        status = 1;
                        dest = new Point(bot.signal/1024, (bot.signal%1024)/16);
                    }
                    break;
                } else if (bot.unit == robo.SPECS.CHURCH) {
                    home_church = new Point(bot.x, bot.y);
                    type = false;
                    if (robo.isRadioing(bot) && bot.signal%16 == 2) {
                        status = 1;
                        dest = new Point(bot.signal/1024, (bot.signal%1024)/16);
                    }
                    break;
                }
            }
        }

        if (dest != null) {
            robo.log("Current destination " + Integer.toString(dest.x) + " " + Integer.toString(dest.y));
        }
    }

    // Bot AI
    public Action AI() {
        robo.log("I am at " + Integer.toString(me.x) + "," + Integer.toString(me.y));
        manager.updateData();

        // status 1 move to target location
        if (status == 1) {
            LinkedList<Point> src = new LinkedList<>();
            src.add(dest);
            Point next = manager.findNextStep(manager.me_location.x, manager.me_location.y, manager.copyMap(manager.passable_map), true, src);
            if (next.x == dest.x && next.y == dest.y) {
                robo.log("reaching destination");
                status = 0;
            }
            return robo.move(next.x - me.x, next.y - me.y);
        } else if (status == 0) {
            // scan for enemies
            Point retreat = combat.preacherRetreat(manager.vis_robots, robo);
            if (retreat != null) {
                return robo.move(retreat.x - me.x, retreat.y - me.y);
            }
            Robot next_enemy = combat.preacherChooseBestEnemy(manager.vis_robots, robo);
            return robo.attack(next_enemy.x - me.x, next_enemy.y - me.y);
        }
    }
}