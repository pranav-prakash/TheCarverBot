package robo;

import robocode.*;
import robocode.util.Utils;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;


/**
 * TheCarver -- A modular bot adhering to the RoboPart Interface.
 *
 * Multi-modal radar and movement
 *
 * 1v1 Mode:
 *           Perfect lock radar with 1pt beam diameter
 *           Switches between two different movement patterns based on win rate
 *                  Randomized movement designed to counter pattern matching
 *                  Stop/go movement designed to counter linear/radial prediction
 *
 * Melee Mode:
 *            Corner movement
 *            Sweeping radar that chooses target based on various factors
 *                  Switches beam diameter at regular intervals
 *            Switches into 1v1 mode when necessary
 *
 * @author Pranav Prakash
 * @author Period - 7
 * @author Assignment - PartsBot
 * @author Sources - LemonDrop, Acero, Lib, Robowiki
 * @version 5/14/15
 */
public class TheCarver extends AdvancedRobot
{
    ///Hold information about currently targetted enemy (heading, speed, etc.)
    private AdvancedEnemyBot enemy = new AdvancedEnemyBot();

    ///Hold information about the last bullet that impacted me
    private BulletLocation bul = new BulletLocation();

    ///Array of hot-swappable modular robot parts
    private RobotPart[] parts = new RobotPart[3]; // make three parts

    ///Indices of those parts

    ///Radar is index 0
    private final static int RADAR = 0;

    ///Gun is index 1
    private final static int GUN = 1;

    ///Tank is index 2
    private final static int TANK = 2;

    ///Hold the current beam diameter
    double scanWidth = -1;

    ///Threshold to switch into 1v1 mode from melee mode
    private final int ONEvONE_THRESHOLD = 1;

    ///Whether we are in melee mode right now
    boolean isMeleeMode = false;

    /// flippable direction bit for altering direction when moving backwards
    int backDir = 1;

    /// flippable direction bit for altering direction when moving forwards
    int aheadDir = 1;

    /// Random number generated when hit to change movement
    double randHit = 1;

