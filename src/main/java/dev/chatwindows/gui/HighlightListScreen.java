package dev.chatwindows.gui;

import dev.chatwindows.config.ChatTab;
import dev.chatwindows.config.ConfigManager;
import dev.chatwindows.config.HighlightRule;
import dev.chatwindows.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Highlight list for a single tab. */
public class HighlightListScreen extends BaseSettingsScreen {

    private static final int PER_PAGE = 8;

    private final ChatTab tab;
    private int page = 0;

    public HighlightListScreen(Screen parent, ChatTab tab) {
        super(parent, Text.literal("Highlights - " + tab.name));
        this.tab = tab;
    }

    @Override
    protected void init() {
        clearLabels();
        addLabel(this.width / 2 - 158, 26,
                "\u00a77Highlights tint the message background only - text colours stay untouched.");

        int totalPages = Math.max(1, (tab.highlights.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int y = 40;
        int first = page * PER_PAGE;
        int last = Math.min(tab.highlights.size(), first + PER_PAGE);

        for (int i = first; i < last; i++) {
            final int index = i;
            HighlightRule rule = tab.highlights.get(i);
            String state = rule.enabled ? "" : "\u00a78[off] ";
            String text = state + "\u00a7f" + rule.match.display().toLowerCase()
                    + " \u00a7e\"" + shorten(rule.pattern) + "\"  \u00a77" + rule.color;

            addDrawableChild(ButtonWidget.builder(Text.literal(text), b -> {
                if (this.client != null) this.client.setScreen(new HighlightEditScreen(this, tab, rule));
            }).dimensions(this.width / 2 - 138, y, 270, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("\u00a7cX"), b -> {
                tab.highlights.remove(index);
                ConfigManager.save();
                clearAndInit();
            }).dimensions(this.width / 2 + 136, y, 20, 20).build());

            y += 22;
        }

        int bottom = this.height - 52;
        if (totalPages > 1) {
            addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), b -> {
                page--;
                clearAndInit();
            }).dimensions(this.width / 2 - 150, bottom, 60, 20).build());
            addLabel(this.width / 2 - 20, bottom + 6, (page + 1) + " / " + totalPages);
            addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), b -> {
                page++;
                clearAndInit();
            }).dimensions(this.width / 2 + 90, bottom, 60, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Add highlight"), b -> {
            HighlightRule rule = new HighlightRule("", "#FFAA00");
            tab.highlights.add(rule);
            ConfigManager.save();
            if (this.client != null) this.client.setScreen(new HighlightEditScreen(this, tab, rule));
        }).dimensions(this.width / 2 - 158, this.height - 28, 155, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(this.width / 2 + 1, this.height - 28, 155, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int y = 40;
        int first = page * PER_PAGE;
        int last = Math.min(tab.highlights.size(), first + PER_PAGE);
        for (int i = first; i < last; i++) {
            HighlightRule rule = tab.highlights.get(i);
            int x = this.width / 2 - 158;
            context.fill(x, y + 2, x + 16, y + 18, ColorUtil.argb(255, ColorUtil.parseHex(rule.color, 0xFFAA00)));
            y += 22;
        }
    }

    private static String shorten(String s) {
        if (s == null) return "";
        return s.length() > 20 ? s.substring(0, 20) + "..." : s;
    }
}
