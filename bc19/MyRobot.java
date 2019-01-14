package bc19;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays.*;
import java.util.Arrays;
import java.lang.*;

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
    boolean fuelMiner, castleGaurd;
    LinkedList<Point> fuel_pos = new LinkedList<>();
    LinkedList<Point> karbo_pos = new LinkedList<>();
    int map_length;

    //*** Commonly Used Functions ***/
    // Find empty square adjacent to a given unit.
    public Point findEmptyAdj(Robot me, boolean preferDepot) {
        Point to_return = null;
        if (preferDepot) {
            for (Point p: MyRobot.adj_directions) {
                if (passable_map[me.y + p.y][me.x + p.x] && visRobotMap[me.y + p.y][me.x + p.x] == 0) {
                    to_return = p;
                    if (fuel_map[me.y + p.y][me.x + p.x] || karbo_map[me.y + p.y][me.x + p.x]) {
                        return to_return;
                    }
                }
            }
        } else {
            for (Point p: MyRobot.adj_directions) {
                if (passable_map[me.y + p.y][me.x + p.x] && visRobotMap[me.y + p.y][me.x + p.x] == 0) {
                    to_return = p;
                }
            }
        }

        return to_return;
    }

    // Update castle_pos and church_pos
    public void scanCastleChurch(){
        for(int i = 0 ; i < visRobots.length;i++ ){
            if( visRobots[i].team == me.team){                      //check if ally
                if(visRobots[i].unit == SPECS.CASTLE ){             //castle
                    castle_pos.add(new Point(visRobots[i].x, visRobots[i].y));
                }else if(visRobots[i].unit == SPECS.CHURCH) {       //church
                    church_pos.add(new Point(visRobots[i].x, visRobots[i].y));                                                                                                                                                                       
                }
            }
        }
    }

    // Find next point to move to given source, a copy of map, movement speed and possible destinations
    public Point findPath(int x, int y, boolean[][] map, boolean r_four, LinkedList<Point> src) {
        Point current, next = new Point(x, y);
        Point[] directions;

        if (r_four) {
            directions = MyRobot.four_directions;
        } else {
            directions = MyRobot.nine_directions;
        }

        StringBuilder logging = new StringBuilder("finding next point for path x: " + x + " y: " + y);
        log(logging.toString());
        while (!src.isEmpty()) {
            current = src.pollFirst();
            for (Point p: directions) {
                next = new Point(current.x + p.x, current.y + p.y);
                if (next.x >= 0 && next.x < map_length && next.y >= 0 && next.y < map_length) {
                    if (next.x == x && next.y == y) {
                        return current;
                    } else {
                        if (map[next.y][next.x] == true) {
                            map[next.y][next.x] = false;
                            src.add(next);
                        } 
                    }
                }
            }
        }

        return null;
    }

    // Deliver all resources to an adjacent unit of given type. Returns null if unit of given type non adjacent
    public Action ifAdjGive(Robot me, int unit) {
        for (Point p: MyRobot.adj_directions) {
            if (visRobotMap[p.y + me.y][p.x + me.x] > 0) {
                Robot check = getRobot(visRobotMap[p.y + me.y][p.x + me.x]);
                if (check.unit == unit && check.team == me.team) {
                    return give(p.x, p.y, me.karbonite, me.fuel);
                }
            }
        }
        return null;
    }

    // Get as close as possible to nearest church or castle
    public Action homeDelivery(Robot me) {
        LinkedList<Point> dest;
        Point next;
        for (Point p: church_pos) {
            dest.add(p);
        }

        for (Point p: castle_pos) {
            dest.add(p);
        }

        if (me.unit == SPECS.CRUSADER) {
            next = findPath(me.x, me.y, copyMap(passable_map), false, dest);
        } else {
            next = findPath(me.x, me.y, copyMap(passable_map), true, dest);
        }
        // Check if next point is exactly on destination
        if (visRobotMap[next.y][next.x] > 0) {
            Robot check = getRobot(visRobotMap[next.y][next.x]);
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
        boolean adjChurch, adjCrusader, adjCastle, visChurch;
        Point emptyadj;

        // Primary Initialization
        if (me.turn == 1) {
            
            // Get adjacent bots and look for castle. If castle found mine karbo by default.
            visRobots = getVisibleRobots();
            fuelMiner = false;
            for (Robot bot : visRobots) {
                if ((Math.abs(bot.x - me.x) <= 1) && (Math.abs(bot.y - me.y) <= 1)) {
                    if (bot.unit == SPECS.CASTLE) {
                        fuelMiner = true;
                    }
                }
            }
        }                           

        // Am I carrying max capacity resources?
        if ((me.karbonite == 20) || (me.fuel == 100)) {

            // Get adjacent bots
            log("capacity full going to deliver");
            visRobots = getVisibleRobots();
            adjChurch = false;
            adjCrusader = false;
            for (Robot bot : visRobots) {
                if ((Math.abs(bot.x - me.x) <= 1) && (Math.abs(bot.y - me.y) <= 1)) {
                    adjRobots.add(bot);
                    if ((bot.unit == SPECS.CHURCH) || (bot.unit == SPECS.CASTLE)) {
                        adjChurch = true;
                    } else if(bot.unit == SPECS.CRUSADER) {
                        adjCrusader = true;
                    }
                }
            }
                
            // Church nearby? (Or castle)
            if (adjChurch) {
                log("found adjacent building");
                for (Robot bot : adjRobots) {
                    if (((bot.unit == SPECS.CHURCH) || (bot.unit == SPECS.CASTLE)) && (bot.team == me.team)) {
                        return give(bot.x - me.x, bot.y - me.y, me.karbonite, me.fuel);
                    }
                }

            // Crusader then?
            } else if (adjCrusader) {
                log("found adjacent crusader");
                for (Robot bot : adjRobots) {
                    if ((bot.unit == SPECS.CRUSADER) && (bot.team == me.team)) {
                        return give(bot.x - me.x, bot.y - me.y, me.karbonite, me.fuel);
                    }
                }
                
            // Self deliver
            } else {

                Action A1 = ifAdjGive(me, SPECS.CHURCH);
                Action A2 = ifAdjGive(me, SPECS.CASTLE);
                if ((A1 == null) && (A2 == null)) {
                    return homeDelivery(me);                    
                } else if (A1 != null){
                    return A1;
                } else {
                    return A2;
                }
            }
        
        // Check if on depot then mine
        } else if (karbo_map[me.y][me.x] || fuel_map[me.y][me.x]) {
            log("on a depot");

            // Get visible bot types and look for church
            visRobots = getVisibleRobots();
            visChurch = false;                
            for (Robot bot : visRobots) {
                if ((bot.unit == SPECS.CHURCH) || (bot.unit == SPECS.CASTLE)) {
                    visChurch = true;
                }
            }

            // If church in sight, start mining
            if (visChurch) {
                log("mining");
                return mine();

            // If no church in sight, build a church
            } else {
                log("no buildings in sight building one");
                emptyadj = findEmptyAdj(me, false);
                if (emptyadj == null) {
                    return mine();
                } else {
                    return buildUnit(SPECS.CHURCH, emptyadj.x, emptyadj.y);
                }
            }

        // If not, move to a depot
        } else {

            // TODO: decide whether to mine karbo or fuel based on castle talk and set default
            LinkedList<Point> depot_list = new LinkedList<>();
            for (Point p: (fuelMiner ? fuel_pos : karbo_pos)) {
                if (visRobotMap[p.y][p.x] <= 0) {
                    depot_list.add(p);
                }
            }
            Point next = findPath(me.x, me.y, copyMap(this.passable_map), true, depot_list);
            if (next == null) {
                log("did not find valid next path");
            } else {
                log("found next step " + Integer.toString(next.x) + ", " + Integer.toString(next.y));
                return move(next.x - me.x, next.y - me.y);
            }
        }
            
    }

    // Crusader AI
    public Action crusaderAI(Robot me) {
        
        // Variables
        Robot check;

        // Initializations
        if (me.turn == 1) {
            visRobots = getVisibleRobots();
            castleGaurd = false;
            for (Robot bot : visRobots) {
                if ((Math.abs(bot.x - me.x) <= 1) && (Math.abs(bot.y - me.y) <= 1)) {
                    if (bot.unit == SPECS.CASTLE) {
                        castleGaurd = true;
                    }
                }
            }
        }

        // If carrying resource deliver to nearest church and castle
        if (me.fuel == 100 || me.karbonite == 20) {
                
            // If church or castle nearby, give to it.
            for (Point p: MyRobot.adj_directions) {
                if (visRobotMap[me.y + p.y][me.x + p.x] > 0) {
                    check = getRobot(visRobotMap[me.y + p.y][me.x + p.x]);
                    if (check.team == me.team && (check.unit == SPECS.CHURCH || check.unit == SPECS.CASTLE)) {
                        return give(p.x, p.y, me.karbonite, me.fuel);
                    }
                }
            }

            // If not, go to church
            return homeDelivery(me);
                    
        // Target in range?
        } else {
            Robot[] visRobots = getVisibleRobots();
            for (Robot r: visRobots) {
                if ((r.team != me.team) && (((r.x - me.x)^2 + (r.y - me.y)^2) < 16)) {
                    return attack((r.x - me.x), (r.y - me.y));                                                                 
                }                                                                                
            }
        }
    }


    //*** Main Code ***/
    public Action turn() {
        
        // Primary initializations
        if (me.turn == 1) {
            passable_map = getPassableMap();
            fuel_map = getFuelMap();
            karbo_map = getKarboniteMap();
            map_length = passable_map.length;
            
            for (int i = 0; i < fuel_map.length; i++) {
                for (int j = 0; j < fuel_map[i].length; j++) {
                    if (fuel_map[i][j]) {
                        fuel_pos.add(new Point(j, i));
                    }
                }
            }

            for (int i = 0; i < karbo_map.length; i++) {
                for (int j = 0; j < karbo_map[i].length; j++) {
                    if (karbo_map[i][j]) {
                        karbo_pos.add(new Point(j, i));
                    }
                }
            }
        }

        // initialization
        visRobotMap = getVisibleRobotMap();

        // Castle AI
        if (me.unit == SPECS.CASTLE) {
            if (me.turn == 1) {
                // for first turn add position to list
                castle_pos.add(new Point(me.x, me.y));
            }
            if (me.turn <= 5) {
                Point emptyadj = findEmptyAdj(me, true);
                if (emptyadj == null) {
                    return null;
                } else {
                    log("created new pilgrim at " + Integer.toString(me.x + emptyadj.x) + ", " + Integer.toString(me.y + emptyadj.y));
                    return buildUnit(SPECS.PILGRIM, emptyadj.x, emptyadj.y);
                }
            }
        }

        // Pilgrim AI
        if (me.unit == SPECS.PILGRIM) {
            log("launching pilgrim ai");
            return pilgrimAI(me);
        }

        // Church AI
        if (me.unit == SPECS.CHURCH) {
            if (me.turn == 1) {
                // for first turn add position to list
                church_pos.add(new Point(me.x, me.y));
            } else{
                visRobots = getVisibleRobots();
                for(int i = 0 ; i < visRobots.length;i++ ){ //TODO: check for enemy robots in a certain range and if they are of attacking type
                    if( visRobots[i].team != me.team){//check if enemy
                        int code = 0;
                        int range = 0;
                        if(visRobots[i].unit == 3 ||  visRobots[i].unit == 4 || visRobots[i].unit == 5){ //check if unit can attack
                            code = 2;
                            range = 9;     
                        }else{
                            code = 1;
                            range = 9;                                                                                                                                                                        
                        }         
                        signal(code,range);      
                    }
                }
            }
        }

        // Crusader AI
        if (me.unit == SPECS.CRUSADER) {
            log("launching crusader ai");
            return crusaderAI(me);
        }

        // Other units sit tight (for now)
        return null;
    }
}