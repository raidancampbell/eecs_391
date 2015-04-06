package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may add your own methods and members.
 */
public class PEAgent extends Agent {

    // The plan being executed
    private Stack<StripsAction> plan = null;

    // maps the real unit Ids to the plan's unit ids
    // when you're planning you won't know the true unit IDs that sepia assigns. So you'll use placeholders (1, 2, 3).
    // this maps those placeholders to the actual unit IDs.
    private Map<Integer, Integer> peasantIdMap;
    private int townhallId;
    private int peasantTemplateId;

    public PEAgent(int playernum, Stack<StripsAction> plan) {
        super(playernum);
        peasantIdMap = new HashMap<>();
        this.plan = plan;
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
        // gets the townhall ID and the peasant ID
        int dummyIndex = 1;
        for(int unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall")) {
                townhallId = unitId;
            } else if(unitType.equals("peasant")) {
                peasantIdMap.put(dummyIndex, unitId);//I changed this line from unitId, unitId.
                dummyIndex++;
            }
        }

        // Gets the peasant template ID. This is used when building a new peasant with the townhall
        for(Template.TemplateView templateView : stateView.getTemplates(playernum)) {
            if(templateView.getName().toLowerCase().equals("peasant")) {
                peasantTemplateId = templateView.getID();
                break;
            }
        }

