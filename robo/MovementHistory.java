package robo;

/**
 * Created by cs on 5/24/15.
 */
public class MovementHistory
{
    int chosenMovement = 0; // number of movement to use

    int bestMoveNotFound = 1; // tells if the best movement has been found (1 = no, 0 = yes)

    double move1Effectiveness = 0; // tells how effective movement 1 was

    double move2Effectiveness = 0; // tells how effective movement 2 was

    int winsWithMovement1 = 0; // movement 1 win counter
}
