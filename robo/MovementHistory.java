package robo;

/**
 * Stores prior win statistics
 *
 * @author Pranav Prakash
 * @author Period - 7
 * @author Assignment - TheCarver
 * @author Sources
 * @version 5/8/15
 */
public class MovementHistory
{
    /// number of movement to use
    int chosenMovement = 0;

    /// tells if the best movement has been found (1 = no, 0 = yes)
    int bestMoveNotFound = 1;

    /// tells how effective movement 1 was
    double move1Effectiveness = 0; //Movement 1 is oscillating randomness

    /// tells how effective movement 2 was
    double move2Effectiveness = 0;  // Movement 2 is stop go movement

    /// movement 1 win counter
    int winsWithMovement1 = 0;
}
