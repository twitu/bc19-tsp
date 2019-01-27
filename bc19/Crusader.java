package bc19;

import java.util.ArrayList;
import java.util.HashSet;

public class Crusader {

    // Map data
    ResourceManager resData;
    
    // Self references
    MyRobot robo;
    Robot me;
    Management manager;
    CombatManager combat_manager;
    Comms radio;
    RefData refdata;

    // Private variables
    boolean panther_mode;
    Point home_castle, enemy_castle,home_base;
    Point target_loc;
    int state, initial_move_count;
    Point guard_loc, return_loc;
    int mark, guard_loc_count, fuelBuffer;
    ArrayList<Point> edge = new ArrayList<>();
    Point[] stack_directions = new Point[3];

    // Initialization
    public Crusader(MyRobot robo) {

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.manager = robo.manager;
        this.home_base = robo.home_base;
        this.home_castle = robo.home_castle;
        this.enemy_castle = robo.enemy_castle;
        this.combat_manager = robo.combat_manager;
        this.radio = robo.radio;
        this.guard_loc = robo.guard_loc;
        this.target_loc = robo.target_loc;
        this.guard_loc_count = 0;
        this.initial_move_count = robo.initial_move_count;
        this.state = robo.state;
        this.refdata = robo.refdata;
        this.resData = robo.resData;
        fuelBuffer = 2500;
        panther_mode = false;

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map, manager.fuel_map, manager.karbo_map);
        robo.log("Crusader: Initialization state " + Integer.toString(state));

        // Analyze map symmetry and get default direction
        if (manager.vsymmetry) {
            if (manager.map_length - me.y > me.y) {
                for (int i = 0; i < manager.map_length; i++) {
                    if (manager.passable_map[0][i] && !manager.karbo_map[0][i] && !manager.fuel_map[0][i]) {
                        edge.add(new Point(i, 0));
                    }                                                
                }
                
            } else {
                for (int i = 0; i < manager.map_length; i++) {
                    if (manager.passable_map[manager.map_length - 1][i] && !manager.karbo_map[manager.map_length - 1][i] && !manager.fuel_map[manager.map_length - 1][i]) {
                        edge.add(new Point(i, manager.map_length - 1));
                    }
                }
            }
        } else {
            if (manager.map_length - me.x > me.x) {
                for (int i = 0; i < manager.map_length; i++) {
                    if (manager.passable_map[i][0] && !manager.karbo_map[i][0] && !manager.fuel_map[i][0]) {
                        edge.add(new Point(0, i));
                    }
                }
            } else {
                for (int i = 0; i < manager.map_length; i++) {
                    if (manager.passable_map[i][manager.map_length - 1] && !manager.karbo_map[i][manager.map_length - 1] && !manager.fuel_map[i][manager.map_length - 1]) {
                        edge.add(new Point(manager.map_length - 1, i));
                    }
                }
            }
        }



