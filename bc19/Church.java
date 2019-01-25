package bc19;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays.*;
import java.util.Arrays;
import java.lang.*;

public class Church {

    // Map data
    ResourceManager resData;
    RefData refdata;
    
    // Self references
    MyRobot robo;
    Robot me;
    Management manager;
    CombatManager combat_manager;
    Comms radio;

    // Private Variables
    public static int[] emergencyFund = {50, 100};
    LinkedList<Point> fuel_depots;
    LinkedList<Point> karb_depots;
    LinkedList<Point> dead_depots = new LinkedList<>();
    boolean fuelCap, karbCap, combat,new_miner,priority;
    ArrayList<Integer> assigned_miners = new ArrayList<>();
    ArrayList<Point> assigned_depots = new ArrayList<>();
    int unit_no, status, node_no;
    ArrayList<Point> node_location;
    int shield_range;

    // Initialization
    public Church(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.combat_manager = robo.combat_manager;
        this.manager = robo.manager;
        this.radio = robo.radio;
        this.shield_range = 5;
        this.node_location = new ArrayList<>();
        this.refdata = robo.refdata;
        this.resData = robo.resData;

        // Process and store depot clusters
        robo.log("Church: Map data acquired");

        // Initialize church
        robo.log("I am at " + Integer.toString(me.x) + "," + Integer.toString(me.y));
        combat = false;
        new_miner = false;
        unit_no = 0;
        node_no = 0;
        status = 0;
        
        // Record resource point locations
        Cluster D = resData.resourceList.get(resData.getID(me.x, me.y));
        fuel_depots = new LinkedList<>(D.fuelPos);
        karb_depots = new LinkedList<>(D.karboPos);
        fuelCap = (fuel_depots.size() == 0) ? true : false;
        karbCap = (karb_depots.size() == 0) ? true : false;

        // Record depot of first pilgrim
        for(Robot r: manager.vis_robots){
            if(robo.isVisible(r) && robo.isRadioing(r)){
                if(r.signal % 16 == 3){
                    Point m = radio.decodes3(r.signal);
                    if(manager.karbo_map[m.y][m.x]){
                        for (Point p: karb_depots) {
                            if ((p.x == m.x) && (p.y == m.y)) {
                                karb_depots.remove(p);
                                break;
                            }
                        }
                        assigned_miners.add(r.id);
                        assigned_depots.add(m);
                    }
                    if(manager.fuel_map[m.y][m.x]){
                        for (Point p: fuel_depots) {
                            if ((p.x == m.x) && (p.y == m.y)) {
                                fuel_depots.remove(p);
                                break;
                            }
                        }
                        assigned_miners.add(r.id);
                        assigned_depots.add(m);
                    }
                }
            }
        }

        // Check symmetry and map to determine nodes for defense shield
        ArrayList<Point> possible_nodes = new ArrayList<>();
        if (manager.vsymmetry) { // vertical
            if (manager.map_length - me.y > me.y) { // upper half
                possible_nodes.add(new Point(0, shield_range));
                possible_nodes.add(new Point(0, shield_range));
            } else { // lower half
                possible_nodes.add(new Point(0, -shield_range));
                possible_nodes.add(new Point(0, -shield_range));
            }

            if (me.x < manager.map_length/3) { // left
                possible_nodes.add(new Point(shield_range, 0));
            } else if (me.x > (manager.map_length - manager.map_length/3)) {// right 
                possible_nodes.add(new Point(-shield_range, 0));
            } else { //center
                possible_nodes.add(new Point(shield_range, 0));
                possible_nodes.add(new Point(-shield_range, 0));
            }
        } else { // horizontal symmetry
            if (manager.map_length - me.x > me.x) { // left half
                possible_nodes.add(new Point(shield_range, 0));
                possible_nodes.add(new Point(shield_range, 0));
            } else { // right half
                possible_nodes.add(new Point(-shield_range, 0));
                possible_nodes.add(new Point(-shield_range, 0));
            }
            if (me.y < manager.map_length/3) { // upper
                possible_nodes.add(new Point(0, shield_range));
            } else if (me.y > (manager.map_length - manager.map_length/3)) {// lower
                possible_nodes.add(new Point(0, -shield_range));
            } else { // middle
                possible_nodes.add(new Point(0, shield_range));
                possible_nodes.add(new Point(0, -shield_range));
            }
        }

        for (Point p: possible_nodes) {
            if (manager.checkBounds(me.x + p.x, me.y + p.y)) {
                node_location.add(new Point(me.x + p.x, me.y + p.y));
            }
        }
    }

