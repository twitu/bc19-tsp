package bc19;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays.*;
import java.util.Arrays;
import java.lang.*;

//TODO: Enemy Base present at build site. Then what?

public class Pilgrim {

    // Map data
    ResourceManager resData;
    
    // Self references and helpers
    MyRobot robo;
    Robot me;
    Management manager;
    Comms radio;

    // Private variables
    int status, cluster_id;
    Point home, mineLoc;
    Cluster home_cluster;
    boolean combat;

    // Initialization
    public Pilgrim(MyRobot robo) {
        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.radio = robo.radio;
        manager.updateData();
        
        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map,manager.fuel_map, manager.karbo_map);
        combat = false;
        robo.log("Pilgrim: Map data acquired");

        // Look for creator building and initialize status
        for (Point p: MyRobot.adj_directions) {
            if (manager.vis_robot_map[me.y + p.y][me.x + p.x] > 0) {
                Robot bot = robo.getRobot(manager.vis_robot_map[me.y + p.y][me.x + p.x]);
                if (bot.unit == robo.SPECS.CHURCH) {
                    if(robo.isRadioing(bot)){//check for assign depot signal
                        int ch_sig = bot.signal;
                        if(ch_sig%16 == 3){
                            mineLoc = new Point(-1, -1);
                            mineLoc.x = ch_sig/1024;
                            mineLoc.y = (ch_sig % 1024)/16;
                            status = 0;//miner
                            home = new Point(bot.x, bot.y);
                            break;
                        }
                    }
                } else if (bot.unit == robo.SPECS.CASTLE) {
                    if (robo.isRadioing(bot)) {
                        if (bot.signal % 16 == 3){
                            mineLoc = new Point(-1, -1);
                            mineLoc.x = bot.signal/1024;
                            mineLoc.y = (bot.signal % 1024)/16;
                            home = new Point(bot.x, bot.y);
                            status = 0;
                            break;
                        } else if ((bot.signal % 16 == 0) || (bot.signal % 16 == 1)) {
                            cluster_id = bot.signal/16;
                            home_cluster = resData.resourceList.get(cluster_id);
                            status = 1;//go to cluster
                            break;
                        }
                    }
                }
            }
        }

    }

    // Bot AI
    public Action AI() {
        
        this.me = robo.me;
        manager.updateData();
        
        // Leader status. Go to cluster and build a base
        if(status == 1) {
            Point P = resData.getLocation(cluster_id);
            
            // Am I adjacent to the target?
            if (manager.isAdj(manager.me_location, P)) {
                
                // Enough Resources
                if(manager.buildable(robo.SPECS.CHURCH)){
                    
                    // Find nearest depot
                    int max_dist = Integer.MAX_VALUE;
                    mineLoc = home_cluster.karboPos.get(0);
                    for (Point T : home_cluster.karboPos) {
                        int dist = (T.x - me.x)*(T.x - me.x) + (T.y - me.y)*(T.y - me.y);
                        if (dist < max_dist) {
                            max_dist = dist;
                            mineLoc = T;
                        }
                    }
                    
                    // Build church, inform nearest depot and set worker status
                    robo.signal(radio.assignDepot(mineLoc),2);
                    home = P;
                    status = 0;
                    return robo.buildUnit(robo.SPECS.CHURCH, P.x - me.x, P.y - me.y);
                
                // Need more resources
                } else {
                    robo.log("Need more resources");
                    return null;
                }

            // No? I need to move to target
            } else {                          
                Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), true, P);
                if ((next.x == P.x) &&(next.y == P.y)){
                    next = manager.findEmptyNextAdj(next, manager.me_location, MyRobot.four_directions);
                }
                return robo.move(next.x - me.x, next.y - me.y);
            }

        // Worker status. Resource collector
        } else if (status == 0) {
            
            // Enemy Surveilance
            boolean noCombat = true;
            for (Robot bot: manager.vis_robots) {
                if (!robo.isVisible(bot)) {
                    continue;
                }
                if (bot.team != me.team) {
                    noCombat = false;
                    if (!combat) {
                        combat = true;
                        robo.signal(radio.emergency(new Point(bot.x, bot.y)), Cluster.range);
                    }
                }
                if ((bot.team == me.team) && (bot.signal % 16 == 5)) {
                    noCombat = false;
                    combat = true;
                    break;
                }
            }
            if (noCombat) {
                combat = false;
            }
            
            // Am I in combat mode or carrying full capacity?
            if((me.karbonite == 20 || me.fuel == 100) || (combat && (me.karbonite != 0 || me.fuel != 0))){
                
                // Deposit resources if adjacent to home
                if(manager.isAdj(new Point(me.x,me.y),home)){
                    return robo.give(home.x - me.x , home.y - me.y, me.karbonite, me.fuel);
                }
                
                // Not adjacent? Go home
                Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), true, home);
                if(next.x == home.x && next.y == home.y){
                    next = manager.findEmptyNextAdj(home, manager.me_location, MyRobot.four_directions);
                }
                return robo.move(next.x - me.x, next.y - me.y);

            // No? Gather resources
            } else {

                // On a depot? Mine then
                if(me.x == mineLoc.x && me.y == mineLoc.y){
                    return robo.mine();
                }

                // Not on depot. Go to depot
                Point next = manager.findNextStep(me.x, me.y, manager.copyMap(manager.passable_map), true, mineLoc);
                return robo.move(next.x - me.x, next.y - me.y);
            }
        }
        
        // In case I get confused, sit tight
        return null;
    }
}