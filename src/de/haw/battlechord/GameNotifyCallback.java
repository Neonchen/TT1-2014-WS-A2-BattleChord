package de.haw.battlechord;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;

public class GameNotifyCallback implements NotifyCallback{
	@Override
	public void retrieved(ID target) {
		System.out.println("NotifyCallback~retrieved: "+"Target:"+target);
		
	}

	@Override
	public void broadcast(ID source, ID target, Boolean hit) {
		System.out.println("NotifyCallback~broadcast: "+"Source:"+source+" | Boolean: "+hit);
		
	}
}
