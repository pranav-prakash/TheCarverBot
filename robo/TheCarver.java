package robo;

import robocode.AdvancedRobot;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

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
public class TheCarver extends AdvancedRobot {
    private AdvancedEnemyBot enemy = new AdvancedEnemyBot();
    private RobotPart[] parts = new RobotPart[3]; // make three parts

    private final static int RADAR = 0;
    private final static int GUN = 1;
    private final static int TANK = 2;

    private final int ONEONONE_THRESHHOLD = 3;

    /**
     * computes the absolute bearing between two points
     *
     * @param x1 point1x
     * @param y1 point1y
     * @param x2 point2x
     * @param y2 point2y
     * @return absolute bearing
     */
    public double absoluteBearing(double x1, double y1, double x2, double y2) {
        double xo = x2 - x1;
        double yo = y2 - y1;
        double hyp = Point2D.distance(x1, y1, x2, y2);
        double arcSin = Math.toDegrees(Math.asin(xo / hyp));
        double bearing = 0;

        if (xo > 0 && yo > 0) { // both pos: lower-Left
            bearing = arcSin;
        } else if (xo < 0 && yo > 0) { // x neg, y pos: lower-right
            bearing = 360 + arcSin; // arcsin is negative here, actually 360 -
            // ang
        } else if (xo > 0 && yo < 0) { // x pos, y neg: upper-left
            bearing = 180 - arcSin;
        } else if (xo < 0 && yo < 0) { // both neg: upper-right
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
    public double normalizeBearing(double angle) {
        while (angle > 180) {
            angle -= 360;
        }
        while (angle < -180) {
            angle += 360;
        }
        return angle;
    }

    /**
     * Runs the bot
     */
    public void run() {

        if (getOthers() > ONEONONE_THRESHHOLD) //If melee
        {
            parts[RADAR] = new RadarMelee();
            parts[GUN] = new Gun();
            parts[TANK] = new TankMelee();
        } else //If one on one
        {
            parts[RADAR] = new Radar1v1();
            parts[GUN] = new Gun();
            parts[TANK] = new Tank1v1();
        }

        // initialize each part
        for (int i = 0; i < parts.length; i++) {
            // behold, the magic of polymorphism
            parts[i].init();
        }

        // iterate through each part, moving them as we go
        for (int i = 0; true; i = (i + 1) % parts.length) {
            // polymorphism galore!
            parts[i].move();
            if (i == 0) {
                execute();
            }
        }
    }

    /**
     * On robot scan
     *
     * @param e scanevent
     */
    public void onScannedRobot(ScannedRobotEvent e) {
        Radar radar = (Radar) parts[RADAR];
        if (radar.shouldTrack(e)) {
            enemy.update(e, this);
        }
    }

    /**
     * On death
     *
     * @param e deathevent
     */
    public void onRobotDeath(RobotDeathEvent e) {
        Radar radar = (Radar) parts[RADAR];
        if (radar.wasTracking(e)) {
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
    public class RadarMelee implements Radar {
        /**
         * Initialize
         */
        public void init() {
            setAdjustRadarForGunTurn(true);
            setAdjustRadarForRobotTurn(true);
            setTurnRadarRight(3600);
        }


        /**
         * move
         */
        public void move() {
            if (enemy.none()) {
                setTurnRadarRight(3600);
            } else {
                double absoluteBearing = getHeading() + enemy.getBearing();
                double radarTurn = normalizeBearing(absoluteBearing - getRadarHeading());
                double toScan = Math.min(Math.atan(36.0 / enemy.getDistance() * 180 / 3.14), 45);
                radarTurn += radarTurn >= 0 ? toScan : -toScan;
                setTurnRadarRight(radarTurn);

            }
        }


        /**
         * Should track
         *
         * @param e robotevent
         * @return is tracked
         */
        public boolean shouldTrack(ScannedRobotEvent e) {
            // track if we have no enemy, the one we found is significantly
            // closer, or we scanned the one we've been tracking.
            return (enemy.none() || e.getDistance() < enemy.getDistance()
                    || e.getName()
                    .equals(enemy.getName()));
        }


        /**
         * Check whether previously tracking
         *
         * @param e event
         * @return was previously tracking
         */
        public boolean wasTracking(RobotDeathEvent e) {
            return e.getName().equals(enemy.getName());
        }
    }

    public class Radar1v1 implements Radar {
        /**
         * Initialize
         */
        public void init() {
            setAdjustRadarForGunTurn(true);
            setAdjustRadarForRobotTurn(true);
            setTurnRadarRight(3600);
        }


        /**
         * move
         */
        public void move() {
            if (enemy.none()) {
                setTurnRadarRight(3600);
            } else {
                double absoluteBearing = getHeading() + enemy.getBearing();
                double radarTurn = normalizeBearing(absoluteBearing - getRadarHeading());
                double toScan = Math.min(Math.atan(36.0 / enemy.getDistance() * 180 / 3.14), 45);
                radarTurn += radarTurn >= 0 ? toScan : -toScan;
                setTurnRadarRight(radarTurn);

            }
        }


        /**
         * Should track
         *
         * @param e robotevent
         * @return is tracked
         */
        public boolean shouldTrack(ScannedRobotEvent e) {
            // track if we have no enemy, the one we found is significantly
            // closer, or we scanned the one we've been tracking.
            return (enemy.none() || e.getDistance() < enemy.getDistance() - 70
                    || e.getName()
                    .equals(enemy.getName()));
        }


        /**
         * Check whether previously tracking
         *
         * @param e event
         * @return was previously tracking
         */
        public boolean wasTracking(RobotDeathEvent e) {
            return e.getName().equals(enemy.getName());
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
    public class Gun implements RobotPart {
        /**
         * Initialize
         */
        public void init() {
            // divorce gun movement from tank movement
            setAdjustGunForRobotTurn(true);
        }


        /**
         * Move
         */
        public void move() {
            // don't shoot if I've got no enemy
            if (enemy.none()) {
                return;
            }

            // calculate firepower based on distance
            double firePower = Math.min(500 / enemy.getDistance(), 3);
            // calculate speed of bullet
            double bulletSpeed = 20 - firePower * 3;
            // distance = rate * time, solved for time
            long time = (long) (enemy.getDistance() / bulletSpeed);

            // calculate gun turn to predicted x,y location
            double futureX = enemy.getFutureX(time);
            double futureY = enemy.getFutureY(time);

            double absDeg = absoluteBearing(getX(), getY(), futureX, futureY);
            // non-predictive firing can be done like this:
            // double absDeg = absoluteBearing(getX(), getY(), enemy.getX(),
            // enemy.getY());

            // turn the gun to the predicted x,y location
            setTurnGunRight(normalizeBearing(absDeg - getGunHeading()));

            // if the gun is cool and we're pointed in the right direction,
            // shoot!
            if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
                setFire(firePower);
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
    public class TankMelee implements Tank {
        private byte moveDirection = 1;
        Random rand;

        private final int TURN_FACTOR = 14;
        private final int AHEAD_FACTOR = 1020;
        private final int DELAY_FACTOR = 20;

        /**
         * Initialize
         */
        public void init() {
            rand = new Random();
        }

        /**
         * Move
         */
        public void move() {

            setTurnRight(normalizeBearing(enemy.getBearing() + 90
                    - ((rand.nextInt(TURN_FACTOR) + 1) * moveDirection)));

            if (getTime() % (rand.nextInt(DELAY_FACTOR) + 1) == 0) {
                moveDirection *= -1;
                setAhead(rand.nextInt(AHEAD_FACTOR) * moveDirection);
            }
        }
    }

    public class Tank1v1 implements Tank {
        private byte moveDirection = 1;
        Random rand;

        private final int TURN_FACTOR = 14;
        private final int AHEAD_FACTOR = 1020;
        private final int DELAY_FACTOR = 20;

        /**
         * Initialize
         */
        public void init() {
            rand = new Random();
        }

        /**
         * Move
         */
        public void move() {

            setTurnRight(normalizeBearing(enemy.getBearing() + 90
                    - ((rand.nextInt(TURN_FACTOR) + 1) * moveDirection)));

            if (getTime() % (rand.nextInt(DELAY_FACTOR) + 1) == 0) {
                moveDirection *= -1;
                setAhead(rand.nextInt(AHEAD_FACTOR) * moveDirection);
            }
        }
    }
}
