package bc19;

import java.util.ArrayList;
import java.util.LinkedList;

public class Castle {

    // Map data
    ResourceManager resData;
    
    // Self references
    RefData refData;
    MyRobot robot;
    Robot me;
    Management manager;
    Comms radio;

    // private variables
    ArrayList<Robot> castle_pos;
    ArrayList<Cluster> depot_cluster;
    ArrayList<Cluster> my_cluster;
    int cluster_count, my_cluster_count;
    ArrayList<Integer> avoid_clusters;
    boolean preserve_resources,self;

    LinkedList<Point> fuel_depots, karb_depots;
    boolean fuelb;
    ArrayList<Integer> assigned_pilgrims;
    ArrayList<Point> assigned_depots ;
    Point nextP;
    // Initialization
    public Castle(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.radio = robo.radio;
        this.refData = new RefData();
        robo.castleTalk(0);
        assigned_pilgrims = new ArrayList<>();
        assigned_depots = new ArrayList<>();
        my_cluster = new ArrayList<>(); 
        avoid_clusters = new ArrayList<>();
        self = false;
                
        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map,manager.fuel_map, manager.karbo_map);
        resData.pairClusters(me.x, me.y, manager.map_length, manager.vsymmetry);
        depot_cluster = resData.homeClusters;
        cluster_count = depot_cluster.size();
        my_cluster_count = 0;
        robo.log("Castle: Map data acquired");

        // change cluster mean location if castle in cluster
        for (Cluster cluster: depot_cluster) {
            if (cluster.checkRange(me.x,me.y)){
                cluster.locX = me.x;
                cluster.locY = me.y;
            }
        }

        int id = resData.nearestClusterID(me.x, me.y, avoid_clusters);
        avoid_clusters.add(id);
        Cluster chosen_cluster = depot_cluster.get(id);
        if(chosen_cluster.locX == me.x && chosen_cluster.locY == me.y){
            self = true;
        }
        cluster_count--;

        fuelb = false;
        

        // make list of all castles
        castle_pos = new ArrayList<>();
        for (Robot bot: manager.vis_robots) {
            if (bot.unit == robo.SPECS.CASTLE && bot.id != me.id) {
                castle_pos.add(bot);
            }
        }
    }

    // Bot AI
    public Action AI() {
        this.me = robo.me;

        manager.updateData();

        // find closest depot
        if (cluster_count != 0) {

            // remove any clusters that have been claimed
            for (Robot bot: manager.vis_robots) {
                if (bot.castle_talk %16 == 0 && bot.id != me.id) {
                    // depot_cluster.remove(bot.castle_talk);
                    avoid_clusters.add(bot.castle_talk/16);
                    cluster_count--;
                }
            }
        }

        // check for enemy bots and attack before doing anything else
        Robot closest = null;
        int health = Integer.MAX_VALUE;
        for (Robot bot: manager.vis_robots) {
            if (bot.team != me.team && refData.in_attack_range(bot, me) && (bot.health < health)) {
                    health = bot.health;
                    closest = bot;
            }
        }
        if (closest != null) {
            return robo.attack(closest.x - me.x, closest.y - me.y);
        }

        if (cluster_count > 0 ) {
            // choose cluster nearest to me
            //if not assigning to castle cluster
            // if(!self){
            //     int id = resData.nearestClusterID(me.x, me.y, avoid_clusters);
            //     avoid_clusters.add(id);
            //     Cluster chosen_cluster = depot_cluster.get(id);
            //     if(chosen_cluster.locX == me.x && chosen_cluster.locY == me.y){
            //         self = true;
            //     }
            // }
            // robo.log("Castle:dd cluster" + Integer.toString(chosen_cluster.ClusterID) + "pos :" +Integer.toString(chosen_cluster.locX) + "  " + Integer.toString(chosen_cluster.locY) );

            // if closest_bot is me send pilgrim
            if (chosen_cluster != null) {
                my_cluster.add(chosen_cluster);
                if(self){
                    if(manager.buildable(robo.SPECS.PILGRIM)){
                        Point p = manager.findEmptyAdj(me,true);
                        Point nextP;
                        if(p != null){
                            if(fuelb){
                                nextP = fuel_depots.pollFirst();  
                                if(fuel_depots.size()==0){
                                    self=false;
                                }                                      
                            }else{
                                nextP = karb_depots.pollFirst();
                                if(karb_depots.size()==0){
                                    fuelb=true;
                                }
                            }
                            robo.signal(robo.radio.assignDepot(nextP),2);
                            assigned_depots.add(nextP);
                            robo.log("Castle : assigning" + Integer.toString(nextP.x) + ", " + Integer.toString(nextP.y));
                            // created = true;
                            return robo.buildUnit(robo.SPECS.PILGRIM,p.x,p.y);
                        }
                    }
                }else{
                    if(manager.buildable(robo.SPECS.PILGRIM)){
                        Point p = manager.findEmptyAdj(me,true);
                        Point nextP;
                        if(p != null){
                            if(fuelb){
                                nextP = fuel_depots.pollFirst();  
                                if(fuel_depots.size()==0){
                                    self=false;
                                }                                      
                            }else{
                                nextP = karb_depots.pollFirst();
                                if(karb_depots.size()==0){
                                    fuelb=true;
                                }
                            }
                            id = resData.nearestClusterID(me.x, me.y, avoid_clusters);
                            avoid_clusters.add(id);
                            Cluster chosen_cluster = depot_cluster.get(id);
                            if(chosen_cluster.locX == me.x && chosen_cluster.locY == me.y){
                                self = true;
                            }
                            robo.castleTalk(chosen_cluster.ClusterID);
                            robo.signal(radio.baseAssignment(chosen_cluster.ClusterID, false), 2);
                            cluster_count--;
                            robo.log("Castle : assigning cluster :" + Integer.toString(chosen_cluster.locX) + ", " + Integer.toString(chosen_cluster.locY));
                            // created = true;
                            return robo.buildUnit(robo.SPECS.PILGRIM,p.x,p.y);
                        }
                    }
                }
                // Point empty_adj = manager.findEmptyAdj(me, false);
                // robo.log("castle test");
                // // robo.log("Castle:dd empadj" + Integer.toString(empty_adj.x)+ ","  " + Integer.toString(empty_adj.y) );

                // depot_cluster.remove(chosen_cluster);
                // robo.log("building pilgrim at " + Integer.toString(me.signal));
                // return robo.buildUnit(robo.SPECS.PILGRIM, empty_adj.x, empty_adj.y);
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
            int [] church_req = MyRobot.requirements[robo.SPECS.CHURCH];
            if (!((church_req[0] + unit_req[0]) >= robo.karbonite && (church_req[1] + unit_req[1] >= robo.fuel))) {
                return null;
            }
        }

        // create new unit if resources exists
        int unit_type = MyRobot.tiger_squad[unit_number];
        Point emptyadj = manager.findEmptyAdj(me, false);
        unit_number = (unit_number++) % MyRobot.tiger_squad.length;
        robo.log("unit type: " + Integer.toString(MyRobot.tiger_squad[unit_number]));
        return robo.buildUnit(unit_type, emptyadj.x,emptyadj.y);
        
    }

}
