package bc19;


public class RefData {

    //*** Troop resource requirements */
    // karbonite, fuel
    public static int[][] requirements = {
        {-1, -1},
        {50, 200},
        {10, 50},
        {15, 50},
        {25, 50},
        {30, 50}
    };

    public static int[] max_hp = new int[]{200, 100, 10, 40, 20, 60};

    public static int[] dmg = new int[]{ 10, -1, -1, 10, 10, 20};

    public static int[] vision = new int[]{ 100, 100, 100, 49, 64, 16};

    public static int[] speed = new int[]{ -1, -1, 4, 9, 4, 4};

    public static int[][] carry = new int[][]{ {-1, -1}, {-1, -1}, {20, 100}, {20, 100}, {20, 100}, {20, 100}};

    public static int[] atk_cost = new int[]{ 10, -1, -1, 10, 25, 15};
    
    public static int[] atk_range = new int[]{ 64, -1, -1, 16, 64, 16};

    public static int[] move_cost = new int[]{ -1, -1, 1, 1, 2, 3};
    
    // check if me is in visible range of other
    public boolean in_visible_range(Robot me, Robot other) {
        return (((me.x - other.x)*(me.x - other.x) + (me.y - other.y)*(me.y - other.y)) <= RefData.vision[other.unit]) ? true : false;
    }

    // check if me is in attack range of other
    public boolean in_attack_range(Robot me, Robot other) {
        return (((me.x - other.x)*(me.x - other.x) + (me.y - other.y)*(me.y - other.y)) <= RefData.atk_range[other.unit]) ? true : false;
    }
    
}