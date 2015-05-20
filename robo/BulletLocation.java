package robo;

import robocode.HitByBulletEvent;


/**
 * Record the state of an enemy bot.
 *
 * @author Pranav Prakash
 * @author Period - 7
 * @author Assignment - EnemyBot
 * @author Sources
 * @version 5/8/15
 */
public class BulletLocation
{
    private double bearing;

    private double power;

    private double heading;

    private double velocity;

    private String name;


    /**
     * Constructor for enemy bot
     */
    public BulletLocation()
    {
        reset();
    }


    /**
     * Gets bearing
     *
     * @return bearing
     */
    public double getBearing()
    {
        return bearing;
    }


    /**
     * Gets energy
     *
     * @return energy
     */
    public double getPower()
    {
        return power;
    }


    /**
     * Gets heading
     *
     * @return heading
     */
    public double getHeading()
    {
        return heading;
    }


    /**
     * Gets velocity
     *
     * @return velocity
     */
    public double getVelocity()
    {
        return velocity;
    }


    /**
     * Gets name
     *
     * @return name
     */
    public String getName()
    {
        return name;
    }


    /**
     * Updates values
     *
     * @param srEvt event
     */
    public void update( HitByBulletEvent srEvt )
    {
        bearing = srEvt.getBearing();
        power = srEvt.getPower();
        heading = srEvt.getHeading();
        velocity = srEvt.getVelocity();
        name = srEvt.getName();
    }


    /**
     * Resets all
     */
    public void reset()
    {
        name = "";
        bearing = 0;
        power = 0;
        heading = 0;
        velocity = 0.0;
    }


    /**
     * Checks if cleared
     *
     * @return Whether cleared
     */
    public boolean none()
    {
        return name.length() == 0;
    }
}