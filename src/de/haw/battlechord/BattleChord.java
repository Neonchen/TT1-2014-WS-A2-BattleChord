package de.haw.battlechord;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import de.uniba.wiai.lspi.chord.com.Node;
import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class BattleChord {
	
	URL localURL;
	URL bootstrapURL;
	ChordImpl chord; //Chord interface limits functions too much
	String protocol;
	
	int groundsize = 0;
	int shipQuantity = 0;
	
	Map<ID,Battleground> players = new HashMap<ID,Battleground>();
	
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
		 
		 
		 System.out.println("Use default properties? (y/n)");
         String useDefProp = scanner.next();
         
		 String ownIP = "";
		 String ownPort = "";
		 int playerQuantity = 0;
		 int groundSize = 0;
		 int shipQuantity = 0;
		 String mode = "";
		 String bootstrapIP = "";
		 String bootstrapPort = "";
         
         if(useDefProp.equals("y")){
        	 Properties prop = new Properties();
    		 try {
    			prop.load(new FileInputStream("src/battlechord.properties"));
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    		 ownIP = prop.getProperty("ownIP");
    		 ownPort = prop.getProperty("ownPort");
    		 playerQuantity= Integer.parseInt(prop.getProperty("playerQuantity"));
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
             System.out.println("Enter Player Quantity:");
             playerQuantity = scanner.nextInt();
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
         		+ "\nPlayer Quantity: "+playerQuantity
         		+ "\nBattleground Size: "+groundSize
         		+ "\nShip Quantity: "+shipQuantity
         		+ "\nMode: "+mode
         		+ "\nBootstrap IP: "+bootstrapIP
         		+ "\nBootstrap PORT: "+bootstrapPort
         		+ "\n--------------------"
         );

    	BattleChord game = new BattleChord(ownIP,ownPort,playerQuantity,groundSize,shipQuantity);
         
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
	        		break;
	        	case "quit":
	        		game.leaveBattle();
	        		run = false;
	        		break;
	        	case "status":
	        		System.out.println(game.printPlayers());
	        		break;
	        	default:
	        		System.out.println("Sir, I can't follow this command");
	        		break;
        	}
        }
    	System.out.println("Good Bye Commander!");
        scanner.close();
	}
	
	BattleChord(String ownIP, String ownPort, int playerQuantity, int groundSize, int shipQuantity){
		de.uniba.wiai.lspi.chord.service.PropertiesLoader.loadPropertyFile();
		protocol = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
		try {
			localURL = new URL(protocol + "://"+ownIP+":"+ownPort+"/");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		chord = new de.uniba.wiai.lspi.chord.service.impl.ChordImpl();
		NotifyCallback gameNotifyCallback = new GameNotifyCallback();
		chord.setCallback(gameNotifyCallback);
	}
	
	private void createAndJoinBattle(){
		try {
			chord.create(localURL);
		} catch (ServiceException e) {
			throw new RuntimeException("Could not create DHT!", e);
		}
	}
	
	private void joinBattle(String bootstrapIP,String bootstrapPort){
		try {
			bootstrapURL = new URL(protocol + "://"+bootstrapIP+":"+bootstrapPort+"/");
			chord.join(localURL, bootstrapURL);
		} catch (MalformedURLException | ServiceException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void leaveBattle(){
		chord.leave();
	}
	
	private void init(){
		List<Node>knownPlayers = chord.getFingerTable();
		
		players.put(chord.getID(), new Battleground(chord.getID(), groundsize, shipQuantity));
		
		for(Node node : knownPlayers ){
			this.addPlayer(node.getNodeID(), groundsize, shipQuantity);
		}
	}
	
	private String printPlayers(){
		String result = "ID | Ships Intact\n"
						+ "------------------------------------------------------------\n";
		
		for(Map.Entry<ID, Battleground> entry: players.entrySet()){
			result += (entry.getKey().equals(chord.getID()) ? ">" : "") + entry.getKey()+" | "+ entry.getValue().getShipsIntact();
		}
		
		return result;
	}

	private void isNewPlayer(ID player){
		
	}
		
	private void addPlayer(ID player, int groundsize, int shipQuantity){
		 players.put(player, new Battleground(player, groundsize, shipQuantity));
	}
	
	private void attackTarget(){
		
	}
	
	private void evalAttack(){
		
	}
	
	private void announceVictory(){
		
	}
	
	private ID getNextTargetPlayer(){
		return null;
	}
}
