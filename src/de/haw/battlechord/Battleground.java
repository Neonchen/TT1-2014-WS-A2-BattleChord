package de.haw.battlechord;

import de.uniba.wiai.lspi.chord.data.ID;

import java.util.*;

public class Battleground {

    int shipsIntact;
    Map<ID,Integer> board;
    List<ID> boardKeys;

    int UNKNOWN = 0;
    int WATER = 1;
    int SHIP = 2;
    int WRACK = 3;

	public Battleground(int groundSize, int shipQuantity){
		this.shipsIntact = shipQuantity;
        board = new HashMap<ID, Integer>();
        //TODO fill with smth to reach groundSize
        boardKeys = new ArrayList<ID>(board.keySet());
	}

    public void setShips(){
        int shipsToSet = shipsIntact;
        ID field = null;
        while(shipsToSet > 0){
            field = getRandomBoardEntry();
            if(board.get(field) != SHIP){
                board.put(field, SHIP);
                shipsToSet --;
            }
        }
    }
	
	public void setHit(ID target, Boolean hit){
        if(board.get(target) != WRACK){
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
        int result = board.get(target);
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
}
