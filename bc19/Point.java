package bc19;

public class Point {
    public int x;
    public int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point add(Point other) {
        return new Point(this.x + other.x, this.y + other.y);
    }

    public int dist(Point other) {
        return (this.x - other.x)*(this.x - other.x) + (this.y - other.y)*(this.y - other.y);
    }

    public boolean equals(Point other) {
        return (this.x == other.x) && (this.y == other.y);
    }
}