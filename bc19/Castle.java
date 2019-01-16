package bc19;

import java.util.ArrayList;

public class Castle {

    // Map data
    ResourceManager resData;
    
    // Self references
    MyRobot robot;
    Robot me;
    Management manager;
    Comms radio;

    // private variables
    ArrayList<Robot> castle_pos;
    ArrayList<Cluster> depot_cluster;
    ArrayList<Cluster> my_cluster;
    int cluster_count, my_cluster_count;
    boolean preserve_resources;

    // Initialization
    public Castle(MyRobot robot) {

        // Store self references
        this.robot = robot;
        this.me = robot.me;
        this.manager = robot.manager;
        this.radio = robot.radio;
        robot.castleTalk(0);

        // Process and store depot clusters
        resData = new ResourceManager(manager.fuel_map, manager.karbo_map);
        depot_cluster = resData.resourceList;
        cluster_count = depot_cluster.size();
        my_cluster_count = 0;
        robot.log("Castle: Map data acquired");
        
        // store other castles
        castle_pos = new ArrayList<>();
        for (Robot bot: manager.vis_robots) {
            if (bot.unit == robot.SPECS.CASTLE) {
                castle_pos.add(bot);
            }
        }
    }

    // Bot AI
    public Action AI() {

        manager.update_data();

        // find closest depot
        Cluster closest = null;
        if (cluster_count != 0) {

            // remove any clusters that have been claimed
            for (Robot bot: castle_pos) {
                if (bot.castle_talk > 0 && bot.id != me.id) {
                    depot_cluster.remove(bot.castle_talk);
                    cluster_count--;
                }
            }

            // find next cluster with highest number of karbonite depots
            int karb_count = Integer.MIN_VALUE;
            Cluster chosen_cluster = null;
            for (Cluster cluster: depot_cluster) {
                if (cluster.karbonite_count > karb_count) {
                    chosen_cluster = cluster;
                    karb_count = cluster.karbonite_count;
                }
            }

            // check if chosen cluster is nearest to me
            int cluster_dist = Integer.MAX_VALUE;
            int dist;
            Robot closest_bot;
            for (Robot bot: castle_pos) {
                dist = manager.square_distance(bot, new Point(chosen_cluster.locX, chosen_cluster.locY));
                if (dist < cluster_dist) {
                    cluster_dist = dist;
                    closest_bot = bot;
                }
            }

            // if closest_bot is me send pilgrim
            if (closest_bot.id == me.id) {
                my_cluster.add(chosen_cluster);
                radio.baseAssignment(chosen_cluster.ClusterID, false);
                Point empty_adj = manager.findEmptyAdj(manager.me_location, false);
                return robot.buildUnit(robot.SPECS.PILGRIM, empty_adj.x - me.x, empty_adj.y - me.y);
            }
        }

        if (cluster_count != 0) {
            preserve_resources = true;
        } else {
            preserve_resources = false;
        }

        // check if church has been constructed
        // TODO check even if church is not created exactly at mean
        // if (!myclusters.isEmpty()) {
        //     ArrayList<DepotCluster> to_remove = new ArrayList<>();
        //     for (Robot bot : visRobots) {
        //         if (bot.unit == SPECS.CHURCH) {
        //             for (DepotCluster cluster: myclusters) {
        //                 if (cluster.locX == bot.x && cluster.locY == bot.y) {
        //                     to_remove.add(cluster);
        //                 }
        //             }
        //         }
        //     }
        //     myclusters.removeAll(to_remove);
        // }

        // if (!myclusters.isEmpty()) {
        //     for (Cluster cluster: myclusters) {
        //         cluster.turns_to_check--;
        //         if (cluster.turns_to_check == 0) {
        //             this.signal(encoder.baseAssignment(closest.ClusterID, false),2);//temp range 1
        //             Point emptyadj = manager.findEmptyAdj(me, true);
        //             cluster.turns_to_check = 10;
        //             return this.buildUnit(robo.SPECS.PILGRIM, emptyadj.x, emptyadj.y);
        //         }
        //     }
        // }
        

        // all clusters allocated
        // start creating a squad
        int unit_number = 0;
        int[] unit_req = MyRobot.requirements[MyRobot.tiger_squad[unit_number]];
        // if preserve preserve resources for atleast one church
        if (preserve_resources) {
            int [] church_req = MyRobot.requirements[robot.SPECS.CHURCH];
            if (!((church_req[0] + unit_req[0]) >= robot.karbonite && (church_req[1] + unit_req[1] >= robot.fuel))) {
                return null;
            }
        }

        // create new unit if resources exists
        int unit_type = MyRobot.tiger_squad[unit_number];
        Point emptyadj = manager.findEmptyAdj(me, false);
        unit_number = (unit_number++) % MyRobot.tiger_squad.length;
        return robot.buildUnit(unit_type, emptyadj.x,emptyadj.y);
    }

}