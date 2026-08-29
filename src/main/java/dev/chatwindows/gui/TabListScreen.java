package dev.chatwindows.gui;

import dev.chatwindows.config.ChatTab;
import dev.chatwindows.config.ChatWindow;
import dev.chatwindows.config.ConfigManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** The tabs belonging to one window. Each tab keeps its own independent settings. */
public class TabListScreen extends BaseSettingsScreen {

    private static final int PER_PAGE = 7;

    private final ChatWindow window;
    private int page = 0;

    public TabListScreen(Screen parent, ChatWindow window) {
        super(parent, Text.literal("Tabs - " + window.name));
        this.window = window;
    }

    @Override
    protected void init() {
        clearLabels();
        int totalPages = Math.max(1, (window.tabs.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int y = 36;
        int first = page * PER_PAGE;
        int last = Math.min(window.tabs.size(), first + PER_PAGE);

        for (int i = first; i < last; i++) {
            final int index = i;
            ChatTab tab = window.tabs.get(i);

            String summary = tab.receiveAll
                    ? "receives everything"
                    : tab.filters.size() + " filter(s)";
            addDrawableChild(ButtonWidget.builder(
                            Text.literal(tab.name + "  \u00a77(" + summary + ", " + tab.highlights.size() + " highlight(s))"),
                            b -> {
                                if (this.client != null) this.client.setScreen(new TabSettingsScreen(this, window, tab));
                            })
                    .dimensions(this.width / 2 - 150, y, 220, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("\u25b2"), b -> {
                if (index > 0) {
                    window.tabs.add(index - 1, window.tabs.remove(index));
                    ConfigManager.save();
                    clearAndInit();
                }
            }).dimensions(this.width / 2 + 74, y, 20, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("\u25bc"), b -> {
                if (index < window.tabs.size() - 1) {
                    window.tabs.add(index + 1, window.tabs.remove(index));
                    ConfigManager.save();
                    clearAndInit();
                }
            }).dimensions(this.width / 2 + 96, y, 20, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("\u00a7cX"), b -> {
                if (window.tabs.size() > 1) {
                    window.tabs.remove(index);
                    if (window.activeTab >= window.tabs.size()) window.activeTab = window.tabs.size() - 1;
                    ConfigManager.save();
                    clearAndInit();
                }
            }).dimensions(this.width / 2 + 118, y, 20, 20).build());

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

        addDrawableChild(ButtonWidget.builder(Text.literal("Add tab"), b -> {
            window.tabs.add(new ChatTab("Tab " + (window.tabs.size() + 1)));
            ConfigManager.save();
            clearAndInit();
        }).dimensions(this.width / 2 - 150, this.height - 28, 145, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(this.width / 2 + 5, this.height - 28, 145, 20).build());
    }
}
