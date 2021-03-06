package edu.cwru.sepia.agent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Soumya-blessed Feature examples
 – #of other friendly units that are currently attacking E
 – Health of friendly unit F
 – Health of enemy unit E
 – Is E the enemy unit that F is currently attacking?

 To reweight:
 w_i = w_i + \alpha * (R(s,a) + \gamma * max_{a'}(Q_w(s',a')) - Q_w(s,a)) * f_i(s,a)
                      |---------------------------===========-----------|  << showing nested parentheses
 
 */

/**
 * A class for the "f(s,a)" feature vector, and other useful functions
 */
public class FeatureVector {

    public static final int NUM_FEATURES = 7; //The number of features to store
    protected double[] featureWeights; //The weights of each feature

    /**
     * randomizes initial feature values
     */
    public FeatureVector() {
        featureWeights = new double[NUM_FEATURES];
    }

    /**
     * Determines the Q weight value by using the given feature vector.
     *
     * @param valueOfFeatures - the vector of feature values to use in calculation: f_i(s,a)
     * @return the Q weight value of the given feature vector
     * 
     * this is the function Q_w(s,a)
     * *
     */
    public double qFunction(double[] valueOfFeatures) {
        //Q_s(s,a) = summation with i of: w_i*f_i(s,a)
        double qW = 0;
        for (int i = 0; i < featureWeights.length; i++) {
            qW += featureWeights[i] * valueOfFeatures[i];
        }
        return qW;
    }

    /**
     * Gets the feature vector of a given state using the footman id, the enemy id, a list of footmen ids, a list of enemy ids,
     * the health of all units, a map of the unit locations, and a map of attack actions in reference to unit ids.
     *
     * @param myFootman - the footman id in reference to
     * @param enemyFootman - the enemy targeted by the given footman
     * @param myFootmen - a list of the footmen currently in the game state
     * @param enemyFootmen - a list of the enemy footmen currently in the game state
     * @param unitHealth - a list of the health of all units
     * @return the feature vector in reference to the given values
     * 
     *  this is the expanded form of f(s,a)
     */
    public static double[] fFunction(Integer myFootman, Integer enemyFootman, List<Integer> myFootmen, List<Integer> enemyFootmen,
                                     Map<Integer, Integer> unitHealth, Map<Integer, Position> unitLocations) {
        
        double[] featureVector = new double[NUM_FEATURES];
        featureVector[0] = 1;//first feature value is always 1, because Devin said so
        featureVector[1] = .04*unitHealth.get(enemyFootman);//enemy health
        featureVector[2] = .08*unitHealth.get(myFootman);//footman health
        //determine the ratio of hit-points from enemy to those of footman
        featureVector[3] = unitHealth.get(myFootman) / Math.max(unitHealth.get(enemyFootman), .5d);//don't divide by 0, yo!
        
        //determine whether the enemy is the closest possible enemy
        if (isNearestEnemy(myFootman, enemyFootman, enemyFootmen, unitLocations)) featureVector[4] += .3;
        else featureVector[4] = -.4;
        
        Position myPosition = unitLocations.get(myFootman);
        Position enemyPosition = unitLocations.get(enemyFootman);
        //determine if the enemy can be attacked based on range from current footman
        if(myPosition.isAdjacent(enemyPosition)) featureVector[5] = .5; //[0.3]
        
        featureVector[6] = getAdjacentEnemyCount(myFootman, enemyFootmen, unitLocations);
        
        return featureVector;
    }

    /**
     * Updates the feature weights vector given a feature vector, the temporal difference, and alpha.
     *
     * @param updateVector - the feature vector to update the feature weights set with
     * @param temporalDifference - the calculated loss function
     * @param alpha - a variable alpha value
     */
    public double[] updateWeights(double[] updateVector, double temporalDifference, double alpha) {
        /**
         To reweight:
                              |-------------------------------------------------|  << temporalDifference
         w_i = w_i + \alpha * (R(s,a) + \gamma * max_{a'}(Q_w(s',a')) - Q_w(s,a)) * f_i(s,a)
                              |---------------------------===========-----------|  << showing nested parentheses
         */
        
        
        for (int i = 0; i < featureWeights.length; i++) {
            
            featureWeights[i] += (alpha * temporalDifference * updateVector[i]);
            
        }
        normalize();
        return featureWeights;
    }

    /**
     * Determines if the given enemy is the closest enemy
     *
     * @param myFootmanID - the footman to use the reference point of
     * @param enemyFootmanID - the enemy to find the distance from the footman
     * @param enemyFootmen - the list of enemy footmen
     * @param unitLocations - the list of unit locations
     * @return if the given enemy is the closest enemy in terms of the Chebyshev distance
     */
    private static boolean isNearestEnemy(Integer myFootmanID, Integer enemyFootmanID, 
                                          List<Integer> enemyFootmen, Map<Integer, Position> unitLocations) {
        
        Position footmanLoc = unitLocations.get(myFootmanID);
        int closestDistance = footmanLoc.chebyshevDistance(unitLocations.get(enemyFootmanID));
        for (Integer curEnemy : enemyFootmen) {
            Position enemyLoc = unitLocations.get(curEnemy);
            if (footmanLoc.chebyshevDistance(enemyLoc) < closestDistance) return false;
        }
        return true;
    }

    /**
     * You give me the footman's ID, a list of enemy IDs
     *      I give you the number of adjacent enemy footmen
     * @param footman - the footman to use the reference point of
     * @param enemyFootmen - the enemy footmen in the game state
     * @param unitLocations - the unit locations in the game state
     * @return the number of enemies adjacent to the given footman
     */
    private static int getAdjacentEnemyCount(Integer footman, List<Integer> enemyFootmen, Map<Integer, Position> unitLocations) {
        int adjacent = 0;
        Position footmanPos = unitLocations.get(footman);
        for (Integer enemy : enemyFootmen) {
            Position enemyPos = unitLocations.get(enemy);
            if (footmanPos.isAdjacent(enemyPos)) adjacent++;
        }
        return adjacent;
    }
    
    private void normalize(){
        Double objectArr[] = Utils.convertdoubleToDouble(featureWeights);
        Double max = Collections.max(Arrays.asList(objectArr));
        Double min = Collections.min(Arrays.asList(objectArr));
        
        for(int i = 0; i<featureWeights.length;i++){
            featureWeights[i] = 2*(featureWeights[i]-min)/(max-min)-1;
        }
    }//end of normalize

}
