/**
 * BetonQuest - advanced quests for Bukkit
 * Copyright (C) 2016  Jakub "Co0sh" Sapalski
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package pl.betoncraft.betonquest.compatibility.protocollib;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import net.citizensnpcs.api.npc.NPC;
import pl.betoncraft.betonquest.BetonQuest;
import pl.betoncraft.betonquest.ConditionID;
import pl.betoncraft.betonquest.ObjectNotFoundException;
import pl.betoncraft.betonquest.config.Config;
import pl.betoncraft.betonquest.config.ConfigPackage;
import pl.betoncraft.betonquest.utils.Debug;
import pl.betoncraft.betonquest.utils.PlayerConverter;

/**
 * @author Namnodorel
 * @author Jakub Sapalski
 */
public class NPCHider extends BukkitRunnable implements Listener {

    private static NPCHider instance;

    private EntityHider hider = new EntityHider(BetonQuest.getInstance(), EntityHider.Policy.BLACKLIST);
    private Map<Integer, Set<ConditionID>> npcs = new HashMap<>();
    private Integer updateInterval = BetonQuest.getInstance().getConfig().getInt("hidden_npcs_check_interval", 5 * 20);

    public NPCHider() {
        instance = this;

        loadFromConfig();

        runTaskTimer(BetonQuest.getInstance(), 0, updateInterval);
        Bukkit.getPluginManager().registerEvents(this, BetonQuest.getInstance());
    }

    
    public static NPCHider getInstance() {
        return instance;
    }

    private void loadFromConfig() {

        for (ConfigPackage cfgPackage : Config.getPackages().values()) {
            FileConfiguration custom = cfgPackage.getCustom().getConfig();
            if (custom == null) {
                continue;
            }
            ConfigurationSection section = custom.getConfigurationSection("hide_npcs");
            if (section == null) {
                continue;
            }
            npcs:
            for (String npcID : section.getKeys(false)) {
                int id;
                try {
                    id = Integer.parseInt(npcID);
                } catch (NumberFormatException e) {
                    Debug.error("NPC ID '" + npcID + "' is not a valid number, in custom.yml hide_npcs");
                    continue npcs;
                }
                Set<ConditionID> conditions = new HashSet<>();
                String conditionsString = section.getString(npcID);

                for (String condition : conditionsString.split(",")) {
                    try {
                        conditions.add(new ConditionID(cfgPackage, condition));
                    } catch (ObjectNotFoundException e) {
                        Debug.error("Condition '" + condition +
                                "' does not exist, in custom.yml hide_npcs with ID " + npcID);
                        continue npcs;
                    }
                }

                if (npcs.containsKey(id)) {
                    npcs.get(id).addAll(conditions);
                } else {
                    npcs.put(id, conditions);
                }
            }
        }

    }
    
    @Override
    public void run() {
        applyVisibility();
    }
    
    public void stop() {
        cancel();
        HandlerList.unregisterAll(this);
    }

    public void applyVisibility(Player player, Integer npcID) {
        boolean hidden = true;
        Set<ConditionID> conditions = npcs.get(npcID);
        if (conditions == null || conditions.isEmpty()) {
            hidden = false;
        } else {
            for (ConditionID condition : conditions) {
                if (!BetonQuest.condition(PlayerConverter.getID(player), condition)) {
                    hidden = false;
                    break;
                }
            }
        }

        NPC npc = CitizensAPI.getNPCRegistry().getById(npcID);

        if (npc.isSpawned()) {
            if (hidden) {
                hider.hideEntity(player, npc.getEntity());
            } else {
                hider.showEntity(player, npc.getEntity());
            }
        }
    }

    public void applyVisibility(Player player) {
        for (Integer npcID : npcs.keySet()) {
            applyVisibility(player, npcID);
        }
    }

    public void applyVisibility(NPC npcID) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyVisibility(p, npcID.getId());
        }
    }

    public void applyVisibility() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (Integer npcID : npcs.keySet()) {
                applyVisibility(p, npcID);
            }
        }
    }


    public boolean isInvisible(Player player, NPC npc) {
        return !hider.isVisible(player, npc.getEntity().getEntityId());
    }

    @EventHandler
    public void onNPCSpawn(NPCSpawnEvent event) {
        applyVisibility(event.getNPC());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyVisibility(event.getPlayer());
    }
}