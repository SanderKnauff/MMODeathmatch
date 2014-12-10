/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.imine.mmodeathmatch;

import nl.imine.mmodeathmatch.util.Lang;
import nl.makertim.MMOmain.MKTEventHandler;
import nl.makertim.MMOmain.PlayerStats;
import nl.makertim.MMOmain.Refrence;
import nl.makertim.MMOmain.lib.MMOOutlaws;
import nl.makertim.MMOmain.lib.Mission;
import nl.makertim.MMOmain.lib.MissionLocation;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
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

    private int outlawDeaths, sheriffDeaths;
    private int outlawEnd, sheriffEnd;

    private int levelTiming = 30, secondRemain = 600;

    private boolean joinable, inLobby, cooldown;
    public short gameStage;

    public Deathmatch() {
        super.isLobby = true;
    }

    @Override
    public void tick(int i) {
        if (i == 1) {
            super.timer = (isLobby ? levelTiming : secondRemain);
            if (!inGame && !inLobby) {
                inLobby = true;
            }
            super.tick(i);

            calculateRequiredKills();
            updateScoreboard();
            doLobbyCheck();
            doIngameCheck();
        }
    }

    @Override
    public boolean inLobby() {
        return inLobby;
    }

    @Override
    public void joinPlayer(PlayerStats pls) {
        super.joinPlayer(pls);
        Player player = pls.getPlayer();
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent e) {
        if (super.getAllPlayers().contains(e.getPlayer())) {
            leavePlayer(PlayerStats.getPlayerStats(e.getPlayer()));
            checkEnd();
        }
    }

    @Override
    public void onHurt(EntityDamageEvent evt, Entity cause) {
        super.onHurt(evt, cause);
        if (evt.isCancelled()) {
        }
    }

    @Override
    public void onDeath(Player pl, Entity damager) {
        super.onDeath(pl, damager);
        if (teamA.contains(pl)) {
            outlawDeaths++;
            checkEnd();
        } else {
            sheriffDeaths++;
            checkEnd();
        }
        if (damager instanceof Player) {
            this.reward((Player) damager, 64);
        } else if (damager instanceof Projectile && ((((Projectile) damager)).getShooter()) instanceof Player) {
            this.reward(((Player) ((Projectile) damager).getShooter()), 64);
        }
    }

    @Override
    public void leavePlayer(PlayerStats pls) {
        super.leavePlayer(pls);
        Player player = pls.getPlayer();
        pls.isInMission = false;
        super.getAllPlayers().remove(player);
        MKTEventHandler.cleanupPlayerInventory(player);
        player.getInventory().setItem(8, Refrence.customIS(Material.COMPASS, 1, "Direction", null, null));
        if (pls.isOutlaw) {
            teamA.remove(player);
            if (teamA.isEmpty()) {
                stop();
            }
        } else {
            teamB.remove(player);
            if (teamB.isEmpty()) {
                stop();
            }
        }
        super.showPeople(player);
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
        return Lang.MISSION_ID;
    }

    public void sendMessage(boolean outlaws, String message) {
        if (outlaws) {
            for (Player pl : this.teamA) {
                MMOOutlaws.sendActionMessage(pl, message, "Gold");
            }
        } else {
            for (Player pl : this.teamB) {
                MMOOutlaws.sendActionMessage(pl, message, "Gold");
            }
        }
    }

    private void checkEnd() {
        boolean outlawWin = false, teamToSmall = false, sheriffWin = false;
        if (sheriffDeaths >= outlawEnd) {
            secondRemain = 0;
            outlawWin = true;
            for (Player p : teamA) {
                this.reward(p, 1024);
            }
            sendTitle(true, Lang.MISSION_WIN, Lang.ENDMESSAGE_OUTLAW_WIN);
            sendTitle(false, Lang.MISSION_FAIL, Lang.ENDMESSAGE_SHERIFF_LOSE);
            stop();
        } else if (outlawDeaths >= sheriffEnd) {
            secondRemain = 0;
            sheriffWin = true;
            for (Player p : teamB) {
                this.reward(p, 1024);
            }
            sendTitle(false, Lang.MISSION_WIN, Lang.ENDMESSAGE_SHERIFF_WIN);
            sendTitle(true, Lang.MISSION_FAIL, Lang.ENDMESSAGE_OUTLAW_LOSE);
            stop();
        } else if (teamA.size() < minPlayers || teamB.size() < minPlayers) {
            secondRemain = 0;
            teamToSmall = true;
        }
        if (secondRemain <= 0) {  //MAKERTIM WHY
            if (teamToSmall) {
                sendTitle(null, Lang.MISSION_DRAW, null);
                if (outlawWin) {
                    sendTitle(true, Lang.MISSION_WIN, Lang.ENDMESSAGE_OUTLAW_WIN);
                    sendTitle(false, Lang.MISSION_FAIL, Lang.ENDMESSAGE_SHERIFF_LOSE);
                } else if (sheriffWin) {
                    sendTitle(false, Lang.MISSION_WIN, Lang.ENDMESSAGE_SHERIFF_WIN);
                    sendTitle(true, Lang.MISSION_FAIL, Lang.ENDMESSAGE_OUTLAW_LOSE);
                } else {
                    sendTitle(null, Lang.MISSION_DRAW, Lang.ENDMESSAGE_DRAW);
                }
                stop();
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        isLobby = true;
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
        super.outlawsSuffix = String.format(": %d", sheriffDeaths);
        super.sheriffSuffix = String.format(": %d", outlawDeaths);
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
            if ((teamA.size() >= minPlayers && teamB.size() >= minPlayers) && levelTiming-- <= 0) {
                //Spel begint
                inGame = true;
                super.isLobby = false;
                inLobby = false;
                state = 0;
                levelTiming = 0;
                sendTitle(false, Lang.MISSION_NAME.toUpperCase(), Lang.OBJECTIVE_KILL_OUTLAWS);
                sendTitle(true, Lang.MISSION_NAME.toUpperCase(), Lang.OBJECTIVE_KILL_SHERIFFS);
                //super.timer = secondRemain;
            }
            super.timer(levelTiming);
        }
    }

    private void doIngameCheck() {
        if (inGame) {
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
                    sendTitle(true, Lang.MISSION_WIN, Lang.ENDMESSAGE_OUTLAW_WIN);
                    sendTitle(false, Lang.MISSION_FAIL, Lang.ENDMESSAGE_SHERIFF_LOSE);
                } else if (sheriffDeaths < outlawDeaths) {
                    for (Player p : teamB) {
                        this.reward(p, 1024);
                    }
                    sendTitle(false, Lang.MISSION_WIN, Lang.ENDMESSAGE_SHERIFF_WIN);
                    sendTitle(true, Lang.MISSION_FAIL, Lang.ENDMESSAGE_OUTLAW_LOSE);
                } else {
                    sendTitle(true, Lang.MISSION_DRAW, Lang.ENDMESSAGE_DRAW);
                }
                stop();
            }
            cooldown = false;
        }
    }
}
