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

    // Initialization
    public Church(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.radio = robo.radio;


        // Process and store depot clusters
        resData = new ResourceManager(manager.fuel_map, manager.karbo_map);
        robo.log("Church: Map data acquired");

    }

    // Bot AI
    public Action AI() {
        this.me = robo.me;
        LinkedList<Point> fuel_depots,karb_depots = new LinkedList<>();
        boolean fuelb,created;
        ArrayList<Integer> assigned_pilgrims = new ArrayList<>();
        ArrayList<Point> assigned_depots = new ArrayList<>();
        Point nextP;
     
        if(robo.me.turn == 1){
            //initialize church

            robo.log("I am at " + Integer.toString(me.x) + "," + Integer.toString(me.y));
            fuelb = false;
            // created = false;

            for (int i = 0; i < manager.fuel_map.length; i++) {
                for (int j = 0; j < manager.fuel_map[i].length; j++) {
                    if (manager.fuel_map[i][j]) {
                        fuel_depots.add(new Point(j, i));
                    }
                    if (manager.karbo_map[i][j]) {
                        karb_depots.add(new Point(j, i));
                    }
                }
            }

            // record depot of first pilgrim
            manager.update_data();
            for(Robot r: manager.vis_robots){
                if(robo.isRadioing(r)){
                    if(r.signal%16 == 3){
                        Point m = decodes3(r.signal);
                        if(manager.karbo_map[m.y][m.x]){
                            karb_depots.remove(m);
                            assigned_pilgrims.add(r.id);
                            assigned_depots.add(m);
                        }
                        if(manager.fuel_map[m.y][m.x]){
                            fuel_depots.remove(m);
                            assigned_pilgrims.add(r.id);
                            assigned_depots.add(m);
                        }
                    }
                }
            }
        }


        //check for enemies
        manager.update_data();

        for(Robot r: manager.vis_robots){
            if(r.team != me.team){
                //enemy detected!
                robo.signal(r.unit,1);
            }
        }

        // keep track of pilgrims
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

        // produce pilgrims
        if(manager.buildable(robo.SPECS.PILGRIM)){
            Point p = manager.findEmptyAdj(me,true);
            if(p != null){
                if(fuelb){
                    nextP = fuel_depots.pollFirst();                                        
                }else{
                    nextP = karb_depots.pollFirst();
                    if(karb_depots.size()==0){
                        fuelb=true;
                    }
                }
                robo.signal(robo.radio.assignDepot(nextP),2);
                assigned_depots.add(nextP);
                // created = true;
                return robo.buildUnit(robo.SPECS.PILGRIM,p.x,p.y);
            }
        }
        return null;
    }

    public Point decodes3(int signal){
        Point p = new Point(1,1);
        p.x = signal/1024;
        p.y = (signal%1024)/16;
        return p;
    }

}