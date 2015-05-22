package robo;

import robocode.*;

import java.awt.geom.Point2D;
import java.util.Random;


/**
 * A modular bot adhering to the RoboPart Interface.
 *
 * @author Pranav Prakash
 * @author Period - 7
 * @author Assignment - PartsBot
 * @author Sources
 * @version 5/14/15
 */
public class TheCarver extends AdvancedRobot
{
    private AdvancedEnemyBot enemy = new AdvancedEnemyBot();

    private BulletLocation bul = new BulletLocation();

    private RobotPart[] parts = new RobotPart[3]; // make three parts

    private final static int RADAR = 0;

    private final static int GUN = 1;

    private final static int TANK = 2;

    double scanWidth = -1;

    private final int ONEONONE_THRESHHOLD = 1;

    static int moveNum = 0; // number of movement to use

    static int bestMove = 1; // tells if the best movement has been found (1 = no, 0 = yes)

    static double bestMove1 = 0; // tells how effective movement 1 was

    static double bestMove2 = 0; // tells how effective movement 2 was

    static int win1 = 0; // movement 1 win counter

    int backDir = 1;

    int moveNeg = 1; // change direction (for movement)

    double randHit = 1; // random number for movement


    /**
     * computes the absolute bearing between two points
     *
     * @param x1 point1x
     * @param y1 point1y
     * @param x2 point2x
     * @param y2 point2y
     * @return absolute bearing
     */
    public double absoluteBearing( double x1, double y1, double x2, double y2 )
    {
        double xo = x2 - x1;
        double yo = y2 - y1;
        double hyp = Point2D.distance( x1, y1, x2, y2 );
        double arcSin = Math.toDegrees( Math.asin( xo / hyp ) );
        double bearing = 0;

        if ( xo > 0 && yo > 0 )
        { // both pos: lower-Left
            bearing = arcSin;
        }
        else if ( xo < 0 && yo > 0 )
        { // x neg, y pos: lower-right
            bearing = 360 + arcSin; // arcsin is negative here, actually 360 -
            // ang
        }
        else if ( xo > 0 && yo < 0 )
        { // x pos, y neg: upper-left
            bearing = 180 - arcSin;
        }
        else if ( xo < 0 && yo < 0 )
        { // both neg: upper-right
            bearing = 180 - arcSin; // arcsin is negative here, actually 180 +
            // ang
        }

        return bearing;
    }


    /**
     * Normalizes normalizes a bearing to between +180 and -180
     *
     * @param angle angle to normalize
     * @return normalized
     */
    public double normalizeBearing( double angle )
    {
        while ( angle > 180 )
        {
            angle -= 360;
        }
        while ( angle < -180 )
        {
            angle += 360;
        }
        return angle;
    }


    /**
     * Runs the bot
     */
    public void run()
    {
        System.out.println( ( moveNum < 105 ) ? "Oscillate" : "StopGo" );
        if ( getOthers() > ONEONONE_THRESHHOLD ) // If melee
        {
            System.out.println( "Melee Mode" );
            parts[RADAR] = new RadarMelee();
            parts[GUN] = new Gun();
            parts[TANK] = new TankMelee();
        }
        else
        // If one on one
        {
            System.out.println( "1v1 Mode" );
            parts[RADAR] = new Radar1v1();
            parts[GUN] = new Gun();
            parts[TANK] = new Tank1v1();
        }

        // initialize each part
        for ( int i = 0; i < parts.length; i++ )
        {
            // behold, the magic of polymorphism
            parts[i].init();
        }

        // iterate through each part, moving them as we go
        for ( int i = 0; true; i = ( i + 1 ) % parts.length )
        {
            // polymorphism galore!
            parts[i].move();
            if ( i == 0 )
            {
                execute();
            }
        }
    }


    /**
     * On robot scan
     *
     * @param e scanevent
     */
    public void onScannedRobot( ScannedRobotEvent e )
    {
        Radar radar = (Radar)parts[RADAR];
        if ( radar.shouldTrack( e ) || e.getName().equals( bul.getName() ) )
        {
            if ( e.getName().equals( bul.getName() ) )
                bul.reset();
            enemy.update( e, this );
        }
    }


    public void onHitByBullet( HitByBulletEvent event )
    {

        if ( getEnergy() > 30 )
        {
            if ( scanWidth != -1 )
                scanWidth += 5;
            bul.update( event );
        }

        moveNeg *= -1; // change direction
        randHit *= ( Math.random() + 5 )
                        * moveNeg; // generate new random number if hit

        System.out.println( "Ouch. Speed:" + bul.getVelocity() );

    }


