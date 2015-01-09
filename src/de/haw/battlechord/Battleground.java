package de.haw.battlechord;

import de.uniba.wiai.lspi.chord.data.ID;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Battleground {

    int shipsIntact;
    Map<ID,Integer> board;
    List<ID> boardKeys;
    BigInteger intervallSize;
    Integer groundsize;
    BigInteger addressSpace;

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
	public Battleground(ID startID, ID endID, Integer groundsize, int shipQuantity){
        this.groundsize = groundsize;
		this.shipsIntact = shipQuantity;
        this.intervallSize = getDistance(startID,endID).divide(BigInteger.valueOf(groundsize.intValue()));
        board.put(startID, WATER);
        for(Integer i = 1; i < 100; i++){
            board.put(ID.valueOf(startID.toBigInteger().add( BigInteger.valueOf(i.intValue()).multiply(intervallSize) ) ), WATER);
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
        this.groundsize = groundsize;
        this.shipsIntact = shipQuantity;
        board.put(playerID, UNKNOWN);
    }

    public void setShips(){
        //TODO not to the last interval! (is ineffective)
        int shipsToSet = shipsIntact;
        ID field = null;
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
        ID field = null;
        boolean found = false;
        while(!found){
            field = getRandomBoardEntry();
            if(board.get(field) == UNKNOWN){
                found = true;
            }
        }
		return field;
	}

    private ID getRandomBoardEntry(){
        Random r = new Random();
        return boardKeys.get(r.nextInt(boardKeys.size()));
    }

    private BigInteger getDistance( ID from, ID to){
        BigInteger distance=((this.addressSpace.subtract(from.toBigInteger())).add(to.toBigInteger())).mod(this.addressSpace);
        return distance;
    }

}
