package bc19;

import java.util.ArrayList;
import java.util.LinkedList;

public class Castle {

    // Map data
    ResourceManager resData;
    
    // Self references
    MyRobot robo;
    Robot me;
    Management manager;
    CombatManager combat_manager;
    Comms radio;

    // Church extended Private Variables
    public static int[] emergencyFund = {50, 200};
    LinkedList<Point> fuel_depots;
    LinkedList<Point> karb_depots;
    LinkedList<Point> dead_depots = new LinkedList<>();
    boolean fuelCap, karbCap, combat, new_miner,new_atk,priority;
    ArrayList<Integer> assigned_miners = new ArrayList<>();
    ArrayList<Point> assigned_depots = new ArrayList<>();
    ArrayList<Point> node_location = new ArrayList<>();
    ArrayList<Integer> assigned_militia = new ArrayList<>();
    Point nextP;
    int unit_no, mark,state,unit_type;
    int baseID;
    int[] unit_req;
    int shield_range;
    int node_no;

    // Castle Variables
    ArrayList<Integer> castleClusters = new ArrayList<>();
    RefData refdata;

    // Initialization
    public Castle(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.combat_manager = robo.combat_manager;
        this.manager = robo.manager;
        this.radio = robo.radio;
        this.refdata = robo.refdata;
        this.resData = robo.resData;

        // Process and store depot clusters
        resData.targetAssigned(resData.getID(me.x, me.y));
        robo.log("Castle: Map data acquired");                       

        // Initialize castle
        new_atk = false;
        combat = false;
        unit_no = 0;
        mark = -1;
        state = 0;
        baseID = -1;
        node_no = 0;
        shield_range = 5;
        
        // Record resource point locations
        castleClusters.add(resData.getID(me.x, me.y));
        Cluster D = resData.resourceList.get(resData.getID(me.x, me.y));
        fuel_depots = new LinkedList<>(D.fuelPos);
        karb_depots = new LinkedList<>(D.karboPos);
        fuelCap = true;//fuel off by default
        // fuelCap = (fuel_depots.size() == 0) ? true : false;
        karbCap = (karb_depots.size() == 0) ? true : false;

        // Check symmetry and map to determine nodes for defense shield
        ArrayList<Point> possible_nodes = new ArrayList<>();
        if (manager.vsymmetry) { // vertical
            if (me.y < manager.map_length/4) { // upper half
                possible_nodes.add(new Point(0, shield_range));
            } else if (me.y > (manager.map_length - manager.map_length/4)) { // lower half
                possible_nodes.add(new Point(0, -shield_range));
            } else { // center
                possible_nodes.add(new Point(0, -shield_range));
                possible_nodes.add(new Point(0, shield_range));
            }
        } else { // horizontal symmetry
            if (me.x < manager.map_length/4) { // left
                possible_nodes.add(new Point(shield_range, 0));
            } else if (me.x > (manager.map_length - manager.map_length/4)) {// right
                possible_nodes.add(new Point(-shield_range, 0));
            } else { // middle
                possible_nodes.add(new Point(shield_range, 0));
                possible_nodes.add(new Point(-shield_range, 0));
            }
        }

        for (Point p: possible_nodes) {
            if (manager.checkBounds(me.x + p.x, me.y + p.y)) {
                node_location.add(new Point(me.x + p.x, me.y + p.y));
            }
        }

        //calculate distance from mid and set priority
        if(manager.vsymmetry){
            if(me.y > manager.map_length/2){
                if(me.y > (3*manager.map_length/4)){
                    priority = false;
                }else{
                    priority = true;
                }
            }else{
                if(me.y < manager.map_length/4){
                    priority = false;
                }else{
                    priority = true;
                }

            }
        }else{
            if(me.x > manager.map_length/2){
                if(me.x > (3*manager.map_length/4)){
                    priority = false;
                }else{
                    priority = true;
                }
            }else{
                if(me.x < manager.map_length/4){
                    priority = false;
                }else{
                    priority = true;
                }

            }
        }


    }