    // Bot AI
    public Action AI() {

        this.me = robo.me;
        manager.updateData();
        robo.castleTalk(radio.baseID(resData.getID(me.x, me.y)));

        // Check for enemies and broadcast if under attack
        boolean noCombat = true;
        for (Robot bot: manager.vis_robots){
            
            // Avoid enemy comms jamming
            if (!robo.isVisible(bot)) continue;
            
            // Enemy in sight?
            if (bot.team != me.team) {
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

        // Defense successful? Then go to normal mode but be alert. Also tell castle
        if (noCombat) {
            combat = false;
        }

        
        // TODO: Pilgrim Tacking: Keep track of pilgrims
        // Produce pilgrims
        robo.log("karbs is " + Integer.toString(karb_depots.size()) + "fuel is " + Integer.toString(fuel_depots.size()));
        int[] unit_req = RefData.requirements[robo.SPECS.PILGRIM];
        if ((emergencyFund[0] + unit_req[0]) <= robo.karbonite && (emergencyFund[1] + unit_req[1] <= robo.fuel)
         && (!fuelCap || !karbCap)) {
            Point p = manager.findEmptyAdj(me, true);
            if(p != null){
                Point depot_loc = null;
                
                // More karbonite to be mined
                if (!karbCap) {
                    depot_loc = karb_depots.pollFirst();                                        
                    if(karb_depots.isEmpty()) karbCap = true;
                } else if (!fuelCap){ // Full capacity karbonite production. Mine fuel
                    depot_loc = fuel_depots.pollFirst();
                    if(fuel_depots.isEmpty()) fuelCap = true;
                }

                if (depot_loc != null) {
                    // Send the broadcast and build the unit
                    robo.signal(radio.assignDepot(depot_loc), 2);
                    assigned_depots.add(depot_loc);
                    return robo.buildUnit(robo.SPECS.PILGRIM, p.x, p.y);
                }
            }
        }


         // check for new pilgrims and add to list
         if(new_miner){
            for(Robot r:manager.vis_robots){
                if(r.unit==robo.SPECS.PILGRIM && r.team == me.team ){
                    if(assigned_miners.contains(r.id)){ // old miner
                        continue;
                    }
                    assigned_miners.add(r.id);
                    new_miner=false;
                    break;
                }
            }
        }

        // //remove missing pilgrims from list
        // for (int i=0;i<assigned_miners.size();i++){
        //     boolean miss = true;
        //     for(Robot r : manager.vis_robots){
        //         if(r.id == assigned_miners.get(i)){
        //             miss = false;
        //             break;
        //         }
        //     }
        //     if(miss){
        //         robo.log("missing pilgrim :id- " + Integer.toString(assigned_miners.get(i)));
        //         Point p = assigned_depots.get(i);
        //         dead_depots.add(p);
        //         assigned_depots.remove(i);
        //         assigned_miners.remove(i);
        //         i--; // to compensate for the shift due to deletion
        //     }
        // }
        Integer assize = 0;

        //track militia and pilgrims
        //remove missing pilgrims or militia from list
        for(Robot r : manager.vis_robots){
            if(r.team==me.team && (r.unit==robo.SPECS.PREACHER || r.unit == robo.SPECS.PROPHET)){
                assize++;
            }
        }

        if(assize < 6){
            // Set up defences
            int unit_type = MyRobot.tiger_squad[unit_no];
            unit_req = RefData.requirements[unit_type];
            if (1==1) {
                robo.log("reached here");
                Point emptyadj = manager.findEmptyAdj(me, false);
                if(emptyadj == null){
                    return null;
                }
                unit_no = (++unit_no) % MyRobot.tiger_squad.length;

                // signal position
                Point dest = node_location.get(node_no);
                robo.log("unit destination is x " + Integer.toString(dest.x) + "y " + Integer.toString(dest.y));
                robo.signal(radio.assignGuard(dest), 2);
                node_no = (++node_no) % node_location.size();
                return robo.buildUnit(unit_type, emptyadj.x,emptyadj.y);
            }
        }

        // If confused, sit tight
        return null;
    }

}