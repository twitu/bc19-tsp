import java.util.ArrayList;
import java.util.LinkedList;

public class Point {
    public int x, y;

    public static Point[] adj_directions = {
        Point(0, 1),
        Point(1, 1),
        Point(1, 0),
        Point(1, -1),
        Point(0, -1),
        Point(-1, -1),
        Point(-1, 0),
        Point(-1, 1)
    };
    
    // r^2 = 4
    public static Point[] four_directions = {
        Point(0, 1),
        Point(1, 1),
        Point(1, 0),
        Point(1, -1),
        Point(0, -1),
        Point(-1, -1),
        Point(-1, 0),
        Point(-1, 1),
        // extended directions
        Point(0, 2),
        Point(2, 0),
        Point(0, -2),
        Point(-2, 0)
    };

    public static Point[] nine_directions = {
        Point(0, 1),
        Point(1, 1),
        Point(1, 0),
        Point(1, -1),
        Point(0, -1),
        Point(-1, -1),
        Point(-1, 0),
        Point(-1, 1),
        // extended directions
        Point(0, 2),
        Point(2, 0),
        Point(0, -2),
        Point(-2, 0),
        // more extended
        Point(0, 3),
        Point(1, 2),
        Point(2, 1),
        Point(3, 0),
        Point(2, -1),
        Point(1, -2),
        Point(0, -3),
        Point(-1, -2),
        Point(-2, -1),
        Point(-3, 0),
        Point(-2, 1),
        Point(-1, 2),
        // others
        Point(2, 2),
        Point(2, -2),
        Point(-2, 2),
        Point(-2, -2)
    };

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public add(Point other) {
        return Point(this.x + other.x, this.y + other.y);
    }

    public sub(Point other) {
        return Point(other.x - this.x, other.y - this.y)
    }

    // TODO: check if map is modifiable else use copy
    public static findPath(int x, int y, boolean[][] map, bool r_four, LinkedList<Point> src) {
        Point current, next;
        Point[] directions;

        if (r_four) {
            directions = four_directions;
        } else {
            directions = nine_directions;
        }

        while (!src.isEmpty()) {
            current = src.removeFirst();
            for (Point p: directions) {
                next = current.add(p);
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
}