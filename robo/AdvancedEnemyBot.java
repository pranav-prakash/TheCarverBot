package robo;

import robocode.Robot;
import robocode.ScannedRobotEvent;


/**
 * Record the advanced state of an enemy bot.
 *
 * @author Pranav Prakash
 * @author Period - 7
 * @author Assignment - AdvancedEnemyBot
 * @author Sources
 * @version 5/12/15
 */
public class AdvancedEnemyBot extends EnemyBot {
    /**
     * Private x val
     */
    private double x;

    /**
     * Private y val
     */
    private double y;


    /**
     * Constructor initializes values
     */
    public AdvancedEnemyBot() {
        reset();
    }


    /**
     * Get x
     *
     * @return x
     */
    public double getX() {
        return x;
    }


    /**
     * Get y
     *
     * @return y
     */
    public double getY() {
        return y;
    }


    /**
     * Update x y vals
     *
     * @param e     scanned event
     * @param robot robot object
     */
    public void update(ScannedRobotEvent e, Robot robot) {
        super.update(e);
        double absBearingDeg = (robot.getHeading() + e.getBearing());
        if (absBearingDeg < 0) {
            absBearingDeg += 360;
        }

        x = robot.getX() + Math.sin(Math.toRadians(absBearingDeg))
                * e.getDistance();
        y = robot.getY() + Math.cos(Math.toRadians(absBearingDeg))
                * e.getDistance();
    }

    /**
     * Gets future x
     *
     * @param when time
     * @return future x
     */
    public double getFutureX(long when) {
        return x + Math.sin(Math.toRadians(getHeading())) * getVelocity()
                * when;

    }

    /**
     * Gets future y
     *
     * @param when time
     * @return future y
     */
    public double getFutureY(long when) {
        return y + Math.cos(Math.toRadians(getHeading())) * getVelocity()
                * when;

    }

    /**
     * Resets all
     */
    public void reset() {
        super.reset();
        x = 0;
        y = 0;
    }

}