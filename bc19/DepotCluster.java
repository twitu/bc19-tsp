package bc19;

import java.util.ArrayList;

public class DepotCluster {

    public ArrayList<Point> fuelPos = new ArrayList<>();
    public ArrayList<Point> karboPos = new ArrayList<>();
    public int ClusterID;
    public int castle_id;
    public int depots;
    public int turns_to_check;
    public int locX, locY;

    // Initialize Depot Cluster
    public DepotCluster(int ID) {
        this.depots = 1;
        this.turns_to_check = 10;
        this.ClusterID = ID;
        this.castle_id = -1;
        this.locX = -1;
        this.locY = -1;
    }

    // Add a new depot and shift center of mass accordingly
    public void addDepot(int x, int y, boolean fuel) {
        if (locX == -1) {
            locX = x;
            locY = y;
        } else {
            locX = locX*(fuelPos.size() + karboPos.size()) + x;
            locY = locY*(fuelPos.size() + karboPos.size()) + y;
            locX /= (fuelPos.size() + karboPos.size() + 1);
            locY /= (fuelPos.size() + karboPos.size() + 1);
        }

        if (fuel) {
            fuelPos.add(new Point(x, y));
        } else {
            karboPos.add(new Point(x, y));
        }
        depots++;
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

    // Check if (x, y) is within range of given cluster at mx,my
    public boolean checkClusterRange(int x, int y, int mx, int my) {
        return (((x - mx)*(x - mx) + (y - my)*((y - my))) <= 50);
    }

}