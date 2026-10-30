package com.clientmod;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Draws the PNG icons from assets/clientmod/textures/gui. */
public final class Icons {
    private static final Map<String, Identifier> CACHE = new HashMap<>();
    private static final Identifier LOGO = Identifier.of("clientmod", "textures/gui/logo.png");

    private Icons() {}

    public static void draw(DrawContext c, String name, int x, int y, int size) {
        Identifier id = CACHE.computeIfAbsent(name, n -> Identifier.of("clientmod", "textures/gui/icons/" + n + ".png"));
        c.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, size, size, 64, 64, 64, 64);
    }

    public static void logo(DrawContext c, int x, int y, int size) {
        c.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, x, y, 0f, 0f, size, size, 256, 256, 256, 256);
    }
}
