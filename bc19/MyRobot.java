package bc19;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays.*;
import java.util.Arrays;

import java.awt.Point;

public class MyRobot extends BCAbstractRobot {

    //*** Directions ***/
    // Adjacent Squares
    public static Point[] adj_directions = {
        new Point(0, 1),
        new Point(1, 1),
        new Point(1, 0),
        new Point(1, -1),
        new Point(0, -1),
        new Point(-1, -1),
        new Point(-1, 0),
        new Point(-1, 1)
    };

    // r^2 = 4
    public static Point[] four_directions = {
        new Point(0, 1),
        new Point(1, 1),
        new Point(1, 0),
        new Point(1, -1),
        new Point(0, -1),
        new Point(-1, -1),
        new Point(-1, 0),
        new Point(-1, 1),
        // extended directions
        new Point(0, 2),
        new Point(2, 0),
        new Point(0, -2),
        new Point(-2, 0)
    };

    // r^2 = 9
    public static Point[] nine_directions = {
        new Point(0, 1),
        new Point(1, 1),
        new Point(1, 0),
        new Point(1, -1),
        new Point(0, -1),
        new Point(-1, -1),
        new Point(-1, 0),
        new Point(-1, 1),
        // extended directions
        new Point(0, 2),
        new Point(2, 0),
        new Point(0, -2),
        new Point(-2, 0),
        // more extended
        new Point(0, 3),
        new Point(1, 2),
        new Point(2, 1),
        new Point(3, 0),
        new Point(2, -1),
        new Point(1, -2),
        new Point(0, -3),
        new Point(-1, -2),
        new Point(-2, -1),
        new Point(-3, 0),
        new Point(-2, 1),
        new Point(-1, 2),
        // others
        new Point(2, 2),
        new Point(2, -2),
        new Point(-2, 2),
        new Point(-2, -2)
    };

    //*** Private Variables ***/
    ArrayList<Point> church_pos = new ArrayList<>();
    ArrayList<Point> castle_pos = new ArrayList<>();
    Robot[] visRobots;
    int[][] visRobotMap;
    boolean[][] passable_map;
    boolean[][] fuel_map;
    boolean[][] karbo_map;
    Point myLoc;
    LinkedList<Point> fuel_pos = new LinkedList<>();
    LinkedList<Point> karbo_pos = new LinkedList<>();

    //*** Commonly Used Functions ***/
    // Find empty square adjacent to a given unit.
    public Point findEmptyAdj(Robot me) {
        visRobotMap = getVisibleRobotMap();
        for (Point p: MyRobot.adj_directions) {
            if (passable_map[me.x + p.x][me.y + p.y] && (visRobotMap[me.x + p.x][me.y + p.y] == 0)) {
                return new Point(p.x, p.y);
            }
        }

        return null;
    }

    // Find next point to move to given source, a copy of map, movement range and possible destinations
    public Point findPath(int x, int y, boolean[][] map, boolean r_four, LinkedList<Point> src) {
        Point current, next = new Point();
        Point[] directions;

        if (r_four) {
            directions = MyRobot.four_directions;
        } else {
            directions = MyRobot.nine_directions;
        }

        while (!src.isEmpty()) {
            current = src.pollFirst();
            for (Point p: directions) {
                next.move(current.x + p.x, current.y + p.y);
                next = new Point(current.x + p.x, current.y - p.y);
                if (next.x == x && next.y == y) {
                    return next;
                } else {
                    if (map[next.x][next.y] == true) {
                        map[next.x][next.y] = false;
                        src.add(next);
                    } 
                }
            }
        }

        return null;
    }

    public Action ifAdjGive(Robot me, int unit) {
        for (Point p: adj_directions) {
            if (visRobotMap[p.x + me.x][p.y + me.y] > 0) {
                Robot check = getRobot(visRobotMap[p.x + me.x][p.y + me.y]);
                if (check.unit == unit && check.team == me.team) {
                    return give(p.x, p.y, me.karbonite, me.fuel);
                }
            }
        }

        return null;
    }