        if(manager.vsymmetry){
            if(home_base.y>manager.map_length/2){
                    stack_directions[0] =  new Point(0, 1);
                    stack_directions[1] = new Point(1, 1);
                    stack_directions[2] = new Point(-1, 1);
                
            }else{
                    stack_directions[0] = new Point(1, -1);
                    stack_directions[1] =  new Point(0, -1);
                    stack_directions[2] = new Point(-1, -1);
            }
        }else{
            if(home_base.x > manager.map_length/2){
                    stack_directions[0] = new Point(1, 1);
                    stack_directions[1] = new Point(1, 0);
                    stack_directions[2] = new Point(1, -1);
            }else{
                    stack_directions[0] = new Point(-1, -1);
                    stack_directions[1] = new Point(-1, 0);
                    stack_directions[2] = new Point(-1, 1);
            }
        }

    }

    // Bot AI
    public Action AI() {
        
        this.me = robo.me;
        manager.updateData();
        fuelBuffer += 15;

        // variables for iterators
        Robot closest;

        // Check for enemy in attack range and attack
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot) || me.team == bot.team) continue;
            if (refdata.inAttackRange(bot, me)) return robo.attack(bot.x - me.x, bot.y - me.y);
        }

        // // Listen to broadcast for nearby marks
        // HashSet<Integer> marks = new HashSet<>();
        // for (Robot bot: manager.vis_robots) {
        //     if (!robo.isVisible(bot)) continue;
        //     if (me.team == bot.team && bot.signal % 16 == 7) {
        //         mark = (((bot.signal - 7) /16) + 1);
        //         marks.add(mark);
        //     }
        // }

        // // Check visible range for marked target
        // closest = null;
        // for (Robot bot: manager.vis_robots) {
        //     if (!robo.isVisible(bot)) continue;
        //     int max_dist = Integer.MAX_VALUE;
        //     if (marks.contains(bot.id)) {
        //         int dist = (bot.x - me.x)*(bot.x - me.x) + (bot.y - me.y)*(bot.y - me.y);
        //         if (dist < max_dist) {
        //             max_dist = dist;
        //             closest = bot;
        //         }
        //     }
        // }
        
        // if (closest != null) {
        //     Point next = manager.findNextStep(me.x, me.y, MyRobot.nine_directions, false, true, new Point(closest.x, closest.y));
        //     return robo.move(next.x - me.x, next.y - me.y);
        // }

        // Check for enemies in range that me has to defend and escape
        closest = null;
        int max_dist = Integer.MAX_VALUE;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot) || me.team == bot.team) continue;
            if (refdata.inAttackRange(me, bot)) {
                int dist = (bot.x - me.x)*(bot.x - me.x) + (bot.y - me.y)*(bot.y - me.y);
                if (dist < max_dist) {
                    max_dist = dist;
                    closest = bot;
                }
            }
        }

        // Check for Panther Alert
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot) || me.team != bot.team || !robo.isRadioing(bot)) continue;
            if (bot.signal % 16 == 12) {
                guard_loc = radio.decodePantherStrike(bot.signal);
                panther_mode = true;
                return_loc = manager.me_location;
                state = 2;
            }
        }

        if(state == 1) {
            // while initial moves move towards enemy castle
            if (initial_move_count > 0) {
                initial_move_count--;
                Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, true, true, enemy_castle);
                return robo.move(next.x - me.x, next.y - me.y);
            }
            state = 0;
        }

        if (state == 3) {
            // move towards target location
            Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, true, true, target_loc);
            return robo.move(next.x - me.x, next.y - me.y);
        }

        if (state == 2) {
            // find number of steps to reach guard location
            robo.log("guard location is x" + Integer.toString(guard_loc.x) + " y " + Integer.toString(guard_loc.y));
            guard_loc_count = manager.numberOfMoves(manager.me_location, guard_loc, MyRobot.adj_directions);
            state = 4;
        }

        if (state == 4) {
            // move calculated number of steps towards destination
            if (guard_loc_count > 0) {
                if (--guard_loc_count == 0) {
                    if (panther_mode) {
                        guard_loc = return_loc;
                        panther_mode = false;
                        state = 2;
                    } else {
                        state = 0;
                    }
                }
                Point next = combat_manager.stepToGuardPoint(guard_loc, true, MyRobot.adj_directions);
                robo.log("Next move to guard point x" + Integer.toString(next.x) + " y " + Integer.toString(next.y));
                return robo.move(next.x - me.x, next.y - me.y);
            }
        }

        // current swarm is hard coded to 6
        if (state == 0) {


            for(Point p: stack_directions){
                Point next = p.add(manager.me_location);
                if(manager.getRobotIdMap(next.x, next.y) == 0){
                    return robo.move(next.x - me.x, next.y - me.y);
                }
            }

            boolean isolated = true;
            Integer move = 0;
            if(manager.vsymmetry){
                if(manager.getRobotIdMap(me.x +1, me.y) > 0){
                    // robo.log("isolated1 : " + Integer.toString(me.x)  + " " + Integer.toString(me.y));
                    move = 1;       
                    isolated = false;                               
                }else if(manager.getRobotIdMap(me.x -1, me.y) > 0){
                    isolated = false;
                    move = 2;
                    // robo.log("isolated2 : " + Integer.toString(me.x)  + " " + Integer.toString(me.y));
                }
            }else{
                if(manager.getRobotIdMap(me.x, me.y + 1) > 0){
                    isolated = false;
                    move =3;
                    // robo.log("isolated3 : " + Integer.toString(me.x)  + " " + Integer.toString(me.y));
                }else if(manager.getRobotIdMap(me.x, me.y-1 ) > 0){
                    isolated = false;
                    move =4;
                    // robo.log("isolated4 : " + Integer.toString(me.x)  + " " + Integer.toString(me.y));
                }
            }

            if(!isolated) {
                //if not isolated can move
                //lattice
                // determine max x or y reachable
                //do not move below lim
                if(manager.vsymmetry){
                    if(manager.getRobotIdMap(me.x +1, me.y) == 0 && !manager.fuel_map[me.y][me.x +1] && !manager.karbo_map[me.y][me.x+1]){
                        return robo.move(1,0);
                    }
                    if(manager.getRobotIdMap(me.x -1, me.y) == 0 && manager.vis_robot_map[me.y][me.x -1] == 0 && !manager.fuel_map[me.y][me.x  -1] && !manager.karbo_map[me.y][me.x -1]){
                        return robo.move(-1,0);
                    }
                    

                }else{
                    if(manager.getRobotIdMap(me.x, me.y  +1) == 0 && manager.vis_robot_map[me.y + 1][me.x] == 0 && !manager.fuel_map[me.y + 1][me.x  -1] && !manager.karbo_map[me.y + 1][me.x] ){
                        return robo.move(0,1);
                    }
                    if(manager.getRobotIdMap(me.x, me.y  -1) == 0  && manager.vis_robot_map[me.y  -1][me.x] == 0 && !manager.fuel_map[me.y  -1][me.x  -1] && !manager.karbo_map[me.y  -1][me.x]){
                        return robo.move(0,-1);
                    }
                }
            }

            return null;
            
        }
        
        // populate the map
        if (state == 5) {
            
            // In castle range. Run away
            boolean run = false;
            for (Robot bot: manager.vis_robots) {
                if (robo.isVisible(bot) && bot.unit <= 1)
                {
                    run = true;
                }
            }

            // Not in castle range? Go to hell if you have fuel
            // if in castle range, run anyway.
            int count = 0;
            boolean strike = false;
            for (Point p: MyRobot.adj_directions) {
                if (!manager.checkBounds(me.x + p.x, me.y + p.y) || !manager.passable_map[me.y + p.y][me.x + p.x]
                    || manager.vis_robot_map[me.y + p.y][me.x + p.x] > 0) {
                    count++;
                    strike = true;
                } else if (strike) {
                    break;
                }
            }
            if (count >= 4) {
                return null;
            } else {
                ArrayList<Point> edges = new ArrayList<>(edge);
                Point next = manager.findNextStep(me.x, me.y, MyRobot.adj_directions, true, true, edges);
                if ((robo.fuel > fuelBuffer || run) && next != null) {
                    return robo.move(next.x - me.x, next.y - me.y);            
                }
            }
        }

        // Chain give resources if nothing else to do
        if (me.karbonite > 5 || me.fuel > 20) {
            Point next = manager.chainGive();
            if (next!=null) {
                return robo.give(next.x, next.y, me.karbonite, me.fuel);
            }
        }

        // current swarm is hard coded to 6
        Point next = combat_manager.findSwarmedMove(home_base);
        if (next != null) return robo.move(next.x - me.x, next.y - me.y);

        return null;
    }

}