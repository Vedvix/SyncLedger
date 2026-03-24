-- Add AI / 3rd-party integration settings as runtime configurations
-- Allows Super Admin to manage AI provider details from the UI

INSERT INTO runtime_configs (config_key, config_value, default_value, description, category, data_type) VALUES
    ('ai.provider', 'openai', 'openai', 'AI provider to use for extraction (openai)', 'AI', 'STRING'),
    ('ai.openai.api-key', '', '', 'OpenAI API key for AI-powered extraction', 'AI', 'STRING'),
    ('ai.openai.vision-model', 'gpt-4o', 'gpt-4o', 'OpenAI model for vision-based extraction (e.g. gpt-4o, gpt-4o-mini)', 'AI', 'STRING'),
    ('ai.openai.text-model', 'gpt-4o', 'gpt-4o', 'OpenAI model for text-based extraction (e.g. gpt-4o, gpt-4o-mini)', 'AI', 'STRING'),
    ('ai.enable-vision', 'true', 'true', 'Enable vision-based AI extraction (uses image of PDF)', 'AI', 'BOOLEAN'),
    ('ai.enable-text-llm', 'true', 'true', 'Enable text-based AI extraction (uses OCR text)', 'AI', 'BOOLEAN'),
    ('ai.enable-cross-validation', 'true', 'true', 'Enable cross-validation between vision and text results', 'AI', 'BOOLEAN');
