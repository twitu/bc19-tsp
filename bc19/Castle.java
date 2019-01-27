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
    public static int[] emergencyFund = {75, 250};
    LinkedList<Point> fuel_depots;
    LinkedList<Point> karb_depots;
    LinkedList<Point> dead_depots = new LinkedList<>();
    boolean fuelCap, karbCap, combat, new_miner, def_ring_init, priority;
    ArrayList<Integer> assigned_miners = new ArrayList<>();
    ArrayList<Point> assigned_depots = new ArrayList<>();
    ArrayList<Point> node_location = new ArrayList<>();
    ArrayList<Point> guard_locations = new ArrayList<>();
    ArrayList<Integer> assigned_loc = new ArrayList<>();
    Point nextP;
    int unit_no, mark, state, unit_type, next_state, prev_state;
    int baseID, new_baseID;
    int[] unit_req;
    int shield_range, shield_size, shield_priority_count;
    int node_no, nitro_turn, nitro_fuel;

    // Castle Variables
    ArrayList<Integer> castleClusters = new ArrayList<>();
    ArrayList<Integer> churchClusters = new ArrayList<>();
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

        // Initialize castle
        combat = false;
        def_ring_init = true;
        unit_no = 0;
        mark = -1;
        state = 0;
        baseID = -1;
        node_no = 0;
        shield_range = 7;
        nitro_fuel = manager.nitro_fuel;
        nitro_turn = manager.nitro_turn;
        priority = manager.getDangerPriority();
        if(priority){
            robo.log("high priority");
        }
        
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
        if (!manager.vsymmetry) { // horizontal
            if (me.y < manager.map_length/4) { // upper half
                possible_nodes.add(new Point(0, shield_range));
            } else if (me.y > (manager.map_length - manager.map_length/4)) { // lower half
                possible_nodes.add(new Point(0, -shield_range));
            } else { // center
                possible_nodes.add(new Point(0, -shield_range));
                possible_nodes.add(new Point(0, shield_range));
            }
        } else { // vertical symmetry
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

    }

    // Bot AI
    public Action AI() {

        this.me = robo.me;
        manager.updateData();
        robo.castleTalk(radio.baseID(resData.getID(me.x, me.y)));
        
        // If nitro+ turns start populating
        if (me.turn >= nitro_turn && robo.fuel >= nitro_fuel) {
            prev_state = state;
            state = 6;
        } else if (state == 6) {
            state = prev_state;
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

        // Record assigned and active sites
        for (Robot bot: manager.vis_robots) {
            int ID;
            if (bot.id == me.id) continue;
            if ((bot.castle_talk % 8 == 1) || (bot.castle_talk % 8 == 2)) {
                ID = (int) (bot.castle_talk / 8);
                resData.targetAssigned(ID);
                if (state == 0) {
                    if(resData.midClusters.contains(ID)){
                        state = 1;
                    }
                }
            }
            if ((bot.castle_talk % 8 == 1) && (me.turn <= 2)) {
                ID = (int) (bot.castle_talk / 8);
                if (!castleClusters.contains(ID)) {
                    castleClusters.add(ID);
                }
            }
            if (bot.castle_talk %8 == 1 && me.turn > 2) {
                ID = (int) (bot.castle_talk / 8);
                if (!churchClusters.contains(ID)) {
                    churchClusters.add(ID);
                }
            }
        }

        // Check for currently marked target
        for (Robot bot: manager.vis_robots) {
            if(!robo.isVisible(bot)) continue;
            if ((bot.id == mark) && refdata.inAttackRange(bot, me)) {
                return robo.attack(bot.x - me.x, bot.y - me.y);
            }
        }

        // Check for enemy bots and attack if enemy in range
        Robot closest,prophet = null;
        int max_dist = Integer.MAX_VALUE;
        for (Robot bot: manager.vis_robots) {
            if(!robo.isVisible(bot)) continue;
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

        if (state < 10 && count_hostiles > 10) {
            robo.signal(radio.yellowAlert(manager.me_location),manager.map_length);
            state = 10;
        } else if (state == 10 && count_hostiles > 10) {
            robo.signal(radio.redAlert(manager.me_location),manager.map_length);
            state = 11;
        }

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

        // Defense successful? Then go to normal mode but be alert
        if (noCombat) {
            combat = false;
        }

        if(state == 0){//escort pilgrim-prophet
            if (me.turn == 1) {
                Point p = manager.findEmptyAdj(me, true);
                nextP = karb_depots.pollFirst();
                if (karb_depots.size() == 0) {
                    next_state = 2;
                } else {
                    next_state = 1;
                }
                // Send the broadcast and build the unit
                robo.signal(radio.assignDepot(nextP), 2);
                assigned_depots.add(nextP);
                new_miner = true;
                if (resData.midClusters.size() == 0){
                    state = next_state;
                }
                return robo.buildUnit(robo.SPECS.PILGRIM, p.x, p.y);
            } else {
                // Calculate if I am closest to midcluster
                if (baseID == -1) {
                    Cluster D2 = resData.resourceList.get(resData.getID(me.x, me.y));
                    this.new_baseID = resData.nextTargetID(me.x, me.y, true);
                    Cluster D1 = resData.resourceList.get(new_baseID);
                    int dist = (D1.locX - D2.locX)*(D1.locX - D2.locX) + (D1.locY - D2.locY)*(D1.locY - D2.locY);
                    for (int i: castleClusters) {
                        if (resData.getID(me.x, me.y) == i) {
                            continue;
                        }
                        D2 = resData.resourceList.get(i);
                        int other_dist = (D1.locX - D2.locX)*(D1.locX - D2.locX) + (D1.locY - D2.locY)*(D1.locY - D2.locY);
                        if (other_dist < dist){
                            state = next_state;
                            break;
                        }
                    }
                }

                if (this.baseID != -1 && state == 0){
                    robo.signal(radio.baseAssignment(this.baseID, false), 2);
                    Point E = manager.findEmptyAdj(me, true);
                    if (manager.buildable(robo.SPECS.PILGRIM)) {
                        state = next_state;
                        return robo.buildUnit(robo.SPECS.PILGRIM, E.x, E.y);
                    }
                    else {
                        return null;
                    }
                } else if (state == 0) {
                    if(manager.buildable(robo.SPECS.PROPHET)){
                        baseID = new_baseID;
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
        }
        
        if(priority){

            if(shield_priority_count > 0 ){
                shield_priority_count --;
                //count shield units
                Integer proph_count = 0;
                Integer preach_count = 0;

                for(Robot r : manager.vis_robots){
                    if(!robo.isVisible(r)) continue;
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
                    if (unit_req[0] <= robo.karbonite && unit_req[1] <= robo.fuel) {
                        Point emptyadj = manager.findEmptyAdj(me, false);
                        // unit_no = (++unit_no) % MyRobot.tiger_squad.length;
                        robo.signal(radio.stepsToEnemy(3),2);
                        return robo.buildUnit(unit_type, emptyadj.x,emptyadj.y);
                    }
            }else{
                 if(proph_count < 4){
                    unit_type = robo.SPECS.PROPHET;
                    // If enough resources available, build a prophet
                    unit_req = RefData.requirements[robo.SPECS.PREACHER];
                    if (unit_req[0] <= robo.karbonite && unit_req[1] <= robo.fuel) {
                        Point emptyadj = manager.findEmptyAdj(me, false);
                        // unit_no = (++unit_no) % MyRobot.tiger_squad.length;
                        robo.signal(radio.stepsToEnemy(3),2);
                        return robo.buildUnit(unit_type, emptyadj.x,emptyadj.y);
                    }
                }else{
                    // end priority turn
                    return null;}
            }
            }else{
                shield_priority_count = 2;
            }
        }

        if(state == 1 && manager.buildable(robo.SPECS.PILGRIM)){// mine self karb depots

            Point p = manager.findEmptyAdj(me,true);
            nextP = karb_depots.pollFirst();
            if(karb_depots.size()==0){
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
            // send a colonist pilgrim to an inactive base.
            unit_req = RefData.requirements[robo.SPECS.PILGRIM];
            if (unit_req[0] <= robo.karbonite && unit_req[1] <= robo.fuel) {
                int baseID = resData.nextTargetID(me.x, me.y,false);
                robo.signal(radio.baseAssignment(baseID, false), 2);
                robo.castleTalk(radio.baseAssigned(baseID));
                Point E = manager.findEmptyAdj(me, false);
                return robo.buildUnit(robo.SPECS.PILGRIM, E.x, E.y);
            }
        }


        if(state == 3 && manager.buildable(robo.SPECS.PILGRIM)){// mine self fuel depots
            nextP = fuel_depots.pollFirst();                                        
            if(fuel_depots.size()==0){
                fuelCap = true;
                state = 4;
            }
        
            Point p = manager.findEmptyAdj(me,true);
            // Send the broadcast and build the unit
            robo.signal(radio.assignDepot(nextP), 2);
            assigned_depots.add(nextP);
            new_miner = true;
            
            return robo.buildUnit(robo.SPECS.PILGRIM, p.x, p.y);
        }

        if ((state == 4 || state == 5 || state == 6) && dead_depots.size()!=0 ){
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

        if (state == 5) {
            if(robo.fuel <= emergencyFund[1] || robo.karbonite <= emergencyFund[0] ){
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

        if (state == 6) {

            Point emptyadj = manager.findEmptyAdj(me, false);
            if (emptyadj != null) {
                // robo.log("state 6");
                // robo.signal(radio.crusaderDummy(), 2);
                return robo.buildUnit(robo.SPECS.CRUSADER, emptyadj.x, emptyadj.y);                        
            }
        }

        // Nothing to do
        return null;
        
    }

}