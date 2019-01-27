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
    boolean fuelCap, karbCap, combat,new_miner, danger_priority;
    ArrayList<Integer> assigned_miners = new ArrayList<>();
    ArrayList<Point> assigned_depots = new ArrayList<>();
    int unit_no, state, node_no,ch_state;
    ArrayList<Point> node_location;
    int shield_range, shield_size, shield_priority_count;
    Point nextP;
    
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
        this.shield_size = 6;
        this.shield_priority_count = 2;
        this.danger_priority = manager.getDangerPriority();
        if(danger_priority){
            robo.log("high priority");
        }
        // Process and store depot clusters
        robo.log("Church: Map data acquired");

        // Initialize church
        combat = false;
        new_miner = false;
        unit_no = 0;
        node_no = 0;
        state = 0;
        ch_state =1;

        // Record resource point locations
        Cluster D = resData.resourceList.get(resData.getID(me.x, me.y));
        fuel_depots = new LinkedList<>(D.fuelPos);
        karb_depots = new LinkedList<>(D.karboPos);
        fuelCap = (fuel_depots.size() == 0) ? true : false;
        karbCap = (karb_depots.size() == 0) ? true : false;

        // Record depot of first pilgrim
        for(Robot r: manager.vis_robots){
            if(robo.isVisible(r)){
                if(robo.isRadioing(r)){
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
                } else if(r.unit == robo.SPECS.PILGRIM) {
                    if(manager.karbo_map[r.y][r.x]){
                        Point p = new Point(r.x,r.y);
                        assigned_miners.add(r.id);
                        assigned_depots.add(p);
                        karb_depots.remove(p);
                        continue;
                    }
                    
                    if(manager.fuel_map[r.y][r.x]){
                        Point p = new Point(r.x,r.y);
                        assigned_miners.add(r.id);
                        assigned_depots.add(p);
                        fuel_depots.remove(p);
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
            Point next = p.add(manager.me_location);
            if (!manager.checkBounds(next.x, next.y)) continue;
            if (!manager.passable_map[next.y][next.x]) {

            }
            node_location.add(new Point(me.x + p.x, me.y + p.y));
        }
    }

    // Bot AI
    public Action AI() {

        this.me = robo.me;
        manager.updateData();
        robo.castleTalk(radio.baseID(resData.getID(me.x, me.y)));
        boolean low_defense = false;

        
        // check for new pilgrims and add to list
        if(new_miner){
            for(Robot r:manager.vis_robots){
                if(!robo.isVisible(r)) continue;
                if(r.id == me.id) continue;
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

        //remove missing pilgrims  from list
        for (int i=0;i<assigned_miners.size();i++){
            boolean miss = true;
            for(Robot r : manager.vis_robots){
                if(!robo.isVisible(r)) continue;
                if(r.id == assigned_miners.get(i)){
                    miss = false;
                    break;
                }
            }
            if(miss){
                Point p = assigned_depots.get(i);
                dead_depots.add(p);
                assigned_depots.remove(i);
                assigned_miners.remove(i);
                i--; // to compensate for the shift due to deletion
            }
        }

        // if (danger_priority) {
        //     int defence_count = 0;
        //     for(Robot r : manager.vis_robots){
        //         if (!robo.isVisible(r)) continue;
        //         if(r.team==me.team && (r.unit==robo.SPECS.PREACHER || r.unit == robo.SPECS.PROPHET || r.unit == robo.SPECS.CRUSADER)){
        //             defence_count++;
        //         }
        //     }

        //     if (defence_count < shield_size) {
        //         // robo.log("low on defense with shield units " + Integer.toString(defence_count));
        //         low_defense = true;
        //     }

        //     if (low_defense && state != shield_priority_count) {
        //         state++;
        //         // make shield units until state is not in shield priority count
        //         int unit_type = MyRobot.tiger_squad[unit_no];
        //         Point emptyadj = manager.findEmptyAdj(me, false);
        //         if(emptyadj == null){
        //             return null;
        //         }
        //         unit_no = (++unit_no) % MyRobot.tiger_squad.length;

        //         // signal position
        //         Point dest = node_location.get(node_no);
        //         robo.signal(radio.assignGuard(dest), 2);
        //         node_no = (++node_no) % node_location.size();
        //         return robo.buildUnit(unit_type, emptyadj.x, emptyadj.y);
        //     }

        //     // reset state if one round of shield building is complete
        //     if (state == shield_priority_count && low_defense) {
        //         state = 0;
        //     }
        // } else {
        //     // low priority produce units only if threatened
        //     ArrayList<Robot> enemies = combat_manager.visibleEnemies(me);
        //     ArrayList<Robot> allies = combat_manager.visibleAllyList(me);
        //     int enemy_count = enemies.size();
        //     int enemy_preachers = combat_manager.countVisibleUnit(robo.SPECS.PREACHER, enemies);
        //     int preacher_count = combat_manager.countVisibleUnit(robo.SPECS.PREACHER, allies);
        //     int pilgrim_count = combat_manager.countVisibleUnit(robo.SPECS.PILGRIM, enemies);
        //     int crusaders = combat_manager.countVisibleUnit(robo.SPECS.CRUSADER, allies);
        //     int prophets = combat_manager.countVisibleUnit(robo.SPECS.PROPHET, allies);
        //     int unit = -1;
        //     Point next = null;
        //     if (enemies.size() > 0) {
        //         Robot danger = combat_manager.closestEnemy(manager.me_location, enemies);
        //         next = manager.findClosestEmptyAdjacent(new Point(danger.x, danger.y), MyRobot.adj_directions, false);
        //         if (enemy_preachers > 0) {
        //             if (preacher_count < enemy_preachers) { 
        //                 unit = robo.SPECS.PREACHER;
        //             }
        //         } else {
        //             if (pilgrim_count == enemy_count) {
        //                 if (crusaders == 0) {
        //                     unit = robo.SPECS.CRUSADER;
        //                 } else {
        //                     if (crusaders + prophets < (enemy_count + 1)/2) {
        //                         if (crusaders < 2) {
        //                             unit = robo.SPECS.CRUSADER;
        //                         } else {
        //                             unit = robo.SPECS.PROPHET;
        //                         }
        //                     }
        //                 }
        //             }
        //         }

        //         if (next != null) {
        //             robo.signal(radio.pantherStrike(new Point(danger.x, danger.y)), 4);
        //             return robo.buildUnit(unit, next.x - me.x, next.y - me.y);
        //         }
        //     }
        // }

        if(ch_state == 1 && manager.buildable(robo.SPECS.PILGRIM)){// mine self karb depots

            Point p = manager.findEmptyAdj(me,true);
            nextP = karb_depots.pollFirst();
            if(karb_depots.size()==0){
                ch_state=2;
            }
        
            // Send the broadcast and build the unit
            robo.signal(radio.assignDepot(nextP), 2);
            assigned_depots.add(nextP);
            new_miner = true;
            return robo.buildUnit(robo.SPECS.PILGRIM, p.x, p.y);
        }


        if(ch_state == 2 && manager.buildable(robo.SPECS.PILGRIM)){// mine self fuel depots
            nextP = fuel_depots.pollFirst();                                        
            if(fuel_depots.size()==0){
                ch_state = 3;
            }
        
            Point p = manager.findEmptyAdj(me,true);
            // Send the broadcast and build the unit
            robo.signal(radio.assignDepot(nextP), 2);
            assigned_depots.add(nextP);
            new_miner = true;
            
            return robo.buildUnit(robo.SPECS.PILGRIM, p.x, p.y);
        }

        if ((ch_state == 3 || ch_state == 5 || ch_state == 6) && dead_depots.size()!=0 ){
            //reallocate dead depots                                   
            if(manager.buildable(robo.SPECS.PILGRIM)){
                nextP = dead_depots.pollFirst();
                if(manager.vis_robot_map[nextP.y][nextP.x] != 0){
                    Robot r =  robo.getRobot(manager.vis_robot_map[nextP.y][nextP.x] );
                    if(r.team == me.team && r.unit == robo.SPECS.PILGRIM){
                        assigned_depots.add(nextP);
                        assigned_miners.add(r.id);
                    }else{
                        Point p = manager.findEmptyAdj(me,false);
                        // Send the broadcast and build the unit
                        robo.signal(radio.assignDepot(nextP), 2);
                        assigned_depots.add(nextP);
                        new_miner = true;
                        return robo.buildUnit(robo.SPECS.PILGRIM, p.x, p.y);
                    }
                }else{
                        Point p = manager.findEmptyAdj(me,false);
                        // Send the broadcast and build the unit
                        robo.signal(radio.assignDepot(nextP), 2);
                        assigned_depots.add(nextP);
                        new_miner = true;
                        return robo.buildUnit(robo.SPECS.PILGRIM, p.x, p.y);
                }
            }
        }


        //Prophet Lattice
        if (ch_state == 5) {
            if((robo.fuel > emergencyFund[1] && robo.karbonite > emergencyFund[0] ) || danger_priority){
                return null;
            }
            Integer proph_count = 0;
            for(Robot r:manager.vis_robots){
                if(!robo.isVisible(r)) continue;
                if(r.team == me.team && r.unit == robo.SPECS.PROPHET){
                    proph_count ++;
                }
            }

            if(proph_count < 25){
                Point emptyadj;
                for(Point p:MyRobot.adj_directions){
                    if( ((p.y + me.y) - (p.x + me.x)) %2 == 0 ){//even chequered
                        if(manager.getRobotIdMap(me.x+p.x,me.y+p.y) !=0 || manager.fuel_map[me.y + p.y][me.x + p.x] || manager.karbo_map[me.y + p.y][me.x + p.x]){
                            continue;
                        }
                        emptyadj = p;
                        break;
                    }
                }
                if(emptyadj != null && manager.buildable(robo.SPECS.PROPHET)){
                    return robo.buildUnit(robo.SPECS.PROPHET,emptyadj.x,emptyadj.y);
                }
            }
            
        }

        //Health rush        
        if (ch_state == 6) {

            Point emptyadj = manager.findEmptyAdj(me, false);
            if (emptyadj != null) {
                // robo.log("ch_state 6");
                // robo.signal(radio.crusaderDummy(), 2);
                return robo.buildUnit(robo.SPECS.CRUSADER, emptyadj.x, emptyadj.y);                        
            }
        }
        // If confused, sit tight
        return null;
    }
}