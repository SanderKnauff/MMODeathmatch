/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.imine.mmodeathmatch;

import java.util.ArrayList;
import java.util.List;
import nl.imine.mmodeathmatch.util.Lang;
import nl.imine.mmodeathmatch.util.MissionState;
import nl.makertim.MMOmain.MKTEventHandler;
import nl.makertim.MMOmain.PlayerStats;
import nl.makertim.MMOmain.Refrence;
import nl.makertim.MMOmain.lib.MMOOutlaws;
import nl.makertim.MMOmain.lib.Mission;
import nl.makertim.MMOmain.lib.MissionLocation;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/**
 *
 * @author Sander
 */
public class Deathmatch extends Mission {

    private static final int END_KILLS = 50;

    private final Material missionIcon = Material.IRON_SWORD;

    private final List<Player> inGamePlayers = new ArrayList<>();

    private int outlawDeaths, sheriffDeaths;
    private int outlawEnd, sheriffEnd;

    private int levelTimeing = 30, secondRemain = 360;

    private boolean joinable, inLobby, cooldown;
    public short gameStage;

    public Deathmatch() {
        this.scoreAdd.add(String.format("Outlaws: %d", sheriffDeaths));
        this.scoreAdd.add(String.format("Sheriffs: %d", outlawDeaths));
    }

    @Override
    public void tick(int i) {
        super.timer = this.levelTimeing;
        super.tick(i);
        calculateRequiredKills();
        if (i == 1) {
            if (!inGame && !inLobby) {
                inLobby = true;
            }
            updateScoreboard();
            doLobbyCheck();
            //Run het spel
            if (inGame) {
                for (Player outlaw : teamA) {
                    outlaw.getInventory().setItem(7, Refrence.customIS(Material.NAME_TAG, sheriffDeaths, "Sheriffs Silenced", null, null));
                    outlaw.getInventory().setItem(8, Refrence.customIS(Material.COMPASS, 1, "Objective location", new String[]{"Look there! A sheriff!"}, null));
                }
                for (Player popo : teamB) {
                    popo.getInventory().setItem(7, Refrence.customIS(Material.NAME_TAG, outlawDeaths, "Outlaws Incapitated", null, null));
                    popo.getInventory().setItem(8, Refrence.customIS(Material.COMPASS, 1, "Objective location", new String[]{"Heads up! Crooks that way!"}, null));
                }
                /*for (Player tempOutlaw : teamA) {
                    Player closest = null;
                    for (Player tempSheriff : teamB) {
                        if (closest == null) {
                            closest = tempSheriff;
                        }
                        if (tempOutlaw.getLocation().distance(closest.getLocation()) < tempOutlaw.getLocation().distance(tempSheriff.getLocation())) {
                            tempOutlaw.setCompassTarget(closest.getLocation());
                        }
                    }
                }
                for (Player tempSheriff : teamB) {
                    Player closest = null;
                    for (Player tempOutlaw : teamA) {
                        if (closest == null) {
                            closest = tempOutlaw;
                        }
                        if (tempSheriff.getLocation().distance(closest.getLocation()) < tempSheriff.getLocation().distance(tempOutlaw.getLocation())) {
                            tempSheriff.setCompassTarget(closest.getLocation());
                        }
                    }
                }*/
                if (secondRemain <= -1) {
                    stop();
                }
                cooldown = false;
            }
        }
    }

    @Override
    public boolean inLobby() {
        return (gameStage == MissionState.IN_LOBBY);
    }

    @Override
    public void joinPlayer(PlayerStats pls) {
        super.joinPlayer(pls);
        Player player = pls.getPlayer();
        pls.isInMission = true;
        inGamePlayers.add(player);
        MKTEventHandler.cleanupPlayerInventory(player);
        player.getInventory().setItem(8, Refrence.customIS(Material.COMPASS, 1, "Direction", null, null));
        if (pls.isOutlaw) {
            teamA.add(player);
            sendMessage(true, pls.pl.playerName + Lang.JOIN_OUTLAW);
        } else {
            teamB.add(player);
            sendMessage(false, pls.pl.playerName + Lang.JOIN_SHERIFF);
        }
        super.showMissionPeople();
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent e) {
        if (inGamePlayers.contains(e.getPlayer())) {
            leavePlayer(PlayerStats.getPlayerStats(e.getPlayer()));
        }
    }

    @Override
    public void onDeath(Player pl, Entity damager) {
        if (teamA.contains(pl)) {
            outlawDeaths++;
            checkEnd();
        } else {
            sheriffDeaths++;
            checkEnd();
        }
        this.scoreAdd.clear();
        this.scoreAdd.add(String.format("Outlaws: %d", sheriffDeaths));
        this.scoreAdd.add(String.format("Sheriffs: %d", outlawDeaths));
        if (damager instanceof Player) {
            sendMessage(null, pl.getName() + " killed by " + ((Player) damager).getName());
            this.reward((Player) damager, 64);
        } else {
            sendMessage(null, pl.getName() + " killed in action");
        }
    }

    @Override
    public void leavePlayer(PlayerStats pls) {
        super.leavePlayer(pls);
        Player player = pls.getPlayer();
        pls.isInMission = false;
        inGamePlayers.remove(player);
        MKTEventHandler.cleanupPlayerInventory(player);
        player.getInventory().setItem(8, Refrence.customIS(Material.COMPASS, 1, "Direction", null, null));
        if (pls.isOutlaw) {
            teamA.remove(player);
        } else {
            teamB.remove(player);
        }
        super.showPeople(player);
    }

    @Override
    public List<Player> getAllPlayers() {
        return this.inGamePlayers;
    }