    // Bot AI
    public Action AI() {

        this.me = robo.me;
        manager.updateData();
        robo.castleTalk(radio.baseID(resData.getID(me.x, me.y)));
        
        // If 600+ turns start populating
        if (me.turn >= 500) {
            state = 5;
        }

        // Check for enemies and broadcast if under attack
        boolean noCombat = true;
        int count_hostiles = 0;
        for (Robot bot: manager.vis_robots){
            
            // Avoid being jammed by comms
            if (!robo.isVisible(bot)) {
                continue;
            }

            // Enemy in sight?
            if (bot.team != me.team) {
                count_hostiles++;
                noCombat = false;
                if (!combat) {
                    combat = true;
                    robo.signal(radio.emergency(new Point(bot.x, bot.y)), Cluster.range);
                }
                break;
            }

            // Emergency broadcast from nearby unit?
            if ((bot.team == me.team) && (bot.signal % 16 == 5)) {
                Point enemy_loc = new Point(-1, -1);
                enemy_loc.x = bot.signal/1024;
                enemy_loc.y = (bot.signal % 1024)/16;
                noCombat = false;
                if (!combat) {
                    combat = true;
                    robo.signal(radio.emergency(new Point(bot.x, bot.y)), Cluster.range);
                }
                break;
            }
        }

        if (state < 10 && count_hostiles > 10) {
            robo.signal(radio.yellowAlert(manager.me_location),manager.map_length);
            state = 10;
        } else if (state == 10 && count_hostiles > 10) {
            robo.signal(radio.redAlert(manager.me_location),manager.map_length);
            state = 11;
        }

        // check for new pilgrims or militia and add to list
        if(new_miner){
            for(Robot r:manager.vis_robots){
                if(r.unit==robo.SPECS.PILGRIM && r.team == me.team ){
                    if(assigned_miners.contains(r.id)){ // old miner
                        continue;
                    }
                    assigned_miners.add(r.id);
                    // robo.log("adding miner id:" + Integer.toString(r.id));
                    new_miner=false;
                    break;
                }
            }
        }
        // if(new_atk){
        //     for(Robot r:manager.vis_robots){
        //             if(r.unit==robo.SPECS.PILGRIM && r.team == me.team ){
        //                 if(assigned_militia.contains(r.id)){ // old milit
        //                     continue;
        //                 }
        //                 assigned_militia.add(r.id);
        //                 robo.log("adding militia id:" + Integer.toString(r.id));
        //                 robo.log("adding militia size:" + Integer.toString(assigned_militia.size()));
        //                 new_atk=false;
        //                 break;
        //             }
        //     }  
        // }

        //remove missing pilgrims  from list
        for (int i=0;i<assigned_miners.size();i++){
            boolean miss = true;
            for(Robot r : manager.vis_robots){
                if(r.id == assigned_miners.get(i)){
                    miss = false;
                    break;
                }
            }
            if(miss){
                // robo.log("missing pilgrim :id- " + Integer.toString(assigned_miners.get(i)));
                Point p = assigned_depots.get(i);
                dead_depots.add(p);
                assigned_depots.remove(i);
                assigned_miners.remove(i);
                i--; // to compensate for the shift due to deletion
            }
        }

        // Defense successful? Then go to normal mode but be alert
        if (noCombat) {
            combat = false;
        }

        // Record assigned and active sites
        for (Robot bot: manager.vis_robots) {
            int ID;
            if (bot.id == me.id) continue;
            if ((bot.castle_talk % 8 == 1) || (bot.castle_talk % 8 == 2)) {
                ID = (int) (bot.castle_talk / 8);
                resData.targetAssigned(ID);
            }
            if ((bot.castle_talk % 8 == 2) && (me.turn <= 2)) {
                ID = (int) (bot.castle_talk / 8);
                if(state ==0){
                    if(resData.midClusters.contains(ID)){
                        state = 1;
                    }
                }
                if (!castleClusters.contains(ID)) {
                    castleClusters.add(ID);
                }
            }
        }

        // Check for currently marked target
        for (Robot bot: manager.vis_robots) {
            if ((bot.id == mark) && refdata.inAttackRange(bot, me)) {
                return robo.attack(bot.x - me.x, bot.y - me.y);
            }
        }

        // Check for enemy bots and attack if enemy in range
        Robot closest,prophet = null;
        int max_dist = Integer.MAX_VALUE;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) {
                continue;
            }
            int dist = (me.x - bot.x)*(me.x - bot.x) + (me.y - bot.y)*(me.y - bot.y);
            if (dist < max_dist) {
                max_dist = dist;
                if(bot.team != me.team && refdata.inAttackRange(bot, me)){
                closest = bot;
                } 
                if((bot.team == me.team) && bot.unit == robo.SPECS.PROPHET){
                    prophet = bot;
                }
            }
        }
        if (closest != null) {
            mark = closest.id;
            if(prophet == null){
                Point p = manager.findEmptyAdj(me,false);
                robo.signal(radio.prophetMark(mark), 3);
                return robo.buildUnit(robo.SPECS.PROPHET,p.x,p.y);
            }
            robo.signal(radio.prophetMark(mark), prophet.signal_radius);
            return robo.attack(closest.x - me.x, closest.y - me.y);
        }

        if(state == 0){//escort pilgrim-prophet
            if(this.baseID != -1){
                robo.signal(radio.baseAssignment(this.baseID, false), 2);
                Point E = manager.findEmptyAdj(me, false);
                state = 1;
                return robo.buildUnit(robo.SPECS.PILGRIM, E.x, E.y);
            }else if(resData.midClusters.size()==0){
                state =1;
            }else{
                if(manager.buildable(robo.SPECS.PROPHET)){
                    this.baseID = resData.nextTargetID(me.x, me.y,true);
                    Point a = resData.getLocation(this.baseID);
                    if(manager.vsymmetry){
                        if(me.y > manager.map_length/2){
                            a.y--;
                        }else{
                            if(me.x > manager.map_length/2){
                                a.x--;
                            }
                        }
                    }
                    robo.signal(radio.targetLocation(a.x,a.y), 2);
                    robo.castleTalk(radio.baseAssigned(this.baseID));
                    Point E = manager.findEmptyAdj(me, false);        
                    return robo.buildUnit(robo.SPECS.PROPHET, E.x, E.y);
                }
            }
        }


        if(state == 1 && manager.buildable(robo.SPECS.PILGRIM)){// mine self karb depots
            robo.log("state :1");

            Point p = manager.findEmptyAdj(me,false);
            nextP = karb_depots.pollFirst();
            if(karb_depots.size()==0){
                karbCap = true;
                state=2;
            }
        
            // Send the broadcast and build the unit
            robo.signal(radio.assignDepot(nextP), 2);
            assigned_depots.add(nextP);
            new_miner = true;
            return robo.buildUnit(robo.SPECS.PILGRIM, p.x, p.y);
        }

        //turn on fuel mining after sending for inactive bases
        if(resData.targets.size()==0 && state == 2){
            fuelCap = (fuel_depots.size() == 0) ? true : false;
            state = 3;
        }


        if(state == 2 ){
            robo.log("state :2");
            // send a colonist pilgrim to an inactive base.
            unit_req = RefData.requirements[robo.SPECS.PILGRIM];
            if ((emergencyFund[0] + unit_req[0]) <= robo.karbonite && (emergencyFund[1] + unit_req[1] <= robo.fuel)) {
                int baseID = resData.nextTargetID(me.x, me.y,false);
                robo.signal(radio.baseAssignment(baseID, false), 2);
                robo.castleTalk(radio.baseAssigned(baseID));
                Point E = manager.findEmptyAdj(me, false);
                return robo.buildUnit(robo.SPECS.PILGRIM, E.x, E.y);
            }
        }


        if(state == 3 && manager.buildable(robo.SPECS.PILGRIM)){// mine self fuel depots
            robo.log("state :3");
            nextP = fuel_depots.pollFirst();                                        
            if(fuel_depots.size()==0){
                fuelCap = true;
                state = 4;
            }
        
            Point p = manager.findEmptyAdj(me,false);
            // Send the broadcast and build the unit
            robo.signal(radio.assignDepot(nextP), 2);
            assigned_depots.add(nextP);
            robo.log("state : 3 here");
            new_miner = true;
            
            return robo.buildUnit(robo.SPECS.PILGRIM, p.x, p.y);
        }

        if (state == 4){
            robo.log("state 4");
            //reallocate dead depots                                   
            if(dead_depots.size()!=0 && manager.buildable(robo.SPECS.PILGRIM)){
                nextP = dead_depots.pollFirst();
                // robo.log("state 4 reached here1");
                Point p = manager.findEmptyAdj(me,false);
                // Send the broadcast and build the unit
                robo.signal(radio.assignDepot(nextP), 2);
                assigned_depots.add(nextP);
                new_miner = true;
                // robo.log("state 4 reached here2");
                return robo.buildUnit(robo.SPECS.PILGRIM, p.x, p.y);
            }

            

            Integer proph_count = 0;
            Integer preach_count = 0;

            //remove missing pilgrims or militia from list
                for(Robot r : manager.vis_robots){
                    if(r.team==me.team){
                        if(r.unit==robo.SPECS.PREACHER){
                            preach_count++;
                        }if( r.unit == robo.SPECS.PROPHET){
                            proph_count++;
                        }
                    }
                }   

                if(preach_count < 2){
                    unit_type = robo.SPECS.PREACHER;
                    // If enough resources available, build a prophet
                    unit_req = RefData.requirements[unit_type];
                    if(priority){
                        if (unit_req[0] <= robo.karbonite && unit_req[1] <= robo.fuel) {
                            // robo.log("adding militia size :" + Integer.toString(assigned_militia.size()));
                            Point emptyadj = manager.findEmptyAdj(me, false);
                            // unit_no = (++unit_no) % MyRobot.tiger_squad.length;
                            robo.signal(radio.stepsToEnemy(3),2);
                            // robo.log("building preacher");
                            return robo.buildUnit(unit_type, emptyadj.x,emptyadj.y);
                        }else{
                            proph_count = 5; //prevent building prophets                            
                        }
                    }else{
                        if ((emergencyFund[0] + unit_req[0]) <= robo.karbonite && (emergencyFund[1] + unit_req[1] <= robo.fuel)) {
                            // robo.log("adding militia size :" + Integer.toString(assigned_militia.size()));
                            Point emptyadj = manager.findEmptyAdj(me, false);
                            // unit_no = (++unit_no) % MyRobot.tiger_squad.length;
                            robo.signal(radio.stepsToEnemy(3),2);
                            // robo.log("building preacher");
                            return robo.buildUnit(unit_type, emptyadj.x,emptyadj.y);
                        }else{
                            proph_count = 5; //prevent building prophets                            
                        }
                    }
                }else{
                     if(proph_count < 4){
                    unit_type = robo.SPECS.PROPHET;
                    // If enough resources available, build a prophet
                    unit_req = RefData.requirements[robo.SPECS.PREACHER];
                    if(priority){
                        if (unit_req[0] <= robo.karbonite && unit_req[1] <= robo.fuel) {
                            // robo.log("adding militia size :" + Integer.toString(assigned_militia.size()));
                            Point emptyadj = manager.findEmptyAdj(me, false);
                            // unit_no = (++unit_no) % MyRobot.tiger_squad.length;
                            robo.signal(radio.stepsToEnemy(3),2);
                            // robo.log("building prophet");
                            return robo.buildUnit(unit_type, emptyadj.x,emptyadj.y);
                        }
                    }else{
                        if ((emergencyFund[0] + unit_req[0]) <= robo.karbonite && (emergencyFund[1] + unit_req[1] <= robo.fuel)) {
                            // robo.log("adding militia size :" + Integer.toString(assigned_militia.size()));
                            Point emptyadj = manager.findEmptyAdj(me, false);
                            // unit_no = (++unit_no) % MyRobot.tiger_squad.length;
                            robo.signal(radio.stepsToEnemy(3),2);
                            // robo.log("building prophet");
                            return robo.buildUnit(unit_type, emptyadj.x,emptyadj.y);
                        }
                    }
                }
            }
                        


            int a = 0;
            int b = 0;
            if(a!=b){
                if(priority){
                    unit_type = robo.SPECS.PROPHET;
                    unit_req = RefData.requirements[robo.SPECS.PREACHER];
                    if ((emergencyFund[0] + unit_req[0]) <= robo.karbonite && (emergencyFund[1] + unit_req[1] <= robo.fuel)) {
                        // robo.log("adding militia size :" + Integer.toString(assigned_militia.size()));
                        Point emptyadj = manager.findEmptyAdj(me, false);
                        // unit_no = (++unit_no) % MyRobot.tiger_squad.length;
                        robo.signal(radio.stepsToEnemy(3),2);
                        return robo.buildUnit(unit_type, emptyadj.x,emptyadj.y);
                    }
                }

            }

            // // If enough resources available, build a tiger squad
            // int unit_type = MyRobot.tiger_squad[unit_no];
            // unit_req = RefData.requirements[unit_type];
            // if ((emergencyFund[0] + unit_req[0]) <= robo.karbonite && (emergencyFund[1] + unit_req[1] <= robo.fuel)) {
            //     Point emptyadj = manager.findEmptyAdj(me, false);
            //     unit_no = (++unit_no) % MyRobot.tiger_squad.length;
            //     robo.signal(radio.stepsToEnemy(3),2);
            //     if(manager.buildable(unit_type)){
            //         new_atk =true;
            //     }
            //     return robo.buildUnit(unit_type, emptyadj.x,emptyadj.y);
            // }
        }

        if (state == 5) {
            Point emptyadj = manager.findEmptyAdj(me, false);
            if (emptyadj == null) {
                return null;
            }
            Point dest = node_location.get(node_no);
            robo.signal(radio.assignGuard(dest), 2);
            node_no = (++node_no) % node_location.size();
            return robo.buildUnit(robo.SPECS.CRUSADER, emptyadj.x, emptyadj.y);
        }

        // Nothing to do
        return null;
        
    }

}