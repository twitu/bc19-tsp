package bc19;

import java.util.LinkedList;

public class CombatManager {

    RefData refdata;

    public CombatManager() {
        this.refdata = new RefData();
    }

    public Point preacherRetreat(Robot[] visRobots, MyRobot robo) {
        Point retreat = null;
        int high_score = 0;
        Robot dangerous = null;
        for (Robot bot: visRobots) {
            int score = 0;
            if (refdata.in_attack_range(robo.me, bot)) {
                score += 100;
            }

            if (bot.unit == robo.SPECS.PREACHER) {
                score += 100;
            } else if (bot.unit == robo.SPECS.PROPHET) {
                score += 80;
            } else if (bot.unit == robo.SPECS.CRUSADER) {
                score += 50;
            }

            if (score > high_score) {
                high_score = score;
                dangerous = bot;
            }
        }

        if (dangerous != null) {
            Point dest = new Point(dangerous.x, dangerous.y);
            Point next = robo.manager.findNextStep(robo.me.x, robo.me.y, robo.manager.passable_map, true, dest);
            retreat = new Point(-next.x, -next.y);
        }

        return retreat;
    }

    public Robot preacherChooseBestEnemy(Robot[] visRobots, MyRobot robo) {
        int high_score = 0;
        Robot choice = null;
        for (Robot bot: visRobots) {
            int score = 0;
            // checks if me can attack bot
            if (refdata.in_attack_range(bot, robo.me)) {
                score += 100;
            }
            if (bot.health == RefData.dmg[robo.me.unit]) {
                score += 100;
            }
            if (bot.unit == robo.SPECS.PREACHER) {
                score += 100;
            } else if (bot.unit == robo.SPECS.PROPHET) {
                score += 50;
            } else if (bot.unit == robo.SPECS.CRUSADER) {
                score += 30;
            }

            if (score > high_score) {
                high_score = score;
                choice = bot;
            }
        }

        return choice;
    }
}