    public void onHitRobot( HitRobotEvent e )
    {
        double turnGunAmt = normalizeBearing(
                        e.getBearing() + getHeading() - getGunHeading() );
        turnGunRight( turnGunAmt );
        fire( 3 );
    }


    public void onHitWall( HitWallEvent event )
    {
        // if we hit a wall reverse direction
        randHit *= ( Math.random() + 5 ) * moveNeg;
        backDir *= -1;
    }


    // If movement is unsuccessful, switch to a new movement.
    public void onDeath( DeathEvent event )
    { // if we die
        if ( win1 < 3 )
        { // we'll assume if we win 3 before we lose 3, the current movement is fine
            moveNum += ( 35 * bestMove ); // add a little bit to movement number
        }
        if ( moveNum < 110 )
        { // and if we are using movement 1,
            bestMove1 = bestMove1
                            - enemy.getEnergy(); // record how much energy the enemy had left
        }
        else
        { // and if we are using movement 2,
            bestMove2 = bestMove2
                            - enemy.getEnergy(); // record how much energy the enemy had left
        }
    }


    public void onWin( WinEvent event )
    { // if we win,
        if ( moveNum < 1 )
        { // and are using movement 1,
            bestMove1 = bestMove1
                            + enemy.getEnergy(); // record how much energy we had left
            win1 += 1; // add a win to the movement 1 counter
        }
        else
        { // and if we are using movement 2,
            bestMove2 = bestMove2
                            + enemy.getEnergy(); // record how much energy we had left
        }
    }


    /**
     * On death
     *
     * @param e deathevent
     */
    public void onRobotDeath( RobotDeathEvent e )
    {
        Radar radar = (Radar)parts[RADAR];

        if ( getOthers() != 0
                        && getOthers() <= ONEONONE_THRESHHOLD ) // If one on one
        {
            System.out.println( "Switched to 1v1 mode" );
            parts[RADAR] = new Radar1v1();
            parts[GUN] = new Gun();
            parts[TANK] = new Tank1v1();

            // initialize each part
            for ( int i = 0; i < parts.length; i++ )
            {
                // behold, the magic of polymorphism
                parts[i].init();
            }

        }

        if ( e.getName().equals( bul.getName() ) )
        {
            bul.reset();
        }

        if ( radar.wasTracking( e ) )
        {
            enemy.reset();
        }
    }


    /**
     * Melee Radar
     *
     * @author Pranav Prakash
     * @author Period: 7
     * @author Assignment: Robo05PartsBot
     * @author Sources
     * @version May 14, 2015
     */
    public class RadarMelee implements Radar
    {

        /**
         * Initialize
         */
        public void init()
        {
            setAdjustRadarForGunTurn( true );
            setAdjustRadarForRobotTurn( true );
            setTurnRadarRight( 3600 );
            scanWidth = 100;
        }


        /**
         * move
         */
        public void move()
        {
            if ( !bul.none() && !enemy.getName().equals( bul.getName() ) )
            {
                // Absolute angle towards target
                double absoluteBearing = getHeading() + bul.getBearing();

                // Subtract current radar heading to get the turn required to
                // face the enemy, be sure it is normalized
                double radarTurn = normalizeBearing(
                                absoluteBearing - getRadarHeading() );

                //Turn radar
                setTurnRadarRight( radarTurn );
            }
            else if ( enemy.none() )
            {
                // If no enemy currently found, keep spinning
                setTurnRadarRight( 3600 );
                scanWidth = 100;
            }
            else
            {

                if ( getGunHeat() > 2 )
                {
                    scanWidth = Math.min( scanWidth + 1, 10 );
                }
                else if ( scanWidth > 4 )
                {
                    scanWidth--;
                }

                // Absolute angle towards target
                double absoluteBearing = getHeading() + enemy.getBearing();

                // Subtract current radar heading to get the turn required to
                // face the enemy, be sure it is normalized
                double radarTurn = normalizeBearing(
                                absoluteBearing - getRadarHeading() );

                // Distance we want to scan from middle of enemy to either side
                // scanWidth is how many units from the center of the enemy
                // robot it scans.
                double extraScan = Math.min( scanWidth + Math.atan(
                                                scanWidth / enemy.getDistance()
                                                                * 180 / 3.14 ),
                                50 );

                // Adjust the radar turn so it goes that much further in the
                // direction it is going to turn
                // Basically if we were going to turn it left, turn it even more
                // left, if right, turn more right.
                // This allows us to overshoot our enemy so that we get a good
                // sweep that will not slip.
                radarTurn += radarTurn >= 0 ? extraScan : -extraScan;

                //Turn radar
                setTurnRadarRight( radarTurn );

            }
        }


