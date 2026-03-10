package uz.pdp.online_university.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders notification templates by replacing {@code {{variable}}} placeholders
 * with values from the supplied map.
 *
 * <p>
 * Example:
 * 
 * <pre>
 *   template: "Hello {{name}}, your exam {{examName}} is on {{date}}."
 *   variables: {name:"Ali", examName:"Math", date:"2026-04-01"}
 *   result: "Hello Ali, your exam Math is on 2026-04-01."
 * </pre>
 */
@Slf4j
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * Renders a template string by substituting all {@code {{key}}} placeholders.
     *
     * @param template  raw template string
     * @param variables map of variable name → value
     * @return rendered string
     * @throws IllegalArgumentException if a placeholder has no matching variable
     */
    public String render(String template, Map<String, String> variables) {
        if (template == null || template.isBlank()) {
            return "";
        }
        if (variables == null) {
            variables = Map.of();
        }

        StringBuilder sb = new StringBuilder();
        Matcher matcher = PLACEHOLDER.matcher(template);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.get(key);
            if (value == null) {
                throw new IllegalArgumentException(
                        "Template variable '{{" + key + "}}' is required but was not provided");
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
