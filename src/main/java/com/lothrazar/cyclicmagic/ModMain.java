 package com.lothrazar.cyclicmagic;

import com.lothrazar.cyclicmagic.gui.ModGuiHandler;
import com.lothrazar.cyclicmagic.proxy.CommonProxy;
import com.lothrazar.cyclicmagic.registry.BlockRegistry;
import com.lothrazar.cyclicmagic.registry.CommandRegistry;
import com.lothrazar.cyclicmagic.registry.DispenserBehaviorRegistry;
import com.lothrazar.cyclicmagic.registry.EventRegistry; 
import com.lothrazar.cyclicmagic.registry.FuelRegistry;
import com.lothrazar.cyclicmagic.registry.ItemRegistry;
import com.lothrazar.cyclicmagic.registry.MobSpawningRegistry;
import com.lothrazar.cyclicmagic.registry.PacketRegistry;
import com.lothrazar.cyclicmagic.registry.PotionRegistry;
import com.lothrazar.cyclicmagic.registry.ProjectileRegistry;
import com.lothrazar.cyclicmagic.registry.RecipeAlterRegistry;
import com.lothrazar.cyclicmagic.registry.RecipeNewRegistry;
import com.lothrazar.cyclicmagic.registry.ReflectionRegistry;
import com.lothrazar.cyclicmagic.registry.SoundRegistry;
import com.lothrazar.cyclicmagic.registry.SpellRegistry;
import com.lothrazar.cyclicmagic.registry.StackSizeRegistry;
import com.lothrazar.cyclicmagic.registry.TileEntityRegistry;
import com.lothrazar.cyclicmagic.registry.VillageTradeRegistry;
import com.lothrazar.cyclicmagic.registry.WorldGenRegistry;
import com.lothrazar.cyclicmagic.util.Const;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

@Mod(modid = Const.MODID, useMetadata = true, canBeDeactivated = false, updateJSON = "https://raw.githubusercontent.com/PrinceOfAmber/CyclicMagic/master/update.json", guiFactory = "com.lothrazar." + Const.MODID + ".gui.IngameConfigFactory")
public class ModMain {

	@Instance(value = Const.MODID)
	public static ModMain						instance;
	@SidedProxy(clientSide = "com.lothrazar." + Const.MODID + ".proxy.ClientProxy", serverSide = "com.lothrazar." + Const.MODID + ".proxy.CommonProxy")
	public static CommonProxy					proxy;
	public static ModLogger						logger;
	private static Configuration				config;
	public static SimpleNetworkWrapper			network;
	public final static CreativeTabs			TAB	= new CreativeTabs(Const.MODID) {
        @Override
        public Item getTabIconItem() {
            return Items.diamond;//ItemRegistry.ModItems.chest_sack;
        }
    };
    private EventRegistry events;

	@EventHandler
	public void onPreInit(FMLPreInitializationEvent event) {

		logger = new ModLogger(event.getModLog());
		config = new Configuration(event.getSuggestedConfigurationFile());
		
		events = new EventRegistry();
		ItemRegistry.construct();//MAYBE it should be a constructed, not static
		BlockRegistry.construct(); 
		
		config.load();
		syncConfig();

		network = NetworkRegistry.INSTANCE.newSimpleChannel(Const.MODID);
		
		events.register();

		ReflectionRegistry.register(); 
		PacketRegistry.register(network);
		VillageTradeRegistry.register();
		SoundRegistry.register();
	}


	@EventHandler
	public void onInit(FMLInitializationEvent event) {

		PotionRegistry.register();
		ItemRegistry.register();
		BlockRegistry.register();
		SpellRegistry.register();
		MobSpawningRegistry.register();
		WorldGenRegistry.register();
		FuelRegistry.register();
		StackSizeRegistry.register();
		RecipeAlterRegistry.register();
		RecipeNewRegistry.register();

		proxy.register();
		proxy.registerEvents();

		TileEntityRegistry.register();

		NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGuiHandler());

