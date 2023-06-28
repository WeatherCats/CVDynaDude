package org.cubeville.cvdynadude;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.cubeville.cvgames.CVGames;

public class CVDynaDude extends JavaPlugin implements Listener {

    static private CVDynaDude instance;
    static CVDynaDude getInstance() { return instance; }
    
    public void onEnable() {
        instance = this;
        CVGames.gameManager().registerGame("dynadude", DynaDude::new);
    }
}
