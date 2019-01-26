package bc19;

public class CombatManager {

    MyRobot robo;
    Robot me;
    RefData refdata;
    Management manager;

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
        this.me = robo.me; 
        this.manager = robo.manager;                       
    }

    public Robot baseCastleChurch() {
        for (Point p: MyRobot.adj_directions) {
            Point adj = p.add(manager.me_location);
            int bot_id = manager.getRobotIdMap(adj.x, adj.y);
            if (bot_id > 0) {
                Robot bot = robo.getRobot(bot_id);
                if (bot.unit == robo.SPECS.CHURCH || bot.unit == robo.SPECS.CASTLE) {
                    return bot;
                }
            }
        }
        return null;
    }

    public Point preacherRetreat(Robot[] visRobots, MyRobot robo) {
        Point retreat = null;
        int high_score = 0;
        Robot dangerous = null;
        for (Robot bot: visRobots) {
            if (!robo.isVisible(bot)) continue;
            int score = 0;
            if (refdata.inAttackRange(robo.me, bot)) {
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
            Point next = robo.manager.findNextStep(robo.me.x, robo.me.y, MyRobot.four_directions, false,  dest);
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
            if (robo.isVisible(bot) && (bot.team != me.team) && (refdata.inAttackRange(bot, me))) {
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

    public Point findSwarmedMove(Point home_base) {
        int count = 0;
        Point adj = null;
        for (Point p: MyRobot.adj_directions) {
            adj = p.add(robo.manager.me_location);
            if (robo.manager.getRobotIdMap(adj.x,adj.y) > 0) {
                Robot bot = robo.getRobot(robo.manager.getRobotIdMap(adj.x,adj.y));
                if (bot.team == robo.me.team) count++;
                if (count >= 6) {
                    return manager.findFarthestMove(manager.me_location, home_base, MyRobot.adj_directions);
                }
            }
        }

        return null;
    }

    public Point findNextSafePoint(Robot me, Robot other, boolean closest) {
        Point[] directions = (RefData.speed[me.unit] == 4) ? MyRobot.four_directions : MyRobot.nine_directions;
        Point chosen = null, next = null;
        int dist = 0;
        if (closest) {
            int min_dist = Integer.MAX_VALUE;
            for (Point p: directions) {
                next = p.add(manager.me_location);
                if (manager.checkBounds(next.x, next.y) && manager.passable_map[next.y][next.x] && manager.vis_robot_map[next.y][next.x] <= 0) {
                    if (refdata.inAttackRange(next, other)) continue;
                    dist = next.dist(manager.me_location);
                    if (dist < min_dist) {
                        min_dist = dist;
                        chosen = next;
                    }
                }
            }
        } else {
            int max_dist = Integer.MIN_VALUE;
            for (Point p: directions) {
                next = p.add(robo.manager.me_location);
                if (manager.checkBounds(next.x, next.y) && manager.passable_map[next.y][next.x] && manager.vis_robot_map[next.y][next.x] <= 0) {
                    if (refdata.inAttackRange(next, other)) continue;
                    dist = next.dist(manager.me_location);
                    if (dist > max_dist) {
                        max_dist = dist;
                        chosen = next;
                    }
                }
            }
        }

        return chosen;
    }
}