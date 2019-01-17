package bc19;


public class RefData {

    //*** Troop resource requirements */
    // karbonite, fuel
    public static int[][] requirements = {
        {-1, -1},
        {50, 200},
        {10, 50},
        {20, 50},
        {25, 50},
        {30, 50}
    };

    public static int[] max_hp = new int[]{ 100, 50, 10, 40, 20, 60};

    public static int[] dmg = new int[]{ -1, -1, -1, 10, 10, 20};

    public static int[] vision = new int[]{ 100, 100, 100, 36, 64, 16};

    public static int[] speed = new int[]{ -1, -1, 4, 9, 4, 4};

    public static int[][] carry = new int[][]{ {-1, -1}, {-1, -1}, {20, 100}, {20, 100}, {20, 100}, {20, 100}};

    
    public static int[] atk_cost = new int[]{ -1, -1, -1, 10, 25, 15};
    
    public static int[] atk_range = new int[]{ -1, -1, -1, 16, 64, 16};

    public static int[] move_cost = new int[]{ -1, -1, 1, 1, 2, 3};
    
    
    
}