    /// Hashmap mapping enemy to prior win statistics
    static HashMap<String, MovementHistory> moveHistoryMap = new HashMap<String, MovementHistory>();


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
        // If melee mode
        if ( getOthers() > ONEvONE_THRESHOLD )
        {
            System.out.println( "Melee Mode" );
            isMeleeMode = true;
            parts[RADAR] = new RadarMelee();
            parts[GUN] = new Gun();
            parts[TANK] = new TankMelee();
        }
        else // If one on one
        {
            System.out.println( "1v1 Mode" );
            isMeleeMode = false;
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
        //Downcast the robot part to a radar (which is itself an interface)
        Radar radar = (Radar)parts[RADAR];

        //If you are currently tracking this bot
        // or (for Melee mode) if it is the bot that shot at you
        if ( radar.shouldTrack( e ) || e.getName().equals( bul.getName() ) )
        {
            //Reset volatile bullet info (no longer needed as you have found your enemy)
            if ( e.getName().equals( bul.getName() ) )
                bul.reset();

            //Update enemy information
            enemy.update( e, this, getTime() );
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

        setAhead( 100 );

        //aheadDir *= -1; // change direction
        randHit *= ( Math.random() + 5 )
                        * aheadDir; // generate new random number if hit

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
        randHit *= ( Math.random() + 5 ) * aheadDir;
        backDir *= -1;
    }


    // If movement is unsuccessful, switch to a new movement.
    public void onDeath( DeathEvent event )
    {

        if ( !isMeleeMode )
        {
            MovementHistory hist = moveHistoryMap.get( enemy.getName() );

            System.out.println( ( hist.chosenMovement < 105 ) ?
                            "Oscillate" :
                            "StopGo" );

            if ( hist.winsWithMovement1 < 3 )
            { // we'll assume if we win 3 before we lose 3, the current movement is fine
                hist.chosenMovement += ( 35
                                * hist.bestMoveNotFound ); // add a little bit to movement number
            }
            if ( hist.chosenMovement < 110 )
            { // and if we are using movement 1,
                hist.move1Effectiveness = hist.move1Effectiveness
                                - enemy.getEnergy(); // record how much energy the enemy had left
            }
            else
            { // and if we are using movement 2,
                hist.move2Effectiveness = hist.move2Effectiveness
                                - enemy.getEnergy(); // record how much energy the enemy had left
            }
        }
    }


    public void onWin( WinEvent event )
    {

        if ( !isMeleeMode )
        {
            // if we win,
            MovementHistory hist = moveHistoryMap.get( enemy.getName() );

            System.out.println( ( hist.chosenMovement < 105 ) ?
                            "Oscillate" :
                            "StopGo" );

            if ( hist.chosenMovement < 1 )
            { // and are using movement 1,
                hist.move1Effectiveness = hist.move1Effectiveness
                                + getEnergy(); // record how much energy we had left
                hist.winsWithMovement1 += 1; // add a win to the movement 1 counter
            }
            else
            { // and if we are using movement 2,
                hist.move2Effectiveness = hist.move2Effectiveness
                                + getEnergy(); // record how much energy we had left
            }
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
                        && getOthers() <= ONEvONE_THRESHOLD ) // If one on one
        {
            System.out.println( "Switched to 1v1 mode" );
            isMeleeMode = false;
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
            double firePower;
            if ( getEnergy() > 20 )
            {
                firePower = Math.min( 500 / enemy.getDistance(), 3 );
            }
            else
            {
                firePower = 1.2;
            }

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
        private final int PATTERN_LENGTH = 8;

        //These are the coordinates of our corner movement (if it was in the lower-left corner).
        //The length of these strings should be PATTERN_LENGTH and Lib should have to change
        //directions (as in forward or reverse) for each point.
        private final int[] xcoords = { 30, 30, 90, 30, 150, 150, 30, 210 };

        private final int[] ycoords = { 30, 210, 30, 150, 150, 30, 90, 30 };

        //this will normalize to PI/2, but it is big enough that I can use it for setAhead, too.
        private double direction = 41 * Math.PI / 2;

        int index = 0;


        /**
         * Initialize
         */
        public void init()
        {

        }


        /**
         * Move
         */
        public void move()
        {
            double goalY = ycoords[index];

            double curY = getY();

            if ( curY > 500 )
                goalY = getBattleFieldHeight() - goalY;

            double goalX = xcoords[index];

            double curX = getX();
            if ( curX > 500 )
                goalX = getBattleFieldWidth() - goalX;

            if ( Point2D.distance( goalX, goalY, curX, curY ) < 10 )
            {
                direction = -direction;
                index = ( index + 1 ) % PATTERN_LENGTH;
            }

            setAhead( direction );

            setAhead( getTurnRemainingRadians() == 0 ? direction : 0 );

            setTurnRightRadians( robocode.util.Utils.normalRelativeAngle(
                            Math.atan2( goalX - curX, goalY - curY )
                                            + Math.PI / 2 - direction
                                            - getHeadingRadians() ) );
        }
    }


    public class Tank1v1 implements Tank
    {

        double direction = 1;

        int startStop = 0;

        boolean flip = false;


        /**
         * Initialize
         */
        public void init()
        {
            chooseBestMovement();
        }


        public void chooseBestMovement()
        {
            // Determining Most Effective Movement

            if ( moveHistoryMap.get( enemy.getName() ) == null )
            {
                moveHistoryMap.put( enemy.getName(), new MovementHistory() );
            }

            MovementHistory hist = moveHistoryMap.get( enemy.getName() );

            if ( hist.chosenMovement >= 210 )
            { // if we have died 3 times (to get enough sample size)
                hist.bestMoveNotFound = 0; // You have found tentative best movement
                if ( hist.move2Effectiveness > hist.move1Effectiveness )
                { // else if movement 2 performed the best,
                    hist.chosenMovement = 150; // use movement 2
                }
                else
                { // else if movement 1 performed the best,
                    hist.chosenMovement = 0; // use movement 1
                }
            }
        }


        public void oscillate()
        {
            double goalDirection = ( Math.toRadians( enemy.getBearing() )
                            + getHeadingRadians() ) -
                            ( Math.PI / 2 + ( ( enemy.getDistance() ) >= 600 ?
                                            0 :
                                            0.4 ) ) * direction;

            while ( !new Rectangle2D.Double( 19.0,
                            19.0,
                            getBattleFieldWidth() - 38,
                            getBattleFieldHeight() - 38 ).contains(
                            getX() + Math.sin( goalDirection ) * 120,
                            getY() + Math.cos( goalDirection ) * 120 ) )
            {
                goalDirection = goalDirection + direction * .1;
                if ( Math.random() < .01 )
                    direction = -direction;
            }

            goalDirection = Utils.normalRelativeAngle(
                            goalDirection - getHeadingRadians() );

            setTurnRightRadians( Math.tan( goalDirection ) );
            setAhead( 100 * ( Math.abs( goalDirection ) > Math.PI / 2 ?
                            -1 :
                            1 ) );
            if ( Math.random() < .04 )
                direction = -direction;
        }


        public void stopAndGo()
        {

            // Stop & Go
            // Second type of movement

            double goalDirection = ( Math.toRadians( enemy.getBearing() )
                            + getHeadingRadians() ) - ( flip ?
                            -Math.PI / 2 :
                            Math.PI / 2 );

            while ( !new Rectangle2D.Double( 20.0,
                            20.0,
                            getBattleFieldWidth() - 50,
                            getBattleFieldHeight() - 50 ).contains(
                            getX() + Math.sin( goalDirection ) * 100,
                            getY() + Math.cos( goalDirection ) * 100 ) )
            {
                goalDirection = goalDirection - 0.1;
                flip = !flip;
            }

            goalDirection = Utils.normalRelativeAngle(
                            goalDirection - getHeadingRadians() );

            setTurnRightRadians( Math.tan( goalDirection ) );
            setAhead( ( 50 + Math.random() * 50 ) * (
                            Math.abs( goalDirection ) > Math.PI / 2 ? -1 : 1 )
                            * startStop );

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

        }


        public void move()
        {
            chooseBestMovement();
            MovementHistory hist = moveHistoryMap.get( enemy.getName() );
            if ( hist.chosenMovement < 105 )
            {
                oscillate();
            }
            else if ( hist.chosenMovement >= 105 && hist.chosenMovement < 210 )
            {
                stopAndGo();
            }
        }
    }
}
