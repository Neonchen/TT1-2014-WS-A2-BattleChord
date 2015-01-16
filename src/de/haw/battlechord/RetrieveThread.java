package de.haw.battlechord;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.ServiceException;

public class RetrieveThread extends Thread{
	
	private ID target;
	private Chord chord;

	RetrieveThread(Chord chord, ID target){
		this.chord = chord;
		this.target = target;
	}
	
	 public void run() {
		 synchronized (this) {
			try {
				this.wait(100);
				chord.retrieve(target);
			} catch (ServiceException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }
	    }
}