        return middleStep(stateView, historyView);
    }

    /**
     * This is where you will read the provided plan and execute it. If your plan is correct then when the plan is empty
     * the scenario should end with a victory. If the scenario keeps running after you run out of actions to execute
     * then either your plan is incorrect or your execution of the plan has a bug.
     *
     * You can create a SEPIA deposit action with the following method
     * Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
     *
     * You can create a SEPIA harvest action with the following method
     * Action.createPrimitiveGather(int peasantId, Direction resourceDirection)
     *
     * You can create a SEPIA build action with the following method
     * Action.createPrimitiveProduction(int townhallId, int peasantTemplateId)
     *
     * You can create a SEPIA move action with the following method
     * Action.createCompoundMove(int peasantId, int x, int y)
     *
     * these actions are stored in a mapping between the peasant unit ID executing the action and the action you created.
     *
     * For the compound actions you will need to check their progress and wait until they are complete before issuing
     * another action for that unit. If you issue an action before the compound action is complete then the peasant
     * will stop what it was doing and begin executing the new action.
     *
     * To check an action's progress you can call getCurrentDurativeAction on each UnitView. If the Action is null nothing
     * is being executed. If the action is not null then you should also call getCurrentDurativeProgress. If the value is less than
     * 1 then the action is still in progress.
     *
     * Also remember to check your plan's preconditions before executing!
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        
        if(stateView.getTurnNumber()==0) {
            System.err.println("ERROR! TURN 0 ENCOUNTERED IN PEAGENT MIDDLESTEP!");
            return null;
        }
        //copy the useful variables from stateView
        List<ResourceNode.ResourceView> resources = stateView.getAllResourceNodes();
        List<Unit.UnitView> units = stateView.getAllUnits();
        Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
        
        //initialize our return variable
        Map<Integer, Action> returnVar = new HashMap<>();
        
        while(!plan.empty()){//while we still have moves to take
            int nextID = new Token(plan.peek().getSentence()).id;
            boolean actionAlreadyTaken;
            boolean durativeComplete;
            
            {//block to show what I'm playing with.  TODO: remove the block, but keep the code.
                actionAlreadyTaken = returnVar.containsKey(nextID);
                durativeComplete = actionResults.containsKey(nextID) && actionResults.get(nextID).toString().toLowerCase().equals("complete");
                if (actionResults.containsKey(nextID)){
                    /*
                    SEPIA documentation to the rescue!
                    just kidding.  I have no idea what to expect from ActionResult,
                    so I'm just printing it out.
                     */
                    System.out.println("Peasant: "+nextID+" has believes its durative status is: "+actionResults.get(nextID).toString().toLowerCase());
                }
            }
            
            if(!actionAlreadyTaken && !durativeComplete){//if we haven't planned an action for this unit
                //create the action from this next planned item, and put it in the map of actions to take
                Action nextAction = createSepiaAction(plan.pop(), resources, units);
                if(nextAction != null)  returnVar.put(nextID, nextAction);
            } else break;
        }
        
        //return a null if it's empty.  Just trying to be nicer to other people's code
        return returnVar.isEmpty() ? null : returnVar;
    }

    /**
     * Returns a SEPIA version of the specified Strips Action.
     * @param action StripsAction action to take, as an Object implementing the StripsAction interface
     * @return SEPIA representation of same action
     */
    private Action createSepiaAction(StripsAction action, List<ResourceNode.ResourceView> resourceList, List<Unit.UnitView> units) {
        Token token = new Token(action);//parse the action into a more easily handled form
        int unitID = peasantIdMap.get(token.id);
        Unit.UnitView myUnit = getUnitFromID(unitID, units);
        if(myUnit != null && myUnit.getTemplateView().getDurationDeposit() + myUnit.getTemplateView().getDurationGatherGold() +
                myUnit.getTemplateView().getDurationGatherWood() + myUnit.getTemplateView().getDurationMove()>0){
            
            System.err.println("Unit "+myUnit.getID()+" is trying to get a new action while unfinished.");
            System.err.println("\nYou're gonna have a bad time, and this code won't work.  Busy loop?");
        }
        Position myPosition = new Position(myUnit);
        System.out.println("Creating action from the following token: ");
        System.out.println(token.toString());
        switch (token.verb){
            case "get":
                System.out.println(unitID+" is gathering resource "+token.nounEnums.get(0));
                return Action.createCompoundGather(unitID, 
                        getNearestNonemptyResource(resourceList,token.nounEnums.get(0), myPosition));
            case "move"://don't care, taken care of by compound statements
                //return Action.createCompoundMove(unitID, x,y);
                System.out.println(unitID + " encountered a 'move' command.  Ignoring...");
                break;
            case "put":
                System.out.println(unitID+" is depositing a resource");
                return Action.createCompoundDeposit(unitID, townhallId);
            case "make":
                System.out.println("Creating new peasant, in a completely PG manner");
                return Action.createPrimitiveProduction(townhallId, peasantTemplateId);
            default:
                System.err.println("Error! unrecognized verb '"+token.verb+"' was used! expected, 'get' or 'put'");
                break;
        }
        return null;
    }

    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {
        System.out.println("Terminal step reached.");
    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }
    
    //Takes a single Plan action, and parses it into a more usable form
    private class Token{
        /*
            Language:
                Verb->ID->(->Args->)
                
                Args := Noun
                     := Noun , Args
                     
                Verb := Move
                     := Get
                     := Put
                
                ID   := 1
                     := 2
                     := 3
                     
            This is code is not resilient at all.
            Do not pass it malformed requests.
         */
        private String value;
        protected String verb;
        protected int id;
        protected ArrayList<ResourceNode.Type> nounEnums;
        
        public Token(String value){
            this.value = value;
            this.nounEnums = new ArrayList<>();
        }
        
        public Token (StripsAction stripsAction){
            this(stripsAction.getSentence());
        }

        public void parse(){
            this.value = this.value.replace(" ","");
            this.value = this.value.toLowerCase();
            this.id = -1;
            parseVerb();
            parseID();
            parseNouns();
        }
        
        private void parseVerb(){
            if(value.contains("move")) verb = "move";//Moves are ignored.  Compound actions are used instead.
            if(value.contains("gather")) verb = "get";
            if(value.contains("harvest")) verb = "get";
            if(value.contains("get")) verb = "get";
            if(value.contains("deposit")) verb = "put";
            if(value.contains("put")) verb = "put";
            if(value.contains("create")) verb = "make";
            if(value.contains("spawn")) verb = "make";
            if(value.contains("make")) verb = "make";
            if(value.contains("build")) verb = "make";
            if(value.contains("build")) verb = "produce";
        }
        
        private void parseID(){
            if(value.contains("1")) id = 1;
            if(value.contains("2")) id = 2;
            if(value.contains("3")) id = 3;
            //if no ID is found, it's set to -1
        }
        
        private void parseNouns(){
            String inQuestion = value.substring(value.indexOf('('), value.indexOf(')'));
            for(String noun: inQuestion.split(",")){
                ResourceNode.Type type = getTypeFromString(noun);
                if(type != null) nounEnums.add(type);
            }
        }
        
        @Override
        public String toString(){
            StringBuilder returnVar = new StringBuilder();
            returnVar.append(verb);
            returnVar.append(id).append('(');
            for(ResourceNode.Type type: nounEnums) {
                returnVar.append(type.toString());
                returnVar.append(',');
            }
            if(returnVar.lastIndexOf(",") >=0) returnVar.deleteCharAt(returnVar.lastIndexOf(","));
            returnVar.append(')');
            return returnVar.toString();
        }
    }

    /**
     * Find the nearest resource to my position
     *      it must be of the specified type
     *      it must not be empty      
     * @param resources arrayList of ResourceViews on the map
     * @param requiredType name of requested resource, as a String
     * @param myPosition my current position
     * @return the best destination, or -1 if impossible
     */
    private int getNearestNonemptyResource(List<ResourceNode.ResourceView> resources, 
                                           ResourceNode.Type requiredType, Position myPosition){
        int shortestDistance = Integer.MAX_VALUE;
        int shortestDistanceID = -1;
        for(ResourceNode.ResourceView resource: resources){
            if(resource.getType() == requiredType && resource.getAmountRemaining()>0){
                //if it's what we want, and there's some left.
                int distance = myPosition.chebyshevDistance(new Position(resource.getXPosition(), resource.getYPosition()));
                if (distance < shortestDistance){
                    shortestDistanceID = resource.getID();
                }
            }
        }
        return shortestDistanceID;
    }

    /**
     *
     * @param resourceName requested resource, as a case insensitive string
     * @return the enum associated with the queried string, null if not found
     */
    private ResourceNode.Type getTypeFromString(String resourceName){
        ResourceNode.Type requiredType = null;
        if(resourceName.toLowerCase().contains("gold")) requiredType = ResourceNode.Type.GOLD_MINE;
        if(resourceName.toLowerCase().contains("wood")) requiredType = ResourceNode.Type.TREE;
        if(resourceName.toLowerCase().contains("tree")) requiredType = ResourceNode.Type.TREE;
        if(requiredType == null){
            System.err.println("Error! Invalid resourceName given in getNearestNonemptyResource!");
            System.err.println("Expected: gold, wood");
            System.err.println("Received: "+resourceName);
        }
        return requiredType;
    }

    /**
     *
     * @param unitID ID number of the unit you're looking for
     * @param units list of units to look through
     * @return the unit with the given ID, null if not found
     */
    private Unit.UnitView getUnitFromID(int unitID, List<Unit.UnitView> units){
        for(Unit.UnitView unit: units){
            if(unit.getID() == unitID) return unit;
        }
        return null;
    }
}
