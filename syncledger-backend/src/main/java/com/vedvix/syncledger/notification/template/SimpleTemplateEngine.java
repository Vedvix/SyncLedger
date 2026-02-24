package com.vedvix.syncledger.notification.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleTemplateEngine implements TemplateEngine {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private final TemplateRepository templateRepository;

    @Override
    public String process(String templateName, Map<String, Object> parameters) {
        String template = templateRepository.getTemplate(templateName);

        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }

        return replaceVariables(template, parameters);
    }

    private String replaceVariables(String template, Map<String, Object> parameters) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object value = parameters.get(key);

            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
