package com.hunllef;

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
import com.hunllef.ui.HunllefPluginPanel;

import net.runelite.api.*;
import net.runelite.api.Point;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import com.hunllef.utils.ExtUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.HeadIcon;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
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

	private HunllefPluginPanel panel;

	private ScheduledExecutorService executorService;

	private int counter;
	private boolean isRanged;
	private boolean wasInInstance;
	private String targetPrayer = "ranged";
	private Random random = new Random();
	private AudioMode audioMode;

	private NavigationButton navigationButton;

	@Override
	protected void startUp() throws Exception
	{
		audioPlayer.tryLoadAudio(config, new String[]{SOUND_MAGE, SOUND_RANGE, SOUND_ONE, SOUND_TWO});
		audioMode = config.audioMode();

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
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (!config.autoHide())
		{
			return;
		}

		boolean isInInstance = isInTheGauntlet();
		if (isInInstance != wasInInstance)
		{
			updateNavigationBar(isInInstance, true);
			wasInInstance = isInInstance;
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

		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleAtFixedRate(this::tickCounter, 0, COUNTER_INTERVAL, TimeUnit.MILLISECONDS);
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
	}

	@Provides
	HunllefConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HunllefConfig.class);
	}

	private void tickCounter()
	{
		counter -= COUNTER_INTERVAL;
		panel.setTime(counter);

		if (counter == 2000)
		{
			playSoundClip(SOUND_TWO);
			return;
		}

		if (counter == 1000)
		{
			playSoundClip(SOUND_ONE);
			return;
		}

		/*net.runelite.api.HeadIcon overheadIcon = client.getLocalPlayer().getOverheadIcon();
		if(targetPrayer == "magic" && overheadIcon != HeadIcon.MAGIC) {
			setPrayer("magic");
		} else if(targetPrayer == "ranged" && overheadIcon != HeadIcon.RANGED) {
			setPrayer("ranged");
		}*/

		if (counter <= 0)
		{
			if (isRanged)
			{
				playSoundClip(SOUND_MAGE);
				panel.setStyle("Mage", Color.CYAN);
				targetPrayer = "magic";
				setPrayer("magic");
			}
			else
			{
				playSoundClip(SOUND_RANGE);
				panel.setStyle("Ranged", Color.GREEN);
				targetPrayer = "ranged";
				setPrayer("ranged");
			}

			isRanged = !isRanged;
			counter = ROTATION_DURATION;
		}
	}

	private void setPrayer(String targetPrayer)
	{
		clientThread.invoke(() ->
		{
			Widget activeWidget = getActiveWidget();

			// Prayer icon
			Widget PRAYER_ICON = client.getWidget(WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID, 70);

			executorService.submit(() -> clickAndDrawPoint(PRAYER_ICON, false));

			Widget PROTECT_FROM_MAGIC = this.client.getWidget(541, 21);
			Widget PROTECT_FROM_RANGED = this.client.getWidget(541, 22);
			//Widget PROTECT_FROM_MELEE = this.client.getWidget(541, 23);


				switch (targetPrayer)
				{
					case "magic":
						executorService.submit(() -> clickAndDrawPoint(PROTECT_FROM_MAGIC, false));
						break;
					case "ranged":
						executorService.submit(() -> clickAndDrawPoint(PROTECT_FROM_RANGED, false));
						break;
				}

			if (activeWidget != null)
				executorService.submit(() -> clickAndDrawPoint(activeWidget, false));
		});
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
