package bc19;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.lang.Math;

public class Management {

    ///*** API ***///
    //
    //  Management(MyRobot robo);
    //  void updateData();
    //  Point findEmptyAdj(Robot me, boolean preferDepot);
    //  Point findEmptyAdj(Point dest, boolean preferDepot);
    //  boolean isAdj(Point a, Point b);
    //  Point findNextStep(int x, int y, Point[] directions, boolean avoidmine, boolean avoidbots, LinkedList<Point> src);
    //  Point findNextStep(int x, int y, Point[] directions, boolean avoidmine, boolean avoidbots, Point P);
    //  int squareDistance(Robot bot, Point other);
    //  boolean[][] copyMap(boolean[][] map);
    //  Point findEmptyNextAdj(Point dest, Point src, Point[] moves);
    //  boolean buildable(int type);
    //  boolean getPriority();
    //  boolean checkBounds(int x, int y);
    //  Point oppPoint(int x, int y);
    //  Point findOffsetClosest(Point src, int range, Point dest, int offset)
    //  Point findFarthestMove(Point current, Point away_from, boolean r_four)
    //
    ///*** END ***///

    ///*** Private Variables and Initialization ***///
    // Identification and map (constant after turn 1)
    MyRobot robo;
    Robot me;
    public boolean[][] passable_map;
    public boolean[][] fuel_map;
    public boolean[][] karbo_map;
    public int map_length;
    public boolean vsymmetry;

    // Current turn data (update every turn)
    public int[][] vis_robot_map;
    public Robot[] vis_robots;
    public Point me_location;

    public Management(MyRobot robo) {
        this.robo = robo;
        this.me = robo.me;
        passable_map = robo.getPassableMap();
        fuel_map = robo.getFuelMap();
        karbo_map = robo.getKarboniteMap();
        map_length = passable_map.length;
        vsymmetry = mapVsym();
        updateData();
    }

