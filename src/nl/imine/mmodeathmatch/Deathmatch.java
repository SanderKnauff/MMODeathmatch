/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.imine.mmodeathmatch;

import nl.imine.mmodeathmatch.util.Lang;
import nl.makertim.MMOmain.PlayerStats;
import nl.makertim.MMOmain.Refrence;
import nl.makertim.MMOmain.lib.MMOOutlaws;
import nl.makertim.MMOmain.lib.MissionLocation;
import nl.makertim.MMOmain.mission.MissionEventHandler;
import nl.makertim.MMOmain.mission.MissionStage;
import nl.makertim.MMOmain.mission.MissionStatus;
import nl.makertim.MMOmain.mission.MissionTeam;
import nl.makertim.MMOmain.mission.TeamMission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/**
 *
 * @author Sander
 */
public class Deathmatch extends TeamMission {

    private static final int END_KILLS = 50;

    private final Material missionIcon = Material.IRON_SWORD;

    private int outlawDeaths, sheriffDeaths;
    private int outlawEnd, sheriffEnd;

    private int levelTiming = 30, secondRemain = 600;

    private boolean joinable, cooldown;
    public short gameStage;

    public Deathmatch() {
        this.setStage("", "", "", MissionStage.NO_STAGE);
        this.status = MissionStatus.IN_LOBBY;
    }

    @Override
    protected void onStart() {

    }

    @Override
    protected void onStop() {

    }

    @Override
    public void tick(int i) {
        if (i == 0) {
            sendDefaultScoreboard((this.getStage().equals(MissionStage.NO_STAGE) ? levelTiming : secondRemain));
            if (status.equals(MissionStatus.IN_LOBBY)) {
                doLobbyCheck();
            }
            calculateRequiredKills();
            updateScoreboard();
            doIngameCheck();
        }
    }

    @Override
    public MissionEventHandler onJoin(PlayerStats ps) {
        return MissionEventHandler.NEXT_TRUE;
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent e) {
        if (super.getAllPlayers().contains(e.getPlayer())) {
            leavePlayer(PlayerStats.getPlayerStats(e.getPlayer()));
            checkEnd();
        }
    }

    @Override
    public MissionEventHandler onHurt(Player p, Entity cause) {
        return MissionEventHandler.NEXT_TRUE;
    }

    @Override
    public void onRespawn(Player pl) {
        if (teamA.contains(pl)) {
            outlawDeaths++;
            checkEnd();
        } else {
            sheriffDeaths++;
            checkEnd();
        }
        if (pl.getKiller() != null) {
            this.reward(pl.getKiller(), 64);
        }
    }

    @Override
    public void removePlayer(PlayerStats pls) {
        super.removePlayer(pls);
        if (pls.isOutlaw) {
            if (teamA.isEmpty()) {
                stop();
            }
        } else {
            if (teamB.isEmpty()) {
                stop();
            }
        }
    }

