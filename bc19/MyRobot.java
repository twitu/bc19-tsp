package bc19;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays.*;
import java.util.Arrays;
import java.lang.*;

public class ShadowBot extends BCAbstractRobot {

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

    //*** Troop resource requirements */
    // karbonite, fuel
    public static int[][] requirements = {
        {-1, -1},
        {50, 200},
        {10, 50},
        {20, 50},
        {25, 50},
        {30, 50}
    };

    // attack troops pipeline
    public int[] tiger_squad = {
        SPECS.CRUSADER,
        SPECS.CRUSADER,
        SPECS.PROPHET,
        SPECS.PROPHET,
        SPECS.PREACHER
    };

    //*** Private Variables ***/
    // Map info (Constant)
    boolean[][] passable_map;
    boolean[][] fuel_map;
    boolean[][] karbo_map;
    ResourceData clusterList;
    LinkedList<Point> fuel_pos = new LinkedList<>();
    LinkedList<Point> karbo_pos = new LinkedList<>();
    int map_length;

    // Personal identification
    boolean castleGaurd;    // Am I gaurding a castle?
    int baseID;             // ID of base cluster
    Point home, myloc;
    
    // Visibility
    Robot[] visRobots;
    int[][] visRobotMap;
    
    // Other Variables
    ArrayList<Point> church_pos = new ArrayList<>();
    ArrayList<Robot> castle_pos = new ArrayList<>();
    ArrayList<DepotCluster> myclusters;
    Encoder encoder;
    

    //*** Commonly Used Functions ***/
    // find map symmetry
    public Boolean map_sym(){
        return true;
    }                

    // Find empty square adjacent to a given unit.
    public Point findEmptyAdj(Robot me, boolean preferDepot) {
        Point to_return = null;
        if (preferDepot) {
            for (Point p: ShadowBot.adj_directions) {
                if (passable_map[me.y + p.y][me.x + p.x] && visRobotMap[me.y + p.y][me.x + p.x] == 0) {
                    to_return = p;
                    if (fuel_map[me.y + p.y][me.x + p.x] || karbo_map[me.y + p.y][me.x + p.x]) {
                        return to_return;
                    }
                }
            }
        } else {
            for (Point p: ShadowBot.adj_directions) {
                if (passable_map[me.y + p.y][me.x + p.x] && visRobotMap[me.y + p.y][me.x + p.x] == 0) {
                    to_return = p;
                }
            }
        }

        return to_return;
    }

    public Point findEmptyAdj(Point dest, boolean preferDepot) {
        Point to_return = null;
        if (preferDepot) {
            for (Point p: ShadowBot.adj_directions) {
                if (passable_map[dest.y + p.y][dest.x + p.x] && visRobotMap[dest.y + p.y][dest.x + p.x] == 0) {
                    to_return = p;
                    if (fuel_map[dest.y + p.y][dest.x + p.x] || karbo_map[dest.y + p.y][dest.x + p.x]) {
                        return to_return;
                    }
                }
            }
        } else {
            for (Point p: ShadowBot.adj_directions) {
                if (passable_map[dest.y + p.y][dest.x + p.x] && visRobotMap[dest.y + p.y][dest.x + p.x] == 0) {
                    to_return = p;
                }
            }
        }

        return to_return;
    }

    public boolean isAdj(Point a, Point b) {
        for (Point p: adj_directions) {
            if ((a.x + p.x == b.x) && (a.y + p.y == b.y)) {
                return true;
            }
        }

        return false;
    }

    // TODO consider cases where adjacent is blocked
    public Point findEmptyNextAdj(Point dest, Point src, Point[] moves) {
        for (Point p: moves) {
            Point temp = new Point(src.x + p.x, src.y + p.y);
            if (isAdj(temp, dest)) {
                return temp;
            }
        }
        return null;
    }

