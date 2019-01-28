package bc19;

public class Prophet {

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
    int mark;
    int state;
    int lattice_radius = 36;
    Point home_castle;
    Point enemy_castle;
    Point guard_loc;
    Point home_base;
    int initial_move_count;
    int guard_loc_count;
    Point target_loc;
    Point attack_point;
    boolean red_alert;
    boolean min;
    boolean yellow, red;
    int attack_base;
    Point attack_enemy_castle;

    // Initialization
    public Prophet(MyRobot robo) {
        this.me = robo.me;

        // Store self references
        this.robo = robo;
        this.me = robo.me;
        this.combat_manager = robo.combat_manager;
        this.manager = robo.manager;
        this.radio = robo.radio;
        this.manager = robo.manager;
        this.home_base = robo.home_base;
        this.home_castle = robo.home_castle;
        this.enemy_castle = robo.enemy_castle;
        this.combat_manager = robo.combat_manager;
        this.radio = robo.radio;
        this.guard_loc = robo.guard_loc;
        this.state = robo.state;
        this.guard_loc_count = 0;
        this.target_loc = robo.target_loc;
        this.initial_move_count = robo.initial_move_count;
        this.resData = robo.resData;
        this.refdata = robo.refdata;
        this.red_alert = false;
        this.attack_point = null;
        this.yellow = false;
        this.red = false;


        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map,manager.fuel_map, manager.karbo_map);
        refdata = new RefData();
        mark = -1;
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
                state = 0;
                break;
            }
        }
        
        // Check for currently marked target
        Point marked = combat_manager.markedTarget(mark, me);
        if (marked != null) {
            return robo.attack(marked.x - me.x, marked.y - me.y);
        }

        // Marked target not in range? Listen to broadcast for nearby marks
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) continue;
            if (me.team == bot.team && bot.signal % 16 == 7) {
                mark = (((bot.signal - 7)/16) + 1);
                break;
            }
        }

        // Check for newly marked target
        marked = combat_manager.markedTarget(mark, me);
        if (marked != null) {
            return robo.attack(marked.x - me.x, marked.y - me.y);
        }

        // if lone wolf try to find company
        // else mark and attack
        int adj_team_count = manager.adjacentTeamCount();
        if (adj_team_count < 2) {
            Robot danger = combat_manager.closestEnemyToDefend(manager.me_location);
            if (danger != null) {
                // if (danger.unit == robo.SPECS.CASTLE) {
                //     // handle differently
                // }
                Point next = combat_manager.findNextSafePoint(me, danger, MyRobot.four_directions, true); // finding closest safe point
                if (next != null) {
                    return robo.move(next.x - me.x, next.y - me.y);
                }
            }
        } else {
            // Check for enemy bots and attack and mark if enemy in range
            Robot closest = combat_manager.closestEnemyToAttack(manager.me_location);
            if (closest != null) {
                mark = closest.id;
                Point strike = combat_manager.pantherStrike(3, 16, 4);
                if (strike != null) {
                    robo.signal(radio.pantherStrike(strike), 16);
                } else {
                    robo.signal(radio.prophetMark(mark), 4);
                }
                return robo.attack(closest.x - me.x, closest.y - me.y);
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

        if(state == 1){
            // while initial moves move towards enemy castle
            if (initial_move_count > 0) {
                initial_move_count--;
                Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, true, true, enemy_castle);
                return robo.move(next.x - me.x, next.y - me.y);
            }
            state = 5;
        }

        if (state == 3) {
            // move towards target location
            Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, true, true, target_loc);
            if (next.equals(target_loc)) {
                next = manager.findEmptyNextAdj(target_loc, manager.me_location, MyRobot.four_directions, true);
                state = 5;
            }

            if (next != null) {
                return robo.move(next.x - me.x, next.y - me.y);
            }
        }

        if(state == 2){
            // find number of steps to reach guard location
            guard_loc_count = manager.numberOfMoves(manager.me_location, guard_loc, MyRobot.adj_directions);
            state = 4;
        }

        if (state == 4) {
            // move calculated number of steps towards destination
            if (guard_loc_count > 0) {
                if (--guard_loc_count == 0) {
                    state = 5;
                }
                Point next = combat_manager.stepToGuardPoint(guard_loc, true, MyRobot.adj_directions);
                return robo.move(next.x - me.x, next.y - me.y);
            }
        }

        if(state == 0){

            boolean isolated = true;

            if((me.y-me.x)%2==1){
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
                if((me.x - me.y)%2==1 && me.turn%5==0){
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
        if (state == 5) {
            Point next = combat_manager.findSwarmedMove(home_base);
            if (next != null) return robo.move(next.x - me.x, next.y - me.y);
        }

        // recieved red alert at communication problems
        if (state == 11) {
            attack_point = manager.oppPoint(attack_point.x, attack_point.y);
            state = 12;
        }

        if (state == 12) {
            Point next_step = manager.findNextStep(me.x, me.y, MyRobot.four_directions, true, true, attack_point);
            return robo.move(next_step.x - me.x, next_step.y - me.y);
        }


        // Nothing to do
        return null;
    }

}