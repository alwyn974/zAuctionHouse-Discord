package fr.maxlego08.discord.zcore.utils;

import fr.maxlego08.discord.ZDiscordPlugin;
import fr.maxlego08.discord.zcore.ZPlugin;
import fr.maxlego08.discord.zcore.enums.Message;
import fr.maxlego08.ztranslator.api.Translator;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("deprecation")
public abstract class ZUtils extends MessageUtils {

	private static transient List<String> teleportPlayers = new ArrayList<String>();
	protected transient ZDiscordPlugin plugin = (ZDiscordPlugin) ZPlugin.z();
	public static final Pattern STRIP_EXTRAS_PATTERN = Pattern.compile("(?i)§+[0-9A-FK-ORX]");

	/**
	 * @param item
	 * @return the encoded item
	 */
	protected String encode(ItemStack item) {
		return ItemDecoder.serializeItemStack(item);
	}

	/**
	 * @param item
	 * @return the decoded item
	 */
	protected ItemStack decode(String item) {
		return ItemDecoder.deserializeItemStack(item);
	}

	/**
	 * @param a
	 * @param b
	 * @return number between a and b
	 */
	protected int getNumberBetween(int a, int b) {
		return ThreadLocalRandom.current().nextInt(a, b);
	}

	/**
	 * @param player
	 * @return true if the player's inventory is full
	 */
	protected boolean hasInventoryFull(Player player) {
		int slot = 0;
		PlayerInventory inventory = player.getInventory();
		for (int a = 0; a != 36; a++) {
			ItemStack itemStack = inventory.getContents()[a];
			if (itemStack == null)
				slot++;
		}
		return slot == 0;
	}

	protected boolean give(ItemStack item, Player player) {
		if (hasInventoryFull(player))
			return false;
		player.getInventory().addItem(item);
		return true;
	}

	/**
	 * Gives an item to the player, if the player's inventory is full then the
	 * item will drop to the ground
	 * 
	 * @param player
	 * @param item
	 */
	protected void give(Player player, ItemStack item) {
		if (hasInventoryFull(player))
			player.getWorld().dropItem(player.getLocation(), item);
		else
			player.getInventory().addItem(item);
	}

	private static transient Material[] byId;

	static {
		if (!ItemDecoder.isNewVersion()) {
			byId = new Material[0];
			for (Material material : Material.values()) {
				if (byId.length > material.getId()) {
					byId[material.getId()] = material;
				} else {
					byId = Arrays.copyOfRange(byId, 0, material.getId() + 2);
					byId[material.getId()] = material;
				}
			}
		}
	}

	/**
	 * @param id
	 * @return the material according to his id
	 */
	protected Material getMaterial(int id) {
		return byId.length > id && id >= 0 ? byId[id] : Material.AIR;
	}

	/**
	 * Check if the item name is the same as the given string
	 * 
	 * @param stack
	 * @param name
	 * @return true if the item name is the same as string
	 */
	protected boolean same(ItemStack stack, String name) {
		return stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
				&& stack.getItemMeta().getDisplayName().equals(name);
	}

	/**
	 * Check if the item name contains the given string
	 * 
	 * @param stack
	 * @param name
	 * @return true if the item name contains the string
	 */
	protected boolean contains(ItemStack stack, String name) {
		return stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
				&& stack.getItemMeta().getDisplayName().contains(name);
	}

	/**
	 * Remove the item from the player's hand
	 * 
	 * @param player
	 *            of items to withdraw
	 */
	protected void removeItemInHand(Player player) {
		removeItemInHand(player, 64);
	}

	/**
	 * Remove the item from the player's hand
	 * 
	 * @param player
	 * @param how
	 *            of items to withdraw
	 */
	protected void removeItemInHand(Player player, int how) {
		if (player.getItemInHand().getAmount() > how)
			player.getItemInHand().setAmount(player.getItemInHand().getAmount() - 1);
		else
			player.setItemInHand(new ItemStack(Material.AIR));
		player.updateInventory();
	}

