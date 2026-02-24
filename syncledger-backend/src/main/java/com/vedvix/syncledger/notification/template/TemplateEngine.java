package com.vedvix.syncledger.notification.template;

import java.util.Map;

public interface TemplateEngine {
    String process(String templateName, Map<String, Object> parameters);
}