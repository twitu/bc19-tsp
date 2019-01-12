import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Arrays.*;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MoveAction;

public class ShadowBot extends BCAbstractRobot {
    
    ArrayList<Point> church_pos = new ArrayList<>();
    ArrayList<Point> castle_pos = new ArrayList<>();
    boolean[][] passable_map;
    boolean[][] fuel_map;
    boolean[][] karbo_map;

    public ShadowBot() {
        passable_map = getPassableMap();
        fuel_map = getFuelMap();
        karbo_map = getKarboniteMap();
    }

    public copyMap(boolean[][] map) {
        if (map == null) {
            return null;
        }

        boolean[][] copy = new boolean[map.length][];
        for (int i = 0; i < map.length; i++) {
            copy[i] = Arrays.copyOf(map[i], map[i].length);
        }

        return copy;
    }

    public Action turn() {

        ArrayList<ShadowBot> visRobots = new ArrayList<>();
        ArrayList<ShadowBot> adjRobots = new ArrayList<>();
        ArrayList<Integer> visRobotType = new ArrayList<>();
        int[][] visRobotMap;
        Point buildLoc;
        boolean adjChurch, adjCrusader, visChurch;

        if (me.unit == SPECS.CASTLE) {
            if (me.turn == 1) {
                // for first turn add position to list
                castle_pos.add(Point(me.x, me.y));
                return buildUnit(SPECS.PILGRIM,1,0);
            }
        }

        if (me.unit == SPECS.PILGRIM) {
            
            // Am I carrying max capacity resources?
            if ((me.karbonite == 20) || (me.fuel == 100)) {

                // Get adjacentBots
                visRobots = getVisibleRobots();
                adjChurch = 0;
                adjCrusader = 0;
                for (ShadowBot bot : visRobots) {
                    if ((Math.abs(bot.x) <= 1) && (Math.abs(bot.y) <= 1)) {
                        adjRobots.add(bot);
                        if (bot.unit == SPECS.CHURCH) {
                            adjChurch = 1;
                        }
                        else if(bot.unit == SPECS.CRUSADER) {
                            adjCrusader = 1;
                        }
                    }
                }
                
                // Church nearby?
                if (adjChurch) {
                    for (ShadowBot bot : adjRobots) {
                        if (bot.unit == SPECS.CHURCH) {
                            return give(bot.x, bot.y, me.karbonite, me. fuel);
                        }
                    }
                }

                // Crusader then?
                else if (adjCrusader) {
                    for (ShadowBot bot : adjRobots) {
                        if (bot.unit == SPECS.CRUSADER) {
                            return give(bot.x, bot.y, me.karbonite, me. fuel);
                        }
                    }
                }
                
                // Self deliver
                else {
                    
                    // TODO:1. Identify nearest church/castle
                    // TODO:2. Identify path
                    // TODO:3. Advance
                }

            }

            // Check if unit is on a depot
            else if (karbo_map[me.x][me.y]) {

                // Get visible bot types and look for church
                visRobots = getVisibleRobots();
                visChurch = 0;                
                for (ShadowBot bot : visRobots) {
                    if (bot.unit == SPECS.CHURCH) {
                        visChurch = 1;
                    }
                }

                // If church in sight, start mining
                if (visChurch) {
                    return mine();
                }

                // If no church in sight, build a church
                else {
                    buildLoc.x = 2;
                    buildLoc.y = 2;                    
                    visRobotMap = getVisibleRobotMap();
                    for (Point P : Point.adj_directions) {
                        if ((passable_map[P.x][P.y]) && (visRobotMap[P.x][P.y] == 0)) {
                            buildLoc.x = P.x;
                            buildLoc.y = P.y;
                        }                        
                    }
                    if ((buildLoc.x == 2) || (buildLoc.y == 2)) {
                        return mine();
                    }
                    else {
                        return buildUnit(SPECS.CHURCH, buildLoc.x, buildLoc.y);
                    }
                }
            
            }

            // If not, move to a depot
            else {

                // TODO:1. Identify nearest depot
                // TODO:2. Identify path
                // TODO:3. Advance
            }

        }

        if (me.unit == SPECS.CHURCH) {
            if (me.turn == 1) {
                // for first turn add position to list
                church_pos.add(Point(me.x, me.y));
            }
        }

        if (me.unit == SPECS.CRUSADER) {
            // if carrying resource deliver to nearest church and castle
            // TODO: refine algo to fill to max capacity before going to deliver
            if (me.fuel > 0 || me.karbonite > 0) {
                Point myloc = Point(me.x, me.y);
                int[][] visRobMap = getVisibleRobotMap();
                for (Point p: adj_directions) {
                    if (visRobMap[me.x+p.x][me.y+p.y] > 0) {
                        Robot check = getRobot(visRobMap[me.x+p.x][me.y+p.y]);
                        if (check.team == me.team && (check.unit == SPECS.CHURCH || check.unit == SPECS.CASTLE)) {
                            return give(p.x, p.y, me.karbonite, me.fuel);
                        }
                    }
                }

                LinkedList<Point> dest;
                for (Point p: church_pos) {
                    dest.add(p);
                }

                for (Point p: castle_pos) {
                    dest.add(p);
                }

                Point next = Point.findPath(me.x, me.y, copyMap(passable_map), true, dest);

                // check if next point is exactly on destination
                if (visRobMap[next.x][next.y] > 0) {
                    Robot check = getRobot(visRobMap[next.x][next.y]);
                    if (check.team == me.team && (check.unit == SPECS.CASTLE || check.unit == SPECS.CHURCH)) {
                        if (next.x - me.x == 0){
                            if (next.y > me.y) {
                                next.y -= 1;
                            } else {
                                next.y += 1;
                            }
                        } else {
                            if (next.x > me.x) {
                                next.x -= 1;
                            } else {
                                next.x += 1;
                            }
                        }
                    }
                }
                return move(next.x - me.x, next.y - me.y);
            }
        }

        return null;

    }
}