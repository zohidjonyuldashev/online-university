package uz.pdp.online_university.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    @DisplayName("Renders all placeholders correctly")
    void render_allPlaceholders() {
        String template = "Hello {{name}}, your exam {{examName}} is on {{date}}.";
        Map<String, String> vars = Map.of("name", "Ali", "examName", "Math", "date", "2026-04-01");

        String result = renderer.render(template, vars);

        assertThat(result).isEqualTo("Hello Ali, your exam Math is on 2026-04-01.");
    }

    @Test
    @DisplayName("Returns template as-is when no placeholders")
    void render_noPlaceholders() {
        String result = renderer.render("No variables here.", Map.of());
        assertThat(result).isEqualTo("No variables here.");
    }

    @Test
    @DisplayName("Throws when required variable is missing")
    void render_missingVariable_throws() {
        String template = "Hello {{name}}, your grade is {{grade}}.";
        Map<String, String> vars = Map.of("name", "Zara"); // "grade" missing

        assertThatThrownBy(() -> renderer.render(template, vars))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("grade");
    }

    @Test
    @DisplayName("Returns empty string for blank template")
    void render_blankTemplate() {
        assertThat(renderer.render("", Map.of())).isEqualTo("");
        assertThat(renderer.render(null, Map.of())).isEqualTo("");
    }

    @Test
    @DisplayName("Handles null variables map gracefully")
    void render_nullVariables_noPlaceholders() {
        assertThat(renderer.render("Static text", null)).isEqualTo("Static text");
    }

    @Test
    @DisplayName("Handles null variables map with placeholder — throws")
    void render_nullVariables_withPlaceholder_throws() {
        assertThatThrownBy(() -> renderer.render("Hi {{name}}", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
