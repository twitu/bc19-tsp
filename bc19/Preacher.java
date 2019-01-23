package bc19;

import java.util.ArrayList;

public class Preacher {

    // AOE
    public static Point[] aoe = {
        new Point(0, 1),
        new Point(1, 1),
        new Point(1, 0),
        new Point(1, -1),
        new Point(0, -1),
        new Point(-1, -1),
        new Point(-1, 0),
        new Point(-1, 1),
        new Point(0, 0)
    };

    // Map data
    ResourceManager resData;
    
    // Self references
    MyRobot robo;
    Robot me;
    Management manager;
    Comms radio;

    // Private Variables
    RefData refdata;
    boolean castle_gaurd;
    Point gaurd_loc;

    // Initialization
    public Preacher(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.radio = robo.radio;

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map, manager.fuel_map, manager.karbo_map);
        refdata = new RefData();
        gaurd_loc = new Point(me.x, me.y);
        manager.updateData();
        robo.log("Preacher: Map data acquired");

        // Am I a castle gaurd?
        castle_gaurd = false;
        for (Point p: MyRobot.adj_directions) {
            if (manager.vis_robot_map[me.y + p.y][me.x + p.x] > 0) {
                Robot bot = robo.getRobot(manager.vis_robot_map[me.y + p.y][me.x + p.x]);
                if (bot.unit == robo.SPECS.CASTLE) {
                    castle_gaurd = true;
                }
            }
        }

    }

    // Bot AI
    public Action AI() {

        this.me = robo.me;
        manager.updateData();
        robo.log("I am at " + Integer.toString(me.x) + "," + Integer.toString(me.y));
        
        // If not castle gaurd, listen to broadcast and get gaurd location
        for (Robot bot: manager.vis_robots) {
            if ((bot.team == me.team) && (bot.signal % 16 == 6)) {
                gaurd_loc.x = bot.signal/1024;
                gaurd_loc.y = (bot.signal % 1024)/16;
                break;
            }
        }

        // Get enemy list in range and identify target
        Point target = new Point(0, 0);
        int min_score = 0;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) {
                continue;
            }
            if ((bot.team != me.team) && (refdata.in_attack_range(bot, me))) {
                for (Point p: aoe) {
                    int dist = (me.x - bot.x - p.x)*(me.x - bot.x - p.x) + (me.y - bot.y - p.y)*(me.y - bot.y - p.y);
                    if (dist > RefData.atk_range[robo.SPECS.PREACHER]) {
                        continue;
                    }
                    int score = 0;
                    for (Point q: aoe) {
                        if ((manager.vis_robot_map[bot.y + p.y + q.y][bot.x + p.x + q.x]) > 0) {
                            Robot adj_bot = robo.getRobot(manager.vis_robot_map[bot.y + p.y + q.y][bot.x + p.x + q.x]);
                            score += ((adj_bot.team == me.team) ? -1: 1);
                        }
                    }
                    if (score >= min_score) {
                        min_score = score;
                        target.x = bot.x + p.x;
                        target.y = bot.y + p.y;
                    }
                }
            }
        }

        // Attack if valid target exists
        if (min_score > 0) {
            return robo.attack(target.x - me.x, target.y - me.y);
        }

        return null;

    }

}