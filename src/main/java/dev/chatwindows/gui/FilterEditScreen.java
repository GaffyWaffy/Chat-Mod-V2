package dev.chatwindows.gui;

import dev.chatwindows.config.ChatTab;
import dev.chatwindows.config.ConfigManager;
import dev.chatwindows.config.FilterRule;
import dev.chatwindows.util.MatchType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class FilterEditScreen extends BaseSettingsScreen {

    private final ChatTab tab;
    private final FilterRule rule;
    private TextFieldWidget testField;

    public FilterEditScreen(Screen parent, ChatTab tab, FilterRule rule) {
        super(parent, Text.literal("Edit filter"));
        this.tab = tab;
        this.rule = rule;
    }

    @Override
    protected void init() {
        clearLabels();
        int cx = this.width / 2;
        int y = 40;

        addLabel(cx - 158, y + 6, "Keyword / phrase");
        TextFieldWidget patternField = new TextFieldWidget(this.textRenderer, cx - 50, y, 206, 18, Text.literal("Pattern"));
        patternField.setMaxLength(200);
        patternField.setText(rule.pattern);
        patternField.setChangedListener(s -> rule.pattern = s);
        addDrawableChild(patternField);
        setInitialFocus(patternField);
        y += 26;

        addDrawableChild(CyclingButtonWidget.builder((FilterRule.Mode m) -> Text.literal(m.display()), rule.mode)
                .values(FilterRule.Mode.values())
                .build(cx - 158, y, 155, 20, Text.literal("Action"), (b, v) -> rule.mode = v));

        addDrawableChild(CyclingButtonWidget.builder((MatchType m) -> Text.literal(m.display()), rule.match)
                .values(MatchType.values())
                .build(cx + 1, y, 155, 20, Text.literal("Match"), (b, v) -> {
                    rule.match = v;
                    clearAndInit();
                }));
        y += 24;

        addDrawableChild(CyclingButtonWidget.onOffBuilder(rule.caseSensitive)
                .build(cx - 158, y, 155, 20, Text.literal("Case sensitive"), (b, v) -> rule.caseSensitive = v));
        addDrawableChild(CyclingButtonWidget.onOffBuilder(rule.enabled)
                .build(cx + 1, y, 155, 20, Text.literal("Enabled"), (b, v) -> rule.enabled = v));
        y += 30;

        addLabel(cx - 158, y - 10, "\u00a77Test a line against this rule:");
        testField = new TextFieldWidget(this.textRenderer, cx - 158, y, 314, 18, Text.literal("Test"));
        testField.setMaxLength(200);
        addDrawableChild(testField);
        y += 40;

        addDrawableChild(ButtonWidget.builder(Text.literal("\u00a7cDelete"), b -> {
            tab.filters.remove(rule);
            ConfigManager.save();
            close();
        }).dimensions(cx - 158, this.height - 28, 155, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(cx + 1, this.height - 28, 155, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (testField != null && !testField.getText().isEmpty()) {
            boolean hit = rule.matches(testField.getText());
            String verdict = hit
                    ? (rule.mode == FilterRule.Mode.SHOW_ONLY ? "\u00a7aMatches - this line would be shown" : "\u00a7cMatches - this line would be hidden")
                    : "\u00a77No match";
            context.drawTextWithShadow(this.textRenderer, verdict,
                    testField.getX(), testField.getY() + 24, 0xFFFFFFFF);
        }
        if (rule.match == dev.chatwindows.util.MatchType.REGEX) {
            context.drawTextWithShadow(this.textRenderer,
                    "\u00a78Java regex, applied with find(). Example: ^\\[Party\\].*",
                    this.width / 2 - 158, 26, 0xFF777777);
        }
    }
}
