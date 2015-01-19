package de.haw.battlechord;

import de.uniba.wiai.lspi.chord.com.Node;
import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class BattleChord {
	
	URL localURL;
	URL bootstrapURL;
	ChordImpl chord; //Chord interface limits functions too much
	String protocol;
	
	int groundsize = 0;
	int shipQuantity = 0;

    Node successor;
    Battleground battleground;
	Map<ID,Battleground> players = new HashMap<ID,Battleground>();
	private boolean gameover = false;
	private boolean chordInitPhase = true;
	private ID highestKey = ID.valueOf(new BigDecimal( Math.pow(2, 160) - 1).toBigInteger());;
	private ID curAttackSuc;
	private boolean attacking;
	private ID lastTarget;
	
	public static void main(String[] args) {
		 Scanner scanner = new Scanner(System.in);
         System.out.println("Hello, Commander!");
		 System.out.println("Welcome to Battlecord");
         System.out.println("       _~");
		 System.out.println("    _~ )_)_~");
         System.out.println("    )_))_))_)");
         System.out.println("    _!__!__!_");
		 System.out.println("    \\______t/");
		 System.out.println("   ~~~~~~~~~~~~~");
		 
		 
		 System.out.println("Use default properties? (join/create/no)");
         String useDefProp = scanner.next();
         
		 String ownIP = "";
		 String ownPort = "";
		 int groundSize = 0;
		 int shipQuantity = 0;
		 String mode = "";
		 String bootstrapIP = "None";
		 String bootstrapPort = "None";
         
         if(useDefProp.startsWith("join") || useDefProp.equals("create")){        	 
        	 Properties prop = new Properties();
    		 try {
    			prop.load(new FileInputStream("src/battlechord_"+useDefProp+".properties"));
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    		 ownIP = prop.getProperty("ownIP");
    		 ownPort = prop.getProperty("ownPort");
    		 groundSize= Integer.parseInt(prop.getProperty("groundSize"));
    		 shipQuantity = Integer.parseInt(prop.getProperty("shipQuantity"));
    		 mode = prop.getProperty("mode");
    		 bootstrapIP = prop.getProperty("bootstrapIP");
    		 bootstrapPort = prop.getProperty("bootstrapPort");
         } else {
             System.out.println("Enter Chord IP:");
             ownIP = scanner.next();
             System.out.println("Enter Chord PORT:");
             ownPort = scanner.next();
             System.out.println("Enter Battleground Size:");
             groundSize = scanner.nextInt();
             System.out.println("Enter Ship Quantity:");
             shipQuantity = scanner.nextInt();
             
             
	         System.out.println("Create or join battle? (create,join)");
	         mode = scanner.next();
	         if(mode.equals("join")){
	             System.out.println("Enter Bootstrap IP:");
	             bootstrapIP = scanner.next();
	             System.out.println("Enter Bootstrap PORT:");
	             bootstrapPort = scanner.next();
	         }
         }    
		 
         System.out.println("Your values are:");
         System.out.println("\n--------------------"
         		+ "\nChord IP: "+ownIP
         		+ "\nChord PORT: "+ownPort
         		+ "\nBattleground Size: "+groundSize
         		+ "\nShip Quantity: "+shipQuantity
         		+ "\nMode: "+mode
         		+ "\nBootstrap IP: "+bootstrapIP
         		+ "\nBootstrap PORT: "+bootstrapPort
         		+ "\n--------------------"
         );

    	BattleChord game = new BattleChord(ownIP,ownPort,groundSize,shipQuantity);
         
        if(mode.equals("create")){
        	 game.createAndJoinBattle();
         } else if(mode.equals("join")){
        	 game.joinBattle(bootstrapIP, bootstrapPort);
         } else {
        	 System.out.println("Thats not an option sir!");
         }
        
        boolean run = true;
        while(run){
        	String cmd = scanner.next();
        	
        	switch(cmd) {
	        	case "init":
	        		game.init();
	        		System.out.println("Ready for battle commander!");
	        		if(game.ownsHighestKey()) System.out.println("Enemies in range! FIIIIIRE ON YOUR COMMAND!");
	        		break;
	        	case "fire":
	        		game.fire();
	        		break;
	        	case "quit":
	        		game.leaveBattle();
	        		run = false;
	        		break;
	        	case "status":
	        		System.out.println(game.printPlayers());
	        		break;
	        	case "ft":
	        		System.out.println(game.printft());
	        		break;
	        	case "id":
	        		System.out.println(game.getID());
	        		break;
	        	case "target":
	        		System.out.println(game.evalTarget(game.getNextTargetPlayer()));
	        		break;
	        	case "board":
	        		System.out.println(game.battleground);
	        		break;
                case "sucboard":
                    System.out.println(game.players.get(game.successor.getNodeID()));
                    break;
	        	default:
	        		System.out.println("Sir, I can't follow this command");
	        		break;
        	}
        }
    	System.out.println("Good Bye Commander!");
        scanner.close();
	}
	/**
	 * sets basic information for open chord and game
	 * @param ownIP
	 * @param ownPort
	 * @param groundSize
	 * @param shipQuantity
	 */
	BattleChord(String ownIP, String ownPort, int groundSize, int shipQuantity){
		de.uniba.wiai.lspi.chord.service.PropertiesLoader.loadPropertyFile();
		protocol = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
		try {
			localURL = new URL(protocol + "://"+ownIP+":"+ownPort+"/");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		chord = new de.uniba.wiai.lspi.chord.service.impl.ChordImpl();
		GameNotifyCallback gameNotifyCallback = new GameNotifyCallback();
        gameNotifyCallback.setBattleChord(this);
		chord.setCallback(gameNotifyCallback);
		this.groundsize = groundSize;
		this.shipQuantity = shipQuantity;
	}
	/**
	 * creates the chord ring
	 */
	private void createAndJoinBattle(){
		try {
			chord.create(localURL);
		} catch (ServiceException e) {
			throw new RuntimeException("Could not create DHT!", e);
		}
	}
	/**
	 * join a chord ring
	 * @param bootstrapIP
	 * @param bootstrapPort
	 */
	private void joinBattle(String bootstrapIP,String bootstrapPort){
		try {
			bootstrapURL = new URL(protocol + "://"+bootstrapIP+":"+bootstrapPort+"/");
			chord.join(localURL, bootstrapURL);
		} catch (MalformedURLException | ServiceException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * leave a chord ring
	 */
	private void leaveBattle(){
		chord.leave();
	}
	/**
	 * init game client, sets known players from ft aswell as suc and pred nodes
	 */
	//NOTE: exceptions not handled when chord ring is empty (chould just happen for chordring create node and no successor is available)
	private void init(){
		List<Node>knownPlayers = chord.getFingerTable();
		this.battleground = new Battleground(chord.getPredecessorID(), chord.getID(), groundsize, shipQuantity);
        this.battleground.setShips();

        //set successor
        this.successor = getSuccessor();
        this.curAttackSuc = getSuccessor().getNodeID();
        //only known Player-Battleground is Successor
        if(successor != null) {
            ID sucID = this.successor.getNodeID();
            this.addPlayerWithSpace(chord.getID(), sucID, groundsize, shipQuantity);
            for (Node node : knownPlayers) {
                if (node.getNodeID().compareTo(sucID) != 0) {
                    this.addPlayer(node.getNodeID(), groundsize, shipQuantity);
                }
            }
        }
	}
	/**
	 * formated string of other known players with ID ships intact
	 * @return formated string
	 */
	private String printPlayers(){
		String result = "ID | Ships Intact\n"
						+ "------------------------------------------------------------\n";
		
		for(Map.Entry<ID, Battleground> entry: players.entrySet()){
			result += (entry.getKey().equals(chord.getID()) ? ">" : "") + entry.getKey()+" | "+ entry.getValue().getShipsIntact()+"\n";
		}
		
		return result;
	}
	/**
	 * formated string of fingertable
	 * @return formated string
	 */
	private String printft() {
		return chord.printFingerTable();
	}
	/**
	 * own node id
	 * @return node id
	 */
	private ID getID(){
		return chord.getID();
	}
	/**
	 * check if own node is highest node
	 * @return true if own node is highest node
	 */
	private boolean ownsHighestKey(){
		boolean result = false;
		if(chord.getID().equals(this.highestKey) || (!chord.getPredecessorID().equals(this.highestKey) && chord.getPredecessorID().compareTo(getID()) == 1) ){
			result = true;
		}

		return result;
	}
	/**
	 * gets successor node from fingertable
	 * @return successor node
	 */
	private Node getSuccessor(){
		Node successor = null;
		List<Node> fingerTable = chord.getSortedUniqueFingerTable();
		if(fingerTable.size() > 0) {
            int i = 0;
            while ((i < fingerTable.size()) && (chord.getID().compareTo(fingerTable.get(i).getNodeID()) > 0)) {
                i++;
            }
            if (i < fingerTable.size()) {
                successor = fingerTable.get(i);
            } else {
                successor = fingerTable.get(0);
            }
        }
		return successor;
	}

    /**
     * attacks player
     * 1. if it is his last ship
     * 2. next player in chord ring with unknown address space
     * 3. player with known address space and least ships left
     */
    public void fire(){
    	ID target;
    	ID critTarget = getCriticalPlayer();
    	if(critTarget != null){
    		target = evalTarget(critTarget);
    	}else if(chordInitPhase){
    		target = getNextTargetByPred(curAttackSuc);
    	} else {
    		target = evalTarget(getNextTargetPlayer());
    	}
        if(target.isInInterval(chord.getPredecessorID(), this.getID())){
        	System.out.println("THIS COULD BE YOUR OWN SHIP! ;___;");
        } else {
    		attacking = true;
    		lastTarget = target;
    		attackTarget(target);
        }
    }
    /**
     * checks if a shoot hit you, logs new information and goes on with firing if game is still running
     * @param target an other player attacked
     */
    public void getShoot(ID target){
        boolean hit = battleground.isHit(target);
        System.out.println("They shoot on us! "+hit);
        battleground.newInformation(target, hit);
        chord.broadcast(target, hit);
        if(!gameover){
        	fire();
        } else {
        	System.out.println("Stop shooting!");
        }
    }
    /**
     * checks if a player is new
     * @param player
     * @return true if he is knew
     */
	private boolean isNewPlayer(ID player){
		return !players.containsKey(player);
	}
	/**
	 * adds new player to the game
	 * @param player
	 * @param groundsize
	 * @param shipQuantity
	 */
	private void addPlayer(ID player, int groundsize, int shipQuantity){
		 players.put(player, new Battleground(player, groundsize, shipQuantity));
	}
	/**
	 * adds new player to the game and inits his address space
	 * @param fromID
	 * @param toID
	 * @param groundsize
	 * @param shipQuantity
	 */
	private void addPlayerWithSpace(ID fromID, ID toID, int groundsize, int shipQuantity){
		Battleground battleground = new Battleground(fromID, toID, groundsize, shipQuantity);
        players.put(toID, battleground);
	}

    /**
     * send retrieve to execute attack
     * @param target to shoot on
     */
	private void attackTarget(ID target){
		System.out.println("BAM!");
		RetrieveThread retrieve= new RetrieveThread(chord, target);
		retrieve.run();
	}
	/**
	 * evaluates a field from a player which was not attacked yet
	 * @param player
	 * @return
	 */
	private ID evalTarget(ID player){
        return players.get(player).getNextTargetField();
	}
	/**
	 * logs new information from broadcast, adds new players and announces if you have one the game
	 * @param source
	 * @param target
	 * @param hit
	 */
    public void newInformation(ID source, ID target, boolean hit){
        System.out.println("New information from: "+source);
        if(chordInitPhase && attacking && target.equals(lastTarget)){
            if(isNewPlayer(source)){
            	addPlayerWithSpace(curAttackSuc,source, groundsize, shipQuantity);
            } else {
            	 players.get(source).setPredecessorID(curAttackSuc);
            }
        	curAttackSuc = source;
        	if(source.equals(chord.getPredecessorID())){
        		System.out.println("-------------INIT OVER-------------");
        		this.chordInitPhase = false;
        	}
        }
        
        if(isNewPlayer(source)){
            System.out.println("Adding player: "+source);
            addPlayer(source, groundsize, shipQuantity);
        }
        players.get(source).newInformation(target, hit);
        if(players.get(source).isGameOver() && attacking && target.equals(lastTarget)){
        	gameover  = true;
            announceVictory(source);
        }
    	this.attacking = false;
    }
    
    /**
     * output for winning the game
     * @param loser who just lost the game
     */
	private void announceVictory(ID loser){
		DateFormat df = new SimpleDateFormat("HH:mm:ss:SSS");
	    Date dateobj = new Date();
	    
		System.out.println(
				"------------------------------------------------------------------\n"
				+ "VIIICTORY!!!"+df.format(dateobj)+"\n"
				+ "You have fought well commander! It is an honor to serve you.\n"
				+ "Following fleet has been destroyed:\n\t"+loser+"\n"
				+ "Last hit information was:\n\t"+chord.getTransactionId()+"\n"
				+"-----------------------------------------------------------------"
		);
	}
	
	/**
	 * gets ID to attack from address space of a known node
	 * (the predecessor oft the know we like to know the address space from to init him)
	 * @param pred
	 * @return target to attack
	 */
	private ID getNextTargetByPred(ID pred){
		ID result;
    	if(pred.equals(this.highestKey)) {
    		result = ID.valueOf(BigInteger.ZERO);
    	} else {
    		//double add plus one because ID.isInInterval(from,to) does not work when from is interval border
    		result = ID.valueOf(pred.toBigInteger().add(BigInteger.ONE).add(BigInteger.ONE));
    	}
    	return result;
	}
	
	/**
	 * gets player with least ships intact
	 * @return player to attack
	 */
	private ID getNextTargetPlayer(){
		int playerShips;
        int leastShipsIntact = this.shipQuantity +1;
        ID nextTargetPlayer = null;
        for(ID playerID : players.keySet()){
            playerShips = players.get(playerID).getShipsIntact();
            if((playerShips > 0) && (playerShips < leastShipsIntact)){
                leastShipsIntact = playerShips;
                nextTargetPlayer = playerID;
            }
        }
        return nextTargetPlayer;
	}
	
	/**
	 * checks if a player is in a critical situation (just 1 ship left)
	 * @return player to attack or null if no critical player exists
	 */
	private ID getCriticalPlayer(){
		int playerShips;
        ID critPlayer = null;
        for(ID playerID : players.keySet()){
            playerShips = players.get(playerID).getShipsIntact();
            if(playerShips == 1){
            	critPlayer = playerID;
            }
        }
        return critPlayer;
	}
}
