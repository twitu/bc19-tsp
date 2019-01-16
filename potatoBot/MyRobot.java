package bc19;

public class MyRobot extends BCAbstractRobot {

    ///*** Directions ***///
    // Adjacent Squares
    public static Point[] adj_directions = {
        new Point(0, 1),
        new Point(1, 1),
        new Point(1, 0),
        new Point(1, -1),
        new Point(0, -1),
        new Point(-1, -1),
        new Point(-1, 0),
        new Point(-1, 1)
    };

    // r^2 = 4
    public static Point[] four_directions = {
        new Point(0, 1),
        new Point(1, 1),
        new Point(1, 0),
        new Point(1, -1),
        new Point(0, -1),
        new Point(-1, -1),
        new Point(-1, 0),
        new Point(-1, 1),
        // extended directions
        new Point(0, 2),
        new Point(2, 0),
        new Point(0, -2),
        new Point(-2, 0)
    };

    // r^2 = 9
    public static Point[] nine_directions = {
        new Point(0, 1),
        new Point(1, 1),
        new Point(1, 0),
        new Point(1, -1),
        new Point(0, -1),
        new Point(-1, -1),
        new Point(-1, 0),
        new Point(-1, 1),
        // extended directions
        new Point(0, 2),
        new Point(2, 0),
        new Point(0, -2),
        new Point(-2, 0),
        // more extended
        new Point(0, 3),
        new Point(1, 2),
        new Point(2, 1),
        new Point(3, 0),
        new Point(2, -1),
        new Point(1, -2),
        new Point(0, -3),
        new Point(-1, -2),
        new Point(-2, -1),
        new Point(-3, 0),
        new Point(-2, 1),
        new Point(-1, 2),
        // others
        new Point(2, 2),
        new Point(2, -2),
        new Point(-2, 2),
        new Point(-2, -2)
    };

    ///*** Private Variables ***///
    Castle castle;
    Church church;
    Pilgrim pilgrim;
    Crusader crusader;
    Prophet prophet;
    Preacher preacher;
    
    ///*** Helpers ***///
    public Management manager;
    public Comms radio;

    ///*** Main Code ***///
    public Action turn() {
        
        // Initialization
        if (me.turn == 1) {
            manager = new Management(this);
            radio = new Comms(this);
            switch (me.unit) {
                case 0: castle = new Castle(this);
                        break;
                case 1: church = new Church(this);
                        break;
                case 2: pilgrim = new Pilgrim(this);
                        break;
                case 3: crusader = new Crusader(this);
                        break;
                case 4: prophet = new Prophet(this);
                        break;
                case 5: preacher = new Preacher(this);
                        break;
            }
        }

        // Select Unit type and AI
        switch (me.unit) {
            case 0:     return castle.AI();
            case 1:     return church.AI();
            case 2:     return pilgrim.AI();
            case 3:     return crusader.AI();
            case 4:     return prophet.AI();
            case 5:     return preacher.AI();
            default:    return null;
        }

    }
}