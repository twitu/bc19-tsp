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
    int lattice_radius = 7;
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
        

        // Process and store depot clusters
        resData = new ResourceManager(manager.passable_map,manager.fuel_map, manager.karbo_map);
        refdata = new RefData();
        mark = -1;
        robo.log("Prophet: Map data acquired in state " + Integer.toString(this.state));
        if (state == 3) {
            relocateMidcluster();
        }
    }


    // Relocate Mid Cluster home to nearer side
    public void relocateMidcluster() {
        Point a, b;
        Cluster home_cluster = resData.resourceList.get(resData.getID(target_loc.x, target_loc.y));
        a = new Point(home_cluster.locX, home_cluster.locY);
        b = manager.oppPoint(a.x, a.y);
        if (manager.me_location.dist(a) > manager.me_location.dist(b)) {
            home_cluster.locX = b.x;
            home_cluster.locY = b.y;
        }
    }

    // Bot AI
    public Action AI() {
        
        this.me = robo.me;        
        manager.updateData();
        robo.log("I am at " + Integer.toString(me.x) + "," + Integer.toString(me.y));

        // Listen on comms for emergency signals from castle
        if (state <= 10) {
            if (!red_alert) {
                for (Robot bot: manager.vis_robots) {
                    if (bot.signal%16 == 10) { // make more complex
                        if (attack_point.equals(home_base)) continue;
                        red_alert = true;
                        attack_point = new Point(bot.signal/1024, bot.signal%1024/16);
                        break;
                    }
                }
            } else {
                for (Robot bot: manager.vis_robots) {
                    if (bot.signal%16 == 11) {
                        Point possible_attack = new Point(bot.signal/1024, bot.signal/1024%16);
                        if (possible_attack.equals(attack_point)) {
                            state = 11;
                            red_alert = false;
                            break;
                        }
                    }
                }
            }
        }
        
        // Check for currently marked target
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) continue;
            int dist = (me.x - bot.x)*(me.x - bot.x) + (me.y - bot.y)*(me.y - bot.y);
            if ((bot.id == mark) && dist<=64 && dist >= 16) {
                return robo.attack(bot.x - me.x, bot.y - me.y);
            }
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
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) continue;
            int dist = (me.x - bot.x)*(me.x - bot.x) + (me.y - bot.y)*(me.y - bot.y);
            if ((bot.id == mark) && dist<=64 && dist >= 16) {
                return robo.attack(bot.x - me.x, bot.y - me.y);
            }
        }
        
        // Check for enemy bots and attack and mark if enemy in range
        Robot closest = null;
        int max_dist = Integer.MAX_VALUE;
        for (Robot bot: manager.vis_robots) {
            if (!robo.isVisible(bot)) continue;
            int dist = (me.x - bot.x)*(me.x - bot.x) + (me.y - bot.y)*(me.y - bot.y);
            if (bot.team != me.team && refdata.inAttackRange(bot, me) && (dist >= 16) && (dist < max_dist)) {
                    max_dist = dist;
                    closest = bot;
            }
        }
        if (closest != null) {
            mark = closest.id;
            robo.signal(radio.prophetMark(mark), 4);
            return robo.attack(closest.x - me.x, closest.y - me.y);
        }

        if(state == 1){
            // while initial moves move towards enemy castle
            if (initial_move_count > 0) {
                initial_move_count--;
                Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, true, enemy_castle);
                return robo.move(next.x - me.x, next.y - me.y);
            }
            state = 0;
        }

        if (state == 3) {
            // move towards target location
            Point next = manager.findNextStep(me.x, me.y, MyRobot.four_directions, true, target_loc);
            // robo.log("found my target loc moving there :" + Integer.toString(next.y) +", " + Integer.toString(next.x));
            if (next.equals(target_loc)) {
                state = 0;
            } else {
                return robo.move(next.x - me.x, next.y - me.y);
            }
        }

        if(state == 2){
            // find number of steps to reach guard location
            guard_loc_count = manager.numberOfMoves(manager.me_location, guard_loc, MyRobot.adj_directions);
            robo.log("guard log count :" + Integer.toString(guard_loc_count));
            state = 4;
        }

        if (state == 4) {
            // move calculated number of steps towards destination
            if (guard_loc_count > 0) {
                if (--guard_loc_count == 0) {
                    state = 0;
                }
                Point next = manager.findNextStep(me.x, me.y, MyRobot.adj_directions, true, guard_loc);
                return robo.move(next.x - me.x, next.y - me.y);
            }
        }

        if(state == 0){
            //lattice
            // determine max x or y reachable
                    //do not move below lim
                    for(Point p : MyRobot.diag_directions){
                        if(!manager.passable_map[me.y + p.y][me.x + p.x] || manager.fuel_map[me.y + p.y][me.x + p.x] || manager.karbo_map[me.y + p.y][me.x + p.x] || manager.vis_robot_map[me.y + p.y][me.x + p.x] != 0){
                            continue;
                        }
                        if( home.dist(new Point(me.x+p.x,me.y+p.y)) > lattice_radius){
                            //do not move further from lattice radius
                            continue;
                        }
                        if(((p.x + me.x - home_base.x)*(p.x + me.x - home_base.x) + (p.y + me.y - home_base.y)*(p.y + me.y - home_base.y)) > ((me.x - home_base.x)*(me.x - home_base.x) + (me.y - home_base.y)*(me.y - home_base.y))){
                            //if moving farther from home_base
                            return robo.move(p.x,p.y);
                        }
                    }
                }
        // // current swarm is hard coded to 6
        // if (state == 0) {
        //     Point next = combat_manager.findSwarmedMove(home_base);
        //     if (next != null) return robo.move(next.x - me.x, next.y - me.y);
        // }

        // recieved red alert at communication problems
        if (state == 11) {
            attack_point = manager.oppPoint(attack_point.x, attack_point.y);
            state = 12;
        }

        if (state == 12) {
            Point next_step = manager.findNextStep(me.x, me.y, MyRobot.four_directions, true, attack_point);
            return robo.move(next_step.x - me.x, next_step.y - me.y);
        }


        // Nothing to do
        return null;
    }
}