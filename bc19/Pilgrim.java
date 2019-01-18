package bc19;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays.*;
import java.util.Arrays;
import java.lang.*;

public class Pilgrim {

    // Map data
    ResourceManager resData;
    
    // Self references and helpers
    MyRobot robo;
    Robot me;
    Management manager;
    Comms radio;

    // private variables
    int status, cluster_id;
    Point home,mineLoc;
    Cluster home_cluster;

    // Initialization
    public Pilgrim(MyRobot robo) {
        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.radio = robo.radio;
        
        manager.update_data();
        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map,manager.fuel_map, manager.karbo_map);
        // robo.log("Pilgrim: Map data acquired");

        for (Point p: MyRobot.adj_directions) {
            if (manager.vis_robot_map[me.y + p.y][me.x + p.x] > 0) {
                Robot bot = robo.getRobot(manager.vis_robot_map[me.y + p.y][me.x + p.x]);
                if (bot.unit == robo.SPECS.CHURCH) {
                    if(robo.isRadioing(bot)){//check for assign depot signal
                        int ch_sig = bot.signal;
                        if(ch_sig%16 == 3){
                            mineLoc = new Point(1,1);
                            mineLoc.y = ((ch_sig - 3)/16)%1024;
                            mineLoc.x = (ch_sig - 3)/1024;
                            status = 0;//miner
                            robo.log("Pilgrim initialized status:" + Integer.toString(status) + "mineloc:"  + Integer.toString(mineLoc.x) + ", " +Integer.toString(mineLoc.y)+ "home:"  + Integer.toString(home.x) + ", " +Integer.toString(home.y));
                            home = new Point(bot.x, bot.y);
                            break;
                        }
                    }
                } else if (bot.unit == robo.SPECS.CASTLE) {
                    if (robo.isRadioing(bot)) {
                        if(bot.signal%16==3){
                            mineLoc = new Point(1,1);
                            mineLoc.x = bot.signal/1024;
                            mineLoc.y = (bot.signal%1024)/16;
                            home = new Point(bot.x,bot.y);
                            status = 0;
                            robo.log("Pilgrim initialized status:" + Integer.toString(status) + "mineloc:"  + Integer.toString(mineLoc.x) + ", " +Integer.toString(mineLoc.y)+ "home:"  + Integer.toString(home.x) + ", " +Integer.toString(home.y));
                            break;
                            
                        }
                        cluster_id = bot.signal % 16;
                        home_cluster = resData.resourceList.get(cluster_id);
                        
                        status = 1;//go to cluster
                            robo.log("Pilgrim initialized status:" + Integer.toString(status));
                        
                        break;
                    }
                }
            }
        }

    }

    // Bot AI
    public Action AI() {
        this.me = robo.me;
     
        robo.log("I am at " + Integer.toString(me.x) + "," + Integer.toString(me.y) + "status: "  + Integer.toString(status));

        if(status == 1){
            // move towards cluster
            Point P = resData.getLocation(cluster_id);
            if (manager.isAdj(manager.me_location, P)) {
                if(manager.buildable(robo.SPECS.CHURCH)){
                    mineLoc = home_cluster.fuelPos.get(0);
                    robo.log("building @ " + Integer.toString(mineLoc.y) + " , " + Integer.toString(mineLoc.x));
                    robo.signal(radio.assignDepot(mineLoc),2);
                    home = P;
                    status = 0;//miner
                    robo.log("building @ " + Integer.toString(P.y) + " , " + Integer.toString(P.x));
                    return robo.buildUnit(robo.SPECS.CHURCH, P.x - me.x, P.y - me.y);
                }
            } else {                          
                Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), true, P);
                robo.log("next is " + Integer.toString(next.x) + ", " + Integer.toString(next.y));
                if ((next.x == P.x) &&(next.y == P.y)){
                    next = manager.findEmptyNextAdj(next, manager.me_location, MyRobot.four_directions);
                }
                robo.log("found next step " + Integer.toString(next.x) + ", " + Integer.toString(next.y));
                return robo.move(next.x - me.x, next.y - me.y);
            }
        }
        
        
        
        if (status == 0){
            if(me.karbonite == 20 || me.fuel == 100){
                //deposit
                robo.log("returning");
                if(manager.isAdj(new Point(me.x,me.y),home)){
                    robo.log("giving");
                    return robo.give(home.x - me.x , home.y-me.y,me.karbonite,me.fuel);
                }
                
                Point next = manager.findNextStep(me.x,me.y,manager.copyMap(manager.passable_map),true,home);
                robo.log("found next step " + Integer.toString(next.x) + ", " + Integer.toString(next.y));
                if(next.x==home.x && next.y == home.y){
                    next = manager.findEmptyNextAdj(next, manager.me_location, MyRobot.four_directions);
                }
                return robo.move(next.x - me.x, next.y - me.y);
            } else {
                //mine
                if(me.x == mineLoc.x && me.y == mineLoc.y){
                    return robo.mine();
                }
                Point next = manager.findNextStep(me.x,me.y,manager.copyMap(manager.passable_map),true,mineLoc);
                robo.log("found next step " + Integer.toString(next.x) + ", " + Integer.toString(next.y));
                return robo.move(next.x - me.x, next.y - me.y);
                
                
            }
            
        }
        return null;
        
    }

}