package robo;

import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

public interface Radar extends RobotPart {
    void init();

    void move();

    boolean shouldTrack(ScannedRobotEvent e);

    boolean wasTracking(RobotDeathEvent e);
}
