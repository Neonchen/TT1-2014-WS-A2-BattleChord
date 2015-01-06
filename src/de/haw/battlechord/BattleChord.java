package de.haw.battlechord;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.Scanner;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;
import de.uniba.wiai.lspi.chord.service.ServiceException;

public class BattleChord {
	
	URL localURL;
	URL bootstrapURL;
	Chord chord;
	String protocol;
	
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
		try {
			chord.leave();
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}
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
