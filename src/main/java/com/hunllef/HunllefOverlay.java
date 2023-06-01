package com.hunllef;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;
import java.awt.*;

public class HunllefOverlay extends Overlay {

    @Inject
    private OverlayManager overlayManager;

    private final Client client;
    private final HunllefPlugin plugin;
    private final ModelOutlineRenderer outlineRenderer;
    private int timeout;

    @Inject
    private HunllefOverlay(Client client, HunllefPlugin plugin, ModelOutlineRenderer outlineRenderer) {
        this.client = client;
        this.plugin = plugin;
        this.outlineRenderer = outlineRenderer;

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGH);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        plugin.getTornadoes().forEach(tornado ->
        {
            if (tornado.getTimeLeft() <= 0) {
                return;
            }

            final String textOverlay = Integer.toString(tornado.getTimeLeft());
            final Point textLoc = Perspective.getCanvasTextLocation(client, graphics, tornado.getNpc().getLocalLocation(), textOverlay, 0);
            final LocalPoint lp = LocalPoint.fromWorld(client, tornado.getNpc().getWorldLocation());

            if (lp == null) {
                return;
            }

            final Polygon tilePoly = Perspective.getCanvasTilePoly(client, lp);
            OverlayUtil.renderPolygon(graphics, tilePoly, Color.YELLOW);

            if (textLoc == null) {
                return;
            }

            Font oldFont = graphics.getFont();
            graphics.setFont(new Font("Arial", Font.BOLD, 20));
            Point pointShadow = new Point(textLoc.getX() + 1, textLoc.getY() + 1);
            OverlayUtil.renderTextLocation(graphics, pointShadow, textOverlay, Color.BLACK);
            OverlayUtil.renderTextLocation(graphics, textLoc, textOverlay, Color.YELLOW);
            graphics.setFont(oldFont);
        });

        return null;
    }
}
