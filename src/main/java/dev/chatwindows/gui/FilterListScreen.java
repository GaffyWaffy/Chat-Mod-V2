package dev.chatwindows.gui;

import dev.chatwindows.config.ChatTab;
import dev.chatwindows.config.FilterRule;
import dev.chatwindows.config.ConfigManager;
import dev.chatwindows.util.MatchType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Filter list for a single tab. */
public class FilterListScreen extends BaseSettingsScreen {

    private static final int PER_PAGE = 8;

    private final ChatTab tab;
    private int page = 0;

    public FilterListScreen(Screen parent, ChatTab tab) {
        super(parent, Text.literal("Filters - " + tab.name));
        this.tab = tab;
    }

    @Override
    protected void init() {
        clearLabels();
        addLabel(this.width / 2 - 158, 26,
                "\u00a77\"Show only\" acts as a whitelist; \"Hide\" always wins.");

        int totalPages = Math.max(1, (tab.filters.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int y = 40;
        int first = page * PER_PAGE;
        int last = Math.min(tab.filters.size(), first + PER_PAGE);

        for (int i = first; i < last; i++) {
            final int index = i;
            FilterRule rule = tab.filters.get(i);
            String state = rule.enabled ? "" : "\u00a78[off] ";
            String color = rule.mode == FilterRule.Mode.SHOW_ONLY ? "\u00a7a" : "\u00a7c";
            String text = state + color + rule.mode.display() + " \u00a7f" + rule.match.display().toLowerCase()
                    + " \u00a7e\"" + shorten(rule.pattern) + "\"";

            addDrawableChild(ButtonWidget.builder(Text.literal(text), b -> {
                if (this.client != null) this.client.setScreen(new FilterEditScreen(this, tab, rule));
            }).dimensions(this.width / 2 - 158, y, 290, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("\u00a7cX"), b -> {
                tab.filters.remove(index);
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

        addDrawableChild(ButtonWidget.builder(Text.literal("Add filter"), b -> {
            FilterRule rule = new FilterRule("", MatchType.CONTAINS, FilterRule.Mode.SHOW_ONLY);
            tab.filters.add(rule);
            ConfigManager.save();
            if (this.client != null) this.client.setScreen(new FilterEditScreen(this, tab, rule));
        }).dimensions(this.width / 2 - 158, this.height - 28, 155, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(this.width / 2 + 1, this.height - 28, 155, 20).build());
    }

    private static String shorten(String s) {
        if (s == null) return "";
        return s.length() > 24 ? s.substring(0, 24) + "..." : s;
    }
}
