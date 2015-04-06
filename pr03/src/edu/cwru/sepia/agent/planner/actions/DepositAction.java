package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.environment.model.state.ResourceType;

/**
 * Created by luc on 3/28/15.
 */
public class DepositAction implements StripsAction{
    private GameState.ExistentialPeasant depositPeasant;

    public DepositAction(GameState.ExistentialPeasant p){
        depositPeasant = p;
    }

    @Override
    public String getSentence() {
        //TODO: check if peasant and resource of interest are real first?

        return "DEPOSIT("+depositPeasant.getPeasantID()+", "+depositPeasant.getCargoType().toString()+")";
    }

    @Override
    //A peasant needs to have gold to deposit
    //The peasant in question needs to be next to a town hall
    //So,   1. Find peasant next to a town hall
    //      2. Check if that peasant is carrying cargo
    public boolean preconditionsMet(GameState state) {
        if(depositPeasant.isNextToTownHall() && depositPeasant.hasWood() || depositPeasant.hasGold()){ //TODO: Change this when aidan updates the booleans
            return true;
        } else return false;
    }

    @Override
    //The peasant has no more gold
    //The town hall has more gold
    public GameState apply(GameState state) {
        GameState postDepositState = new GameState(state, 0d); //TODO: Maybe change 0d to something that makes sense for Astar
        for (GameState.ExistentialPeasant p : postDepositState.getPeasantTracker()){
            if(p.getPeasantID() == depositPeasant.getPeasantID()){
                // Set all boolean tracking features of the deposit peasant in the new state to false
                //
            }
        }
        return null;
    }
}
