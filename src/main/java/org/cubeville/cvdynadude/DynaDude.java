package org.cubeville.cvdynadude;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionData;
import org.bukkit.inventory.Inventory;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.Listener;

import org.cubeville.cvgames.models.Game;
import org.cubeville.cvgames.models.GameRegion;
import org.cubeville.cvgames.vartypes.GameVariableList;
import org.cubeville.cvgames.vartypes.GameVariableLocation;
import org.cubeville.cvgames.vartypes.GameVariableInt;
import org.cubeville.cvgames.vartypes.GameVariableMaterial;
import org.cubeville.cvgames.vartypes.GameVariableString;

// TODO: No winning
// TODO: Barriers, otherwise players can hop and place tnt on other side
//       and also can climb on pile

public class DynaDude extends Game implements Listener
{
    Random rnd = new Random();

    int speedChance = 3;
    int powerChance = 4;
    int countChance = 3;

    int initialTntCount = 1;
    int initialFuseTime = 100;
    int initialExplosionPower = 3;

    public DynaDude(String id, String arenaName) {
        super(id, arenaName);

        addGameVariable("spawn", new GameVariableList<>(GameVariableLocation.class));
        addGameVariable("exit", new GameVariableLocation());
        addGameVariable("countdown-length", new GameVariableInt());
        addGameVariable("materials-breakable", new GameVariableList<>(GameVariableMaterial.class));
        addGameVariable("materials-transparent", new GameVariableList<>(GameVariableMaterial.class));
        addGameVariable("chance-power", new GameVariableInt());
        addGameVariable("chance-speed", new GameVariableInt());
        addGameVariable("chance-count", new GameVariableInt());
        addGameVariable("initial-count", new GameVariableInt());
        addGameVariable("initial-power", new GameVariableInt());
        addGameVariable("restore-arena-cmd", new GameVariableString());
        addGameVariable("message-portal", new GameVariableString());
    }

    @Override
    protected DynaDudeState getState(Player p) {
        if(state.get(p) == null) return null;
        return (DynaDudeState) state.get(p);
    }

    @Override
    public void onCountdown(int Counter) {
    }

    @Override
    public void onPlayerLeave(Player player) {
        state.remove(player);
        if(state.size() < 2) finishGame();
    }

    @Override
    public void onGameFinish() {

    }
    