        /**
         * Should track
         *
         * @param e robotevent
         * @return is tracked
         */
        public boolean shouldTrack( ScannedRobotEvent e )
        {
            // track if we have no enemy, the one we found is significantly
            // closer, or we scanned the one we've been tracking.
            return ( ( enemy.none() || e.getDistance() < enemy.getDistance()
                            || ( e.getEnergy() < 40
                            && e.getDistance() < enemy.getDistance() * 2 )
                            || e.getName().equals( enemy.getName() ) ) );
        }


        /**
         * Check whether previously tracking
         *
         * @param e event
         * @return was previously tracking
         */
        public boolean wasTracking( RobotDeathEvent e )
        {
            return e.getName().equals( enemy.getName() );
        }
    }


    public class Radar1v1 implements Radar
    {
        /**
         * Initialize
         */
        public void init()
        {
            setAdjustRadarForGunTurn( true );
            setAdjustRadarForRobotTurn( true );
            setTurnRadarRight( 3600 );
        }


        /**
         * move
         */
        public void move()
        {
            double scanWidth = 1.0;

            if ( enemy.none() )
            {
                //If no enemy found, keep spinning
                setTurnRadarRight( 3600 );
            }
            else
            {
                // Absolute angle towards target
                double absoluteBearing = getHeading() + enemy.getBearing();

                // Subtract current radar heading to get the turn required to
                // face the enemy, be sure it is normalized
                double radarTurn = normalizeBearing(
                                absoluteBearing - getRadarHeading() );

                // Distance we want to scan from middle of enemy to either side
                // scanWidth is how many units from the center of the enemy
                // robot it scans.
                double extraScan = Math.min( Math.atan(
                                                scanWidth / enemy.getDistance()
                                                                * 180 / 3.14 ),
                                45 );

                // Adjust the radar turn so it goes that much further in the
                // direction it is going to turn
                // Basically if we were going to turn it left, turn it even more
                // left, if right, turn more right.
                // This allows us to overshoot our enemy so that we get a good
                // sweep that will not slip.
                radarTurn += radarTurn >= 0 ? extraScan : -extraScan;

                //Turn radar
                setTurnRadarRight( radarTurn );

            }
        }


        /**
         * Should track
         *
         * @param e robotevent
         * @return is tracked
         */
        public boolean shouldTrack( ScannedRobotEvent e )
        {
            // track if we have no enemy, the one we found is significantly
            // closer, or we scanned the one we've been tracking.
            return ( enemy.none() || e.getDistance() < enemy.getDistance() - 70
                            || e.getName().equals( enemy.getName() ) );
        }


        /**
         * Check whether previously tracking
         *
         * @param e event
         * @return was previously tracking
         */
        public boolean wasTracking( RobotDeathEvent e )
        {
            return e.getName().equals( enemy.getName() );
        }
    }


    /**
     * Robot gun
     *
     * @author Pranav Prakash
     * @author Period: 7
     * @author Assignment: Robo05PartsBot
     * @author Sources
     * @version May 14, 2015
     */
    public class Gun implements RobotPart
    {
        /**
         * Initialize
         */
        public void init()
        {
            // divorce gun movement from tank movement
            setAdjustGunForRobotTurn( true );
        }


        /**
         * Move
         */
        public void move()
        {
            // don't shoot if I've got no enemy
            if ( enemy.none() )
            {
                return;
            }

            // calculate firepower based on distance
            double firePower = Math.min( 500 / enemy.getDistance(), 3 );
            // calculate speed of bullet
            double bulletSpeed = 20 - firePower * 3;
            // distance = rate * time, solved for time
            long time = (long)( enemy.getDistance() / bulletSpeed );

            // calculate gun turn to predicted x,y location
            double futureX = enemy.getFutureX( time );
            double futureY = enemy.getFutureY( time );

            double absDeg = absoluteBearing( getX(), getY(), futureX, futureY );
            // non-predictive firing can be done like this:
            // double absDeg = absoluteBearing(getX(), getY(), enemy.getX(),
            // enemy.getY());

            // turn the gun to the predicted x,y location
            setTurnGunRight( normalizeBearing( absDeg - getGunHeading() ) );

            // if the gun is cool and we're pointed in the right direction,
            // shoot!
            if ( getGunHeat() == 0 && Math.abs( getGunTurnRemaining() ) < 10 )
            {
                setFire( firePower );
            }
        }
    }


