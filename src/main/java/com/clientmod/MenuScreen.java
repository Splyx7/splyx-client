package com.clientmod;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuScreen extends Screen {
    private static final int TXT = 0xFFEDEEF1, SUB = 0xFF8B8E96, DIM = 0xFF5F626B;
    private static final String[][] NAV = {{"Modules", "grid"}, {"Appearance", "brush"}, {"Profiles", "user"}, {"Settings", "cog"}};
    private static final String[] CATS = {"All", "HUD", "Visual", "Utility"};

    private interface Act { void run(double mx, double my); }

    private static final class Hit {
        final int x, y, w, h;
        final Act click, drag;
        final boolean content;

        Hit(int x, int y, int w, int h, Act click, Act drag, boolean content) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.click = click; this.drag = drag; this.content = content;
        }

        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Hit> hits = new ArrayList<>();
    private Hit dragging;
    private int wx, wy, ww, wh, sbw, cx, cy, cw, ch;
    private String page = "Modules", cat = "All";
    private Module sel;
    private double scroll, maxScroll;
    private int accent = 0xFFE1E4EB;

    public MenuScreen() {
        super(Text.literal("Client"));
    }

    @Override
    protected void init() {
        ww = Math.min(width - 24, 540);
        wh = Math.min(height - 24, 262);
        wx = (width - ww) / 2;
        wy = (height - wh) / 2;
        sbw = Math.round(ww * 0.25f);
        cx = wx + sbw + 14;
        cw = ww - sbw - 28;
        cy = wy + 56;
        ch = wh - 56 - 10;
    }

    // ------------------------------------------------------------ helpers

    private void txt(DrawContext c, String s, int x, int y, int col) {
        c.drawText(textRenderer, Text.literal(s), x, y, col, false);
    }

    private void txtS(DrawContext c, String s, float x, float y, float sc, int col) {
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(sc, sc);
        c.drawText(textRenderer, Text.literal(s), 0, 0, col, false);
        m.popMatrix();
    }

    private void rect(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + h, col);
    }

    private boolean inContent(double mx, double my) {
        return mx >= cx - 4 && mx < cx + cw + 4 && my >= cy && my < cy + ch;
    }

    private void hit(int x, int y, int w, int h, Act click, Act drag) {
        if (y + h < cy || y > cy + ch) return;
        hits.add(new Hit(x, y, w, h, click, drag, true));
    }

    private void fixed(int x, int y, int w, int h, Act click) {
        hits.add(new Hit(x, y, w, h, click, null, false));
    }

    private void toggle(DrawContext c, int x, int y, boolean on) {
        Gfx.pill(c, x, y, 24, 12, on ? 0x70FFFFFF : 0x30FFFFFF);
        Gfx.ring(c, x, y, 24, 12, 6, on ? 0x40FFFFFF : 0x22FFFFFF);
        Gfx.rrect(c, x + (on ? 14 : 2), y + 2, 8, 8, 4, on ? 0xFFF1F2F5 : 0xFF9A9DA6);
    }

    private void checker(DrawContext c, int x, int y, int w, int h) {
        for (int yy = 0; yy < h; yy += 4)
            for (int xx = 0; xx < w; xx += 4)
                c.fill(x + xx, y + yy, Math.min(x + xx + 4, x + w), Math.min(y + yy + 4, y + h),
                        (((xx >> 2) + (yy >> 2)) & 1) == 0 ? 0xFF6A6A6A : 0xFF3A3A3A);
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        hits.clear();
        accent = ModuleManager.MENU.c("accent") | 0xFF000000;
        int a = Math.max(0, Math.min(255, Math.round(ModuleManager.MENU.n("opacity") * 2.55f)));

        // window
        Gfx.rrect(c, wx - 1, wy - 1, ww + 2, wh + 2, 16, 0x2CFFFFFF);
        Gfx.rrect(c, wx, wy, ww, wh, 15, (a << 24) | 0x0B0C10);
        c.fillGradient(wx + 10, wy + 1, wx + ww - 10, wy + 40, 0x0AFFFFFF, 0x00FFFFFF);

        // sidebar panel
        int sx = wx + 6, sy = wy + 6, sw2 = sbw - 4, sh2 = wh - 12;
        Gfx.rrect(c, sx, sy, sw2, sh2, 11, 0x14FFFFFF);
        Gfx.ring(c, sx, sy, sw2, sh2, 11, 0x1CFFFFFF);
        Icons.logo(c, sx + sw2 / 2 - 17, sy + 8, 34);

        int ny = sy + 52;
        for (String[] n : NAV) {
            boolean on = page.equals(n[0]);
            boolean hov = mx >= sx + 6 && mx < sx + sw2 - 6 && my >= ny && my < ny + 22;
            if (on) Gfx.rrect(c, sx + 6, ny, sw2 - 12, 22, 6, 0x26FFFFFF);
            else if (hov) Gfx.rrect(c, sx + 6, ny, sw2 - 12, 22, 6, 0x12FFFFFF);
            Icons.draw(c, n[1], sx + 14, ny + 5, 12);
            txt(c, n[0], sx + 32, ny + 7, on ? TXT : 0xFFA6A9B1);
            final String target = n[0];
            fixed(sx + 6, ny, sw2 - 12, 22, (x, y) -> { page = target; sel = null; scroll = 0; });
            ny += 26;
        }
        Gfx.rrect(c, sx + 12, sy + sh2 - 16, 5, 5, 2, 0xFF8B8E96);
        txtS(c, "Client v1.0", sx + 22, sy + sh2 - 17, 0.85f, SUB);

        // header
        int titleX = cx;
        String title = page;
        String icon = page.equals("Modules") ? "grid" : page.equals("Appearance") ? "brush" : page.equals("Profiles") ? "user" : "cog";
        Icons.draw(c, icon, titleX, wy + 17, 14);
        txtS(c, title, titleX + 22, wy + 17, 1.3f, TXT);
        Icons.draw(c, "close", wx + ww - 28, wy + 17, 12);
        fixed(wx + ww - 32, wy + 13, 20, 20, (x, y) -> client.setScreen(null));

        // category tabs (modules page)
        if (page.equals("Modules") && sel == null) {
            int tx = cx;
            for (String k : CATS) {
                int tw = textRenderer.getWidth(k) + 14;
                boolean on = cat.equals(k);
                Gfx.rrect(c, tx, wy + 36, tw, 13, 6, on ? 0x2AFFFFFF : 0x0EFFFFFF);
                txt(c, k, tx + 7, wy + 39, on ? TXT : SUB);
                final String kk = k;
                fixed(tx, wy + 36, tw, 13, (x, y) -> { cat = kk; scroll = 0; });
                tx += tw + 4;
            }
        }

        // content
        c.enableScissor(cx - 4, cy, cx + cw + 4, cy + ch);
        int y = cy - (int) scroll;
        int end;
        switch (page) {
            case "Appearance" -> end = settings(c, ModuleManager.MENU, y, mx, my);
            case "Profiles" -> end = profiles(c, y);
            case "Settings" -> end = settingsPage(c, y, mx, my);
            default -> end = sel != null ? detail(c, sel, y, mx, my) : grid(c, y, mx, my);
        }
        c.disableScissor();

        maxScroll = Math.max(0, (end - (cy - (int) scroll)) - ch);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    // ------------------------------------------------------------ pages

    private int profiles(DrawContext c, int y) {
        Icons.draw(c, "user", cx + cw / 2 - 12, y + 30, 24);
        String s = "Profiles are coming soon.";
        txt(c, s, cx + cw / 2 - textRenderer.getWidth(s) / 2, y + 62, SUB);
        return y + 90;
    }

    private int settingsPage(DrawContext c, int y, int mx, int my) {
        txt(c, "GENERAL", cx, y, SUB);
        y += 16;
        txt(c, "Open menu key", cx + 2, y + 5, TXT);
        txt(c, "Options > Controls > Client Mod", cx + cw - 4 - textRenderer.getWidth("Options > Controls > Client Mod"), y + 5, SUB);
        y += 24;
        int bw = textRenderer.getWidth("Open HUD editor") + 24;
        boolean hov = mx >= cx && mx < cx + bw && my >= y && my < y + 18 && inContent(mx, my);
        Gfx.rrect(c, cx, y, bw, 18, 7, hov ? 0x2AFFFFFF : 0x18FFFFFF);
        Gfx.ring(c, cx, y, bw, 18, 7, 0x24FFFFFF);
        Icons.draw(c, "layout", cx + 6, y + 3, 12);
        txt(c, "Open HUD editor", cx + 20, y + 5, TXT);
        hit(cx, y, bw, 18, (x, yy) -> client.setScreen(new HudEditorScreen()), null);
        return y + 30;
    }

    private int grid(DrawContext c, int y, int mx, int my) {
        int gap = 8;
        int cols = cw >= 330 ? 3 : 2;
        int cardW = (cw - (cols - 1) * gap) / cols;
        int cardH = 46;
        int i = 0;
        for (Module m : ModuleManager.all()) {
            if (!cat.equals("All") && !m.category.equals(cat)) continue;
            int x = cx + (i % cols) * (cardW + gap);
            int yy = y + (i / cols) * (cardH + gap);
            card(c, m, x, yy, cardW, cardH, mx, my);
            i++;
        }
        return y + ((i + cols - 1) / cols) * (cardH + gap);
    }

    private void card(DrawContext c, Module m, int x, int y, int w, int h, int mx, int my) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h && inContent(mx, my);
        Gfx.rrect(c, x, y, w, h, 8, hov ? 0x1EFFFFFF : 0x12FFFFFF);
        Gfx.ring(c, x, y, w, h, 8, m.enabled && m.implemented ? 0x40FFFFFF : 0x1CFFFFFF);
        Icons.draw(c, m.id, x + 9, y + 9, 14);
        txt(c, m.name, x + 31, y + 8, m.implemented ? TXT : 0xFF9A9DA6);
        txtS(c, m.desc, x + 31, y + 21, 0.75f, SUB);
        Icons.draw(c, "arrow", x + w - 17, y + h - 15, 9);
        final Module mm = m;
        hit(x, y, w, h, (px, py) -> { sel = mm; scroll = 0; }, null);
        if (m.implemented) {
            toggle(c, x + w - 33, y + 8, m.enabled);
            hit(x + w - 36, y + 5, 30, 18, (px, py) -> mm.enabled = !mm.enabled, null);
        } else {
            txtS(c, "SOON", x + w - 32, y + 10, 0.8f, DIM);
        }
    }

    private int detail(DrawContext c, Module m, int y, int mx, int my) {
        Icons.draw(c, "back", cx, y + 1, 10);
        txt(c, "Modules", cx + 14, y + 2, SUB);
        hit(cx, y - 2, 60, 14, (px, py) -> { sel = null; scroll = 0; }, null);
        y += 20;

        Gfx.rrect(c, cx, y, cw, 40, 9, 0x12FFFFFF);
        Gfx.ring(c, cx, y, cw, 40, 9, 0x1CFFFFFF);
        Icons.draw(c, m.id, cx + 11, y + 11, 18);
        txt(c, m.name, cx + 38, y + 11, TXT);
        txtS(c, m.desc, cx + 38, y + 23, 0.8f, SUB);
        if (m.implemented) {
            toggle(c, cx + cw - 36, y + 14, m.enabled);
            hit(cx + cw - 40, y + 10, 34, 20, (px, py) -> m.enabled = !m.enabled, null);
            int rx = cx + cw - 36 - 54;
            Gfx.rrect(c, rx, y + 13, 44, 14, 6, 0x18FFFFFF);
            txt(c, "Reset", rx + 11, y + 16, TXT);
            hit(rx, y + 13, 44, 14, (px, py) -> { for (Setting s : m.all()) s.reset(); }, null);
        }
        y += 52;

        if (!m.implemented) {
            txt(c, "This module is planned and not built yet.", cx + 2, y, SUB);
            return y + 20;
        }
        if (m.id.equals("itf")) {
            ItemStack held = client != null && client.player != null ? client.player.getMainHandStack() : ItemStack.EMPTY;
            if (held.isEmpty()) {
                txt(c, "Hold an item in your hand to edit it.", cx + 2, y + 4, SUB);
            } else {
                c.drawItem(held, cx + 2, y);
                txt(c, "Editing: " + held.getName().getString(), cx + 24, y + 4, TXT);
            }
            y += 24;
        }
        return settings(c, m, y, mx, my);
    }

    // ------------------------------------------------------------ settings

    private int settings(DrawContext c, Module m, int y, int mx, int my) {
        for (Map.Entry<String, List<Setting>> g : m.groups.entrySet()) {
            String name = g.getKey().toUpperCase();
            txtS(c, name, cx + 2, y, 0.85f, SUB);
            int tw = Math.round(textRenderer.getWidth(name) * 0.85f);
            rect(c, cx + tw + 8, y + 4, cw - tw - 8, 1, 0x18FFFFFF);
            y += 14;
            for (Setting s : g.getValue()) y = row(c, s, y, mx, my);
            y += 6;
        }
        return y;
    }

    private int row(DrawContext c, Setting s, int y, int mx, int my) {
        int right = cx + cw - 4;
        txt(c, s.label, cx + 2, y + 6, TXT);

        if (s instanceof Setting.Bool b) {
            toggle(c, right - 24, y + 4, b.value);
            hit(right - 28, y, 30, 20, (px, py) -> b.value = !b.value, null);
            return y + 22;
        }
        if (s instanceof Setting.Num n) {
            int tw = 96, tx = right - tw - 46, ty = y + 9;
            Gfx.rrect(c, tx, ty, tw, 3, 1, 0x3CFFFFFF);
            int fw = Math.round(tw * Math.max(0f, Math.min(1f, n.fraction())));
            Gfx.rrect(c, tx, ty, Math.max(3, fw), 3, 1, accent);
            Gfx.rrect(c, Math.max(tx, Math.min(tx + tw - 8, tx + fw - 4)), ty - 3, 8, 8, 4, 0xFFFFFFFF);
            txt(c, n.fmt(), tx + tw + 10, y + 6, SUB);
            Act set = (px, py) -> n.set(n.min + (float) Math.max(0, Math.min(1, (px - tx) / tw)) * (n.max - n.min));
            hit(tx - 4, y + 2, tw + 8, 16, set, set);
            return y + 22;
        }
        if (s instanceof Setting.Choice ch2) {
            int total = 0;
            for (String o : ch2.opts) total += textRenderer.getWidth(o) + 12 + 3;
            int x = right - total + 3;
            for (int i = 0; i < ch2.opts.length; i++) {
                int pw = textRenderer.getWidth(ch2.opts[i]) + 12;
                boolean on = ch2.value == i;
                Gfx.rrect(c, x, y + 4, pw, 13, 6, on ? 0x38FFFFFF : 0x12FFFFFF);
                txt(c, ch2.opts[i], x + 6, y + 7, on ? TXT : SUB);
                final int idx = i;
                hit(x, y + 4, pw, 13, (px, py) -> ch2.value = idx, null);
                x += pw + 3;
            }
            return y + 22;
        }
        if (s instanceof Setting.Color col) {
            int sx = right - 30;
            checker(c, sx + 1, y + 5, 28, 12);
            Gfx.rrect(c, sx + 1, y + 5, 28, 12, 4, col.argb());
            Gfx.ring(c, sx, y + 4, 30, 14, 5, 0x66FFFFFF);
            String hex = String.format("#%08X", col.argb());
            txt(c, hex, sx - 8 - textRenderer.getWidth(hex), y + 6, SUB);
            hit(sx - 90, y, 124, 20, (px, py) -> col.open = !col.open, null);
            y += 22;
            if (col.open) y = picker(c, col, y);
            return y;
        }
        return y + 22;
    }

    // ------------------------------------------------------------ color picker

    private int picker(DrawContext c, Setting.Color col, int y) {
        int px = cx + 8, pw = 168, svh = 72;
        int top = y + 4;
        Gfx.rrect(c, cx, y, pw + 16 + 64, svh + 104, 8, 0x12FFFFFF);
        Gfx.ring(c, cx, y, pw + 16 + 64, svh + 104, 8, 0x1CFFFFFF);

        for (int i = 0; i < pw; i += 2) {
            float s = i / (float) (pw - 1);
            int topCol = 0xFF000000 | Setting.Color.hsv(col.h, s, 1f);
            c.fillGradient(px + i, top, px + i + 2, top + svh, topCol, 0xFF000000);
        }
        int mxp = px + Math.round(col.s * (pw - 1)), myp = top + Math.round((1 - col.v) * (svh - 1));
        Gfx.rrect(c, mxp - 4, myp - 4, 9, 9, 4, 0xFFFFFFFF);
        Gfx.rrect(c, mxp - 3, myp - 3, 7, 7, 3, 0xFF000000 | col.rgb());
        final int ftop = top;
        Act sv = (mx, my) -> {
            col.s = (float) Math.max(0, Math.min(1, (mx - px) / (pw - 1)));
            col.v = 1f - (float) Math.max(0, Math.min(1, (my - ftop) / (svh - 1)));
        };
        hit(px, top, pw, svh, sv, sv);

        int hy = top + svh + 8;
        for (int i = 0; i < pw; i++) c.fill(px + i, hy, px + i + 1, hy + 8, 0xFF000000 | Setting.Color.hsv(i * 360f / pw, 1f, 1f));
        int hx = px + Math.round(col.h / 360f * (pw - 1));
        Gfx.rrect(c, hx - 2, hy - 2, 5, 12, 2, 0xFFFFFFFF);
        Act hue = (mx, my) -> col.h = (float) Math.max(0, Math.min(1, (mx - px) / (pw - 1))) * 359.9f;
        hit(px, hy - 2, pw, 12, hue, hue);

        int oy = hy + 16;
        checker(c, px, oy, pw, 8);
        int rgb = col.rgb();
        for (int i = 0; i < pw; i += 2) {
            int al = Math.round(i / (float) (pw - 1) * 255f);
            c.fill(px + i, oy, px + i + 2, oy + 8, (al << 24) | rgb);
        }
        int ox = px + Math.round(col.a * (pw - 1));
        Gfx.rrect(c, ox - 2, oy - 2, 5, 12, 2, 0xFFFFFFFF);
        Act op = (mx, my) -> col.a = (float) Math.max(0, Math.min(1, (mx - px) / (pw - 1)));
        hit(px, oy - 2, pw, 12, op, op);
        txt(c, "Opacity " + Math.round(col.a * 100) + " %", px + pw + 10, oy, SUB);

        checker(c, px + pw + 10, top, 44, 40);
        Gfx.rrect(c, px + pw + 10, top, 44, 40, 6, col.argb());

        int ry = oy + 18;
        txt(c, "Chroma", px, ry + 3, TXT);
        toggle(c, px + 54, ry + 1, col.chroma);
        hit(px + 50, ry - 2, 32, 18, (mx, my) -> col.chroma = !col.chroma, null);
        txt(c, String.format("#%08X", col.argb()), px + 100, ry + 3, SUB);
        return y + svh + 104 + 6;
    }

    // ------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            double mx = click.x(), my = click.y();
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (!h.in(mx, my)) continue;
                if (h.content && !inContent(mx, my)) continue;
                h.click.run(mx, my);
                if (h.drag != null) dragging = h;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging != null) {
            dragging.drag.run(click.x(), click.y());
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 14));
        return true;
    }

    @Override
    public void removed() {
        ModuleManager.save();
        ClientModMain.oneConfigPush();
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
