package com.hunllef;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;

import static com.hunllef.PluginConstants.ATTACK_DURATION;
import static com.hunllef.PluginConstants.COUNTER_INTERVAL;
import static com.hunllef.PluginConstants.INITIAL_COUNTER;
import static com.hunllef.PluginConstants.REGION_IDS_GAUNTLET;
import static com.hunllef.PluginConstants.ROTATION_DURATION;
import static com.hunllef.PluginConstants.SOUND_MAGE;
import static com.hunllef.PluginConstants.SOUND_ONE;
import static com.hunllef.PluginConstants.SOUND_RANGE;
import static com.hunllef.PluginConstants.SOUND_TWO;

import com.hunllef.mappers.WidgetID;
import com.hunllef.objects.Tornado;
import com.hunllef.ui.HunllefPluginPanel;
import lombok.Getter;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.ui.overlay.OverlayManager;

import net.runelite.api.*;
import net.runelite.api.Point;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import com.hunllef.utils.ExtUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Hunllef Automater"
)
public class HunllefPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private HunllefConfig config;

	@Inject
	private AudioPlayer audioPlayer;

	@Inject
	private ExtUtils extUtils;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HunllefOverlay overlay;


	private HunllefPluginPanel panel;

	private ScheduledExecutorService executorService;

	private int counter;
	private boolean isRanged;
	private boolean wasInInstance;
	private String targetPrayer = "ranged";
	private Random random = new Random();
	private AudioMode audioMode;

	private boolean protectFromMissilesActivated = false;
	private boolean protectFromMagicActivated = false;
	private int waitTicks = 0;

	private Item lastWeapon = null;
	private boolean steelSkinWasDisabled = false;
	private boolean running = false;

	private String protectionToEnable = "";
	private String offensiveToEnable = "";
	private String defenseToEnable = "";

	private int attacks = 0;

	private NavigationButton navigationButton;

	private static final Set<Integer> TORNADO_NPC_IDS = ImmutableSet.of(9025, 9039);
	private static final int BOW_ATTACK = 426;
	private static final int STAFF_ATTACK = 1167;
	//private static final int LIGHTNING_ANIMATION = 8418;
	private static final Set<Integer> MELEE_ATTACK = ImmutableSet.of(395, 401, 400, 401, 386, 390, 422, 423, 401, 428, 440);
	private static final Set<Integer> CRYSTAL_BOWS = ImmutableSet.of(ItemID.CRYSTAL_BOW_PERFECTED, ItemID.CRYSTAL_BOW_ATTUNED, ItemID.CRYSTAL_BOW_BASIC,  ItemID.CORRUPTED_BOW_PERFECTED,  ItemID.CORRUPTED_BOW_ATTUNED, ItemID.CORRUPTED_BOW_BASIC);
	private static final Set<Integer> CRYSTAL_STAFF = ImmutableSet.of(ItemID.CRYSTAL_STAFF_PERFECTED, ItemID.CRYSTAL_STAFF_ATTUNED, ItemID.CORRUPTED_STAFF_BASIC, ItemID.CORRUPTED_STAFF_PERFECTED, ItemID.CORRUPTED_STAFF_ATTUNED, ItemID.CORRUPTED_STAFF_BASIC);


			/*
			if ((newWeapon.getId() == ItemID.CRYSTAL_BOW_PERFECTED || newWeapon.getId() == ItemID.CRYSTAL_BOW_ATTUNED || newWeapon.getId() == ItemID.CRYSTAL_BOW_BASIC
						|| newWeapon.getId() == ItemID.CORRUPTED_BOW_PERFECTED || newWeapon.getId() == ItemID.CORRUPTED_BOW_ATTUNED || newWeapon.getId() == ItemID.CORRUPTED_BOW_BASIC)) {
					offensiveToEnable = "eagle_eye";
				} else if ((newWeapon.getId() == ItemID.CRYSTAL_STAFF_PERFECTED || newWeapon.getId() == ItemID.CRYSTAL_STAFF_ATTUNED || newWeapon.getId() == ItemID.CRYSTAL_STAFF_BASIC
						|| newWeapon.getId() == ItemID.CORRUPTED_STAFF_PERFECTED || newWeapon.getId() == ItemID.CORRUPTED_STAFF_ATTUNED || newWeapon.getId() == ItemID.CORRUPTED_STAFF_BASIC)) {
					offensiveToEnable = "mystic_might";
				}
			 */

	//private static final Set<Integer> PLAYER_ANIMATIONS = ImmutableSet.of(395, 401, 400, 401, 386, 390, 422, 423, 401, 428, 440, 426, 1167);

	@Getter
	private Set<Tornado> tornadoes = new HashSet<>();

	/*
		8754 mage
		8755 range
	 */
	@Override
	protected void startUp() throws Exception
	{
		audioPlayer.tryLoadAudio(config, new String[]{SOUND_MAGE, SOUND_RANGE, SOUND_ONE, SOUND_TWO});
		audioMode = config.audioMode();

		overlayManager.add(overlay);

		panel = injector.getInstance(HunllefPluginPanel.class);

		navigationButton = NavigationButton
			.builder()
			.tooltip("Hunllef Helper")
			.icon(ImageUtil.loadImageResource(getClass(), "/nav-icon.png"))
			.priority(100)
			.panel(panel)
			.build();

		panel.setCounterActiveState(false);

		wasInInstance = isInTheGauntlet();
		updateNavigationBar((!config.autoHide() || wasInInstance), false);
	}

	@Override
	protected void shutDown() throws Exception
	{
		updateNavigationBar(false, false);
		shutdownExecutorService();
		panel = null;
		navigationButton = null;
		audioPlayer.unloadAudio();
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged animationChanged) {
		if(!running) {
			return;
		}

		final Actor actor = animationChanged.getActor();
		if(actor instanceof Player) {
			final Player player = (Player) actor;
			final int anim = player.getAnimation();

			if (player.getName() == null || client.getLocalPlayer() == null || !player.getName().equals(client.getLocalPlayer().getName()) || anim == -1)
			{
				return;
			}

			switch(anim) {
				case BOW_ATTACK:
				case STAFF_ATTACK:
					attacks++;
					break;

				default:
					//TODO: check if attack really goes through all anims, only count 1 as attack.. otherwise it will switch right away
					if(MELEE_ATTACK.contains(anim)) {
						attacks++;
					}
					break;
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		final NPC npc = event.getNpc();
		if (TORNADO_NPC_IDS.contains(npc.getId()))
		{
			tornadoes.add(new Tornado(npc));
		}
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned event)
	{
		final NPC npc = event.getNpc();
		if (TORNADO_NPC_IDS.contains(npc.getId()))
		{
			tornadoes.removeIf(tornado -> tornado.getNpc() == npc);
		}
	}

	private Item GetWeapon() {
		return client.getItemContainer(InventoryID.INVENTORY).getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
	}

	private void SwitchWeapon() {
		Item newWeapon = Arrays.stream(client.getItemContainer(InventoryID.INVENTORY).getItems()).filter(i -> CRYSTAL_BOWS.contains(i.getId()) || CRYSTAL_STAFF.contains(i.getId())).findFirst().get();
		equipItem(newWeapon);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (!config.autoHide()) {
			return;
		}

		if (!tornadoes.isEmpty())
		{
			tornadoes.forEach(Tornado::updateTimeLeft);
		}

		boolean isInInstance = isInTheGauntlet();
		if (isInInstance != wasInInstance)
		{
			updateNavigationBar(isInInstance, true);
			wasInInstance = isInInstance;
		}

		if(config.autoSwitchWeapons()) {
			if(attacks >= 6) {
				SwitchWeapon();
				attacks = 0;
			}
		}

		protectFromMissilesActivated = client.getLocalPlayer().getOverheadIcon() == HeadIcon.RANGED;
		protectFromMagicActivated = client.getLocalPlayer().getOverheadIcon() == HeadIcon.MAGIC;

		if(waitTicks > 0) {
			waitTicks--;
			return;
		}

		if(running) {
			/*if(!client.isPrayerActive(Prayer.STEEL_SKIN)) {
				if(steelSkinWasDisabled) {
					defenseToEnable = "steel_skin";
					steelSkinWasDisabled = false;
				} else {
					steelSkinWasDisabled = true;
				}
			}*/

			Item newWeapon = GetWeapon();
			if (newWeapon != null && (lastWeapon == null || (lastWeapon != null && lastWeapon.getId() != newWeapon.getId()))) {
				lastWeapon = newWeapon;
				final int newId = newWeapon.getId();
				if(CRYSTAL_BOWS.contains(newId)) {
					offensiveToEnable = "eagle_eye";
				} else if(CRYSTAL_STAFF.contains(newId)) {
					offensiveToEnable = "mystic_might";
				}
			}
			//}

			if (config.autoPray()) {
				if (targetPrayer == "magic" && !protectFromMagicActivated) {
					protectionToEnable = "magic";
				} else if (targetPrayer == "ranged" && !protectFromMissilesActivated) {
					protectionToEnable = "ranged";
				}
			}

			if(protectionToEnable != "" || offensiveToEnable != "" || defenseToEnable != "") {
				setPrayers();
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		wasInInstance = isInTheGauntlet();
		updateNavigationBar((!config.autoHide() || wasInInstance), false);

		if (audioMode != config.audioMode())
		{
			audioPlayer.unloadAudio();
			audioPlayer.tryLoadAudio(config, new String[]{SOUND_MAGE, SOUND_RANGE, SOUND_ONE, SOUND_TWO});
			audioMode = config.audioMode();
		}
	}

	public void start(boolean withRanged)
	{
		isRanged = withRanged;

		if (withRanged)
		{
			panel.setStyle("Ranged", Color.GREEN);
		}
		else
		{
			panel.setStyle("Mage", Color.CYAN);
		}
		panel.setCounterActiveState(true);
		counter = INITIAL_COUNTER;
		attacks = 1;

		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleAtFixedRate(this::tickCounter, 0, COUNTER_INTERVAL, TimeUnit.MILLISECONDS);
		running = true;
	}

	public void trample()
	{
		counter += ATTACK_DURATION;
	}

	public void reset()
	{
		shutdownExecutorService();
		panel.setCounterActiveState(false);
		targetPrayer = "ranged";
		running = false;
	}

	@Provides
	HunllefConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HunllefConfig.class);
	}

	private void tickCounter() {
		try {
			counter -= COUNTER_INTERVAL;
			panel.setTime(counter);

			if (counter == 2000) {
				//playSoundClip(SOUND_TWO);
				return;
			}

			if (counter == 1000) {
				//playSoundClip(SOUND_ONE);
				return;
			}

			//if(config.autoDmgPrays()) {

			if (counter <= 0) {
				if (isRanged) {
					//playSoundClip(SOUND_MAGE);
					panel.setStyle("Mage", Color.CYAN);
					targetPrayer = "magic";
					//setPrayer("magic");
				} else {
					//playSoundClip(SOUND_RANGE);
					panel.setStyle("Ranged", Color.GREEN);
					targetPrayer = "ranged";
					//setPrayer("ranged");
				}

				isRanged = !isRanged;
				counter = ROTATION_DURATION;
			}
		}
		catch (Exception e) {
			log.error("error: " + e);
		}
	}

	private void equipItem(Item item) {
		Widget activeWidget = getActiveWidget();
		if (activeWidget.getId() != WidgetID.INVENTORY_GROUP_ID) {
			Widget PRAYER_ICON = client.getWidget(WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID, 61);
			executorService.submit(() -> clickAndDrawPoint(PRAYER_ICON, false));
		}

		Widget itemWidget = extUtils.getItems(item.getId()).stream().findFirst().get();
		if(itemWidget == null) {
			return;
		}

		executorService.submit(() -> clickAndDrawPoint(itemWidget, false));
	}


	private void setPrayers() {
		waitTicks = 1;

		//clientThread.invoke(() ->
		//{

		Widget activeWidget = getActiveWidget();

		if (activeWidget.getId() != WidgetID.PRAYER_GROUP_ID) {
			Widget PRAYER_ICON = client.getWidget(WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID, 70);
			executorService.submit(() -> clickAndDrawPoint(PRAYER_ICON, false));
		}

		String[] allToEnable = new String[]{offensiveToEnable, protectionToEnable, defenseToEnable};
		for (String enable : allToEnable) {


			switch (enable) {
				case "magic":
					executorService.submit(() -> clickAndDrawPoint(this.client.getWidget(541, 21), false));
					break;

				case "ranged":
					executorService.submit(() -> clickAndDrawPoint(this.client.getWidget(541, 22), false));
					break;

				case "eagle_eye":
					executorService.submit(() -> clickAndDrawPoint(this.client.getWidget(541, 29), false));
					break;

				case "mystic_might":
					executorService.submit(() -> clickAndDrawPoint(this.client.getWidget(541, 32), false));
					break;

				case "steel_skin":
					executorService.submit(() -> clickAndDrawPoint(this.client.getWidget(541, 18), false));
					break;
			}
		}

		offensiveToEnable = "";
		protectionToEnable = "";
		defenseToEnable = "";

		if (activeWidget != null)
			executorService.submit(() -> clickAndDrawPoint(activeWidget, false));
		//});
	}

	private void clickAndDrawPoint(Widget widgetToClick, boolean simulateOnly)
	{
		Point pt = new Point((int) widgetToClick.getBounds().getX(), (int) widgetToClick.getBounds().getY());

		double width = 1;
		double height = 1;

		if (client.isStretchedEnabled())
		{
			final Dimension stretched = client.getStretchedDimensions();
			final Dimension real = client.getRealDimensions();
			width = (stretched.width / real.getWidth());
			height = (stretched.height / real.getHeight());
			pt = new Point((int) (pt.getX() * width), (int) (pt.getY() * height));
		}

		Point finalPt = pt;
		double finalWidth = width;
		double finalHeight = height;

		Rectangle rect = new Rectangle(finalPt.getX(), finalPt.getY(), (int) (widgetToClick.getWidth() * finalWidth), (int) (widgetToClick.getHeight() * finalHeight));
		Point clickPoint = extUtils.getClickPoint(rect);

		clientThread.invoke(() ->
				{
//					log.info(finalPt.getX() + "," + finalPt.getY() + "," + (int) (widgetToClick.getWidth() * finalWidth) + "," +  (int) (widgetToClick.getHeight() * finalHeight));
					log.info("[" + widgetToClick.getId() + "]" + "(" + widgetToClick.getName() + ") " + clickPoint.getX() + "," + clickPoint.getY());

//					client.getCanvas().getGraphics().drawRect(finalPt.getX(), finalPt.getY(), (int) (widgetToClick.getWidth() * finalWidth), (int) (widgetToClick.getHeight() * finalHeight));
					client.getCanvas().getGraphics().drawOval(clickPoint.getX() - 5, clickPoint.getY() - 5, 10, 10);
				}
		);

		if (!simulateOnly)
			extUtils.click(clickPoint, false);

		try
		{
			Thread.sleep(randomDelay());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private long randomDelay() {
		return (long) clamp(
				Math.round(random.nextGaussian() * config.getDeviation() + config.getTarget())
		);
	}

	private double clamp(double val)
	{
		return Math.max(120, Math.min(240, val));
	}

	private Widget getActiveWidget()
	{
		Widget COMBAT = client.getWidget(WidgetID.COMBAT_GROUP_ID, 0);

		if (COMBAT != null && !COMBAT.isHidden())
			return client.getWidget(WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID, 58);

		Widget SKILLS = client.getWidget(WidgetID.SKILLS_GROUP_ID, 0);

		if (SKILLS != null && !SKILLS.isHidden())
			return client.getWidget(WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID, 59);

		Widget INVENTORY = client.getWidget(WidgetID.INVENTORY_GROUP_ID, 0);

		if (INVENTORY != null && !INVENTORY.isHidden())
			return client.getWidget(WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID, 61);

		Widget QUESTS = client.getWidget(WidgetID.QUESTLIST_GROUP_ID, 0);

		if (QUESTS != null && !QUESTS.isHidden())
			return client.getWidget(WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID, 60);

		Widget EQUIPMENT = client.getWidget(WidgetID.EQUIPMENT_GROUP_ID, 0);

		if (EQUIPMENT != null && !EQUIPMENT.isHidden())
			return client.getWidget(WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID, 62);

		Widget PRAYER = client.getWidget(WidgetID.PRAYER_GROUP_ID, 0);

		if (PRAYER != null && !PRAYER.isHidden())
			return client.getWidget(WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID, 63);

		Widget SPELLBOOK = client.getWidget(WidgetID.SPELLBOOK_GROUP_ID, 0);

		if (SPELLBOOK != null && !SPELLBOOK.isHidden())
			return client.getWidget(WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID, 64);

		return null;
	}

	private void playSoundClip(String soundFile)
	{
		if (config.audioMode() == AudioMode.Disabled)
		{
			return;
		}

		executorService.submit(() -> audioPlayer.playSoundClip(soundFile));
	}

	private boolean isInTheGauntlet()
	{
		Player player = client.getLocalPlayer();

		if (player == null)
		{
			return false;
		}

		int regionId = WorldPoint.fromLocalInstance(client, player.getLocalLocation()).getRegionID();
		return REGION_IDS_GAUNTLET.contains(regionId);
	}

	private void updateNavigationBar(boolean enable, boolean selectPanel)
	{
		if (enable)
		{
			clientToolbar.addNavigation(navigationButton);
			if (selectPanel)
			{
				SwingUtilities.invokeLater(() ->
				{
					if (!navigationButton.isSelected())
					{
						navigationButton.getOnSelect().run();
					}
				});
			}
		}
		else
		{
			reset();
			navigationButton.setSelected(false);
			clientToolbar.removeNavigation(navigationButton);
		}
	}

	private void shutdownExecutorService()
	{
		if (executorService != null)
		{
			executorService.shutdownNow();
			try
			{
				if (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS))
				{
					log.warn("Executor service dit not shut down within the allocated timeout.");
				}
			}
			catch (InterruptedException ex)
			{
				Thread.currentThread().interrupt();
			}
			executorService = null;
		}
	}
}
