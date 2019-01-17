package bc19;

import java.util.*;

import bc19.MyRobot;

public class Management {

    ///*** Private Variables and Initialization ***///
    // Identification and map
    MyRobot robot;
    Robot me;
    Point me_location;
    boolean[][] passable_map;
    boolean[][] fuel_map;
    boolean[][] karbo_map;
    int[][] vis_robot_map;
    Robot[] vis_robots;
    int map_length;
    

    // Other Variables

    public Management(MyRobot robot) {
        this.robot = robot;
        this.me = robot.me;
        passable_map = robot.getPassableMap();
        fuel_map = robot.getFuelMap();
        karbo_map = robot.getKarboniteMap();
        map_length = passable_map.length;
    }

    ///*** Helper functions ***///

    // update all turn related data
    public void update_data() {
        vis_robot_map = robot.getVisibleRobotMap();
        vis_robots = robot.getVisibleRobots();
        me_location = new Point(me.x, me.y);
    }

    // Find empty square adjacent to a given unit.
    // prefer depot if preferDepot true
    public Point findEmptyAdj(Robot me, boolean preferDepot) {
        Point to_return = null;
        if (preferDepot) {
            for (Point p: MyRobot.adj_directions) {
                if (passable_map[me.y + p.y][me.x + p.x] && vis_robot_map[me.y + p.y][me.x + p.x] == 0) {
                    to_return = p;
                    if (fuel_map[me.y + p.y][me.x + p.x] || karbo_map[me.y + p.y][me.x + p.x]) {
                        return to_return;
                    }
                }
            }
        } else {
            for (Point p: MyRobot.adj_directions) {
                if (passable_map[me.y + p.y][me.x + p.x] && vis_robot_map[me.y + p.y][me.x + p.x] == 0) {
                    to_return = p;
                }
            }
        }

        return to_return;
    }

    // Find empty square adjacent to a given point.
    // prefer depot if preferDepot true
    public Point findEmptyAdj(Point dest, boolean preferDepot) {
        Point to_return = null;
        if (preferDepot) {
            for (Point p: MyRobot.adj_directions) {
                if (passable_map[dest.y + p.y][dest.x + p.x] && vis_robot_map[dest.y + p.y][dest.x + p.x] == 0) {
                    to_return = p;
                    if (fuel_map[dest.y + p.y][dest.x + p.x] || karbo_map[dest.y + p.y][dest.x + p.x]) {
                        return to_return;
                    }
                }
            }
        } else {
            for (Point p: MyRobot.adj_directions) {
                if (passable_map[dest.y + p.y][dest.x + p.x] && vis_robot_map[dest.y + p.y][dest.x + p.x] == 0) {
                    to_return = p;
                }
            }
        }

        return to_return;
    }
    
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
    public Point findNextStep(int x, int y, boolean[][] map, boolean r_four, LinkedList<Point> src) {
        Point current, next = new Point(x, y);
        Point[] directions;

        directions = (r_four) ? MyRobot.four_directions : MyRobot.nine_directions;

        // robot.log("finding next point for path x: " + Integer.toString(x) + " y: " + Integer.toString(y));
        while (!src.isEmpty()) {
            current = src.pollFirst();
            for (Point p: directions) {
                next = new Point(current.x + p.x, current.y + p.y);
                    // robot.log("aaa " + Integer.toString(p.x) +  "aa" + Integer.toString(current.x) + "aa" +  Integer.toString(next.x));
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
    
    //alternative accepting single destination
    public Point findNextStepP(int x, int y, boolean[][] map, boolean r_four, Point p) {
        LinkedList<Point> l = new LinkedList<>();
        l.add(p);
        return findNextStep(x,y,map,r_four,l);
    }

    // square distance between two points represented by robot and point
    public int square_distance(Robot bot, Point other) {
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

    // find point adjacent to destination that is within given set of moves
    public Point findEmptyNextAdj(Point dest, Point src, Point[] moves) {
        for (Point p: moves) {
            Point temp = new Point(src.x + p.x, src.y + p.y);
            if (isAdj(temp, dest) && passable_map[temp.x][temp.y] && vis_robot_map[temp.x][temp.y] <= 0) {
                return temp;
            }
        }
        return null;
    }

    public boolean buildable(int type){
        if(type == 0){//castle
            return false;
        }else if(type == 1){//church
            if (robot.karbonite > 50 && robot.fuel > 100){
                return true;
            } else return false;
        }else if(type == 2){//pilgrim
            if (robot.karbonite > 10 && robot.fuel > 50){
                return true;
            } else return false;
        }else if(type == 3){//crusader
            if (robot.karbonite > 20 && robot.fuel > 50){
                return true;
            } else return false;
        }else if(type == 4){//prophet
            if (robot.karbonite > 25 && robot.fuel > 50){
                return true;
            } else return false;
        }else if(type == 5){//preacher
            if (robot.karbonite > 30 && robot.fuel > 50){
                return true;
            } else return false;
        }else{//default
            return false;
        }
    }

}