	/**
	 * Check if two locations are identical
	 * 
	 * @param l
	 *            location
	 * @param l2
	 *            location
	 * @return true if both rentals are the same
	 */
	protected boolean same(Location l, Location l2) {
		return (l.getBlockX() == l2.getBlockX()) && (l.getBlockY() == l2.getBlockY())
				&& (l.getBlockZ() == l2.getBlockZ()) && l.getWorld().getName().equals(l2.getWorld().getName());
	}

	/**
	 * Teleport a player to a given location with a given delay
	 * 
	 * @param player
	 *            who will be teleported
	 * @param delay
	 *            before the teleportation of the player
	 * @param location
	 *            where the player will be teleported
	 */
	protected void teleport(Player player, int delay, Location location) {
		teleport(player, delay, location, null);
	}

	/**
	 * Teleport a player to a given location with a given delay
	 * 
	 * @param player
	 *            who will be teleported
	 * @param delay
	 *            before the teleportation of the player
	 * @param location
	 *            where the player will be teleported
	 * @param cmd
	 *            executed when the player is teleported or not
	 */
	protected void teleport(Player player, int delay, Location location, Consumer<Boolean> cmd) {
		if (teleportPlayers.contains(player.getName())) {
			message(player, Message.TELEPORT_ERROR);
			return;
		}
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
		Location playerLocation = player.getLocation();
		AtomicInteger verif = new AtomicInteger(delay);
		teleportPlayers.add(player.getName());
		if (!location.getChunk().isLoaded())
			location.getChunk().load();
		ses.scheduleWithFixedDelay(() -> {
			if (!same(playerLocation, player.getLocation())) {
				message(player, Message.TELEPORT_MOVE);
				ses.shutdown();
				teleportPlayers.remove(player.getName());
				if (cmd != null)
					cmd.accept(false);
				return;
			}
			int currentSecond = verif.getAndDecrement();
			if (!player.isOnline()) {
				ses.shutdown();
				teleportPlayers.remove(player.getName());
				return;
			}
			if (currentSecond == 0) {
				ses.shutdown();
				teleportPlayers.remove(player.getName());
				player.teleport(location);
				message(player, Message.TELEPORT_SUCCESS);
				if (cmd != null)
					cmd.accept(true);
			} else
				message(player, Message.TELEPORT_MESSAGE, currentSecond);
		}, 0, 1, TimeUnit.SECONDS);
	}

	/**
	 * Format a double in a String
	 * 
	 * @param decimal
	 * @return formatting current duplicate
	 */
	protected String format(double decimal) {
		return format(decimal, "#.##");
	}

	/**
	 * Format a double in a String
	 * 
	 * @param decimal
	 * @param format
	 * @return formatting current double according to the given format
	 */
	protected String format(double decimal, String format) {
		DecimalFormat decimalFormat = new DecimalFormat(format);
		return decimalFormat.format(decimal);
	}

	/**
	 * 
	 * @param player
	 * @param item
	 * @param itemStack
	 */
	protected void removeItems(Player player, int item, ItemStack itemStack) {
		int slot = 0;
		for (ItemStack is : player.getInventory().getContents()) {
			if (is != null && is.isSimilar(itemStack) && item > 0) {
				int currentAmount = is.getAmount() - item;
				item -= is.getAmount();
				if (currentAmount <= 0) {
					if (slot == 40)
						player.getInventory().setItemInOffHand(null);
					else
						player.getInventory().removeItem(is);
				} else
					is.setAmount(currentAmount);
			}
			slot++;
		}
		player.updateInventory();
	}

