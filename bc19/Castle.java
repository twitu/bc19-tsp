package bc19;

import java.util.ArrayList;
import java.util.LinkedList;

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
    Cluster selfC;

    LinkedList<Point> fuel_depots, karb_depots;
    boolean fuelb;
    ArrayList<Integer> assigned_pilgrims;
    ArrayList<Point> assigned_depots ;
    Point nextP;
    // Initialization
    public Castle(MyRobot robot) {

        // Store self references
        this.robot = robot;
        this.me = robot.me;
        this.manager = robot.manager;
        this.radio = robot.radio;
        robot.castleTalk(0);
        assigned_pilgrims = new ArrayList<>();
        assigned_depots = new ArrayList<>();
        my_cluster = new ArrayList<>();
                
        // Process and store depot clusters
        resData = new ResourceManager(manager.fuel_map, manager.karbo_map);
        resData.pairClusters(me.x, me.y, manager.map_length, manager.vsymmetry);
        depot_cluster = resData.resourceList;
        cluster_count = depot_cluster.size();
        my_cluster_count = 0;
        robot.log("Castle: Map data acquired");

        // change cluster mean location if castle in cluster
        for (Cluster cluster: depot_cluster) {
            if (cluster.checkRange(me.x,me.y)){
                cluster.locX = me.x;
                cluster.locY = me.y;
                selfC = cluster;
                break;
            }
        }

        fuelb = false;
        fuel_depots = new LinkedList<>();
        karb_depots = new LinkedList<>();
        for(Point p:selfC.fuelPos){
            fuel_depots.add(p);
        }
        for(Point p:selfC.karboPos){
            karb_depots.add(p);
        }
        

        // make list of all castles
        castle_pos = new ArrayList<>();
        for (Robot bot: manager.vis_robots) {
            if (bot.unit == robot.SPECS.CASTLE && bot.id != me.id) {
                castle_pos.add(bot);
            }
        }
    }

    // Bot AI
    public Action AI() {
        this.me = robot.me;

        manager.update_data();
        
        // find closest depot
        if (cluster_count != 0) {

            // remove any clusters that have been claimed
            for (Robot bot: castle_pos) {
                if (bot.castle_talk > 0 && bot.id != me.id) {
                    depot_cluster.remove(bot.castle_talk);
                    cluster_count--;
                }
            }

            if(cluster_count > 0 ){
                // choose cluster nearest to me
                Cluster chosen_cluster = depot_cluster.get(resData.nearestClusterID(me.x, me.y, null));
                 // robot.log("Castle:dd cluster" + Integer.toString(chosen_cluster.ClusterID) + "pos :" +Integer.toString(chosen_cluster.locX) + "  " + Integer.toString(chosen_cluster.locY) );
    
                // if closest_bot is me send pilgrim
                if (chosen_cluster != null) {
                    my_cluster.add(chosen_cluster);
                    if(chosen_cluster.locX == me.x && chosen_cluster.locY == me.y){
                        if(manager.buildable(robot.SPECS.PILGRIM)){
                            Point p = manager.findEmptyAdj(me,true);
                            Point nextP;
                            if(p != null){
                                if(fuelb){
                                    nextP = fuel_depots.pollFirst();                                        
                                }else{
                                    nextP = karb_depots.pollFirst();
                                    if(karb_depots.size()==0){
                                        fuelb=true;
                                    }
                                }
                                robot.signal(robot.radio.assignDepot(nextP),2);
                                assigned_depots.add(nextP);
                                robot.log("Castle : assigning" + Integer.toString(nextP.x) + ", " + Integer.toString(nextP.y));
                                // created = true;
                                return robot.buildUnit(robot.SPECS.PILGRIM,p.x,p.y);
                            }
                        }
                    }else{
                        robot.signal(radio.baseAssignment(chosen_cluster.ClusterID, false), 2);
                    }
                    robot.castleTalk(chosen_cluster.ClusterID);
                    Point empty_adj = manager.findEmptyAdj(me, false);
                    robot.log("castle test");
                    // robot.log("Castle:dd empadj" + Integer.toString(empty_adj.x)+ ","  " + Integer.toString(empty_adj.y) );
    
               
                    robot.log("building pilgrim at " + Integer.toString(me.signal));
                    return robot.buildUnit(robot.SPECS.PILGRIM, empty_adj.x, empty_adj.y);
                }
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
        robot.log("unit type: " + Integer.toString(MyRobot.tiger_squad[unit_number]));
        return robot.buildUnit(unit_type, emptyadj.x,emptyadj.y);
        
    }

}
