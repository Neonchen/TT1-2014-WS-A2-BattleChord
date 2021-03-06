package de.haw.battlechord;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;
import de.uniba.wiai.lspi.chord.service.ServiceException;

public class JoinChord {
	public static void main(String[] args) throws UnknownHostException, ServiceException {
		de.uniba.wiai.lspi.chord.service.PropertiesLoader.loadPropertyFile();
		String protocol = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
		URL localURL = null;
		try {
//			localURL = new URL(protocol + "://192.168.1.1:8181/");
			localURL = new URL(protocol + "://localhost:8181/");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		URL bootstrapURL = null;
		try {
//			bootstrapURL = new URL(protocol + "://192.168.1.2:8080/");
			bootstrapURL = new URL(protocol + "://localhost:8080/");
			System.out.println(localURL);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		Chord chord = new de.uniba.wiai.lspi.chord.service.impl.ChordImpl();
		
		NotifyCallback gameNotifyCallback = new GameNotifyCallback();
		chord.setCallback(gameNotifyCallback);
				
		try {
			chord.join(localURL, bootstrapURL);
		} catch (ServiceException e) {
			throw new RuntimeException("Could not join DHT!", e);
		}
		String data = "Just an example .";
		StringKey myKey = new StringKey ("123");
		try{

			chord.insert (myKey , data);
		} catch( ServiceException e){
		// handle exception
		
		}
		System.out.println(chord.retrieve(new StringKey("123")));
		chord.leave();
	}
}
