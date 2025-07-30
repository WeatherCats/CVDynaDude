package org.cubeville.cvdynadude;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.TNTPrimed;
import org.cubeville.cvgames.models.PlayerState;

public class DynaDudeState extends PlayerState {
    DynaDudeState(int tntCount, int fuseTime, int explosionPower) {
        this.fuseTime = fuseTime;
        this.explosionPower = explosionPower;
        this.tntCount = tntCount;
    }
    
    public int fuseTime;
    public int explosionPower;
    public int tntCount;
    public boolean isAlive = true;
    public int placement = -1;
    public int kills = 0;
    public Set<TNTPrimed> ownedTnt = new HashSet<>();
    
    @Override
    public int getSortingValue() {
        return 0;
    }
}
