package bc19;

import com.sun.glass.ui.Robot;

public class Prophet {

    // Map data
    ResourceManager resData;
    boolean type; // church type false, castle type = true
    Point home_castle, enemy_castle, home_church;
    
    // Self references
    MyRobot robo;
    Robot me;
    Management manager;
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
        manager.update_data();
        status = 0;

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map,manager.fuel_map, manager.karbo_map);
        robo.log("Prophet: Map data acquired");

        // find nearby church or castle
        for (Point p: MyRobot.adj_directions) {
            Robot bot;
            if (manager.vis_robot_map[p.x + me.x][p.y + me.y] > 0) {
                bot = getRobot(vis_robot_map[p.x + me.x][p.y + me.y]);
                if (bot.unit == SPECS.CASTLE) {
                    home_castle = new Point(bot.x, bot.y);
                    enemy_castle = manager.oppPoint(bot.x, bot.y);
                    type = true;
                    if (isRadioing(bot) && bot.signal%16 == 2) {
                        status = 1;
                        dest = new Point(bot.signal/1024, (bot.signal%1024)/16);
                    }
                    break;
                } else if (bot.unit == SPECS.CHURCH) {
                    home_church = new Point(bot.x, bot.y);
                    type = false;
                    if (isRadioing(bot) && bot.signal%16 == 2) {
                        status = 1;
                        dest = new Point(bot.signal/1024, (bot.signal%1024)/16);
                    }
                    break;
                }
            }
        }
    }

    // Bot AI
    public Action AI() {
        robo.log("I am at " + Integer.toString(me.x) + "," + Integer.toString(me.y));
        manager.update_data();

        // status 1 move to target location
        if (status == 1) {
            Point next = manager.findNextStep(manager.me_location, map, true, src);
            if (next.x == dest.x && next.y == dest.y) {
                status = 0;
            }
            return move(next.x - me.x, next.y - me.y);
        } else if (status == 0) {
            // scan for enemies
        }
    }
}