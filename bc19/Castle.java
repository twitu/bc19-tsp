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
    Comms radio;

    // Church extended Private Variables
    public static int[] emergencyFund = {50, 200};
    LinkedList<Point> fuel_depots;
    LinkedList<Point> karb_depots;
    boolean fuelCap, karbCap, combat;
    ArrayList<Integer> assigned_pilgrims = new ArrayList<>();
    ArrayList<Point> assigned_depots = new ArrayList<>();
    Point nextP;
    int unit_no, mark;

    // Castle Variables
    ArrayList<Integer> castleClusters = new ArrayList<>();
    RefData refdata;

    // Initialization
    public Castle(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.radio = robo.radio;

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map, manager.fuel_map, manager.karbo_map);
        resData.pairClusters(me.x, me.y, manager.map_length, manager.vsymmetry);
        resData.targetAssigned(resData.getID(me.x, me.y));
        refdata = new RefData();
        robo.log("Castle: Map data acquired");

        // Initialize castle
        combat = false;
        unit_no = 0;
        mark = -1;
        manager.updateData();
        
        // Record resource point locations
        castleClusters.add(resData.getID(me.x, me.y));
        Cluster D = resData.resourceList.get(resData.getID(me.x, me.y));
        fuel_depots = new LinkedList<>(D.fuelPos);
        karb_depots = new LinkedList<>(D.karboPos);
        fuelCap = (fuel_depots.size() == 0) ? true : false;
        karbCap = (karb_depots.size() == 0) ? true : false;
    
    }

    // Bot AI
    public Action AI() {

        this.me = robo.me;
        manager.updateData();
        robo.castleTalk(radio.baseID(resData.getID(me.x, me.y)));
        
        // Check for enemies and broadcast if under attack
        boolean noCombat = true;
        for (Robot bot: manager.vis_robots){
            
            // Avoid being jammed by comms
            if (!robo.isVisible(bot)) {
                continue;
            }

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

        // Defense successful? Then go to normal mode but be alert
        if (noCombat) {
            combat = false;
        }

        // Record assigned and active sites
        for (Robot bot: manager.vis_robots) {
            int ID;
            if ((bot.castle_talk % 8 == 1) || (bot.castle_talk % 8 == 2)) {
                ID = (int) (bot.castle_talk / 8);
                resData.targetAssigned(ID);
            }
            if ((bot.castle_talk % 8 == 2) && (me.turn <= 2)) {
                ID = (int) (bot.castle_talk / 8);
                if (!castleClusters.contains(ID)) {
                    castleClusters.add(ID);
                }
            }
        }

         // Check for currently marked target
         for (Robot bot: manager.vis_robots) {
            if ((bot.id == mark) && refdata.in_attack_range(bot, me)) {
                return robo.attack(bot.x - me.x, bot.y - me.y);
            }
        }

        // Check for enemy bots and attack if enemy in range
        Robot closest = null;
        int max_dist = Integer.MAX_VALUE;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) {
                continue;
            }
            int dist = (me.x - bot.x)*(me.x - bot.x) + (me.y - bot.y)*(me.y - bot.y);
            if (bot.team != me.team && refdata.in_attack_range(bot, me) && (dist < max_dist)) {
                    max_dist = dist;
                    closest = bot;
            }
        }
        if (closest != null) {
            mark = closest.id;
            robo.signal(radio.prophetMark(mark), 4);
            return robo.attack(closest.x - me.x, closest.y - me.y);
        }

        // Produce pilgrims if not producing at full capacity
        if((manager.buildable(robo.SPECS.PILGRIM)) && (!fuelCap || !karbCap)) {
            Point p = manager.findEmptyAdj(me, true);
            if(p != null){
                
                // More karbonite to be mined
                if ((karbCap) && (!fuelCap)) {
                    nextP = fuel_depots.pollFirst();                                        
                    if(fuel_depots.size()==0){
                        fuelCap = true;
                    }

                // Full capacity karbonite production. Mine fuel
                } else if (!karbCap) {
                    nextP = karb_depots.pollFirst();
                    if(karb_depots.size()==0){
                        karbCap = true;
                    }
                }

                // Send the broadcast and build the unit
                robo.signal(radio.assignDepot(nextP), 2);
                assigned_depots.add(nextP);
                return robo.buildUnit(robo.SPECS.PILGRIM, p.x, p.y);
            }
        }

        // If enough resources available, build a tiger squad
        int unit_type = MyRobot.tiger_squad[unit_no];
        int[] unit_req = RefData.requirements[unit_type];
        if ((emergencyFund[0] + unit_req[0]) <= robo.karbonite && (emergencyFund[1] + unit_req[1] <= robo.fuel)) {
            Point emptyadj = manager.findEmptyAdj(me, false);
            unit_no = (++unit_no) % MyRobot.tiger_squad.length;
            return robo.buildUnit(unit_type, emptyadj.x,emptyadj.y);
        }

        // TODO: Eco Combat Balancing
        // If not, send a colonist pilgrim to an inactive base.
        unit_req = RefData.requirements[robo.SPECS.PILGRIM];
        if ((emergencyFund[0] + unit_req[0]) <= robo.karbonite && (emergencyFund[1] + unit_req[1] <= robo.fuel)
        && resData.targets.size() != 0) {
            int baseID = resData.nextTargetID(me.x, me.y);
            robo.signal(radio.baseAssignment(baseID, false), 2);
            robo.castleTalk(radio.baseAssigned(baseID));
            Point E = manager.findEmptyAdj(me, false);
            return robo.buildUnit(robo.SPECS.PILGRIM, E.x, E.y);
        }

        // Nothing to do
        return null;
        
    }

}