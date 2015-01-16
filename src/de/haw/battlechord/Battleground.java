package de.haw.battlechord;

import de.uniba.wiai.lspi.chord.data.ID;

import java.math.BigInteger;
import java.util.*;

public class Battleground {

    int shipsIntact;
    Map<ID,Integer> board = new HashMap<ID, Integer>();
    List<ID> boardKeys;
    BigInteger intervallSize;
    Integer groundsize;
    BigInteger addressSpace = BigInteger.valueOf( Math.round(Math.pow(2, 160) - 1) );
    Map<ID, Boolean> collectedHits = new HashMap<ID, Boolean>();
    Boolean instantiated = false;

    ID ownID;
    ID predecessorID = null;

    int UNKNOWN = 0;
    int WATER = 1;
    int SHIP = 2;
    int WRACK = 3;

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
        this.intervallSize = getDistance(predecessorID,ownID).divide(BigInteger.valueOf(groundsize));
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
            for (Integer i = 0; i < 100; i++) {
                board.put(ID.valueOf(predecessorID.toBigInteger().add(BigInteger.valueOf(i).multiply(intervallSize))), UNKNOWN);
            }
            boardKeys = new ArrayList<ID>(board.keySet());
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
    private void setPredecessorID (ID predecessor){
        this.predecessorID = predecessor;
        //calculate board
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
     * @param target interval ID, that was attacked
     * @param hit boolean hit ship or water
     */
	private void setHit(ID target, Boolean hit){
        if(board.get(target) != null) board.remove(target);
        if(!hit){
            board.put(target, WATER);
        }else if(hit){
            board.put(target, WRACK);
            shipsIntact --;
        }
	}

    /**
     * method to check attack on own Battleground
     * @param target attacked ID
     * @return true, if ID is in SHIP or WRACK interval, else false (WATER)
     */
	public boolean isHit(ID target){
		boolean hit = true;
        ID interval = null;
        ID from = boardKeys.get(0);
        ID to;
        for(int i = 1; i < 100; i ++){
            to = boardKeys.get(i);
            if(target.isInInterval(from, to)){
                interval = from;
                i = 100;
            }
            from = to;
        }
        int result = board.get(interval);
        if(result == WATER || result == WRACK){
            hit = false;
        }
        return hit;
	}

    /**
     * check if battleground-owner is game over
     * @return true, if all ships are destroyed. false else
     */
	public boolean isGameOver(){
		return shipsIntact == 0;
	}

    private void setCollectedHitsToBoard(){
        //TODO sort collected hits and set to board
        //attention: intervals might be attacked several times
    }

    /**
     * For foreign players: get next field, that has not been attacked yet
     * @return ID in interval UNKNOWN
     */
	public ID getNextTargetField(){
        //TODO this has to be player specific, to be called from the attacker
        //which ID might get a hit?
        return this.ownID;
	}

    /**
     * Random ID in Adress Space, but not in the own board-intervall
     * Used for a first-shoot, random
     * @return ID of a foreign Player
     */
    public ID getForeignTargetField(){
        ID target = null;
        boolean found = false;
        Random r = new Random();
        boolean ring = false;
        if(predecessorID.compareTo(ownID)== 1) ring = true; //This means, the intervall is passing zero
        while(!found){
            BigInteger address;
            do {
                address = new BigInteger(this.addressSpace.bitLength(), r);
            } while (address.compareTo(this.addressSpace) >= 0);
            target = ID.valueOf(address);
            if( (!ring      && !target.isInInterval(predecessorID, ownID)) ||
                    (ring   && !target.isInInterval(predecessorID,new ID(addressSpace.toByteArray()))
                            && !target.isInInterval(new ID(BigInteger.ZERO.toByteArray()),ownID) )){
                found = true;
            }
        }

        return target;
    }

    public void newInformation (ID target, boolean hit){
        //handle new Information about this player
        if(predecessorID != null){
            setHit(target, hit);
        }else{
            collectedHits.put(target, hit);
        }
        //TODO strategy stuff
    }
	
	//TODO: random field of board should just be random field of address space
    private ID getRandomBoardEntry(){
        Random r = new Random();
        return boardKeys.get(r.nextInt(boardKeys.size()));
    }

    private BigInteger getDistance( ID from, ID to){
        return((this.addressSpace.subtract(from.toBigInteger())).add(to.toBigInteger())).mod(this.addressSpace);
    }

}