	/**
	 * @param delay
	 * @param runnable
	 */
	protected void schedule(long delay, Runnable runnable) {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (runnable != null)
					runnable.run();
			}
		}, delay);
	}

	/**
	 * 
	 * @param string
	 * @return
	 */
	protected String name(String string) {
		String name = string.replace("_", " ").toLowerCase();
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	/**
	 * 
	 * @param string
	 * @return
	 */
	protected String name(Material string) {
		String name = string.name().replace("_", " ").toLowerCase();
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	/**
	 * 
	 * @param itemStack
	 * @return
	 */
	protected String name(ItemStack itemStack) {
		return this.getItemName(itemStack);
	}

	/**
	 * 
	 * @param items
	 * @return
	 */
	protected int getMaxPage(Collection<?> items) {
		return (items.size() / 45) + 1;
	}

	/**
	 * 
	 * @param items
	 * @param a
	 * @return
	 */
	protected int getMaxPage(Collection<?> items, int a) {
		return (items.size() / a) + 1;
	}

	/**
	 * 
	 * @param value
	 * @param total
	 * @return
	 */
	protected double percent(double value, double total) {
		return (double) ((value * 100) / total);
	}

	/**
	 * 
	 * @param total
	 * @param percent
	 * @return
	 */
	protected double percentNum(double total, double percent) {
		return (double) (total * (percent / 100));
	}

	/**
	 * 
	 * @param delay
	 * @param count
	 * @param runnable
	 */
	protected void schedule(long delay, int count, Runnable runnable) {
		new Timer().scheduleAtFixedRate(new TimerTask() {
			int tmpCount = 0;

			@Override
			public void run() {

				if (!ZPlugin.z().isEnabled()) {
					cancel();
					return;
				}

				if (tmpCount > count) {
					cancel();
					return;
				}

				tmpCount++;
				Bukkit.getScheduler().runTask(ZPlugin.z(), runnable);

			}
		}, 0, delay);
	}

	/**
	 * 
	 * @param permissible
	 * @param permission
	 * @return
	 */
	protected boolean hasPermission(Permissible permissible, String permission) {
		return permissible.hasPermission(permission);
	}

	/**
	 * @param delay
	 * @param runnable
	 */
	protected void scheduleFix(long delay, BiConsumer<TimerTask, Boolean> runnable) {
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (!ZPlugin.z().isEnabled()) {
					cancel();
					runnable.accept(this, false);
					return;
				}
				Bukkit.getScheduler().runTask(ZPlugin.z(), () -> runnable.accept(this, true));
			}
		}, delay, delay);
	}

	/**
	 * 
	 * @param element
	 * @return
	 */
	protected <T> T randomElement(List<T> element) {
		if (element.size() == 0)
			return null;
		if (element.size() == 1)
			return element.get(0);
		Random random = new Random();
		return element.get(random.nextInt(element.size() - 1));
	}

	/**
	 * 
	 * @param item
	 * @return
	 */
	protected String getItemName(ItemStack item) {
		return getItemName(item, false);
	}

	protected String getItemName(ItemStack itemStack, boolean stripExtrasCode) {
		if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
			if (stripExtrasCode)
				return STRIP_EXTRAS_PATTERN.matcher(itemStack.getItemMeta().getDisplayName()).replaceAll("");
			return itemStack.getItemMeta().getDisplayName();
		}

		// Translation
		if (Bukkit.getPluginManager().isPluginEnabled("zTranslator")) {

			RegisteredServiceProvider<Translator> provider = Bukkit.getServer().getServicesManager()
					.getRegistration(Translator.class);
			Translator translator = provider.getProvider();
			return translator.translate(itemStack);
		}

		String name = itemStack.serialize().get("type").toString().replace("_", " ").toLowerCase();
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	/**
	 * This method is used to strip extra characters from a given string.
	 * It uses a predefined pattern (STRIP_EXTRAS_PATTERN) to find the characters to be replaced.
	 *
	 * @param string The input string from which extras are to be stripped.
	 * @return The modified string after replacement.
	 */
	protected String stripExtrasCode(String string) {
		return stripExtrasCode(string, true);
	}

	/**
	 * This method is used to strip extra characters from a given string.
	 * It uses a predefined pattern (STRIP_EXTRAS_PATTERN) to find the characters to be replaced.
	 *
	 * @param string The input string from which extras are to be stripped.
	 * @param replace A boolean flag indicating whether to perform the replacement operation or not.
	 *                If it's false, the method will return the original string without any modification.
	 * @return The modified string after replacement.
	 */
	protected String stripExtrasCode(String string, boolean replace) {
		return stripExtrasCode(string, replace, "");
	}

	/**
	 * This method is used to strip or replace extra characters from a given string.
	 * It uses a predefined pattern (STRIP_EXTRAS_PATTERN) to find the characters to be replaced.
	 *
	 * @param string The input string from which extras are to be stripped.
	 * @param replace A boolean flag indicating whether to perform the replacement operation or not.
	 *                If it's false, the method will return the original string without any modification.
	 * @param replacement The string that will replace the matched pattern in the original string.
	 *                    This parameter is ignored if 'replace' is false.
	 * @return The modified string after replacement. If 'replace' is false, it returns the original string.
	 */
	protected String stripExtrasCode(String string, boolean replace, String replacement) {
		if (!replace) return string;
		return STRIP_EXTRAS_PATTERN.matcher(string).replaceAll(replacement);
	}

	/**
	 * 
	 * @param message
	 * @return
	 */
	protected String color(String message) {
		return message.replace("&", "§");
	}

	/**
	 * 
	 * @param message
	 * @return
	 */
	protected String colorReverse(String message) {
		return message.replace("§", "&");
	}

	/**
	 * 
	 * @param messages
	 * @return
	 */
	protected List<String> color(List<String> messages) {
		return messages.stream().map(message -> color(message)).collect(Collectors.toList());
	}

	/**
	 * 
	 * @param messages
	 * @return
	 */
	protected List<String> colorReverse(List<String> messages) {
		return messages.stream().map(message -> colorReverse(message)).collect(Collectors.toList());
	}

	/**
	 * 
	 * @param flagString
	 * @return
	 */
	protected ItemFlag getFlag(String flagString) {
		for (ItemFlag flag : ItemFlag.values()) {
			if (flag.name().equalsIgnoreCase(flagString))
				return flag;
		}
		return null;
	}

	/**
	 * 
	 * @param list
	 * @return
	 */
	protected <T> List<T> reverse(List<T> list) {
		List<T> tmpList = new ArrayList<>();
		for (int index = list.size() - 1; index != -1; index--)
			tmpList.add(list.get(index));
		return tmpList;
	}

	/**
	 * 
	 * @param price
	 * @return
	 */
	protected String price(long price) {
		return String.format("%,d", price);
	}

	/**
	 * Permet de générer un string
	 * 
	 * @param length
	 * @return
	 */
	protected String generateRandomString(int length) {
		RandomString randomString = new RandomString(length);
		return randomString.nextString();
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	protected String getDisplayBalence(double value) {
		if (value < 10000)
			return format(value, "#.#");
		else if (value < 1000000)
			return String.valueOf(Integer.valueOf((int) (value / 1000))) + "k ";
		else if (value < 1000000000)
			return String.valueOf(format((value / 1000) / 1000, "#.#")) + "m ";
		else if (value < 1000000000000l)
			return String.valueOf(Integer.valueOf((int) (((value / 1000) / 1000) / 1000))) + "M ";
		else
			return "to much";
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	protected String getDisplayBalence(long value) {
		if (value < 10000)
			return format(value, "#.#");
		else if (value < 1000000)
			return String.valueOf(Integer.valueOf((int) (value / 1000))) + "k ";
		else if (value < 1000000000)
			return String.valueOf(format((value / 1000) / 1000, "#.#")) + "m ";
		else if (value < 1000000000000l)
			return String.valueOf(Integer.valueOf((int) (((value / 1000) / 1000) / 1000))) + "M ";
		else
			return "to much";
	}

	/**
	 * Permet de conter le nombre d'item
	 * 
	 * @param inventory
	 * @param material
	 * @return
	 */
	protected int count(org.bukkit.inventory.Inventory inventory, Material material) {
		int count = 0;
		for (ItemStack itemStack : inventory.getContents())
			if (itemStack != null && itemStack.getType().equals(material))
				count += itemStack.getAmount();
		return count;
	}

	protected Enchantment enchantFromString(String str) {
		for (Enchantment enchantment : Enchantment.values())
			if (enchantment.getName().equalsIgnoreCase(str))
				return enchantment;
		return null;
	}

	/**
	 * 
	 * @param direction
	 * @return
	 */
	protected BlockFace getClosestFace(float direction) {

		direction = direction % 360;

		if (direction < 0)
			direction += 360;

		direction = Math.round(direction / 45);

		switch ((int) direction) {
		case 0:
			return BlockFace.WEST;
		case 1:
			return BlockFace.NORTH_WEST;
		case 2:
			return BlockFace.NORTH;
		case 3:
			return BlockFace.NORTH_EAST;
		case 4:
			return BlockFace.EAST;
		case 5:
			return BlockFace.SOUTH_EAST;
		case 6:
			return BlockFace.SOUTH;
		case 7:
			return BlockFace.SOUTH_WEST;
		default:
			return BlockFace.WEST;
		}
	}

	/**
	 * 
	 * @param price
	 * @return
	 */
	protected String betterPrice(long price) {
		String betterPrice = "";
		String[] splitPrice = String.valueOf(price).split("");
		int current = 0;
		for (int a = splitPrice.length - 1; a > -1; a--) {
			current++;
			if (current > 3) {
				betterPrice += ".";
				current = 1;
			}
			betterPrice += splitPrice[a];
		}
		StringBuilder builder = new StringBuilder().append(betterPrice);
		builder.reverse();
		return builder.toString();
	}

	/**
	 * 
	 * @param enchantment
	 * @param itemStack
	 * @return
	 */
	protected boolean hasEnchant(Enchantment enchantment, ItemStack itemStack) {
		return itemStack.hasItemMeta() && itemStack.getItemMeta().hasEnchants()
				&& itemStack.getItemMeta().hasEnchant(enchantment);
	}

	/**
	 * @param list
	 * @return
	 */
	protected String toList(Stream<String> list) {
		return toList(list.collect(Collectors.toList()), "§e", "§6");
	}

	/**
	 * @param list
	 * @return
	 */
	protected String toList(List<String> list) {
		return toList(list, "§e", "§6§n");
	}

	/**
	 * @param list
	 * @param color
	 * @param color2
	 * @return
	 */
	protected String toList(List<String> list, String color, String color2) {
		if (list == null || list.size() == 0)
			return null;
		if (list.size() == 1)
			return list.get(0);
		String str = "";
		for (int a = 0; a != list.size(); a++) {
			if (a == list.size() - 1 && a != 0)
				str += color + " et " + color2;
			else if (a != 0)
				str += color + ", " + color2;
			str += list.get(a);
		}
		return str;
	}

	/**
	 * 
	 * @param message
	 * @return
	 */
	protected String removeColor(String message) {
		for (ChatColor color : ChatColor.values())
			message = message.replace("§" + color.getChar(), "").replace("&" + color.getChar(), "");
		return message;
	}

	/**
	 * 
	 * @param l
	 * @return
	 */
	protected String format(long l) {
		return format(l, ' ');
	}

	/**
	 * 
	 * @param l
	 * @param c
	 * @return
	 */
	protected String format(long l, char c) {
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
		DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
		symbols.setGroupingSeparator(c);
		formatter.setDecimalFormatSymbols(symbols);
		return formatter.format(l);
	}

	/**
	 * 
	 * @param itemStack
	 * @param player
	 * @return itemstack
	 */
	public ItemStack playerHead(ItemStack itemStack, OfflinePlayer player) {
		String name = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()
				? itemStack.getItemMeta().getDisplayName() : null;
		if (ItemDecoder.isNewVersion()) {
			if (itemStack.getType().equals(Material.PLAYER_HEAD) && name != null && name.startsWith("HEAD")) {
				SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
				name = name.replace("HEAD", "");
				if (name.length() == 0)
					meta.setDisplayName(null);
				else
					meta.setDisplayName(name);
				meta.setOwningPlayer(player);
				itemStack.setItemMeta(meta);
			}
		} else {
			if (itemStack.getType().equals(getMaterial(397)) && itemStack.getData().getData() == 3 && name != null
					&& name.startsWith("HEAD")) {
				SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
				name = name.replace("HEAD", "");
				if (name.length() == 0)
					meta.setDisplayName(null);
				else
					meta.setDisplayName(name);
				meta.setOwner(player.getName());
				itemStack.setItemMeta(meta);
			}
		}
		return itemStack;
	}

	/**
	 * 
	 * @return itemstack
	 */
	protected ItemStack playerHead() {
		return ItemDecoder.isNewVersion() ? new ItemStack(Material.PLAYER_HEAD)
				: new ItemStack(getMaterial(397), 1, (byte) 3);
	}

	/**
	 * 
	 * @param plugin
	 * @param classz
	 * @return T
	 */
	protected <T> T getProvider(Plugin plugin, Class<T> classz) {
		RegisteredServiceProvider<T> provider = plugin.getServer().getServicesManager().getRegistration(classz);
		if (provider == null)
			return null;
		return provider.getProvider() != null ? (T) provider.getProvider() : null;
	}

	/**
	 * 
	 * @param configuration
	 * @return
	 */
	protected PotionEffectType getPotion(String configuration) {
		for (PotionEffectType effectType : PotionEffectType.values())
			if (effectType.getName().equalsIgnoreCase(configuration))
				return effectType;
		return null;
	}

	/**
	 * 
	 * @param runnable
	 */
	protected void runAsync(Runnable runnable) {
		Bukkit.getScheduler().runTaskAsynchronously(this.plugin, runnable);
	}

	/**
	 * 
	 * @param itemStack
	 * @return boolean
	 */
	protected boolean isPlayerHead(ItemStack itemStack) {
		Material material = itemStack.getType();
		if (ItemDecoder.isNewVersion())
			return material.equals(Material.PLAYER_HEAD);
		return (material.equals(getMaterial(397))) && (itemStack.getDurability() == 3);
	}

	/**
	 * 
	 * NMS
	 * 
	 */

	protected final void sendPacket(Player player, Object packet) {
		try {
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
			playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected final Class<?> getNMSClass(String name) {
		try {
			return Class.forName("net.minecraft.server."
					+ Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Send title to player
	 * 
	 * @param player
	 * @param title
	 * @param subtitle
	 * @param fadeInTime
	 * @param showTime
	 * @param fadeOutTime
	 */
	protected void title(Player player, String title, String subtitle, int fadeInTime, int showTime, int fadeOutTime) {
		try {
			Object chatTitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class)
					.invoke(null, "{\"text\": \"" + title + "\"}");
			Constructor<?> titleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(
					getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"),
					int.class, int.class, int.class);
			Object packet = titleConstructor.newInstance(
					getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TITLE").get(null), chatTitle,
					fadeInTime, showTime, fadeOutTime);

			Object chatsTitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class)
					.invoke(null, "{\"text\": \"" + subtitle + "\"}");
			Constructor<?> timingTitleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(
					getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"),
					int.class, int.class, int.class);
			Object timingPacket = timingTitleConstructor.newInstance(
					getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("SUBTITLE").get(null),
					chatsTitle, fadeInTime, showTime, fadeOutTime);

			sendPacket(player, packet);
			sendPacket(player, timingPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected Object getPrivateField(Object object, String field)
			throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = object.getClass();
		Field objectField = field.equals("commandMap") ? clazz.getDeclaredField(field)
				: field.equals("knownCommands") ? ItemDecoder.isNewVersion()
						? clazz.getSuperclass().getDeclaredField(field) : clazz.getDeclaredField(field) : null;
		objectField.setAccessible(true);
		Object result = objectField.get(object);
		objectField.setAccessible(false);
		return result;
	}

	protected void unRegisterBukkitCommand(PluginCommand cmd) {
		try {
			Object result = getPrivateField(plugin.getServer().getPluginManager(), "commandMap");
			SimpleCommandMap commandMap = (SimpleCommandMap) result;

			Object map = getPrivateField(commandMap, "knownCommands");
			@SuppressWarnings("unchecked")
			HashMap<String, Command> knownCommands = (HashMap<String, Command>) map;
			knownCommands.remove(cmd.getName());
			for (String alias : cmd.getAliases())
				knownCommands.remove(alias);
			knownCommands.remove("zshop:" + cmd.getName());
			for (String alias : cmd.getAliases())
				knownCommands.remove("zshop:" + alias);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
