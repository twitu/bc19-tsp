package bc19;

import java.util.ArrayList;
import java.util.LinkedList;

public class ResourceManager {

    ///*** API ***///
    //
    //  public ResourceManager(boolean[][] fuel, boolean[][] karbo);
    //  public int getID(int x, int y);
    //  public Point getLocation(int ID);
    //  public LinkedList<Point> depotList(int ID, boolean fuel);
    //  public int nearestClusterID(int x, int y, ArrarList<Integer> avoid);
    //  public void pairClusters(int x, int y, int map_length, boolean vsymmetry);
    //
    ///*** END ***///

    // Private Variables
    public ArrayList<Cluster> resourceList = new ArrayList<>();
    public ArrayList<Cluster> homeClusters = new ArrayList<>();
    public ArrayList<Cluster> enemyClusters = new ArrayList<>();

    // Process map data and generate clusters with unique ID
    public ResourceManager(boolean[][] terrain, boolean[][] fuel, boolean[][] karbo) {
        for (int i = 0; i < fuel.length; i++) {
            for (int j = 0; j < fuel.length; j++) {
                if (fuel[i][j] || karbo[i][j]) {
                    boolean newCluster = true;
                    for (Cluster D : resourceList) {
                        if (D.checkRange(j, i)) {
                            D.addDepot(j, i, fuel[i][j]);
                            newCluster = false;
                            break;
                        }
                    }
                    if (newCluster) {
                        Cluster D = new Cluster(resourceList.size());
                        D.addDepot(j, i, fuel[i][j]);
                        resourceList.add(D);
                    }
                }
            }
        }
        for (Cluster D : resourceList) {
            D.homePoint(terrain, fuel, karbo);
        }
    }

    // Get ID of cluster where (x, y) is included, -1 if not in any cluster and out of range.
    public int getID(int x, int y) {
        for (Cluster D : resourceList) {
            if (D.checkDepot(x, y)) {
                return D.ClusterID;
            }
        }
        for (Cluster D : resourceList) {
            if (D.checkRange(x, y)) {
                return D.ClusterID;
            }
        }
        return -1;
    }

    // Get location of cluster with given ID.
    public Point getLocation(int ID) {
        Cluster D = resourceList.get(ID);
        return new Point(D.locX, D.locY);
    }

    // Get linked list of all fuel/karbonite depots in cluster
    public LinkedList<Point> depotList(int ID, boolean fuel) {
        Cluster D = resourceList.get(ID);
        LinkedList<Point> out = new LinkedList<>();
        for (Point P : (fuel ? D.fuelPos : D.karboPos)) {
            out.add(new Point(P.x, P.y));
        }
        return out;
    }

    // Get cluster ID of nearest cluster, avoiding certain clusters
    public int nearestClusterID(int x, int y, ArrayList<Integer> avoid) {
        int dist, ID = 0, max_dist = Integer.MAX_VALUE;
        if (avoid == null) {
            avoid = new ArrayList<>();
        }
        for (Cluster D: resourceList) {
            if (avoid.contains(D.ClusterID)) {
                continue;
            }
            dist = (D.locX - x)*(D.locX - x) + (D.locY - y)*(D.locY - y);
            if (dist < max_dist) {
                ID = D.ClusterID;
                max_dist = dist;
            }
        }
        return ID;
    }

    // Classify clusters into pairs of home, enemy.
    public void pairClusters(int x, int y, int map_length, boolean vsymmetry) {
        Cluster Dual;
        int ID;
        for (Cluster D : resourceList) {
            if ((homeClusters.contains(D) || (enemyClusters.contains(D)))) {
                continue;
            }
            if (vsymmetry) {
                ID = getID(D.locX, map_length - 1 - D.locY);
            }
            else {
                ID = getID(map_length - 1 - D.locX, D.locY);
            }
            Dual = resourceList.get(ID);
            int R = (D.locX - x)*(D.locX - x) + (D.locY - y)*(D.locY - y);
            int Rdual = (Dual.locX - x)*(Dual.locX - x) + (Dual.locY - y)*(Dual.locY - y);
            if (R <= Rdual) {
                homeClusters.add(D);
                D.status = 0;
                enemyClusters.add(Dual);
                Dual.status = -1;
            } else {
                homeClusters.add(Dual);
                Dual.status = 0;
                enemyClusters.add(D);
                D.status = -1;
            }
        }
    }

}