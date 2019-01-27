package bc19;

import java.util.ArrayList;
import java.util.LinkedList;

public class ResourceManager {

    ///*** API ***///
    //
    //  ResourceManager(boolean[][] fuel, boolean[][] karbo);
    //  int getID(int x, int y);
    //  Point getLocation(int ID);
    //  LinkedList<Point> depotList(int ID, boolean fuel);
    //  int nearestClusterID(int x, int y, ArrayList<Integer> avoid);
    //  void pairClusters(int x, int y, int map_length, boolean vsymmetry);
    //  int nextTargetID(int x, int y);
    //  void targetAssigned(int ID);
    //  void addTarget(int ID)
    //
    ///*** END ***///

    // Private Variables
    public ArrayList<Cluster> resourceList = new ArrayList<>();
    public ArrayList<Integer> homeClusters = new ArrayList<>();
    public ArrayList<Integer> enemyClusters = new ArrayList<>();
    public ArrayList<Integer> midClusters = new ArrayList<>();
    public ArrayList<Integer> targets;
    public ArrayList<Integer> midTargets;

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
    public void pairClusters(int x, int y, int map_length, boolean vsymmetry, Management manager) {
        Cluster Dual;
        int ID;
        for (Cluster D : resourceList) {
            if ((homeClusters.contains(D.ClusterID) || (enemyClusters.contains(D.ClusterID)))) {
                continue;
            }

            if (vsymmetry) {
                ID = getID(D.locX, map_length - 1 - D.locY);
            } else {
                ID = getID(map_length - 1 - D.locX, D.locY);
            }
            
            Dual = resourceList.get(ID);
            int R = (D.locX - x)*(D.locX - x) + (D.locY - y)*(D.locY - y);
            int Rdual = (Dual.locX - x)*(Dual.locX - x) + (Dual.locY - y)*(Dual.locY - y);
            if (R < Rdual) {
                homeClusters.add(D.ClusterID);
                D.status = 0;
                enemyClusters.add(Dual.ClusterID);
                Dual.status = -1;
            } else if (R == Rdual) {
                midClusters.add(D.ClusterID);
                D.status = 0;
            } else {
                homeClusters.add(Dual.ClusterID);
                Dual.status = 0;
                enemyClusters.add(D.ClusterID);
                D.status = -1;
            }
        }
        targets = new ArrayList<>(homeClusters);
        midTargets = new ArrayList<>(midClusters);
        relocateMidClusters(manager);
    }

    // Get the next cluster to target
    public int nextTargetID(int x, int y, boolean mid) {
        int karbo, out = Integer.MAX_VALUE, min_karbo = Integer.MIN_VALUE;
        int dist, max_dist = Integer.MAX_VALUE;
        if (mid) {
            for (int i: midTargets) {
                Cluster D = resourceList.get(i);
                karbo = D.karboPos.size();
                if (karbo > min_karbo) {
                    out = D.ClusterID;
                    min_karbo = karbo;
                }
            }
            midTargets.remove(Integer.valueOf(out));
        } else {
            for (int i: targets) {
                Cluster D = resourceList.get(i);
                dist = (D.locX - x)*(D.locX - x) + (D.locY - y)*(D.locY - y);
                if (dist < max_dist) {
                    out = D.ClusterID;
                    max_dist = dist;
                }
            }
            targets.remove(Integer.valueOf(out));
        }
        return out;
    }

    // Mark cluster as assigned
    public void targetAssigned(int ID) {
        if (midTargets.contains(ID)) {
            midTargets.remove(Integer.valueOf(ID));
        }
        if (targets.contains(ID)) {
            targets.remove(Integer.valueOf(ID));
        }
    }

    // Add a target cluster
    public void addTarget(int ID) {
        targets.add(ID);
    }

    // Sort enemyClusters by distance from point
    public void sortEnemy(Point P) {
        for (int i = 0; i < enemyClusters.size(); i++) {
            int smallest = i;
            int max_dist = Integer.MAX_VALUE;
            for (int j = i; j < enemyClusters.size(); j++) {
                Cluster D = resourceList.get(enemyClusters.get(j));
                int dist = (P.x - D.locX)*(P.x - D.locX) + (P.y - D.locY)*(P.y - D.locY);
                if (dist < max_dist) {
                    max_dist = dist;
                    smallest = j;
                }
            }
            int temp = enemyClusters.get(smallest);
            enemyClusters.set(smallest, enemyClusters.get(i));
            enemyClusters.set(i, temp);
        }
    }

    // Relocate Mid Cluster home to nearer side
    public void relocateMidClusters(Management manager) {
        for (int i: midClusters) {
            Cluster D = resourceList.get(i);
            Point a, b;
            a = new Point(D.locX, D.locY);
            b = manager.oppPoint(a.x, a.y);
            if (manager.me_location.dist(a) > manager.me_location.dist(b)) {
                D.locX = b.x;
                D.locY = b.y;
            }
        }
    }

}