    public Action homeDelivery(Robot me) {
        LinkedList<Point> dest;
        for (Point p: church_pos) {
            dest.add(p);
        }

        for (Point p: castle_pos) {
            dest.add(p);
        }

        Point next = findPath(me.x, me.y, copyMap(passable_map), true, dest);
        visRobotMap = getVisibleRobotMap();
        // check if next point is exactly on destination
        if (visRobotMap[next.x][next.y] > 0) {
            Robot check = getRobot(visRobotMap[next.x][next.y]);
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

    // Make a copy of a given map.
    public boolean[][] copyMap(boolean[][] map) {
        if (map == null) {
            return null;
        }

        boolean[][] copy = new boolean[map.length][];
        for (int i = 0; i < map.length; i++) {
            copy[i] = Arrays.copyOf(map[i], map[i].length);
        }

        return copy;
    }

    //*** Bot AI ***/
    public Action pilgrimAI(Robot me) {

        // Variables
        ArrayList<Robot> adjRobots = new ArrayList<>();
        boolean adjChurch, adjCrusader, visChurch;
        Point emptyadj;

        // Am I carrying max capacity resources?
        if ((me.karbonite == 20) || (me.fuel == 100)) {

            // Get adjacentBots
            visRobots = getVisibleRobots();
            adjChurch = false;
            adjCrusader = false;
            for (Robot bot : visRobots) {
                if ((Math.abs(bot.x) <= 1) && (Math.abs(bot.y) <= 1)) {
                    adjRobots.add(bot);
                    if (bot.unit == SPECS.CHURCH) {
                        adjChurch = true;
                    } else if(bot.unit == SPECS.CRUSADER) {
                        adjCrusader = true;
                    }
                }
            }
                
            // Church nearby?
            if (adjChurch) {
                for (Robot bot : adjRobots) {
                    if ((bot.unit == SPECS.CHURCH) && (bot.team == me.team)) {
                        return give(bot.x, bot.y, me.karbonite, me. fuel);
                    }
                }

            // Crusader then?
            } else if (adjCrusader) {
                for (Robot bot : adjRobots) {
                    if ((bot.unit == SPECS.CRUSADER) && (bot.team == me.team)) {
                        return give(bot.x, bot.y, me.karbonite, me. fuel);
                    }
                }
                
            // Self deliver
            } else {

                Action A = ifAdjGive(me);
                if (A == null) {
                    return homeDelivery(me);                    
                } else {
                    return A;
                }
            }
        
        // Check if on depot then mine
        } else if (karbo_map[me.x][me.y]) {

            // Get visible bot types and look for church
            visRobots = getVisibleRobots();
            visChurch = false;                
            for (Robot bot : visRobots) {
                if (bot.unit == SPECS.CHURCH) {
                    visChurch = true;
                }
            }

            // If church in sight, start mining
            if (visChurch) {
                return mine();

            // If no church in sight, build a church
            } else {
                emptyadj = findEmptyAdj(me);
                if (emptyadj == null) {
                    return mine();
                } else {
                    return buildUnit(SPECS.CHURCH, emptyadj.x, emptyadj.y);
                }
            }

        // If not, move to a depot
        } else {

            // TODO: decide whether to mine karbo or fuel based on castle talk and set default
            LinkedList<Point> choose_fuel = new LinkedList<>();
            boolean fuel = true;
            for (Point p: fuel_pos) {
                choose_fuel.add(p);
            }
            Point next = findPath(me.x, me.y, copyMap(this.passable_map), true, choose_fuel);
            return move(next.x - me.x, next.y - me.y);
        }
            
    }

    //*** Main Code ***/
    public Action turn() {
        
        // Initializations
        if (me.turn == 1) {
            passable_map = getPassableMap();
            fuel_map = getFuelMap();
            karbo_map = getKarboniteMap();

            for (int i = 0; i < fuel_map.length; i++) {
                for (int j = 0; j < fuel_map[i].length; i++) {
                    if (fuel_map[i][j]) {
                        fuel_pos.add(new Point(i, j));
                    }
                }
            }

            for (int i = 0; i < karbo_map.length; i++) {
                for (int j = 0; j < karbo_map[i].length; i++) {
                    if (karbo_map[i][j]) {
                        karbo_pos.add(new Point(i, j));
                    }
                }
            }
        }

        // Castle AI
        if (me.unit == SPECS.CASTLE) {
            if (me.turn == 1) {
                // for first turn add position to list
                castle_pos.add(new Point(me.x, me.y));
                emptyadj = findEmptyAdj(me);
                if (emptyadj == null) {
                    return null;
                } else {
                    return buildUnit(SPECS.PILGRIM, emptyadj.x, emptyadj.y);
                }
            }
        }

        // Point object for location of selected bot
        myLoc = new Point(me.x, me.y);

        // Pilgrim AI
        if (me.unit == SPECS.PILGRIM) {
            return pilgrimAI(me);
        }

        // Church AI
        if (me.unit == SPECS.CHURCH) {
            if (me.turn == 1) {
                // for first turn add position to list
                church_pos.add(new Point(me.x, me.y));
            }
        }

        // Crusader AI
        if (me.unit == SPECS.CRUSADER) {
            // if carrying resource deliver to nearest church and castle
            // TODO: refine algo to fill to max capacity before going to deliver
            if (me.fuel > 0 || me.karbonite > 0) {
                int[][] visRobMap = getVisibleRobotMap();
                for (Point p: adj_directions) {
                    if (visRobMap[me.x+p.x][me.y+p.y] > 0) {
                        Robot check = getRobot(visRobMap[me.x+p.x][me.y+p.y]);
                        if (check.team == me.team && (check.unit == SPECS.CHURCH || check.unit == SPECS.CASTLE)) {
                            return give(p.x, p.y, me.karbonite, me.fuel);
                        }
                    }
                }
            } else {
                // find and attack
                Robot[] visRobots = getVisibleRobots();

                for (Robot r: visRobots) {

                }
            }
        }

        // Other units sit tight (for now)
        return null;
    }
}
