package bc19;

import java.util.ArrayList;

public class Preacher {

    // Map data
    ResourceManager resData;
    
    // Self references
    MyRobot robo;
    Robot me;
    Management manager;
    CombatManager combat_manager;
    Comms radio;

    // Private Variables
    RefData refdata;
    int state;
    int initial_move_count;
    Point home_castle, enemy_castle,home_base;
    Point guard_loc, target_loc, return_loc,attack_enemy_castle;
    int guard_loc_count,attack_base;
    boolean panther_mode;
    int lattice_radius = 36;
    boolean yellow, red;


    // Initialization
    public Preacher(MyRobot robo) {
        
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
        this.yellow = false;
        this.red = false;

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map, manager.fuel_map, manager.karbo_map);
        refdata = new RefData();
        panther_mode = false;
        lattice_radius = 25;

    }

    // Bot AI
    public Action AI() {
        this.me = robo.me;
        manager.updateData();     

        // listen for yellow
        if (!yellow) {
            for (Robot bot: manager.vis_robots) {
                if (!robo.isRadioing(bot)) continue;
                int id = radio.decodeYellowAlert(bot.signal);
                if (id > 0 && id-64 < 50) {
                    yellow = true;
                    attack_base = id - 64;
                    break;
                }
            }
        } else {
            for (Robot bot: manager.vis_robots) {
                if (!robo.isRadioing(bot)) continue;
                int id = radio.decodeRedAlert(bot.signal);
                if (id > 0 && id-64 < 50 && attack_base == id - 64) {
                    red = true;
                    Cluster D = resData.resourceList.get(attack_base);
                    attack_enemy_castle = manager.oppPoint(D.locX, D.locY);
                    guard_loc_count = 0;
                    break;
                }
            }
        }

        // listen for lattice
        for (Robot bot: manager.vis_robots) {
            if (bot.signal == 110) {
                state = 5;
                break;
            }
        }   

        // Get enemy list in range, identify target and attack if valid
        Point target = combat_manager.preacherTarget();
        if (target != null) {
            return robo.attack(target.x - me.x, target.y - me.y);            
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

        // mount attack on enemy castle
        if (red) {
            if (guard_loc_count == 0) {
                Point next = manager.findNextStep(me.x, me.y, MyRobot.adj_directions, false, true, attack_enemy_castle);
                return robo.move(next.x - me.x, next.y - me.y);
            }
            return null;
        }

        // move initial number of moves
        if(state == 1){
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
            if (next.equals(target_loc)) {
                state = 0;
            }
            return robo.move(next.x - me.x, next.y - me.y);
        }

        if(state == 2){
            // find number of steps to reach guard location
            guard_loc_count = manager.numberOfMoves(manager.me_location, guard_loc, MyRobot.four_directions);
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
                Point next = combat_manager.stepToGuardPoint(guard_loc, true, MyRobot.four_directions);
                return robo.move(next.x - me.x, next.y - me.y);
            }
        }

        if(state == 5) {

            boolean isolated = true;

            if((me.y-me.x)%2==0){
                //snap if adjacent else maintain checkered anfd move otut
                for(Point p : MyRobot.non_diag_directions){
                    if(!manager.checkBounds(me.x+p.x,me.y+p.y)) continue;
                    if(manager.passable_map[me.y + p.y][me.x + p.x] && !manager.fuel_map[me.y + p.y][me.x + p.x] && !manager.karbo_map[me.y + p.y][me.x + p.x] && manager.vis_robot_map[me.y + p.y][me.x + p.x] == 0){
                        //if empty tile
                        return robo.move(p.x,p.y);
                    }
                }
            }


            for(Point p : MyRobot.diag_directions){
                if(manager.vis_robot_map[me.y + p.y][me.x + p.x] != 0){
                    isolated = false;
                    break;
                }
            }

            if(!isolated) {
                //if not isolated can move
                Point prev = new Point(home_base.x,home_base.y);
                //lattice
                // determine max x or y reachable
                //do not move below lim
                if((me.x - me.y)%2==0){
                    for(Point p : MyRobot.diag_directions){
                        if(!manager.checkBounds(me.x+p.x,me.y+p.y)) continue;
                        if(!manager.passable_map[me.y + p.y][me.x + p.x] || manager.fuel_map[me.y + p.y][me.x + p.x] || manager.karbo_map[me.y + p.y][me.x + p.x] || manager.vis_robot_map[me.y + p.y][me.x + p.x] != 0){
                            continue;
                        }
                        if( home_base.dist(new Point(me.x+p.x,me.y+p.y)) > lattice_radius){
                            //do not move further from lattice radius
                            continue;
                        }
                        if( home_base.dist(new Point(me.x+p.x,me.y+p.y)) > home_base.dist(new Point(me.x,me.y)) ){
                            //if moving towards enemy
                            return robo.move(p.x,p.y);
                        }
                    }
                }

                for(Point p : MyRobot.diag_directions){
                    if(!manager.checkBounds(me.x+p.x,me.y+p.y)) continue;
                    if(prev.equals(new Point(p.x + me.x , p.y + me.y) ) ) {
                        prev = home_base;
                        continue;
                    }
                    if(!manager.passable_map[me.y + p.y][me.x + p.x] || manager.fuel_map[me.y + p.y][me.x + p.x] || manager.karbo_map[me.y + p.y][me.x + p.x] || manager.vis_robot_map[me.y + p.y][me.x + p.x] != 0){
                        continue;
                    }
                    if( home_base.dist(new Point(me.x+p.x,me.y+p.y)) > lattice_radius){
                        //do not move further from lattice radius
                        continue;
                    }if( combat_manager.towardsEnemy( me.x,me.y,me.x+p.x,me.y+p.y) ){
                        //if moving towards enemy
                        return robo.move(p.x,p.y);
                    }
                }
                if(me.turn%10 == 0){
                    for(Point p : MyRobot.diag_directions){
                        if(!manager.checkBounds(me.x+p.x,me.y+p.y)) continue;
                        if(!manager.passable_map[me.y + p.y][me.x + p.x] || manager.fuel_map[me.y + p.y][me.x + p.x] || manager.karbo_map[me.y + p.y][me.x + p.x] || manager.vis_robot_map[me.y + p.y][me.x + p.x] != 0){
                            continue;
                        }
                        if((home_base.dist(new Point(me.x+p.x,me.y+p.y)) > lattice_radius) ){
                            //do not move further from lattice radius
                            continue;
                        }
                        if(manager.vsymmetry){
                            if(home_base.y > manager.map_length/2){
                                if((p.y + me.y) > home_base.y){
                                    continue;
                                }
                            }
                        }
                        prev = manager.me_location;
                        //spread at turn 3
                        return robo.move(p.x,p.y);
                    }
                }
            }


            if(me.karbonite != 0 || me.karbonite != 0){
                for(Point p : MyRobot.diag_directions){
                    if(!manager.checkBounds(me.x+p.x,me.y+p.y)) continue;
                    if(manager.vis_robot_map[me.y + p.y][me.x + p.x] != 0){
                        if( home_base.dist(manager.me_location) > home_base.dist(new Point(me.x+p.x,me.y+p.y)) ){
                            return robo.give(p.x,p.y,me.karbonite,me.fuel);
                        }
                    }
                }

            }
            
        }

        // current swarm is hard coded to 6
        if (state == 0) {
            Point next;
            // if nothing else to do pass resources to home base
            if (me.karbonite > 5 || me.fuel > 20) {
                next = manager.chainGive();
                if (next!=null) {
                    return robo.give(next.x, next.y, me.karbonite, me.fuel);
                }
            }
            next = combat_manager.findSwarmedMove(home_base);
            if (next != null) return robo.move(next.x - me.x, next.y - me.y);
        }

        // If confused sit tight
        return null;
    }
}