    @Override
    public void onGameStart(Set<Player> players) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), (String) getVariable("restore-arena-cmd"));
        
        speedChance = (Integer) getVariable("chance-speed");
        powerChance = (Integer) getVariable("chance-power");
        countChance = (Integer) getVariable("chance-count");

        initialTntCount = (Integer) getVariable("initial-count");
        initialExplosionPower = (Integer) getVariable("initial-power");
        
        List<Location> spawns = (List<Location>) getVariable("spawn");

        int pcount = 0;
        for(Player player: players) {
            state.put(player, new DynaDudeState(initialTntCount, initialFuseTime, initialExplosionPower));
            player.teleport(spawns.get(pcount++));
            player.getInventory().setItem(0, new ItemStack(Material.TNT, initialTntCount));
            player.getInventory().setItem(7, getPotion(PotionType.SPEED, 1));
            player.getInventory().setItem(8, getPotion(PotionType.STRENGTH, initialExplosionPower));
        }

    }

    private void message(String title, String subtitle) {
        String portalName = (String) getVariable("message-portal");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cvportal sendtitle " + portalName + " \"" + title + "\" \"" + subtitle + "\" 20 40 20");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cvportal sendmessage " + portalName + " \"" + title + " &r- " + subtitle + "\"");
    }
    
    class Splody {
        Splody(int startX, int startY, int startZ, int duration, int xdir, int zdir) {
            x = startX;
            y = startY;
            z = startZ;
            remain = duration;
            this.xdir = xdir;
            ydir = 0;
            this.zdir = zdir;
        }

        void step() {
            x += xdir;
            y += ydir;
            z += zdir;
            remain--;
        }

        void stop() {
            remain = 0;
        }
        
        public int x, xdir;
        public int y, ydir;
        public int z, zdir;
        public int remain;
    }

    private double horizontalDistance(Location loc1, double x2, double z2) {
        double xd = loc1.getX() - x2;
        double zd = loc1.getZ() - z2;
        return Math.sqrt(xd * xd + zd * zd);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if(event.isCancelled()) return;
        if(event.getBlock().getType() != Material.TNT) return;

        DynaDudeState st = getState(event.getPlayer());
        if(st == null) return;

        Location loc = event.getBlock().getLocation();
        event.getBlock().setType(Material.AIR);
        TNTPrimed tnt = (TNTPrimed) loc.getWorld().spawnEntity(new Location(loc.getWorld(), loc.getX() + 0.5, loc.getY(), loc.getZ() + 0.5), EntityType.PRIMED_TNT);
        tnt.setFuseTicks(st.fuseTime);
        st.ownedTnt.add(tnt);
    }

    private Player getTntOwnerAndRemoveOwnership(TNTPrimed tnt) {
        for(Player player: state.keySet()) {
            DynaDudeState st = getState(player);
            if(st.ownedTnt.contains(tnt)) {
                st.ownedTnt.remove(tnt);
                return player;
            }
        }
        return null;
    }

    private int countItems(Inventory inventory, Material material)
    {
        Map<Integer, ? extends ItemStack> items = inventory.all(material);
        int totalCount = 0;
        for(int slot: items.keySet()) {
            totalCount += items.get(slot).getAmount();
        }
        return totalCount;
    }

    private ItemStack getPotion(PotionType type, int count)
    {
        ItemStack item = new ItemStack(Material.POTION, count);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionData(new PotionData(type));
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if(event.isCancelled()) return;
        if(event.getEntityType() != EntityType.PRIMED_TNT) return;

        GameRegion gameRegion = (GameRegion) getVariable("region");
        if(!gameRegion.containsEntity(event.getEntity())) return;

        event.setCancelled(true);

        Player player = getTntOwnerAndRemoveOwnership((TNTPrimed) event.getEntity());
        DynaDudeState st = getState(player);

        int splodyPower = 3;
        if(st != null) {
            splodyPower = st.explosionPower;
        }

        Location loc = event.getLocation();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        int c = 12;
        Splody[] splodies = new Splody[c];
        // xdir = -1
        splodies[0] = new Splody(x, y, z, splodyPower, -1, 0);
        splodies[1] = new Splody(x, y, z - 1, splodyPower, -1, 0);
        splodies[2] = new Splody(x, y, z + 1, splodyPower, -1, 0);
        // xdir = 1
        splodies[3] = new Splody(x, y, z, splodyPower, 1, 0);
        splodies[4] = new Splody(x, y, z - 1, splodyPower, 1, 0);
        splodies[5] = new Splody(x, y, z + 1, splodyPower, 1, 0);
        // zdir = -1
        splodies[6] = new Splody(x, y, z, splodyPower, 0, -1);
        splodies[7] = new Splody(x - 1, y, z, splodyPower, 0, -1);
        splodies[8] = new Splody(x + 1, y, z, splodyPower, 0, -1);
        // zdir = 1
        splodies[9] = new Splody(x, y, z, splodyPower, 0, 1);
        splodies[10] = new Splody(x - 1, y, z, splodyPower, 0, 1);
        splodies[11] = new Splody(x + 1, y, z, splodyPower, 0, 1);

        loc.getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 15.0f, 1.0f);

        List<Material> breakableMaterials = (List<Material>) getVariable("materials-breakable");
        List<Material> transparentMaterials = (List<Material>) getVariable("materials-transparent");

        int brokenBlocks = 0;
        
        while(true) {

            int remainingCount = 0;

            for(int i = 0; i < c; i++) {

                if(splodies[i].remain == 0) continue;

                remainingCount++;
                splodies[i].step();

                int bx = splodies[i].x;
                int by = splodies[i].y;
                int bz = splodies[i].z;

                {
                    List<Player> playersToRemove = new ArrayList<>();
                    
                    for(Player p: state.keySet()) {
                        if(horizontalDistance(p.getLocation(), bx + 0.5, bz + 0.5) < 0.5) {
                            playersToRemove.add(p);
                        }
                    }

                    for(Player p: playersToRemove) {
                        p.setHealth(0); // TODO: deleting in an iteration
                        p.getInventory().clear();
                        state.remove(p);
                    }
                }
                

                loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, bx + 0.5, by + 0.5, bz + 0.5, 0);
                loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, bx + 0.5, by + 1.5, bz + 0.5, 0);

                for(int yoffset = 0; yoffset <= 1; yoffset++) {
                    Block block = loc.getWorld().getBlockAt(bx, by + yoffset, bz);
                    if(block.getType() != Material.AIR) {
                        if(breakableMaterials.contains(block.getType())) {
                            block.setType(Material.AIR);
                            brokenBlocks++;
                            splodies[i].stop();
                        }
                        else if(!transparentMaterials.contains(block.getType())) {
                            splodies[i].stop();
                        }
                    }
                }


            }

            if(remainingCount == 0) break;
        }

        if(state.size() < 2) {
            if(state.size() == 1) {
                for(Player p: state.keySet())
                    message("&aGame Over", "&e" + p.getName() + " &rwon DynaDude!");
            }
            else {
                message("&aGame Over", "Nobody won.");
            }
            finishGame();
            return;
        }

        st = getState(player);
        if(st != null) {
            Inventory inventory = player.getInventory();

            int speed = 0;
            int power = 0;
            int count = 0;

            for(int b = 0; b < brokenBlocks; b++) {
                if(rnd.nextInt(100) <= speedChance) speed++;
                if(rnd.nextInt(100) <= powerChance) power++;
                if(rnd.nextInt(100) <= countChance) count++;
            }

            if(speed > 0) {
                st.fuseTime -= speed * 10;
                if(st.fuseTime < 20) st.fuseTime = 20;
                player.getInventory().setItem(7, getPotion(PotionType.SPEED, 11 - st.fuseTime / 10));
            }

            if(power > 0) {
                st.explosionPower += power;
                if(st.explosionPower > 10) st.explosionPower = 10;
                player.getInventory().setItem(8, getPotion(PotionType.STRENGTH, st.explosionPower));
            }
            
            st.tntCount += count;
            if(st.tntCount > 10) st.tntCount = 10;

            int tntCount = countItems(inventory, Material.TNT);
            int targetTntCount = st.tntCount - st.ownedTnt.size();
            int slot0count = 0;
            if(inventory.getItem(0) != null && inventory.getItem(0).getType() == Material.TNT) {
                slot0count = inventory.getItem(0).getAmount();
            }
            int otherSlotsCount = tntCount - slot0count;
            int slot0TargetCount = targetTntCount - otherSlotsCount;
            if(slot0TargetCount == 0)
                inventory.setItem(0, new ItemStack(Material.AIR));
            else
                inventory.setItem(0, new ItemStack(Material.TNT, slot0TargetCount));
        }
    }
}