    // Determine map symmetry
    private boolean mapVsym(){
        if(map_length%2 == 0){
            for(int i = 0; i< map_length/2;i++){
                for(int j = 0; j< map_length;j++){
                    if(passable_map[i][j]!=passable_map[map_length-i-1][j]){
                        return false;
                    }
                }
            }
        }else{
            for(int i = 0; i< (map_length-1)/2;i++){
                for(int j = 0; j< map_length;j++){
                    if(passable_map[i][j]!=passable_map[map_length-i-1][j]){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    ///*** Helper functions ***///

    // Update all turn related data
    public void updateData() {
        this.me = robo.me;
        vis_robot_map = robo.getVisibleRobotMap();
        vis_robots = robo.getVisibleRobots();
        me_location = new Point(me.x, me.y);
    }

    // Find empty square adjacent to a given unit.
    // prefer depot if preferDepot true
    public Point findEmptyAdj(Robot me, boolean preferDepot) {
        return findEmptyAdj(new Point(me.x, me.y), preferDepot);
    }

    // Find empty square adjacent to a given point.
    // prefer depot if preferDepot true
    public Point findEmptyAdj(Point dest, boolean preferDepot) {
        Point to_return = null;
        if (preferDepot) {
            for (Point p: MyRobot.adj_directions) {
                if (!checkBounds(dest.x + p.x, dest.y + p.y)) {
                    continue;
                }
                if (passable_map[dest.y + p.y][dest.x + p.x] && vis_robot_map[dest.y + p.y][dest.x + p.x] == 0) {
                    to_return = p;
                    if (fuel_map[dest.y + p.y][dest.x + p.x] || karbo_map[dest.y + p.y][dest.x + p.x]) {
                        return to_return;
                    }
                }
            }
        } else {
            for (Point p: MyRobot.adj_directions) {
                if (!checkBounds(dest.x + p.x, dest.y + p.y)) {
                    continue;
                }
                if (passable_map[dest.y + p.y][dest.x + p.x] && vis_robot_map[dest.y + p.y][dest.x + p.x] == 0) {
                    to_return = p;
                }
            }
        }
        return to_return;
    }
    
    // Check adjacency of two points
    public boolean isAdj(Point a, Point b) {
        for (Point p: MyRobot.adj_directions) {
            if ((a.x + p.x == b.x) && (a.y + p.y == b.y)) {
                return true;
            }
        }
        return false;
    }

    // Find next point to move to closest source in given list
    // choose r^2=4 moves when r_four is true
    public Point findNextStep(int x, int y, Point[] directions, boolean avoidmine, boolean avoidbots, ArrayList<Point> src) {
        Point current, next = new Point(x, y);
        Point result;
        boolean[][] map = copyMap(passable_map);

        for (int i = 0; i < map_length; i++) {
            for (int j = 0; j < map_length; j++) {
                if (avoidbots && vis_robot_map[i][j] > 0) {
                    map[i][j] = false;
                }
                if(avoidmine && (fuel_map[i][j] || karbo_map[i][j])){
                    map[i][j] = false;                    
                }
            }
        }

        int i = 0;
        while (i!=src.size()) {
            current = src.get(i);
            for (Point p: directions) {
                next = new Point(current.x + p.x, current.y + p.y);
                if (checkBounds(next.x, next.y)) {
                    if (next.x == x && next.y == y) {
                        // if(!avoidmine){
                        return current;
                        // }
                        // if(!fuel_map[next.y][next.x] && !karbo_map[next.y][next.x]){
                        //     return current;
                        // }else{
                        //     result = current;
                        // }
                        
                    } else {
                        if (map[next.y][next.x] == true) {
                            map[next.y][next.x] = false;
                            src.add(next);
                        } 
                    }
                }
            }
            i++;
        }

        return result;
    }

    // Find next point to move to given point
    // choose r^2=4 moves when r_four is true
    public Point findNextStep(int x, int y, Point[] directions, boolean avoidmine, boolean avoidbots, Point P) {
        ArrayList<Point> temp = new ArrayList<>();
        temp.add(P);
        return findNextStep(x, y, directions, avoidmine, avoidbots, temp);
    }

    public Point findNextStepToPoint(Point current, Point[] directions, boolean avoidmines, Point dest) {
        int min_dist = current.dist(dest), dist = 0;
        Point closest = current;
        for (Point p: directions) {
            Point next = p.add(me_location);
            if (!checkBounds(next.x, next.y)) continue;
            if (avoidmines && (!fuel_map[next.y][next.x] || !karbo_map[next.y][next.x])) continue;
            if (!passable_map[next.y][next.x]) continue;
            dist = next.dist(dest);
            if (dist < min_dist) {
                min_dist = dist;
                closest = next;
            }
        }

        return closest;
    }

    // Square distance between two points represented by robot and point
    public int squareDistance(Robot bot, Point other) {
        return (bot.x - other.x)*(bot.x - other.x) + (bot.y - other.y)*(bot.y - other.y);
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

    // Find point adjacent to destination that is within given set of moves
    public Point findEmptyNextAdj(Point dest, Point src, Point[] moves) {
        for (Point p: moves) {
            Point temp = src.add(p);
            if (!checkBounds(temp.x, temp.y)) {
                continue;
            }
            if (isAdj(temp, dest) && passable_map[temp.y][temp.x] && vis_robot_map[temp.y][temp.x] <= 0) {
                return temp;
            }
        }
        return null;
    }

    // Check if sufficient resources are available to build given unit
    public boolean buildable(int type) {
        if(type == 0){//castle
            return false;
        }else if(type == 1){//church
            if (robo.karbonite >= 50 && robo.fuel >= 100){
                return true;
            } else return false;
        }else if(type == 2){//pilgrim
            if (robo.karbonite >= 10 && robo.fuel >= 50){
                return true;
            } else return false;
        }else if(type == 3){//crusader
            if (robo.karbonite >= 15 && robo.fuel >= 50){
                return true;
            } else return false;
        }else if(type == 4){//prophet
            if (robo.karbonite >= 25 && robo.fuel >= 50){
                return true;
            } else return false;
        }else if(type == 5){//preacher
            if (robo.karbonite >= 30 && robo.fuel >= 50){
                return true;
            } else return false;
        }else{//default
            return false;
        }
    }

    // Check if given point is within map bounds
    public boolean checkBounds(int x, int y) {
        if ((x < 0) || (x >= map_length) || (y < 0) || (y >= map_length)) {
            return false;
        } else {
            return true;
        }
    }

    // Return symmetrically opposite point
    public Point oppPoint(int x, int y) {
        int oppx, oppy;
        if (vsymmetry) {
            oppx = x;
            oppy = map_length - 1 - y;
        } else {
            oppx = map_length - 1 - x;
            oppy = y;
        }

        return new Point(oppx, oppy);
    }

    Point findOffsetClosest(Point src, int range, Point dest, int offset, boolean[][] passable_map) {
        LinkedList<Point> queue = new LinkedList<>();
        queue.add(dest);

        Point current, next_move;
        while (!queue.isEmpty()) {
            current = queue.pollFirst();
            for (Point p: MyRobot.adj_directions) {
                next_move = current.add(p);
                if (next_move.dist(dest) >= offset) {
                    return next_move;
                }
                if (passable_map[next_move.y][next_move.x] && next_move.dist(src) <= range) {
                    passable_map[next_move.y][next_move.x] = false;
                    queue.add(next_move);
                }
            }
        }
        return null;
    }

    public int getRobotIdMap(int x, int y) {
        if (x >= 0 && x < map_length && y >= 0 && y < map_length) {
            return vis_robot_map[y][x];
        }
        return -1;
    }

    public Point findFarthestMove(Point current, Point away_from, Point[] directions) {
        Point next = null, farthest = null;
        int min_dist = 0, dist = 0;
        for (Point p: directions) {
            next = p.add(current);
            if (!passable_map[next.y][next.x] || vis_robot_map[next.y][next.x] > 0) continue;
            dist = next.dist(away_from);
            if (dist > min_dist) {
                min_dist = dist;
                farthest = next;
            }
        }

        return farthest;   
    }

    public int numberOfMoves(Point current, Point dest, Point[] directions) {
        boolean[][] map = copyMap(passable_map);
        ArrayList<Integer> moves_count = new ArrayList<>();
        ArrayList<Point> moves = new ArrayList<>();
        moves_count.add(0);
        moves.add(current);
        map[current.y][current.x] = false;
        int i = 0;
        Point last_move, next;
        int last_count;
        while (i != moves.size()) {
            last_move = moves.get(i);
            last_count = moves_count.get(i);
            for (Point p: directions) {
                next = p.add(last_move);
                if (next.equals(dest)) {
                    return last_count + 1;
                }
                if (checkBounds(next.x, next.y) && map[next.y][next.x]) {
                    map[next.y][next.x] = false;
                    moves.add(next);
                    moves_count.add(last_count + 1);
                }
            }
            i++;
        }
    }

    boolean getDangerPriority(){
        // strip for 1/3
        //calculate distance from mid and set priority
        if(vsymmetry){
            if(me.y > map_length/2){
                if(me.y > (2*map_length/3)){
                    return false;
                }else{
                    return true;
                }
            }else{
                if(me.y < map_length/3){
                    return false;
                }else{
                    return true;
                }

            }
        }else{
            if(me.x > map_length/2){
                if(me.x > (2*map_length/3)){
                    return false;
                }else{
                    return true;
                }
            }else{
                if(me.x < map_length/3){
                    return false;
                }else{
                    return true;
                }

            }
        }
    }

    public Point coulombRepel() {
        boolean stay_put = true;
        Point next;
        Point dest = null;
        int max_score = 0;
        ArrayList<Point> possible_moves = new ArrayList<>();
        for (Point p: MyRobot.adj_directions) {
            next = p.add(me_location);
            if (checkBounds(next.x, next.y)) continue;
            if (vis_robot_map[next.y][next.x] >= 0) stay_put = false;
            if (vis_robot_map[next.y][next.x] == 0 && !passable_map[next.y][next.x] && !fuel_map[next.y][next.x] && !karbo_map[next.y][next.x]) {
                int score = 0;
                for (Point q: MyRobot.adj_directions) {
                    if (vis_robot_map[next.y + q.y][next.x + q.x] > 0) {
                        score++;
                    }
                }
                if (score > max_score) {
                    max_score = score;
                    dest = next;
                }                
            }
        }

        if (stay_put) {
            return null;
        }

        return dest;
    }
}