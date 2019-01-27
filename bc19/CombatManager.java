package bc19;

import java.util.ArrayList;

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
            Point next = robo.manager.findNextStep(me.x, me.y, MyRobot.four_directions, false, true, dest);
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

    public Robot closestEnemyToDefend(Point current) {
        int min_dist = Integer.MAX_VALUE;
        int dist;
        Robot chosen = null;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot) || bot.team == me.team) continue;
            if (refdata.inAttackRange(manager.me_location, bot)){
                dist = (current.x - bot.x)*(current.x - bot.x) + (current.y - bot.y)*(current.y - bot.y);
                if (dist < min_dist) {
                    min_dist = dist;
                    chosen = bot;
                }
            }
        }

        return chosen;
    }

    public Robot closestVisibleEnemy(Point current) {
        int min_dist = Integer.MAX_VALUE;
        int dist;
        Robot chosen = null;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot) || bot.team == me.team) continue;
            if (refdata.inVisibleRange(bot, manager.me)) {
                dist = (current.x - bot.x)*(current.x - bot.x) + (current.y - bot.y)*(current.y - bot.y);
                if (dist < min_dist) {
                    min_dist = dist;
                    chosen = bot;
                }
            }
        }

        return chosen;
    }

    public ArrayList<Robot> visibleEnemies(Robot me) {
        ArrayList<Robot> enemies = new ArrayList<>();
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot) || bot.team == me.team) continue;
            enemies.add(bot);
        }
        return enemies;
    }

    public ArrayList<Robot> defendFromEnemies(Robot me, ArrayList<Robot> visible) {
        ArrayList<Robot> enemies = new ArrayList<>();
        for (Robot bot: visible) {
            if (refdata.inAttackRange(me, bot)) {
                enemies.add(bot);
            }
        }
        return enemies;
    }

    public Robot closestEnemy(Robot me, ArrayList<Robot> bots) {
        Robot chosen = null;
        int min_dist = Integer.MIN_VALUE, dist = 0;
        for (Robot bot: bots) {
            dist = (me.x - bot.x)*(me.x - bot.x) + (me.y - bot.y)*(me.y - bot.y);
            if (dist < min_dist) {
                chosen = bot;
            }
        }

        return chosen;
    }

    public ArrayList<Robot> visibleAllyList(Robot me) {
        ArrayList<Robot> allies = new ArrayList<>();
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot) && bot.team != me.team) continue;
            allies.add(bot);
        }
        return allies;
    }

    public ArrayList<Robot> checkVisibleUnit(int unit, ArrayList<Robot> visible) {
        ArrayList<Robot> allies = new ArrayList<>();
        for (Robot bot: visible) {
            if (bot.unit == unit) {
                allies.add(bot);
            }
        }
        return allies;
    }

    public int countVisibleUnit(int unit, ArrayList<Robot> visible) {
        int count = 0;
        for (Robot bot: visible) {
            if (bot.unit == unit) {
                count++;
            }
        }
        return count;
    }

    public ArrayList<Robot> checkRadioAllies(ArrayList<Robot> bots, int signal) {
        ArrayList<Robot> allies = new ArrayList<>();
        for (Robot bot: bots) {
            if (robo.isRadioing(bot) && bot.signal%16 == signal) {
                allies.add(bot);
            }
        }
        return allies;
    }


    public ArrayList<Robot> unitsInRange(Point current, int range, ArrayList<Robot> bots) {
        ArrayList<Robot> units = new ArrayList<>();
        for (Robot bot: bots) {
            if ((current.x - bot.x)*(current.x - bot.x) + (current.y - bot.y)*(current.y - bot.y) <= range) {
                units.add(bot);
            }
        }
        return units;
    }


    public Robot closestEnemyToAttack(Point current) {
        int min_dist = Integer.MAX_VALUE;
        int dist;
        Robot chosen = null;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot) || bot.team == me.team) continue;
            if (refdata.inAttackRange(bot, manager.me)){
                dist = (current.x - bot.x)*(current.x - bot.x) + (current.y - bot.y)*(current.y - bot.y);
                if (dist < min_dist) {
                    min_dist = dist;
                    chosen = bot;
                }
            }
        }

        return chosen;
    }

    public Robot closestEnemy(Point current, ArrayList<Robot> bots) {
        int min_dist = Integer.MAX_VALUE;
        int dist;
        Robot chosen = null;
        for (Robot bot: bots) {
            dist = (current.x - bot.x)*(current.x - bot.x) + (current.y - bot.y)*(current.y - bot.y);
            if (dist < min_dist) {
                min_dist = dist;
                chosen = bot;
            }
        }

        return chosen;
    }

    public Point markedTarget(int mark, Robot me) {
        Point marked = null;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot) || bot.team == me.team) continue;
            int dist = (me.x - bot.x)*(me.x - bot.x) + (me.y - bot.y)*(me.y - bot.y);
            if (bot.id == mark && dist<=64 && dist >= 16 && bot.team == me.team) {
                marked = new Point(bot.x, bot.y);
            }
        }

        return marked;
    }

    public Point findSwarmedMove(Point home_base) {
        int count = 0;
        Point adj = null;
        for (Point p: MyRobot.adj_directions) {
            adj = p.add(robo.manager.me_location);
            if (robo.manager.getRobotIdMap(adj.x,adj.y) > 0) {
                Robot bot = robo.getRobot(robo.manager.getRobotIdMap(adj.x,adj.y));
                if (bot.team == robo.me.team) count++;
                if (count >= 5) {
                    return manager.findFarthestMove(manager.me_location, home_base, MyRobot.adj_directions);
                }
            }
        }

        return null;
    }

    public Point stepToGuardPoint(Point guard_loc, boolean avoidmines, Point[] directions) {
        Point next = manager.findNextStep(manager.me_location.x, manager.me_location.y, directions, true, true, guard_loc);
        if (next == null) {
            next = manager.findNextStepToPoint(manager.me_location, directions, avoidmines, guard_loc);
        }

        return next;
    }

    public Point nextSafeStep(Robot me, ArrayList<Robot> enemies, Point[] directions, boolean closest) {
        Point chosen = null, next = null;
        int dist = 0;
        if (closest) {
            int min_dist = Integer.MAX_VALUE;
            for (Point p: directions) {
                next = p.add(manager.me_location);
                boolean flag = true;
                if (manager.checkBounds(next.x, next.y) && manager.passable_map[next.y][next.x] && manager.vis_robot_map[next.y][next.x] <= 0) {
                    for (Robot enemy: enemies) {
                        if (refdata.inAttackRange(next, enemy)){ 
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        dist = next.dist(manager.me_location);
                        if (dist < min_dist) {
                            min_dist = dist;
                            chosen = next;
                        }
                    }
                }
            }
        } else {
            int max_dist = Integer.MIN_VALUE;
            for (Point p: directions) {
                next = p.add(robo.manager.me_location);
                boolean flag = true;
                if (manager.checkBounds(next.x, next.y) && manager.passable_map[next.y][next.x] && manager.vis_robot_map[next.y][next.x] <= 0) {
                    for (Robot enemy: enemies) {
                        if (refdata.inAttackRange(next, enemy)){ 
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {    
                        dist = next.dist(manager.me_location);
                        if (dist > max_dist) {
                            max_dist = dist;
                            chosen = next;
                        }
                    }
                }
            }
        }

        return chosen;
    }

    public Point findNextSafePoint(Robot me, Robot other, Point[] directions, boolean closest) {
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