package com.massivecraft.factions.integration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.P;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;

import com.massivecraft.factions.util.HealthBarUtil;

import org.getspout.spoutapi.player.SpoutPlayer;
import org.getspout.spoutapi.SpoutManager;


public class SpoutFeatures
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private static SpoutMainListener mainListener;
	
	private static boolean enabled = false;
	public static boolean isEnabled() { return enabled; }

	// -------------------------------------------- //
	// SETUP AND AVAILABILITY
	// -------------------------------------------- //
	
	public static boolean setup()
	{
		Plugin plugin = Bukkit.getPluginManager().getPlugin("Spout");
		if (plugin == null || ! plugin.isEnabled())
		{
			if (enabled == false) return false;
			enabled = false;
			return false;
		}
		
		if (enabled == true) return true;
		enabled = true;
		
		P.p.log("Found and will use features of "+plugin.getDescription().getFullName());
		mainListener = new SpoutMainListener();
		Bukkit.getPluginManager().registerEvents(mainListener, P.p);
		
		return true;
	}

	// -------------------------------------------- //
	// CAPES
	// -------------------------------------------- //
	// Capes look the same to everyone.
	
	public static void updateCape(final Object ofrom, final Object oto)
	{
		// Enabled and non-null?
		if ( ! isEnabled()) return;
		if ( ! Conf.spoutCapes) return;
		
		Set<Player> fromPlayers = getPlayersFromObject(ofrom);
		Set<Player> toPlayers = getPlayersFromObject(oto);
		
		for (Player player : fromPlayers)
		{
			FPlayer fplayer = FPlayers.i.get(player);
			SpoutPlayer splayer = SpoutManager.getPlayer(player);
			Faction faction = fplayer.getFaction();
			
			String cape = faction.getCape();
			
			for (Player playerTo : toPlayers)
			{
				SpoutPlayer splayerTo = SpoutManager.getPlayer(playerTo);
				
				// Set the cape
				if (cape != null)
				{
					try
					{
						splayer.setCapeFor(splayerTo, cape);
					}
					catch (Exception e)
					{
						
					}
				}
				else
				{
					splayer.resetCapeFor(splayerTo);
				}
			}	
		}
	}
	
	public static void updateCapeShortly(final Object ofrom, final Object oto)
	{
		P.p.getServer().getScheduler().scheduleSyncDelayedTask(P.p, new Runnable()
		{
			@Override
			public void run()
			{
				updateCape(ofrom, oto);
			}
		}, 10);
	}
	
	// -------------------------------------------- //
	// TITLE
	// -------------------------------------------- //
	
	public static void updateTitle(final Object ofrom, final Object oto)
	{
		// Enabled and non-null?
		if ( ! isEnabled()) return;
		if ( ! (Conf.spoutFactionTagsOverNames || Conf.spoutFactionTitlesOverNames || Conf.spoutHealthBarUnderNames)) return;
		
		Set<Player> fromPlayers = getPlayersFromObject(ofrom);
		Set<Player> toPlayers = getPlayersFromObject(oto);
		
		for (Player player : fromPlayers)
		{
			FPlayer fplayer = FPlayers.i.get(player);
			SpoutPlayer splayer = SpoutManager.getPlayer(player);
			Faction faction = fplayer.getFaction();
			
			for (Player playerTo : toPlayers)
			{
				FPlayer fplayerTo = FPlayers.i.get(playerTo);
				SpoutPlayer splayerTo = SpoutManager.getPlayer(playerTo);
				Faction factionTo = fplayerTo.getFaction();
				
				ChatColor relationColor = faction.getRelationTo(factionTo).getColor();
				
				String title = generateTitle(player, fplayer, faction, relationColor);
				splayer.setTitleFor(splayerTo, title);
			}	
		}
	}
	
	public static void updateTitleShortly(final Object ofrom, final Object oto)
	{
		P.p.getServer().getScheduler().scheduleSyncDelayedTask(P.p, new Runnable()
		{
			@Override
			public void run()
			{
				updateTitle(ofrom, oto);
			}
		}, 10);
	}
	
	public static String generateTitle(Player player, FPlayer fplayer, Faction faction, ChatColor relationColor)
	{
		String ret = null;
		
		ret = player.getDisplayName();
		
		if (faction.isNormal())
		{
			String addTag = "";
			if (Conf.spoutFactionTagsOverNames)
			{
				addTag += relationColor.toString() + "[" + fplayer.getRole().getPrefix() + faction.getTag() + "]";
			}
				
			if (Conf.spoutFactionTitlesOverNames && ! fplayer.getTitle().isEmpty())
			{
				addTag += (addTag.isEmpty() ? "" : " ") + fplayer.getTitle();
			}

			ret = addTag + "\n" + ret;
		}
		
		if (Conf.spoutHealthBarUnderNames)
		{
			ret += "\n";
			ret += HealthBarUtil.getHealthbar(player.getHealth() / 20d);
		}
		
		return ret;
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	public static Set<Player> getPlayersFromObject(Object o)
	{
		Set<Player> ret = new HashSet<Player>();
		if (o instanceof Player)
		{
			ret.add((Player)o);
		}
		else if (o instanceof FPlayer)
		{
			ret.add(((FPlayer)o).getPlayer());
		}
		else if (o instanceof Faction)
		{
			ret.addAll(((Faction)o).getOnlinePlayers());
		}
		else
		{
			ret.addAll(Arrays.asList(Bukkit.getOnlinePlayers()));
		}
			
		return ret;
	}
	
	// -------------------------------------------- //
	// TERRITORY DISPLAY
	// -------------------------------------------- //

	// update displayed current territory for all players inside a specified chunk; if specified chunk is null, then simply update everyone online
	public static void updateTerritoryDisplayLoc(FLocation fLoc)
	{
		if ( ! isEnabled()) return;

		Set<FPlayer> players = FPlayers.i.getOnline();

		for (FPlayer player : players)
		{
			if (fLoc == null)
				mainListener.updateTerritoryDisplay(player, false);
			else if (player.getLastStoodAt().equals(fLoc))
				mainListener.updateTerritoryDisplay(player, true);
		}
	}

	// update displayed current territory for specified player; returns false if unsuccessful
	public static boolean updateTerritoryDisplay(FPlayer player)
	{
		if ( ! isEnabled()) return false;
		return mainListener.updateTerritoryDisplay(player, true);
	}

	public static void playerDisconnect(FPlayer player)
	{
		if ( ! isEnabled()) return;
		mainListener.removeTerritoryLabels(player.getName());
	}
}