		ProjectileRegistry.register(event);
	}

	@EventHandler
	public void onPostInit(FMLPostInitializationEvent event) {

		// registers all plantable crops. the plan is to work with non vanilla data
		DispenserBehaviorRegistry.register();
	}

	@EventHandler
	public void onServerStarting(FMLServerStartingEvent event) {
		CommandRegistry.register(event);
	}

	public static Configuration getConfig() {
		return config;
	}

	public  void syncConfig() {
		// hit on startup and on change event from
		//we cant make this a list/loop because the order does matter
		Configuration c = getConfig();
		WorldGenRegistry.syncConfig(c);
		PotionRegistry.syncConfig(c);
		events.syncConfig(c);
		BlockRegistry.syncConfig(c);
		ItemRegistry.syncConfig(c);
		FuelRegistry.syncConfig(c);
		MobSpawningRegistry.syncConfig(c);
		RecipeAlterRegistry.syncConfig(c);
		RecipeNewRegistry.syncConfig(c);
		DispenserBehaviorRegistry.syncConfig(c);
		StackSizeRegistry.syncConfig(c);
		SpellRegistry.syncConfig(c); 
		CommandRegistry.syncConfig(c);
		VillageTradeRegistry.syncConfig(c);

		c.save();
		 
	}

	/* TODO LIST
	 * 
	 ***** BUGS 
	 *
	 *Spell sounds: why does push/pull/rotate sound WORK< but build up spells dont?
	 *		-->> answer: neither one plays the actual sound. if it breaks a block for you, THATS the one you hear
	 *		 need a network packet.sendToAllAround and hit the sound up manual (clientside)
	 *
	 * HARVESTER :: add a GUI with toggle buttons to turn configs on and off
	 * 
	 *[big change[ ENDER BOOK: remove and replace with another invo-tab
	 *  
	 *[disabled] building spells: make phantom/ghost/outline/particle blocks
	 * 
	 * pets live longer and/or respawn
	 *
	 *make silk touch work on silverfish blocks
	 *
	 * clay generation in some biomes , all the way down -- plains and swamp?
	 * 
	 * SPELLS that use build sizes/numbers, put in their name 'name' +" ["+ number + "]";
	 * 
	 * ROTATE: STAIRS: allow switch frop top to bottom
	 * 
	 * WAND: turn particle beam on / off in config
	 * 
     * uncrafter -maybe try using fuel source
	 * 
	 * uncrafter - persist smoking ? like furnace?
	 * 
	 * [refactor] try to auto detect home biomes of saplings for modded compat
	 * 
	 * [refactor]  noteblock/sign into util
	 *   
	 * more plants that give monster loot ie pearls/ blaze powder/ gunpowder?
	 * 
	 * crafting table hotkeys - numpad?
	 * 
	 * SPELL: bring back ghost - let it put you in new location but only if air	 blocks. add SLOWNESS effect??
	 *  
	 * some sort of tool that displays trade of every nearby villager?
	 * 
	 * rebalance emerald armor numbers plan.
	 * 
	 * achievemnets give exp
	 *  // https://github.com/PrinceOfAmber/SamsPowerups/blob/5ac4556cb99266fa3f322ef8bfdf75683aa2f26a/src/main/java/com/lothrazar/samspowerups/util/AchievementFinder.java
	 * 
	 * Make minecarts kill and push through entities that get hit (ex: pigmen on rail)
	 * 
	 * Getting hit by a bat causes blindness to player
	 * 
	 * https://www.reddit.com/r/minecraftsuggestions/comments/4fgcb2/make_dry_sponges_usable_by_dispensers/
	 * 
	 * //IDEA: make boats float
	 * https://www.reddit.com/r/minecraftsuggestions/comments/4d4ob1/
	 * make_boats_float_again/
	 * 
	 * https://www.reddit.com/r/minecraftsuggestions/comments/4ff8bu/armor_stand_with_arms/
	 * ^^ and can they hold weapons???
	 * 
	 * https://www.reddit.com/r/minecraftsuggestions/comments/4f5xzq/golden_compass_points_to_its_creation_spot/
	 * 
	 * https://www.reddit.com/r/minecraftsuggestions/comments/4f6420/a_new_type_of_compass_that_points_to_your_most/
	 * 
	 * https://www.reddit.com/r/minecraftsuggestions/comments/4f35fx/red_sand_should_generate_on_lava_ocean_beaches_in/
	 * 
	 * 
	 */
}
