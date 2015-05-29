package robo;

import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;


/**
 * Radar interface
 * Implements Robot part interface
 *
 * @author Pranav Prakash
 * @author Period: 7
 * @author Assignment: Robo05PartsBot
 * @author Sources
 * @version May 14, 2015
 */
public interface Radar extends RobotPart
{
    /**
     * Initialize
     */
    void init();

    /**
     * Move
     */
    void move();

    /**
     * Should track
     *
     * @param e ScannedRobotEvent
     * @return is tracked
     */
    boolean shouldTrack( ScannedRobotEvent e );

    /**
     * Check whether previously tracking
     *
     * @param e RobotDeathEvent
     * @return was previously tracking
     */
    boolean wasTracking( RobotDeathEvent e );
}
