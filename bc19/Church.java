package bc19;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays.*;
import java.util.Arrays;
import java.lang.*;

public class Church {

    // Map data
    ResourceManager resData;
    
    // Self references
    MyRobot robo;
    Robot me;
    Management manager;
    Comms radio;

    // Private Variables
    LinkedList<Point> fuel_depots;
    LinkedList<Point> karb_depots;
    boolean fuelCap, karbCap, combat;
    ArrayList<Integer> assigned_pilgrims = new ArrayList<>();
    ArrayList<Point> assigned_depots = new ArrayList<>();
    Point nextP;

    // Initialization
    public Church(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.radio = robo.radio;


        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map,manager.fuel_map, manager.karbo_map);
        robo.log("Church: Map data acquired");

        // Initialize church
        robo.log("I am at " + Integer.toString(me.x) + "," + Integer.toString(me.y));
        combat = false;
        manager.updateData();
        
        // Record resource point locations
        Cluster D = resData.resourceList.get(resData.getID(me.x, me.y));
        fuel_depots = new LinkedList<>(D.fuelPos);
        karb_depots = new LinkedList<>(D.karboPos);
        fuelCap = (fuel_depots.size() == 0) ? true : false;
        karbCap = (karb_depots.size() == 0) ? true : false;

        // Record depot of first pilgrim
        for(Robot r: manager.vis_robots){
            if(robo.isRadioing(r)){
                if(r.signal % 16 == 3){
                    Point m = decodes3(r.signal);
                    if(manager.karbo_map[m.y][m.x]){
                        for (Point p: karb_depots) {
                            if ((p.x == m.x) && (p.y == m.y)) {
                                karb_depots.remove(p);
                                break;
                            }
                        }
                        assigned_pilgrims.add(r.id);
                        assigned_depots.add(m);
                    }
                    if(manager.fuel_map[m.y][m.x]){
                        for (Point p: fuel_depots) {
                            if ((p.x == m.x) && (p.y == m.y)) {
                                fuel_depots.remove(p);
                                break;
                            }
                        }
                        assigned_pilgrims.add(r.id);
                        assigned_depots.add(m);
                    }
                }
            }
        }

    }

    // Bot AI
    public Action AI() {
        
        this.me = robo.me;
        manager.updateData();

        // Check for enemies and broadcast if under attack
        boolean noCombat = true;
        for (Robot bot: manager.vis_robots){
            
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
            robo.castleTalk(1);
        } else {
            robo.castleTalk(2);
        }

        // TODO: Pilgrim Tacking: Keep track of pilgrims
        // if(created){
        //     for(Robot r: manager.vis_robots){
        //         if(r.signal_radius < 3 && r.unit == robo.SPECS.PILGRIM){
        //             if(robo.isRadioing(r)){
        //                 if(r.signal == robo.radio.assignDepot(nextP)){
        //                     assigned_pilgrims.add(r.id);
        //                     assigned_depots.add(nextP);
        //                     created = false;
        //                     break;
        //                 }
        //             }
        //         }                                
        //     }            
        // }

        // Produce pilgrims
        if(manager.buildable(robo.SPECS.PILGRIM)) {
            Point p = manager.findEmptyAdj(me, true);
            if(p != null){
                
                // More karbonite to be mined
                if ((karbCap) && (!fuelCap)) {
                    nextP = fuel_depots.pollFirst();                                        
                    if(fuel_depots.size()==0){
                        fuelCap = true;
                    }

                // Full capacity karbonite production. Mine fuel
                } else if (!karbCap){
                    nextP = karb_depots.pollFirst();
                    if(karb_depots.size()==0){
                        karbCap = true;
                    }

                // Base producing at max capacity. Sit tight
                } else {
                    return null;
                }

                // Send the broadcast and build the unit
                robo.signal(robo.radio.assignDepot(nextP), 2);
                assigned_depots.add(nextP);
                return robo.buildUnit(robo.SPECS.PILGRIM, p.x, p.y);
            }
        }

        // If confused, sit tight
        return null;
    }

    // Decode first location
    public Point decodes3(int signal){
        Point p = new Point(-1, -1);
        p.x = signal/1024;
        p.y = (signal % 1024)/16;
        return p;
    }

}