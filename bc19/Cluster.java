package bc19;

import java.util.ArrayList;

public class Cluster {

    ///*** API ***///
    //
    //  Cluster(int ID);
    //  void addDepot(int x, int y, boolean fuel);
    //  boolean checkDepot(int x, int y);
    //  boolean checkRange(int x, int y);
    //  void homePoint(boolean[][] terrain, boolean[][] fuel, boolean[][] karbo);
    //
    ///*** END ***///

    // Private Variables
    public ArrayList<Point> fuelPos = new ArrayList<>();
    public ArrayList<Point> karboPos = new ArrayList<>();
    public int ClusterID, status;
    public int meanX, meanY, locX, locY;
    public int fuel_count, karbonite_count;
    public static int range = 50;
    
    // Initialize Cluster
    public Cluster(int ID) {
        this.ClusterID = ID;
        meanX = -1;
        meanY = -1;
    }

    // Add a new depot and shift center of mass accordingly
    public void addDepot(int x, int y, boolean fuel) {
        if (meanX == -1) {
            meanX = x;
            meanY = y;
        }
        else {
            meanX = meanX*(fuelPos.size() + karboPos.size()) + x;
            meanY = meanY*(fuelPos.size() + karboPos.size()) + y;
            meanX /= (fuelPos.size() + karboPos.size() + 1);
            meanY /= (fuelPos.size() + karboPos.size() + 1);
        }
        if (fuel) {
            fuelPos.add(new Point(x, y));
            fuel_count++;
        }
        else {
            karboPos.add(new Point(x, y));
            karbonite_count++;
        }
    }

    // Check if (x, y) is in the depot cluster
    public boolean checkDepot(int x, int y) {
        for (Point p : fuelPos) {
            if ((p.x == x) && (p.y == y)) {
                return true;
            }
        }
        for (Point p : karboPos) {
            if ((p.x == x) && (p.y == y)) {
                return true;
            }
        }
        return false;
    }

    // Check if (x, y) is within range of given cluster
    public boolean checkRange(int x, int y) {
        return (((x - meanX)*(x - meanX) + (y - meanY)*((y - meanY))) <= range);
    }

    // Set mean to max adjacency point
    public void homePoint(boolean[][] terrain, boolean[][] fuel, boolean[][] karbo) {
        int adj = 0, max_dist = Integer.MAX_VALUE;
        locX = meanX;
        locY = meanY;
        ArrayList<Point> temp = new ArrayList<>();
        temp.addAll(fuelPos);
        temp.addAll(karboPos);
        for (Point P : temp) {
            for (Point Q : MyRobot.adj_directions) {
                if ((P.x + Q.x < 0) || (P.x + Q.x >= terrain.length) || (P.y + Q.y < 0) || (P.y + Q.y >= terrain.length)) {
                    continue;
                }
                if ((!terrain[P.y + Q.y][P.x + Q.x]) || (fuel[P.y + Q.y][P.x + Q.x]) || (karbo[P.y + Q.y][P.x + Q.x])) {
                    continue;
                }
                int adj_count = 0;
                int dist = (P.x + Q.x - meanX)*(P.x + Q.x - meanX) + (P.y + Q.y - meanY)*(P.y + Q.y - meanY);
                for (Point R : MyRobot.adj_directions) {
                    if (checkDepot(P.x + Q.x + R.x, P.y + Q.y + R.y)) {
                        adj_count++;
                    }
                }
                if (adj_count > adj) {
                    adj = adj_count;
                    locX = P.x + Q.x;
                    locY = P.y + Q.y;
                    max_dist = dist;
                } else if ((adj_count == adj) && (dist < max_dist)) {
                    locX = P.x + Q.x;
                    locY = P.y + Q.y;
                    max_dist = dist;
                }
            }
        }
    }

}