    // Find next point to move to given source, a copy of map, movement speed and possible destinations
    public Point findPath(int x, int y, boolean[][] map, boolean r_four, LinkedList<Point> src) {
        Point current, next = new Point(x, y);
        Point[] directions;

        if (r_four) {
            directions = ShadowBot.four_directions;
        } else {
            directions = ShadowBot.nine_directions;
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
        for (Point p: ShadowBot.adj_directions) {
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

        // for (Point p: castle_pos) {
        //     dest.add(p);
        // }

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
    // Pilgrim AI                
    public Action pilgrimAI(Robot me) {

        // Variables
        ArrayList<Robot> adjRobots = new ArrayList<>();
        boolean adjChurch, adjCrusader, adjCastle, visChurch;
        Point emptyadj;

        // Primary Initialization
        if (me.turn == 1) {
            for (Point p: adj_directions) {
                if (visRobotMap[me.x + p.x][me.y + p.y] > 0) {
                    Robot bot = getRobot(visRobotMap[me.x + p.x][me.y + p.y]);
                    if (bot.unit == SPECS.CHURCH) {
                        home = new Point(bot.x, bot.y);
                    }
                }
            }
            baseID = -1;
        }                           

        // Am I carrying max capacity resources?
        if ((me.karbonite == 20) || (me.fuel == 100)) {

            if (isAdj(myloc, home)) {
                return give(home.x - myloc.x, home.y - myloc.y, me.karbonite, me.fuel);
            } else {
                LinkedList<Point> homePoint = new LinkedList<>();
                homePoint.add(home);
                Point next = findPath(me.x, me.y, passable_map, true, homePoint);
                
                if (next.x == home.x && next.y == home.y) {
                    next = findEmptyNextAdj(next, myloc, four_directions);
                }
                return move(next.x - me.x, next.y - me.y);
            }
        
        // Check if on base depot then mine
        } else if (karbo_map[me.y][me.x] || fuel_map[me.y][me.x]){
            log("on a depot");

            // Adjust base ID
            baseID = clusterList.getID(me.x, me.y);            

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

            if ((baseID == -1) && (me.signal % 16 == 1)) {
                baseID = me.signal/16;                                                                                
            }
            Point P = clusterList.getLocation(baseID);
            if (isAdj(myloc, P)) {
                return buildUnit(SPECS.CHURCH, P.x, P.y);
            } else {
                LinkedList<Point> dest = new LinkedList<>();
                dest.add(clusterList.getLocation(baseID));                                    
                Point next = findPath(me.x, me.y, copyMap(this.passable_map), true, dest);
                Point endPoint = clusterList.getLocation(baseID);
                if ((next == null) || (baseID == -1)) {
                    log("did not find valid next path");
                    return null;
                } else {
                    if ((next.x == endPoint.x) &&(next.y == endPoint.y)){
                        next = findEmptyNextAdj(next, myloc, four_directions);
                    }
                    log("found next step " + Integer.toString(next.x) + ", " + Integer.toString(next.y));
                    return move(next.x - me.x, next.y - me.y);
                }
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
            for (Point p: ShadowBot.adj_directions) {
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

    // Castle AI
    public Action castleAI(Robot me) {
        // Castle AI
        visRobots = getVisibleRobots();
        if (me.turn == 1) {
            // add all castles to castle list
            for (Robot bot: visRobots) {
                if (bot.unit == SPECS.CASTLE && bot.id != me.id) {
                    castle_pos.add(bot);
                }
            }
            
            // find depot clusters
            clusterList = new ResourceData(fuel_map, karbo_map);
            myclusters = new ArrayList<>();
        }

        DepotCluster closest = null;
        int min_distance = Integer.MAX_VALUE;
        if (clusterList.count != 0) {
            for (DepotCluster cluster: clusterList.resourceList) {
                for (Robot bot: castle_pos) {
                    if (bot.castle_talk == cluster.ClusterID) {
                        cluster.castle_id = bot.id;
                        clusterList.count--;                                                                        
                    }
                }

                // if not already allocated check cluster for distance                                
                if (cluster.castle_id == -1)  {
                    int dist = (cluster.locX-me.x)*(cluster.locX-me.x) + (cluster.locX-me.y)*(cluster.locY-me.y);
                    if (dist < min_distance) {
                        min_distance = dist;
                        closest = cluster;
                    }                                                                                
                }
            }

            // found closest unclaimed cluster
            // inform others create a new pilgrim and tell it where to go
            if (closest != null) {
                clusterList.count--;
                closest.castle_id = me.id;
                myclusters.add(closest);
                castleTalk(closest.ClusterID);
                signal(encoder.baseAssignment(closest.ClusterID, false), 2);
                Point emptyadj = findEmptyAdj(me, true);
                return buildUnit(SPECS.PILGRIM, emptyadj.x, emptyadj.y); 
            }   
        }

        // check if church has been constructed
        // TODO check even if church is not created exactly at mean
        if (!myclusters.isEmpty()) {
            ArrayList<DepotCluster> to_remove = new ArrayList<>();
            for (Robot bot : visRobots) {
                if (bot.unit == SPECS.CHURCH) {
                    for (DepotCluster cluster: myclusters) {
                        if (cluster.locX == bot.x && cluster.locY == bot.y) {
                            to_remove.add(cluster);
                        }
                    }
                }
            }
            myclusters.removeAll(to_remove);
        }

        if (!myclusters.isEmpty()) {
            for (DepotCluster cluster: myclusters) {
                cluster.turns_to_check--;
                if (cluster.turns_to_check == 0) {
                    signal(encoder.baseAssignment(closest.ClusterID, false),2);//temp range 1
                    Point emptyadj = findEmptyAdj(me, true);
                    cluster.turns_to_check = 10;
                    return buildUnit(SPECS.PILGRIM, emptyadj.x, emptyadj.y);
                }
            }
        }
        

        // all clusters allocated
        // start creating a squad
        int unit_number = 0;
        int[] unit_requirements = requirements[tiger_squad[unit_number]];
        if (unit_requirements[0] > karbonite && unit_requirements[1] > fuel) {
            int unit_type = tiger_squad[unit_number];
            Point emptyadj = findEmptyAdj(me, false);
            unit_number = (unit_number++)%tiger_squad.length;
            return buildUnit(unit_type, emptyadj.x,emptyadj.y);
        }

    }

    // church AI
    public Action churchAI(Robot me){

        
    visRobots = getVisibleRobots();
    
        // TODO: check for depots belonging in the cluster
        LinkedList<Point> fuel_depots = new LinkedList<>();
        LinkedList<Point> karb_depots = new LinkedList<>();
        boolean fuelb =   true;
        ArrayList<Integer> assigned_pilgrims = new ArrayList<>();
        ArrayList<Point> assigned_depots = new ArrayList<>();
        // for(Point p:fuel_pos){
        //     if(DepotCluster.checkClusterRange(me.x,me.y,p.x,p.y)){
        //         fuel_depots.addLast(p);
        //     }
        // }
        // for(Point p:karbo_pos){
        //     if(DepotCluster.checkClusterRange(me.x,me.y,p.x,p.y)){
        //         karb_depots.addLast(p);
        //     }
        // }
        //depot defence management
        if (me.turn == 1) {
            // for first turn add position to list
            church_pos.add(new Point(me.x, me.y));
        } else{
            for(int i = 0 ; i < visRobots.length;i++ ){
                if( visRobots[i].team != me.team){//check if enemy
                    int code = 0;
                    int range = 0;
                    if(visRobots[i].unit == SPECS.CRUSADER ||  visRobots[i].unit == SPECS.PREACHER || visRobots[i].unit == SPECS.PROPHET){ //check if unit can attack
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
        
        //depot mining management
        
        //TODO: assign pilgrims to depots
        if (karbonite > 10 && fuel > 50)
        {   
            Point p = findEmptyAdj(me,true);
            Point nextP;            
            if(p!=null){ // assign a new pilgrim if it can be built
                if(fuelb){
                    nextP = fuel_depots.pollFirst();                                        
                }else{
                    nextP = karb_depots.pollFirst();
                }  
                signal(encoder.assignDepot(nextP),1);
                fuelb = !fuelb;                                
            }                                    
        }

        // // keep track of pilgrims
        // if(turn == 1){//assign depot to the first pilgrim
        //     for(Robot r: visRobots){
        //         if(r.signal_radius < 3 && r.unit == SPECS.PILGRIM){                    
        //         }                                
        //     }            
        // }                            

                
        
                                                 
    }    

    //*** Main Code ***/
    public Action turn() {
        
        // Primary initializations
        if (me.turn == 1) {
            passable_map = getPassableMap();
            fuel_map = getFuelMap();
            karbo_map = getKarboniteMap();
            map_length = passable_map.length;
            clusterList = new ResourceData(fuel_map, karbo_map);
            encoder = new Encoder();

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
        myloc = new Point(me.x, me.y);

        // Pilgrim AI
        if (me.unit == SPECS.PILGRIM) {
            log("launching pilgrim ai");
            return pilgrimAI(me);
        }

        // Castle AI
        if (me.unit == SPECS.CASTLE) {
            log("launching castle ai");
            return castleAI(me);
        }

        // Church AI
        if (me.unit == SPECS.CHURCH) {
            log("launching church AI");
            return churchAI(me);                                                            
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