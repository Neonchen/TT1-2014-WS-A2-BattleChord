package de.haw.battlechord;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;

public class GameNotifyCallback implements NotifyCallback{

    BattleChord battleChord;

    public void setBattleChord(BattleChord battleChord){
        this.battleChord = battleChord;
    }

	@Override
	public void retrieved(ID target) {
		System.out.println("NotifyCallback~retrieved: "+"Target:"+target);
		battleChord.getShoot(target);
	}

	@Override
	public void broadcast(ID source, ID target, Boolean hit) {
		System.out.println("NotifyCallback~broadcast: "+"Source:"+source+" | Boolean: "+hit);
		battleChord.newInformation(source, target, hit);
	}
}
