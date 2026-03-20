-- V18: Add match_conditions_json to mapping_profiles
-- Allows richer auto-selection criteria beyond vendor_pattern.
-- Example: select a profile when gl_account == "51" OR vendor_name contains "Example1".

ALTER TABLE mapping_profiles
    ADD COLUMN IF NOT EXISTS match_conditions_json TEXT DEFAULT '[]';

COMMENT ON COLUMN mapping_profiles.match_conditions_json IS
    'JSON array of ProfileMatchCondition objects evaluated against raw extracted fields '
    'to auto-select this profile. ALL conditions must match (AND logic). '
    'Takes priority over vendor_pattern matching.';
