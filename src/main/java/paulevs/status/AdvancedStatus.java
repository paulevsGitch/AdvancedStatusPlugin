package paulevs.status;

import net.risingworld.api.Internals;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.assets.TextureAsset;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.general.UpdateEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerKeyEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.events.player.PlayerUpdateStatusEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.Position;
import net.risingworld.api.ui.style.Style;
import net.risingworld.api.ui.style.Unit;
import net.risingworld.api.ui.style.Visibility;
import net.risingworld.api.utils.Key;
import net.unilib.config.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdvancedStatus extends Plugin implements Listener {
	private static final String QUICK_HELP_CONTAINER = "HudLayer/hudContainer/statusContainer/statusBarContainer/quickHelpContainer";
	private static final String ELEMENT_CONTAINER = "HudLayer/hudContainer/statusContainer/centerContainer/elementContainer";
	private static final String ICON_CONTAINER = "HudLayer/hudContainer/statusContainer/rightContainer/statusIconContainer";
	private static final String BAR_CONTAINER = "HudLayer/hudContainer/statusContainer/statusBarContainer/barContainer";
	private static final String SELECTOR_INFO_LABEL = "HudLayer/hudContainer/statusContainer/selectorInfoLabel";
	private static final String RIGHT_CONTAINER = "HudLayer/hudContainer/statusContainer/rightContainer";
	private static final String LEFT_CONTAINER = "HudLayer/hudContainer/leftContainer";
	private static final String THIRST_ICON = ICON_CONTAINER + "/thirstIcon";
	private static final String HUNGER_ICON = ICON_CONTAINER + "/hungerIcon";
	
	private static final Map<String, TextureAsset> ICONS = new HashMap<>();
	private final Map<String, PlayerStatusPanel> panelMap = new HashMap<>();
	private static Config config;
	private static int barWidth;
	private static int barHeight;
	private static int barGap;
	private static int barBottom;
	private static int barSides;
	private static int iconSize;
	
	private static int backColor;
	private static int healthColor;
	private static int staminaColor;
	private static int hungerColor;
	private static int thirstColor;
	
	private boolean hideOnScreens;
	private List<String> pluginJars;
	private Key screenshotKey;
	
	@Override
	public void onEnable() {
		registerEventListener(this);
		
		String path = this.getPath();
		ICONS.put("brokenBones", TextureAsset.loadFromFile(path + "/icons/brokenBones.png"));
		ICONS.put("fixedBones", TextureAsset.loadFromFile(path + "/icons/fixedBones.png"));
		ICONS.put("bleeding", TextureAsset.loadFromFile(path + "/icons/bleeding.png"));
		
		config = Config.make(this, "config");
		config.addEntry("replaceHealthAndStamina", true, "Replace health and stamina bars with custom", "Default is true");
		config.addEntry("replaceHungerAndThirst", true, "Replace hunger and thirst icons with custom bars", "Default is true");
		config.addEntry("useCustomIcons", true, "Use custom effect icons instead of built-in", "For example for broken bones status", "Default is true");
		config.addEntry("customIconSize", 64, "Custom icons size (in pixels)", "Default is 64");
		config.addEntry("hideOnScreenshots", true, "Hide custom UI during screenshot", "Default is true");
		config.addEntry("screenshotKey", "F12", "Screenshot key (only detects when user take a screenshot)", "Default is F12");
		config.addEntry("customBarHeight", 20, "Custom bars height (in pixels)", "Default is 20");
		config.addEntry("customBarWidth", 500, "Custom bars width (in pixels)", "Default is 500");
		config.addEntry("customBarGap", 8, "Gap between bars (in pixels)", "Default is 8");
		config.addEntry("bottomOffset", 20, "Offset for custom bars from screen bottom (in pixels)", "Default is 20");
		config.addEntry("sidesOffset", 20, "Offset for custom bars from screen sides (in pixels)", "Default is 20");
		config.addEntry("barsBackgroundColor", "#000000E6", "Background color for bars (outline), in RGBA format", "Default is #000000E6");
		config.addEntry("healthBarColor", "#B01A1AFF", "Color for health bar in RGBA (or RGB) format", "Default is #B01A1AFF");
		config.addEntry("staminaBarColor", "#F7F027FF", "Color for stamina bar in RGBA (or RGB) format", "Default is #F7F027FF");
		config.addEntry("hungerBarColor", "#2FC32CFF", "Color for hunger bar in RGBA (or RGB) format", "Default is #2FC32CFF");
		config.addEntry("thirstBarColor", "#294BE1FF", "Color for thirst bar in RGBA (or RGB) format", "Default is #294BE1FF");
		config.save();
		
		hideOnScreens = config.getBool("hideOnScreenshots");
		screenshotKey = Key.F12.parse(config.getString("screenshotKey"));
		
		barWidth = config.getInt("customBarWidth");
		barHeight = config.getInt("customBarHeight");
		barGap = config.getInt("customBarGap");
		barBottom = config.getInt("bottomOffset");
		barSides = config.getInt("sidesOffset");
		
		backColor = parseColor(config.getString("barsBackgroundColor"));
		healthColor = parseColor(config.getString("healthBarColor"));
		staminaColor = parseColor(config.getString("staminaBarColor"));
		hungerColor = parseColor(config.getString("hungerBarColor"));
		thirstColor = parseColor(config.getString("thirstBarColor"));
		iconSize = config.getInt("customIconSize");
		
		pluginJars = getPluginJars(this);
		
		System.out.println("Enabled AdvancedStatus plugin");
	}
	
	@Override
	public void onDisable() {
		System.out.println("Disabled AdvancedStatus plugin");
	}
	
	@EventMethod
	public void onPlayerConnect(PlayerConnectEvent event) {
		Player player = event.getPlayer();
		
		if (player.isAdmin() && pluginJars.size() > 1) {
			StringBuilder message = new StringBuilder("Plugin Advanced Status have ");
			message.append(pluginJars.size());
			message.append(" jars:\n");
			pluginJars.forEach(jar -> {
				message.append(jar);
				message.append("\n");
			});
			message.append("There should be only one jar (the latest one with largest version number), otherwise plugin will not work correctly");
			player.showErrorMessageBox("Too Many Jars", message.toString());
		}
		
		boolean icons = config.getBool("useCustomIcons");
		
		if (config.getBool("replaceHungerAndThirst")) {
			Internals.overwriteUIStyle(player, THIRST_ICON, offsetInvisible());
			Internals.overwriteUIStyle(player, HUNGER_ICON, offsetInvisible());
		}
		
		if (icons) {
			Internals.overwriteUIStyle(player, ICON_CONTAINER, invisible());
		}
		
		if (config.getBool("replaceHealthAndStamina")) {
			Internals.overwriteUIStyle(player, BAR_CONTAINER, offsetInvisible());
			Internals.overwriteUIStyle(player, LEFT_CONTAINER, offsetLeft(0));
			Internals.overwriteUIStyle(player, RIGHT_CONTAINER, offsetRight(barSides));
			Internals.overwriteUIStyle(player, SELECTOR_INFO_LABEL, offsetLeft(barSides));
			Internals.overwriteUIStyle(player, ELEMENT_CONTAINER, offsetLeft(0));
			Internals.overwriteUIStyle(player, QUICK_HELP_CONTAINER, offsetLeft(0));
		}
		
		player.setListenForKeyInput(true);
		player.registerKeys(screenshotKey);
		
		getPanel(player).update(player);
	}
	
	@EventMethod
	public void onPlayerRespawn(PlayerSpawnEvent event) {
		Player player = event.getPlayer();
		getPanel(player).update(player);
	}
	
	@EventMethod
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		panelMap.remove(event.getPlayer().getUID());
	}
	
	@EventMethod
	public void updateStatus(PlayerUpdateStatusEvent event) {
		Player player = event.getPlayer();
		getPanel(player).update(player);
	}
	
	@EventMethod
	public void updateAllPlayers(UpdateEvent event) {
		Arrays.stream(Server.getAllPlayers()).forEach(player ->
			getPanel(player).update(player)
		);
	}
	
	@EventMethod
	public void onKeyPress(PlayerKeyEvent event) {
		if (!hideOnScreens || event.getKey() != screenshotKey || !event.isPressed()) return;
		Player player = event.getPlayer();
		getPanel(player).screenshot(player);
	}
	
	private Style offsetLeft(int x) {
		Style style = new Style();
		style.position.set(Position.Absolute);
		style.bottom.set(barHeight * 2 + barGap + barBottom + 8, Unit.Pixel);
		style.left.set(x, Unit.Pixel);
		return style;
	}
	
	private Style offsetRight(int x) {
		Style style = new Style();
		style.position.set(Position.Absolute);
		style.bottom.set(barHeight * 2 + barGap + barBottom + 8, Unit.Pixel);
		style.right.set(x, Unit.Pixel);
		return style;
	}
	
	private Style offsetInvisible() {
		Style style = new Style();
		style.position.set(Position.Absolute);
		style.bottom.set(-1000, Unit.Pixel);
		return style;
	}
	
	private Style invisible() {
		Style style = new Style();
		style.visibility.set(Visibility.Hidden);
		return style;
	}
	
	private PlayerStatusPanel getPanel(final Player player) {
		return panelMap.computeIfAbsent(player.getUID(), key -> new PlayerStatusPanel(player));
	}
	
	private int parseColor(String color) {
		color = color.substring(1);
		int a = color.length() == 6 ? 255 : Integer.parseInt(color.substring(6), 16);
		int r = Integer.parseInt(color.substring(0, 2), 16);
		int g = Integer.parseInt(color.substring(2, 4), 16);
		int b = Integer.parseInt(color.substring(4, 6), 16);
		return r << 24 | g << 16 | b << 8 | a;
	}
	
	private List<String> getPluginJars(AdvancedStatus self) {
		File[] files = new File(self.getPath()).listFiles();
		if (files == null) return Collections.emptyList();
		return Arrays.stream(files).map(File::getName).filter(name -> name.endsWith(".jar")).collect(Collectors.toList());
	}
	
	private static final class PlayerStatusPanel {
		private final List<UIElement> icons = new ArrayList<>();
		private final UIElement globalPanel;
		
		private UIElement panelLeft;
		private UIElement panelRight;
		private UIElement barHealth;
		private UIElement barHunger;
		private UIElement barThirst;
		private UIElement barStamina;
		
		private UIElement brokenBonesIcon;
		private UIElement fixedBonesIcon;
		private UIElement bleedingIcon;
		private int screenTimer;
		
		PlayerStatusPanel(Player player) {
			globalPanel = new UIElement();
			globalPanel.setSize(100, 0, true);
			globalPanel.style.left.set(0, Unit.Pixel);
			globalPanel.style.bottom.set(0, Unit.Pixel);
			globalPanel.style.position.set(Position.Absolute);
			
			if (config.getBool("replaceHealthAndStamina")) {
				panelLeft = new UIElement();
				panelLeft.setSize(barWidth + 4, barHeight * 2 + barGap, false);
				panelLeft.style.left.set(barSides, Unit.Pixel);
				panelLeft.style.bottom.set(barBottom, Unit.Pixel);
				panelLeft.style.position.set(Position.Absolute);
				panelLeft.setBackgroundColor(0x00000000);
				globalPanel.addChild(panelLeft);
				
				barHealth = makeBar(panelLeft, 0, 0, healthColor, Pivot.UpperLeft);
				barStamina = makeBar(panelLeft, 0, barHeight + barGap, staminaColor, Pivot.UpperLeft);
			}
			
			boolean hunger = config.getBool("replaceHungerAndThirst");
			boolean icons = config.getBool("useCustomIcons");
			
			if (hunger || icons) {
				panelRight = new UIElement();
				panelRight.setSize(barWidth + 4, barHeight * 2 + barGap, false);
				panelRight.style.right.set(barSides, Unit.Pixel);
				panelRight.style.bottom.set(barBottom, Unit.Pixel);
				panelRight.style.position.set(Position.Absolute);
				panelRight.setBackgroundColor(0x00000000);
				globalPanel.addChild(panelRight);
			}
			
			if (hunger) {
				barHunger = makeBar(panelRight, 0, 0, hungerColor, Pivot.UpperRight);
				barThirst = makeBar(panelRight, 0, barHeight + barGap, thirstColor, Pivot.UpperRight);
			}
			
			if (icons) {
				brokenBonesIcon = makeIcon("brokenBones", player.hasBrokenBones());
				panelRight.addChild(brokenBonesIcon);
				
				fixedBonesIcon = makeIcon("fixedBones", false);
				panelRight.addChild(fixedBonesIcon);
				
				bleedingIcon = makeIcon("bleeding", player.isBleeding());
				panelRight.addChild(brokenBonesIcon);
				
				sortIcons();
			}
			
			player.addUIElement(globalPanel);
		}
		
		void screenshot(Player player) {
			screenTimer = 10;
			globalPanel.style.visibility.set(Visibility.Hidden);
			player.addUIElement(globalPanel);
		}
		
		void update(Player player) {
			boolean update = false;
			
			if (screenTimer > 0) {
				screenTimer--;
				if (screenTimer == 0) {
					globalPanel.style.visibility.set(Visibility.Visible);
					update = true;
				}
				else return;
			}
			
			if (panelLeft != null) {
				update |= updateBar(barHealth, (float) player.getHealth() / (float) player.getMaxHealth());
				update |= updateBar(barStamina, (float) player.getStamina() / (float) player.getMaxStamina());
			}
			
			if (panelRight != null) {
				update |= updateBar(barHunger, (float) player.getHunger() / 100.0F);
				update |= updateBar(barThirst, (float) player.getThirst() / 100.0F);
				
				if (brokenBonesIcon != null) {
					update |= updateIcon(brokenBonesIcon, player.hasBrokenBones());
					update |= updateIcon(fixedBonesIcon, player.hasHealedBones());
					update |= updateIcon(bleedingIcon, player.isBleeding());
					if (update) {
						sortIcons();
					}
				}
			}
			
			if (update) player.addUIElement(globalPanel);
		}
		
		private boolean updateBar(UIElement bar, float value) {
			int width1 = (int) bar.style.width.get();
			bar.style.width.set(Math.round(value * barWidth));
			int width2 = (int) bar.style.width.get();
			return width1 != width2;
		}
		
		private boolean updateIcon(UIElement icon, boolean visible) {
			Visibility visibility1 = icon.style.visibility.get();
			icon.style.visibility.set(visible ? Visibility.Visible : Visibility.Hidden);
			Visibility visibility2 = icon.style.visibility.get();
			return visibility1 != visibility2;
		}
		
		private UIElement makeBar(UIElement panel, int x, int y, int color, Pivot pivot) {
			UIElement back = new UIElement();
			back.setBackgroundColor(backColor);
			back.setPosition(x, y, false);
			back.setPivot(Pivot.UpperLeft);
			back.setSize(barWidth + 4, barHeight + 4, false);
			panel.addChild(back);
			
			UIElement bar = new UIElement();
			bar.setBackgroundColor(color);
			bar.setPivot(pivot);
			bar.setSize(barWidth, barHeight, false);
			x = pivot == Pivot.UpperRight ? barWidth + 2 : 2;
			bar.setPosition(x, 2, false);
			back.addChild(bar);
			
			return bar;
		}
		
		private UIElement makeIcon(String name, boolean visible) {
			UIElement icon = new UIElement();
			icon.style.backgroundImage.set(ICONS.get(name));
			icon.style.visibility.set(visible ? Visibility.Visible : Visibility.Hidden);
			icon.style.position.set(Position.Absolute);
			icon.style.bottom.set(barHeight * 2 + barGap + barBottom - 8, Unit.Pixel);
			icon.style.right.set(barSides, Unit.Pixel);
			icon.setSize(iconSize, iconSize, false);
			icons.add(icon);
			return icon;
		}
		
		private void sortIcons() {
			int x = barSides;
			for (UIElement icon: icons) {
				if (icon.style.visibility.get() == Visibility.Hidden) continue;
				icon.style.right.set(x);
				x += iconSize;
			}
		}
	}
}
