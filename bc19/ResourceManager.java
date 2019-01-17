package bc19;

import java.util.ArrayList;
import java.util.LinkedList;

public class ResourceManager {

    ///*** API ***///
    //
    //  public ResourceManager(boolean[][] fuel, boolean[][] karbo)
    //      Analyze fuel and karbo maps and process data to form clusters.
    //      Assign a unique ID to each cluster starting as 0, 1, 2, 3, ...
    //
    //  public int getID(int x, int y)
    //      Get ID of cluster to which (x, y) belongs
    //
    //  public Point getLocation(int ID)
    //      Get centre of mass of cluster with given ID
    //
    //  public LinkedList<Point> depotList(int ID, boolean fuel)
    //      Return a linked list of all fuel/karbonite depots in cluster with given ID.
    //
    //  public int nearestClusterID(int x, int y, ArrarList<Integer> avoid)
    //      Get cluster ID of nearest cluster, avoiding certain clusters
    //
    ///*** END ***///

    // Private Variables
    public ArrayList<Cluster> resourceList;
    public ArrayList<Cluster> homeClusters;
    public ArrayList<Cluster> enemyClusters;

    // Process map data and generate clusters with unique ID
    public ResourceManager(boolean[][] fuel, boolean[][] karbo) {
        resourceList = new ArrayList<>();
        homeClusters = new ArrayList<>();
        enemyClusters =  new ArrayList<>();
        
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
        for (Cluster D: resourceList) {
            dist = (D.locX - x)*(D.locX - x) + (D.locY - y)*(D.locY - y);
            if (dist < max_dist) {
                ID = D.ClusterID;
                max_dist = dist;
            }
        }
        return ID;
    }

    // Classify clusters
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
                enemyClusters.add(Dual);
            } else {
                homeClusters.add(Dual);
                enemyClusters.add(D);
            }
        }
    }
}