    /**
     * Robot tank
     *
     * @author Pranav Prakash
     * @author Period: 7
     * @author Assignment: Robo05PartsBot
     * @author Sources
     * @version May 14, 2015
     */
    public class TankMelee implements Tank
    {
        private byte moveDirection = 1;

        Random rand;

        private final int TURN_FACTOR = 14;

        private final int AHEAD_FACTOR = 1020;

        private final int DELAY_FACTOR = 20;


        /**
         * Initialize
         */
        public void init()
        {
            rand = new Random();
        }


        /**
         * Move
         */
        public void move()
        {

            setTurnRight( normalizeBearing( enemy.getBearing() + 90 - (
                            ( rand.nextInt( TURN_FACTOR ) + 1 )
                                            * moveDirection ) ) );

            if ( getTime() % ( rand.nextInt( DELAY_FACTOR ) + 1 ) == 0 )
            {
                moveDirection *= -1;
                setAhead( rand.nextInt( AHEAD_FACTOR ) * moveDirection );
            }
        }
    }


    public class Tank1v1 implements Tank
    {
        double moveAmnt = ( ( getX() + getY() ) / 2 + 700 )
                        / 2; // movement number

        double wallDist = Math.min( Math.min( getX(), getY() ),
                        Math.min( getBattleFieldWidth() - getX(),
                                        getBattleFieldHeight()
                                                        - getY() ) ); // distance from nearest wall

        int startStop; // records whether to go or stop


        /**
         * Initialize
         */
        public void init()
        {
        }


        public void chooseBestMovement()
        {
            // Determining Most Effective Movement
            if ( moveNum >= 210 )
            { // if we have died 3 times with both movements,
                bestMove = 0; // make it so the movement type won't change
                if ( bestMove2 > bestMove1 )
                { // else if movement 2 performed the best,
                    moveNum = 150; // use movement 2
                }
                else
                { // else if movement 1 performed the best,
                    moveNum = 0; // use movement 1
                }
            }
        }


        public void oscillate()
        {
            // Oscillator
            // If it is still first movement

            setTurnRightRadians( Math.cos( enemy.getBearing() * 3.141592
                            / 180 ) ); // "square off" against the enemy

            setTurnRight( enemy.getBearing() - 90.0D );
            if ( getDistanceRemaining() == 0.0D )
            {
                double ahead = ( Math.random() - 0.5D ) * enemy.getDistance()
                                * 1.2D;
                setAhead( ahead );
            }

            if ( enemy.getPreviousEnergy() > ( enemy.getEnergy() ) )
            { // if the enemy fires
                setMaxVelocity( 15 * Math.random()
                                + 5 ); // make the velocity randomized, but no less than 5
                setAhead( ( Math.random() * moveAmnt - ( moveAmnt * .5 ) )
                                * randHit ); // go ahead (or behind) a random amount
            }
            if ( wallDist < 20 )
            { // if we get close to a wall
                setBack( 150 * backDir ); // go back a little bit
                moveNeg *= -1; // when we continue, go the other way
            }

        }


        public void stopAndGo()
        {
            // Stop & Go
            // Second type of movement

            setTurnRightRadians( Math.cos( enemy.getBearing() * 3.141592
                            / 180 ) ); // "square off" against the enemy
            setAhead( 25 * moveNeg * startStop ); // move a bit
            if ( enemy.getPreviousEnergy() > ( enemy.getEnergy() ) )
            { // if the enemy fires
                if ( startStop == 0 )
                { // and we arn't moving,
                    startStop = 1; // move.
                }
                else
                { // but if we are moving,
                    startStop = 0; // stop.
                }
            }
            if ( wallDist < 23 )
            { // if we get close to a wall
                setBack( 100 * backDir ); // go back a little bit
                moveNeg *= -1; // when we continue, go the other way
            }

        }


        /**
         * Move inspired by SuperCrazy
         */
        public void move()
        {
            chooseBestMovement();
            if ( moveNum < 105 )
            {
                oscillate();
            }
            else if ( moveNum >= 105 && moveNum < 210 )
            {
                stopAndGo();
            }
        }
    }
}