    @Override
    public MissionLocation getLobbyLocation(PlayerStats ps) {
        if (ps.isOutlaw) {
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
        return Lang.MISSION_ID;
    }

    private void checkEnd() {
        boolean outlawWin = false, teamToSmall = false, sheriffWin = false;
        if (sheriffDeaths >= outlawEnd) {
            secondRemain = 0;
            outlawWin = true;
            for (Player p : teamA) {
                this.reward(p, 1024);
            }
            sendTitle(MissionTeam.OUTLAWS, Lang.MISSION_WIN, Lang.ENDMESSAGE_OUTLAW_WIN);
            sendTitle(MissionTeam.SHERIFFS, Lang.MISSION_FAIL, Lang.ENDMESSAGE_SHERIFF_LOSE);
            stop();
        } else if (outlawDeaths >= sheriffEnd) {
            secondRemain = 0;
            sheriffWin = true;
            for (Player p : teamB) {
                this.reward(p, 1024);
            }
            sendTitle(MissionTeam.OUTLAWS, Lang.MISSION_WIN, Lang.ENDMESSAGE_SHERIFF_WIN);
            sendTitle(MissionTeam.SHERIFFS, Lang.MISSION_FAIL, Lang.ENDMESSAGE_OUTLAW_LOSE);
            stop();
        } else if (teamA.size() < minPlayers || teamB.size() < minPlayers) {
            secondRemain = 0;
            teamToSmall = true;
        }
        if (secondRemain <= 0) {  //MAKERTIM WHY
            if (teamToSmall) {
                sendTitle(MissionTeam.ALL, Lang.MISSION_DRAW, null);
                if (outlawWin) {
                    sendTitle(MissionTeam.OUTLAWS, Lang.MISSION_WIN, Lang.ENDMESSAGE_OUTLAW_WIN);
                    sendTitle(MissionTeam.SHERIFFS, Lang.MISSION_FAIL, Lang.ENDMESSAGE_SHERIFF_LOSE);
                } else if (sheriffWin) {
                    sendTitle(MissionTeam.SHERIFFS, Lang.MISSION_WIN, Lang.ENDMESSAGE_SHERIFF_WIN);
                    sendTitle(MissionTeam.OUTLAWS, Lang.MISSION_FAIL, Lang.ENDMESSAGE_OUTLAW_LOSE);
                } else {
                    sendTitle(MissionTeam.ALL, Lang.MISSION_DRAW, Lang.ENDMESSAGE_DRAW);
                }
                stop();
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.setStage(null, null, null, MissionStage.NO_STAGE);
        this.status = MissionStatus.IN_LOBBY;
        secondRemain = 600;
        levelTiming = 30;
        outlawDeaths = 0;
        sheriffDeaths = 0;
    }

    private void calculateRequiredKills() {
        outlawEnd = teamA.size() * 10;
        sheriffEnd = teamB.size() * 10;
    }

    private void updateScoreboard() {
        this.teamASuffix = String.format(": %d", sheriffDeaths);
        this.teamBSuffix = String.format(": %d", outlawDeaths);
    }

    private void doLobbyCheck() {

        //TP terug naar lobby
        for (Player outlaw : teamA) {
            if (!getLobbyLocation(PlayerStats.getPlayerStats(outlaw)).isInsideRange(outlaw.getLocation())) {
                for (int j = 0; j < 10; j++) {
                    outlaw.getWorld().playEffect(outlaw.getLocation(), Effect.ENDER_SIGNAL, j);
                }
                outlaw.playSound(outlaw.getLocation(), Sound.ENDERMAN_TELEPORT, 1F, 1F);
                outlaw.teleport(getLobbyLocation(PlayerStats.getPlayerStats(outlaw)).getRandomLocation());
                outlaw.getInventory().setItem(8, Refrence.IS_EMPTY);
            }
        }
        for (Player sheriff : teamB) {
            if (!getLobbyLocation(PlayerStats.getPlayerStats(sheriff)).isInsideRange(sheriff.getLocation())) {
                for (int j = 0; j < 10; j++) {
                    sheriff.getWorld().playEffect(sheriff.getLocation(), Effect.ENDER_SIGNAL, j);
                }
                sheriff.playSound(sheriff.getLocation(), Sound.ENDERMAN_TELEPORT, 1F, 1F);
                sheriff.teleport(getLobbyLocation(PlayerStats.getPlayerStats(sheriff)).getRandomLocation());
                sheriff.getInventory().setItem(8, Refrence.IS_EMPTY);
            }
        }
        if ((teamA.size() >= minPlayers && teamB.size() >= minPlayers) && levelTiming-- <= 0) {
            //Spel begint
            this.setStage(null, null, null, MissionStage.STAGE1);
            this.status = MissionStatus.IN_GAME;
            levelTiming = 0;
            for(Player p : this.getAllPlayers()){
                DMMain.getCrackShot().giveWeapon(p, "Revolver", 1);
            }
            sendTitle(MissionTeam.SHERIFFS, Lang.MISSION_NAME.toUpperCase(), Lang.OBJECTIVE_KILL_OUTLAWS);
            sendTitle(MissionTeam.OUTLAWS, Lang.MISSION_NAME.toUpperCase(), Lang.OBJECTIVE_KILL_SHERIFFS);
        }
        super.showCountDown(levelTiming);

    }

    private void doIngameCheck() {
        if (this.getStage().equals(MissionStage.STAGE1)) {
            for (Player outlaw : teamA) {
                outlaw.getInventory().setItem(8, Refrence.customIS(Material.COMPASS, 1, "Objective location", new String[]{"Look there! A sheriff!"}, null));
            }
            for (Player sheriff : teamB) {
                sheriff.getInventory().setItem(8, Refrence.customIS(Material.COMPASS, 1, "Objective location", new String[]{"Heads up! Crooks that way!"}, null));
            }
            if (secondRemain-- <= -1) {
                if (outlawDeaths < sheriffDeaths) {
                    for (Player p : teamA) {
                        this.reward(p, 1024);
                    }
                    sendTitle(MissionTeam.OUTLAWS, Lang.MISSION_WIN, Lang.ENDMESSAGE_OUTLAW_WIN);
                    sendTitle(MissionTeam.SHERIFFS, Lang.MISSION_FAIL, Lang.ENDMESSAGE_SHERIFF_LOSE);
                } else if (sheriffDeaths < outlawDeaths) {
                    for (Player p : teamB) {
                        this.reward(p, 1024);
                    }
                    sendTitle(MissionTeam.SHERIFFS, Lang.MISSION_WIN, Lang.ENDMESSAGE_SHERIFF_WIN);
                    sendTitle(MissionTeam.OUTLAWS, Lang.MISSION_FAIL, Lang.ENDMESSAGE_OUTLAW_LOSE);
                } else {
                    sendTitle(MissionTeam.ALL, Lang.MISSION_DRAW, Lang.ENDMESSAGE_DRAW);
                }
                stop();
            }
            cooldown = false;
        }
    }

    @Override
    protected MissionEventHandler allowJoin(PlayerStats ps) {
        return MissionEventHandler.NEXT_TRUE;
    }

    @Override
    public String getAuth() {
        return "Sansko1337";
    }

    @Override
    public Material getIcon() {
        return Material.STONE_SWORD;
    }

    private void sendMessage(boolean outlaws, String message) {
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
}
