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

    ID ownID;
    ID successorID;

    int UNKNOWN = 0;
    int WATER = 1;
    int SHIP = 2;
    int WRACK = 3;

    /**
     * This constructor is used to instantiate own battleground
     * @param startID and
     * @param endID define the range in adress space, used for battleground
     * @param groundsize intervals on the battleground
     * @param shipQuantity number of ships to set
     */
	public Battleground(ID startID, ID endID, int groundsize, int shipQuantity){
        this.ownID = startID;
        this.successorID = endID;
        this.groundsize = groundsize;
		this.shipsIntact = shipQuantity;
        this.intervallSize = getDistance(ownID, successorID).divide(BigInteger.valueOf(groundsize));
        board.put(ownID, UNKNOWN);
        for(Integer i = 1; i < 100; i++){
            board.put(ID.valueOf(ownID.toBigInteger().add( BigInteger.valueOf(i).multiply(intervallSize) ) ), WATER);
        }
        boardKeys = new ArrayList<ID>(board.keySet());
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
        board = new HashMap<ID, Integer>();
        board.put(playerID, UNKNOWN);
        this.addressSpace = BigInteger.valueOf( Math.round(Math.pow(2,160)-1) );
    }

    public int getShipsIntact(){
        return this.shipsIntact;
    }

    public void setShips(){
        //TODO not to the last interval! (is ineffective)
        int shipsToSet = shipsIntact;
        ID field = null;
        for(ID id : boardKeys){
            board.remove(id);
            board.put(id, WATER);
        }
        while(shipsToSet > 0){
            field = getRandomBoardEntry();
            if(board.get(field) != SHIP){
                board.remove(field);
                board.put(field, SHIP);
                shipsToSet --;
            }
        }
    }
	
	public void setHit(ID target, Boolean hit){
        if(board.get(target) != WRACK){
            if(board.get(target) != null) board.remove(target);
            if(!hit){
                board.put(target, WATER);
            }else if(hit){
                board.put(target, WRACK);
                shipsIntact --;
            }
        }else{
            //ship on this field has been attacked before
        }
	}
	
	public boolean isHit(ID target){
		boolean hit = true;
        ID interval = null;
        ID from = boardKeys.get(0);
        ID to = null;
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
	
	public boolean isGameOver(ID target){
		return shipsIntact == 0;
	}
	
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
        if(ownID.compareTo(successorID)== 1) ring = true; //This means, the intervall is passing zero
        while(!found){
            BigInteger address;
            do {
                address = new BigInteger(this.addressSpace.bitLength(), r);
            } while (address.compareTo(this.addressSpace) >= 0);
            target = ID.valueOf(address);
            if( (!ring      && !target.isInInterval(ownID, successorID)) ||
                    (ring   && !target.isInInterval(ownID,new ID(addressSpace.toByteArray()))
                            && !target.isInInterval(new ID(BigInteger.ZERO.toByteArray()),successorID) )){
                found = true;
            }
        }

        return target;
    }

    public void newInformation (ID target, boolean hit){
        //handle new Information about this player
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
