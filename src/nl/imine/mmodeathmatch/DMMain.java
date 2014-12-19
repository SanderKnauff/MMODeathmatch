/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.imine.mmodeathmatch;

import com.shampaggon.crackshot.CSUtility;
import nl.makertim.MMOmain.lib.MMOOutlaws;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Sander
 */
public class DMMain extends JavaPlugin{

    private static Plugin plugin;
    private static CSUtility crackShot;
    
    @Override
    public void onEnable(){
        plugin = this;
        crackShot = new CSUtility();
        MMOOutlaws.getInstance().addMission(Deathmatch.class);
    }
    
    @Override
    public void onDisable(){
        MMOOutlaws.getInstance().removeMission(Deathmatch.class);
    }
    
    public static Plugin getInstance(){
        return plugin;
    }
    
    public static CSUtility getCrackShot(){
        return crackShot;
    }
    
}
