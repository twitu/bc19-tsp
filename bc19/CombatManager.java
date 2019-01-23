package bc19;

import java.util.LinkedList;

public class CombatManager {

    MyRobot robo;
    Robot me;
    RefData refdata;

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

    public CombatManager(MyRobot robo) {
        this.refdata = new RefData();
        this.robo = robo;
        this.me = me;                        
    }

    public Point preacherRetreat(Robot[] visRobots, MyRobot robo) {
        Point retreat = null;
        int high_score = 0;
        Robot dangerous = null;
        for (Robot bot: visRobots) {
            int score = 0;
            if (refdata.in_attack_range(robo.me, bot)) {
                score += 100;
            }

            if (bot.unit == robo.SPECS.PREACHER) {
                score += 100;
            } else if (bot.unit == robo.SPECS.PROPHET) {
                score += 80;
            } else if (bot.unit == robo.SPECS.CRUSADER) {
                score += 50;
            }

            if (score > high_score) {
                high_score = score;
                dangerous = bot;
            }
        }

        if (dangerous != null) {
            Point dest = new Point(dangerous.x, dangerous.y);
            Point next = robo.manager.findNextStep(robo.me.x, robo.me.y, robo.manager.passable_map, true, dest);
            retreat = new Point(-next.x, -next.y);
        }

        return retreat;
    }

    // Find target for max AOE effect
    public Point preacherTarget() {
        this.me = robo.me;
        int min_score = 0;
        Point target = null;
        for (Robot bot: robo.manager.vis_robots) {
            if (robo.isVisible(bot) && (bot.team != me.team) && (refdata.in_attack_range(bot, me))) {
                for (Point p: CombatManager.aoe) {
                    int dist = (me.x - bot.x - p.x)*(me.x - bot.x - p.x) + (me.y - bot.y - p.y)*(me.y - bot.y - p.y);
                    if (dist > RefData.atk_range[robo.SPECS.PREACHER]) {
                        continue;
                    }
                    int score = 0;
                    for (Point q: CombatManager.aoe) {
                        if ((robo.manager.getRobotIdMap(bot.x + p.x + q.x,bot.y + p.y + q.y) > 0)) {
                            Robot adj_bot = robo.getRobot(robo.manager.getRobotIdMap(bot.x + p.x + q.x, bot.y + p.y + q.y));
                            score += ((adj_bot.team == me.team) ? -1: 1);
                        }
                    }
                    if (score >= min_score) {
                        min_score = score;
                        target = new Point(bot.x + p.x, bot.y + p.y);
                    }
                }
            }
        }

        if (min_score > 0) {
            return target;
        } else {
            return null;            
        }
    }

    public boolean findSwarmed(MyRobot robo) {
        int count = 0;
        Point adj;
        for (Point p: MyRobot.adj_directions) {
            adj = p.add(robo.manager.me_location);
            if (robo.manager.getRobotIdMap(adj.x,adj.y) > 0) {
                Robot bot = robo.getRobot(robo.manager.getRobotIdMap(adj.x,adj.y));
                if (bot.team == robo.me.team) count++;
                if (count >= 6) return true;                                                                            
            }
        }
        return false;
    }
}