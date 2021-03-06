package bc19;

import java.util.ArrayList;
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
    CombatManager combat_manager;
    Comms radio;

    // Private variables
    public static int[] emergencyFund = {30, 20};
    int state, cluster_id;
    Point home, mineLoc;
    Cluster home_cluster;
    boolean combat,emergency;
    RefData refdata = new RefData();

    // Initialization
    public Pilgrim(MyRobot robo) {
        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.combat_manager = robo.combat_manager;
        this.manager = robo.manager;
        this.radio = robo.radio;
        this.refdata = robo.refdata;
        this.resData = robo.resData;
        
        // Process and store depot clusters
        combat = false;
        emergency = false;

        // Look for creator building and initialize state
        for (Point p: MyRobot.adj_directions) {
            if (manager.getRobotIdMap(me.x + p.x, me.y + p.y) > 0) {
                Robot bot = robo.getRobot(manager.getRobotIdMap(me.x + p.x, me.y + p.y));
                if (bot.unit == robo.SPECS.CHURCH) {
                    if(robo.isRadioing(bot)){//check for assign depot signal
                        int ch_sig = bot.signal;
                        if(ch_sig%16 == 3){
                            mineLoc = new Point(-1, -1);
                            mineLoc.x = ch_sig/1024;
                            mineLoc.y = (ch_sig % 1024)/16;
                            state = 0;//miner
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
                            Cluster D = resData.resourceList.get(resData.getID(mineLoc.x,mineLoc.y));
                            if(D.checkRange(bot.x,bot.y)){
                                home = new Point(bot.x, bot.y);
                            }else{
                                home = new Point(D.locX, D.locY);
                            }
                            state = 0;
                            break;
                        } else if ((bot.signal % 16 == 0) || (bot.signal % 16 == 1)) {
                            cluster_id = bot.signal/16;
                            home_cluster = resData.resourceList.get(cluster_id);
                            home = resData.getLocation(cluster_id);
                            state = 1;//go to cluster
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
        
        // Leader state. Go to cluster and build a base
        if(state == 1) {
            Point P = resData.getLocation(cluster_id);
            
            // Am I adjacent to the target?
            if (manager.isAdj(manager.me_location, P)) {
                
                // Enough Resources
                int[] unit_req = RefData.requirements[robo.SPECS.CHURCH];
                if((emergencyFund[0] + unit_req[0]) <= robo.karbonite && (emergencyFund[1] + unit_req[1] <= robo.fuel)){
                    
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
                    
                    // Build church, inform nearest depot and set worker state
                    robo.signal(radio.assignDepot(mineLoc),2);
                    home = P;
                    state = 0;
                    return robo.buildUnit(robo.SPECS.CHURCH, P.x - me.x, P.y - me.y);
                
                // Need more resources
                } else {
                    int max_dist = Integer.MAX_VALUE;
                    mineLoc = home_cluster.karboPos.get(0);
                    for (Point T : home_cluster.karboPos) {
                        int dist = (T.x - me.x)*(T.x - me.x) + (T.y - me.y)*(T.y - me.y);
                        if (dist < max_dist) {
                            max_dist = dist;
                            mineLoc = T;
                        }
                    }

                    home = P;
                    state = 0;
                    Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, false, true, mineLoc);
                    if(next == null){
                        return null;
                    }
                    return robo.move(next.x - me.x, next.y - me.y);
                }

            // No? I need to move to target
            } else {                          
                Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, false, true,  P);
                if ((next.x == P.x) &&(next.y == P.y)){
                    next = manager.findEmptyNextAdj(next, manager.me_location, MyRobot.four_directions, false);
                }
                return robo.move(next.x - me.x, next.y - me.y);
            }

        // Worker state. Resource collector
        } else if (state == 0) {
            
            // Enemy Surveilance
            ArrayList<Robot> enemies = combat_manager.visibleEnemies(me);
            Point strike = combat_manager.pantherStrike(3, 16, 2);

            // escape from attacks
            ArrayList<Robot> danger = combat_manager.defendFromEnemies(me, enemies);
            if (danger.size() > 0) {
                combat = true;
                Point safe_step = combat_manager.nextSafeStep(manager.me, enemies, MyRobot.four_directions, true);
                return robo.move(safe_step.x - me.x, safe_step.y - me.y);
            }

            if((robo.karbonite < emergencyFund[0] || robo.fuel < emergencyFund[1]) && me.turn < 100){
                emergency = true;
            }
            
            // Am I in combat mode or carrying full capacity?
            if((me.karbonite == 20 || me.fuel == 100) || (combat && (me.karbonite != 0 || me.fuel != 0)) || (emergency && (me.karbonite > 10 || me.fuel > 50) )){
                
                // Deposit resources if adjacent to home
                if(manager.isAdj(new Point(me.x,me.y),home)){
                    Robot r = robo.getRobot(manager.getRobotIdMap(home.x, home.y));
                    if(r!=null && (r.unit==robo.SPECS.CASTLE || r.unit==robo.SPECS.CHURCH) && r.team == me.team){
                        return robo.give(home.x - me.x , home.y - me.y, me.karbonite, me.fuel);
                    }
                    if(!manager.buildable(robo.SPECS.CHURCH)) return null;
                    robo.signal(radio.assignDepot(mineLoc),2);
                    emergency = false;
                    return robo.buildUnit(robo.SPECS.CHURCH, home.x-me.x, home.y-me.y);
                }
                
                // Not adjacent? Go home
                Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, false, true,  home);
                if(next.x == home.x && next.y == home.y){
                    next = manager.findEmptyNextAdj(home, manager.me_location, MyRobot.four_directions, false);
                    if(next == null){//maybe starvation
                        return null;
                    }
                }
                return robo.move(next.x - me.x, next.y - me.y);

            // No? Gather resources
            } else {

                // On a depot? Mine then
                if(me.x == mineLoc.x && me.y == mineLoc.y){
                    // Perform surveillance and send messages
                    return robo.mine();
                }

                // Not on depot. Go to depot
                Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, false, true,  mineLoc);
                return robo.move(next.x - me.x, next.y - me.y);
            }
        }
        
        // In case I get confused, sit tight
        return null;
    }
}