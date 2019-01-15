package bc19;

import java.util.ArrayList;

public class DepotCluster {

    public ArrayList<Point> fuelPos = new ArrayList<>();
    public ArrayList<Point> karboPos = new ArrayList<>();
    public int ClusterID;
    public int locX, locY;

    // Initialize Depot Cluster
    public DepotCluster(int ID) {
        this.ClusterID = ID;
        locX = -1;
        locY = -1;
    }

    // Add a new depot and shift center of mass accordingly
    public void addDepot(int x, int y, boolean fuel) {
        if (locX == -1) {
            locX = x;
            locY = y;
        }
        else {
            locX = locX*(fuelPos.size() + karboPos.size()) + x;
            locY = locY*(fuelPos.size() + karboPos.size()) + y;
            locX /= (fuelPos.size() + karboPos.size() + 1);
            locY /= (fuelPos.size() + karboPos.size() + 1);
        }
        if (fuel) {
            fuelPos.add(new Point(x, y));
        }
        else {
            karboPos.add(new Point(x, y));
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
        return (((x - locX)*(x - locX) + (y - locY)*((y - locY))) <= 50);
    }

}