    @Override
    public MissionLocation getLocation(boolean isOutlaw
    ) {
        if (isOutlaw) {
            return new MissionLocation(new Location(Bukkit.getWorlds().get(0), 345, 74, 39), 5);
        } else {
            return new MissionLocation(new Location(Bukkit.getWorlds().get(0), 420, 69, 37), 5);
        }
    }

    @Override
    public void registerEvents(PluginManager pm, Plugin plugin
    ) {
        pm.registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return Lang.MISSION_NAME;
    }

    public void sendMessage(boolean outlaws, String message) {
        if (outlaws) {
            for (Player pl : this.teamA) {
                MMOOutlaws.sendActionMessage(pl, ChatColor.GOLD + message);
            }
        } else {
            for (Player pl : this.teamB) {
                MMOOutlaws.sendActionMessage(pl, ChatColor.GOLD + message);
            }
        }
    }

    private void checkEnd() {
        boolean outlawWin = false, teamToSmall = false, sheriffWin = false;
        if (sheriffDeaths >= END_KILLS) {
            secondRemain = 0;
            outlawWin = true;
            for (Player p : teamA) {
                this.reward(p, 1024);
            }
        } else if (outlawDeaths >= END_KILLS) {
            secondRemain = 0;
            sheriffWin = true;
            for (Player p : teamB) {
                this.reward(p, 1024);
            }
        } else if (teamA.size() < minPlayers || teamB.size() < minPlayers) {
            secondRemain = 0;
            teamToSmall = true;
        }
        if (secondRemain-- <= 0) {  //MAKERTIM WHY
            if (teamToSmall) {
                sendTitle(true, "Mission disturbed", null);
                sendTitle(false, "Mission disturbed", null);
                if (outlawWin) {
                    sendMessage(true, "Mission completed: The sheriffs are either dead or running");
                    sendMessage(false, "Mission failed: The outlaw have outnumberd you");
                } else if (sheriffWin) {
                    sendMessage(true, "Mission completed: The outlaws are in cutody now");
                    sendMessage(false, "Mission failed: The your friends are dead or in prison now");
                }
                stop();
            }
        }
    }

    private void checkWin() {
        if (outlawDeaths >= END_KILLS) {
            for (Player p : teamA) {
                this.reward(p, 1024);
            }
            for (Player p : teamB) {
                this.reward(p, 512);
            }
            this.stop();
        } else if (sheriffDeaths >= END_KILLS) {
            for (Player p : teamB) {
                this.reward(p, 1024);
            }
            for (Player p : teamA) {
                this.reward(p, 512);
            }
            this.stop();
        }
    }

    @Override
    public void stop() {
        super.stop();
        secondRemain = 240;
        levelTimeing = 30;
        outlawDeaths = 0;
        sheriffDeaths = 0;
    }

    private void calculateRequiredKills() {
        outlawEnd = teamA.size() * 10;
        sheriffEnd = teamB.size() * 10;
    }

    private void updateScoreboard() {
        for (Player pl : this.inGamePlayers) {
            PlayerStats pls = PlayerStats.getPlayerStats(pl);
            ArrayList<String> score = new ArrayList<>();
            score.add(ChatColor.GOLD.toString() + ChatColor.BOLD + "Mission");
            score.add(this.getName());
            score.add("");
            score.add(ChatColor.RED.toString() + ChatColor.BOLD + "Outlaws");
            for (Player outlaw : teamA) {
                score.add(outlaw.getName());
            }
            for (int j = 0; j < minPlayers - teamA.size(); j++) {
                score.add("Empty" + StringUtils.repeat(" ", j));
            }
            score.add(" ");
            score.add(ChatColor.BLUE.toString() + ChatColor.BOLD + "Sherrifs");
            for (Player popo : teamB) {
                score.add(popo.getName());
            }
            if (inLobby) {
                score.add("  ");
                score.add(ChatColor.GREEN.toString() + ChatColor.BOLD + "Time ");
                score.add(Integer.toString(levelTimeing));
            } else if (inGame) {
                score.add("   ");
                score.add(ChatColor.GREEN.toString() + ChatColor.BOLD + "Time ");
                score.add(Integer.toString(secondRemain));
            }
        }
    }

    private void doLobbyCheck() {
        if (inLobby) {
            //TP terug naar lobby
            for (Player outlaw : teamA) {
                if (!getLocation(true).isInsideRange(outlaw.getLocation())) {
                    for (int j = 0; j < 10; j++) {
                        outlaw.getWorld().playEffect(outlaw.getLocation(), Effect.ENDER_SIGNAL, j);
                    }
                    outlaw.playSound(outlaw.getLocation(), Sound.ENDERMAN_TELEPORT, 1F, 1F);
                    outlaw.teleport(getLocation(true).getRandomLocation());
                    outlaw.getInventory().setItem(8, Refrence.slot8i);
                }
            }
            for (Player popo : teamB) {
                if (!getLocation(false).isInsideRange(popo.getLocation())) {
                    for (int j = 0; j < 10; j++) {
                        popo.getWorld().playEffect(popo.getLocation(), Effect.ENDER_SIGNAL, j);
                    }
                    popo.playSound(popo.getLocation(), Sound.ENDERMAN_TELEPORT, 1F, 1F);
                    popo.teleport(getLocation(false).getRandomLocation());
                    popo.getInventory().setItem(8, Refrence.slot8i);
                }
            }
            if ((teamA.size() >= minPlayers && teamB.size() >= minPlayers) && levelTimeing-- <= 0) {
                //Spel begint
                inGame = true;
                inLobby = false;
                state = 0;
                levelTimeing = 0;
                super.timer = secondRemain;
            }
        }
    }
}
