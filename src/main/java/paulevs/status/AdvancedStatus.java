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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancedStatus extends Plugin implements Listener {
	private static final Map<String, TextureAsset> ICONS = new HashMap<>();
	private final Map<String, PlayerStatusPanel> panelMap = new HashMap<>();
	private Style invisible;
	private Style statusIcons;
	private Style offset;
	
	@Override
	public void onEnable() {
		registerEventListener(this);
		invisible = new Style();
		invisible.visibility.set(Visibility.Hidden);
		
		offset = new Style();
		offset.position.set(Position.Absolute);
		offset.bottom.set(-1000);
		
		statusIcons = new Style();
		statusIcons.position.set(Position.Absolute);
		statusIcons.bottom.set(70);
		statusIcons.right.set(-50);
		
		String path = this.getPath();
		ICONS.put("brokenBones", TextureAsset.loadFromFile(path + "/icons/brokenBones.png"));
		ICONS.put("fixedBones", TextureAsset.loadFromFile(path + "/icons/fixedBones.png"));
		ICONS.put("bleeding", TextureAsset.loadFromFile(path + "/icons/bleeding.png"));
		
		System.out.println("Enabled AdvancedStatus plugin");
	}
	
	@Override
	public void onDisable() {
		System.out.println("Disabled AdvancedStatus plugin");
	}
	
	@EventMethod
	public void onPlayerConnect(PlayerConnectEvent event) {
		Player player = event.getPlayer();
		
		Internals.overwriteUIStyle(player, "HudLayer/hudContainer/statusContainer/rightContainer/statusIconContainer", invisible);
		Internals.overwriteUIStyle(player, "HudLayer/hudContainer/statusContainer/statusBarContainer/barContainer", offset);
		
		player.setListenForKeyInput(true);
		player.registerKeys(Key.F12);
		
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
		Arrays.stream(Server.getAllPlayers()).forEach(player -> {
			getPanel(player).update(player);
		});
	}
	
	@EventMethod
	public void onKeyPress(PlayerKeyEvent event) {
		if (event.getKey() != Key.F12 || !event.isPressed()) return;
		Player player = event.getPlayer();
		getPanel(player).screenshot(player);
	}
	
	private PlayerStatusPanel getPanel(final Player player) {
		return panelMap.computeIfAbsent(player.getUID(), key -> new PlayerStatusPanel(player));
	}
	
	private static final class PlayerStatusPanel {
		private final UIElement panelLeft;
		private final UIElement panelRight;
		private final UIElement barHealth;
		private final UIElement barHunger;
		private final UIElement barThirst;
		private final UIElement barStamina;
		
		private final List<UIElement> icons = new ArrayList<>();
		private final UIElement brokenBonesIcon;
		private final UIElement fixedBonesIcon;
		private final UIElement bleedingIcon;
		private int screenTimer;
		
		PlayerStatusPanel(Player player) {
			panelLeft = new UIElement();
			panelLeft.setSize(504, 54, false);
			panelLeft.style.left.set(20, Unit.Pixel);
			panelLeft.style.bottom.set(20, Unit.Pixel);
			panelLeft.style.position.set(Position.Absolute);
			panelLeft.setBackgroundColor(0x00000000);
			
			panelRight = new UIElement();
			panelRight.setSize(504, 54, false);
			panelRight.style.right.set(20, Unit.Pixel);
			panelRight.style.bottom.set(20, Unit.Pixel);
			panelRight.style.position.set(Position.Absolute);
			panelRight.setBackgroundColor(0x00000000);
			
			barHealth = makeBar(panelLeft, 0, 0, 0XB01A1AFF, Pivot.UpperLeft);
			barStamina = makeBar(panelLeft, 0, 28, 0XF7F027FF, Pivot.UpperLeft);
			
			barHunger = makeBar(panelRight, 0, 0, 0X2FC32CFF, Pivot.UpperRight);
			barThirst = makeBar(panelRight, 0, 28, 0X294BE1FF, Pivot.UpperRight);
			
			brokenBonesIcon = makeIcon("brokenBones", player.hasBrokenBones());
			panelRight.addChild(brokenBonesIcon);
			
			fixedBonesIcon = makeIcon("fixedBones", false);
			panelRight.addChild(fixedBonesIcon);
			
			bleedingIcon = makeIcon("bleeding", player.isBleeding());
			panelRight.addChild(brokenBonesIcon);
			
			sortIcons();
			
			player.addUIElement(panelLeft);
			player.addUIElement(panelRight);
		}
		
		void screenshot(Player player) {
			screenTimer = 20;
			panelLeft.style.visibility.set(Visibility.Hidden);
			panelRight.style.visibility.set(Visibility.Hidden);
			player.addUIElement(panelLeft);
			player.addUIElement(panelRight);
		}
		
		void update(Player player) {
			boolean forceUpdate = false;
			
			if (screenTimer > 0) {
				screenTimer--;
				if (screenTimer == 0) {
					panelLeft.style.visibility.set(Visibility.Visible);
					panelRight.style.visibility.set(Visibility.Visible);
					forceUpdate = true;
				}
				else return;
			}
			
			boolean update = forceUpdate;
			update |= updateBar(barHealth, (float) player.getHealth() / (float) player.getMaxHealth());
			update |= updateBar(barStamina, (float) player.getStamina() / (float) player.getMaxStamina());
			
			if (brokenBonesIcon.style.visibility.get() == Visibility.Visible && !player.hasBrokenBones()) {
				brokenBonesIcon.style.visibility.set(Visibility.Hidden);
				fixedBonesIcon.style.visibility.set(Visibility.Visible);
				update = true;
			}
			else if (fixedBonesIcon.style.visibility.get() == Visibility.Visible && player.getHealth() == player.getMaxHealth()) {
				fixedBonesIcon.style.visibility.set(Visibility.Hidden);
				update = true;
			}
			
			update |= updateIcon(brokenBonesIcon, player.hasBrokenBones());
			update |= updateIcon(bleedingIcon, player.isBleeding());
			
			if (update) {
				sortIcons();
				player.addUIElement(panelLeft);
			}
			
			update = forceUpdate;
			update |= updateBar(barHunger, (float) player.getHunger() / 100.0F);
			update |= updateBar(barThirst, (float) player.getThirst() / 100.0F);
			if (update) player.addUIElement(panelRight);
		}
		
		private boolean updateBar(UIElement bar, float value) {
			int width1 = (int) bar.style.width.get();
			bar.style.width.set(Math.round(value * 500));
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
			back.setBackgroundColor(0x000000E6);
			back.setPosition(x, y, false);
			back.setPivot(Pivot.UpperLeft);
			back.setSize(504, 24, false);
			panel.addChild(back);
			
			UIElement bar = new UIElement();
			bar.setBackgroundColor(color);
			bar.setPivot(pivot);
			bar.setSize(500, 20, false);
			x = pivot == Pivot.UpperRight ? 502 : 2;
			bar.setPosition(x, 2, false);
			back.addChild(bar);
			
			return bar;
		}
		
		private UIElement makeIcon(String name, boolean visible) {
			UIElement icon = new UIElement();
			icon.style.backgroundImage.set(ICONS.get(name));
			icon.style.visibility.set(visible ? Visibility.Visible : Visibility.Hidden);
			icon.style.position.set(Position.Absolute);
			icon.style.bottom.set(80, Unit.Pixel);
			icon.style.right.set(20, Unit.Pixel);
			icon.setSize(64, 64, false);
			icons.add(icon);
			return icon;
		}
		
		private void sortIcons() {
			int x = 20;
			for (UIElement icon: icons) {
				if (icon.style.visibility.get() == Visibility.Hidden) continue;
				icon.style.right.set(x);
				x += 64;
			}
		}
	}
}
