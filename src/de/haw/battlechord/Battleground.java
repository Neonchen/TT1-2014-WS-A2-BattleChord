package de.haw.battlechord;

import de.uniba.wiai.lspi.chord.data.ID;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class Battleground {

    private int shipsIntact;
    private Map<ID,Integer> board = new HashMap<ID, Integer>();
    private List<ID> boardKeys;
    private BigInteger intervallSize;
    private Integer groundsize;
    private BigInteger addressSpace = new BigDecimal( Math.pow(2, 160) - 1).toBigInteger();
    private Map<ID, Boolean> collectedHits = new HashMap<ID, Boolean>();
    private Boolean instantiated = false;

    private Random random = new Random();

    private ID ownID;
    private ID predecessorID = null;
    private ID maxID = ID.valueOf(addressSpace);
    private ID zeroID = ID.valueOf(BigInteger.ZERO);

    private int UNKNOWN = 0;
    private int WATER = 1;
    private int SHIP = 2;
    private int WRACK = 3;

    /**
     * This constructor is used to instantiate known battleground with UNKNOWN
     * @param startID and
     * @param endID define the range in adress space, used for battleground
     * @param groundsize intervals on the battleground
     * @param shipQuantity number of ships to set
     */
	public Battleground(ID startID, ID endID, int groundsize, int shipQuantity){
        this.predecessorID = startID;
        this.ownID = endID;
        this.groundsize = groundsize;
		this.shipsIntact = shipQuantity;
        this.intervallSize = getDistance(ID.valueOf(predecessorID.toBigInteger().add(BigInteger.ONE)),ownID).divide(BigInteger.valueOf(groundsize)).abs();
        initBoard();
	}

    /**
     * Instantiate blank battleground for unknown Player
     * @param playerID will be the startID for the battleground
     * @param groundsize intervals on the battleground
     * @param shipQuantity number of ships to set
     */
    public Battleground(ID playerID, int groundsize, int shipQuantity){
        this.ownID = playerID;
        this.groundsize = groundsize;
        this.shipsIntact = shipQuantity;
    }

    /**
     * init board based on calculatable intervals
     * predecessorID has to be known
     */
    private void initBoard(){
        if(predecessorID != null) {
            BigInteger startID = predecessorID.toBigInteger().add(BigInteger.ONE);
            boardKeys = new ArrayList<ID>();
            for (Long i = 0l; i < groundsize; i++) {
                BigInteger id = startID.add(new BigInteger(i.toString()).multiply(intervallSize));
                board.put(
                        ID.valueOf(id.mod(addressSpace)),
                UNKNOWN);
                boardKeys.add(ID.valueOf(id.mod(addressSpace)));
            }
            instantiated = true;
        }
    }

    /**
     * Initially setting own ships into intervals
     * set WATER on every field, then set SHIPs
     */
    public void setShips(){
        int shipsToSet = shipsIntact;
        for(ID id : boardKeys){
            board.remove(id);
            board.put(id, WATER);
        }
        while(shipsToSet > 0){
            ID field = getRandomBoardEntry();
            if(board.get(field) != SHIP){
                board.remove(field);
                board.put(field, SHIP);
                shipsToSet --;
            }
        }
    }

    /**
     * When predecessor is found, the Battleground can be instantiated
     * @param predecessor
     */
    public void setPredecessorID (ID predecessor){
        this.predecessorID = predecessor;
        this.intervallSize = getDistance(ID.valueOf(predecessorID.toBigInteger().add(BigInteger.ONE)),ownID).divide(BigInteger.valueOf(groundsize)).abs();
        initBoard();
        setCollectedHitsToBoard();
    }

    public int getShipsIntact(){
        return this.shipsIntact;
    }

    /**
     * Get attackable Player. Unknown player are difficult to attack
     * @return true, when board is instantiated
     */
    public boolean isInstantiatedPlayer(){
        return this.instantiated;
    }

    /**
     * Changes Battleground to save broadcast informations
     * set WATER / WRACK informations into Player intervals
     * Do nothing, when content of field is already known
     * @param id interval ID, that was attacked
     * @param hit boolean hit ship or water
     */
	private void setHit(ID id, Boolean hit){
        ID target = attackedInterval(id);
        if (board.get(target) != null) {
            if(board.get(target) == UNKNOWN) {
                board.remove(target);
                if (!hit) {
                    board.put(target, WATER);
                } else if (hit) {
                    board.put(target, WRACK);
                    shipsIntact--;
                }
            }else if(hit && board.get(target) != WRACK){ //own board
                board.remove(target);
                board.put(target,WRACK);
            }
        }
    }

    /**
     * gets interval ID, where the target hits
     * @param target attacked ID
     * @return ID of the hit interval
     */
    private ID attackedInterval(ID target){
        ID interval = null;
        ID to;
        for(int i = 0; i < groundsize; i++){
            ID from = boardKeys.get(i);
            if(i+1 < groundsize){
                to = boardKeys.get(i+1);
            } else{//last interval
                to = ownID;
            }
            if((from.compareTo(to) < 0) && (target.isInInterval(from, to))){
                interval = from;
                i = groundsize;
            }else if((from.compareTo(to)) > 0 && (target.isInInterval(from, maxID) ||target.isInInterval(zeroID, to) )){
                interval = from;
                i = groundsize;
            }
        }
        if(interval == null){
            System.out.println("No interval found for "+target);
        }
        /*
        for(ID from : boardKeys) {
            to = ID.valueOf((from.toBigInteger().add(intervallSize)).mod(addressSpace));
            if(target.isInInterval(from, to)){
                interval = from;
                break;
            }
        }*/
        return interval;
    }

    /**
     * method to check attack on own Battleground
     * @param target attacked ID
     * @return true, if ID is in SHIP or WRACK interval, else false (WATER)
     */
	public boolean isHit(ID target){
		System.out.println(">>>>>>>>"+target);
        int result = board.get(attackedInterval(target));
		System.out.println(">>>>>>>>"+attackedInterval(target));
        return (result != WATER);
	}

    /**
     * check if battleground-owner is game over
     * @return true, if all ships are destroyed. false else
     */
	public boolean isGameOver(){
		return shipsIntact == 0;
	}

    /**
     * called, when predecessor is emerged, to evaluate collected information
     */
    private void setCollectedHitsToBoard(){
        for(Map.Entry<ID,Boolean> entry : collectedHits.entrySet()){
            setHit(entry.getKey(), entry.getValue());
        }
    }

    /**
     * For foreign players: get next field, that has not been attacked yet
     * Choose target in the middle of the interval
     * @return ID in interval UNKNOWN
     */
	public ID getNextTargetField(){
        //random UNKNOWN
        ID target;
        do{
            target = getRandomBoardEntry();
        }while((board.get(target) != UNKNOWN) && (board.get(target) != null));
        target = ID.valueOf((target.toBigInteger().add(intervallSize.divide(BigInteger.ONE.add(BigInteger.ONE)))).mod(addressSpace));
        return target;
	}

    /**
     * treat new information about player
     * @param target
     * @param hit
     */
    public void newInformation (ID target, boolean hit){
        if(predecessorID != null){
            setHit(target, hit);
        }else{
            collectedHits.put(target, hit);
        }
    }

    private ID getRandomBoardEntry(){
        return boardKeys.get(random.nextInt(boardKeys.size()));
    }

    private BigInteger getDistance( ID from, ID to){
        return (to.toBigInteger().add(addressSpace).subtract(from.toBigInteger())).mod(addressSpace);
    }

    public String toString(){
        String s = "Battleground("+this.ownID+")\n";
        s+= "Predecessor: "+this.predecessorID+"\n";
        //Collections.sort(boardKeys);
        for(ID id : boardKeys){
            s+= "("+board.get(id)+")"+ id.toString()+"\n";
        }
        return s;
    }

}
