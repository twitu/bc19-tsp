package bc19;

import java.util.ArrayList;
import java.util.LinkedList;

public class ResourceData {

    public ArrayList<DepotCluster> resourceList = new ArrayList<>();
    public int count;

    // Process map data and generate clusters with unique ID
    public ResourceData(boolean[][] fuel, boolean[][] karbo) {
        this.count = 0;
        for (int i = 0; i < fuel.length; i++) {
            for (int j = 0; j < fuel.length; j++) {
                if (fuel[i][j] || karbo[i][j]) {
                    boolean newCluster = true;
                    for (DepotCluster D : resourceList) {
                        if (D.checkClusterRange(j, i, D.locX, D.locY)) {
                            D.addDepot(j, i, fuel[i][j]);
                            newCluster = false;
                            break;
                        }
                    }
                    if (newCluster) {
                        DepotCluster D = new DepotCluster(resourceList.size());
                        D.addDepot(j, i, fuel[i][j]);
                        resourceList.add(D);
                        this.count++;
                    }
                }
            }
        }
    }

    // Get ID of cluster where (x, y) is included, -1 if not in any cluster.
    public int getID(int x, int y) {
        for (DepotCluster D : resourceList) {
            if (D.checkClusterRange(x, y, D.locX, D.locY)) {
                return D.ClusterID;
            }
        }
        return -1;
    }

    // Get location of cluster with given ID.
    public Point getLocation(int ID) {
        DepotCluster D = resourceList.get(ID);
        return new Point(D.locX, D.locY);
    }

    // Get linked list of all fuel/karbonite depots in cluster
    public LinkedList<Point> depotList(int ID, boolean fuel) {
        DepotCluster D = resourceList.get(ID);
        LinkedList<Point> out = new LinkedList<>();
        for (Point P : (fuel ? D.fuelPos : D.karboPos)) {
            out.add(new Point(P.x, P.y));
        }
        return out;
    }

}