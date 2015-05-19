package robo;

import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

public interface Radar extends RobotPart {
    public void init();

    public void move();

    public boolean shouldTrack(ScannedRobotEvent e);

    public boolean wasTracking(RobotDeathEvent e);
}
