package com.example.paceup.shared.runmatching.domain

/**
 * Tags a user can assign to a run partner after attending a run together (spec §4.4).
 *
 * @param value Lowercase snake_case string stored in the partner_ratings.tags text[] column.
 */
enum class PartnerTag(val value: String) {
    KEPT_PACE("kept_pace"),
    GREAT_ENERGY("great_energy"),
    PUSHED_GROUP("pushed_group"),
    EARLY("early"),
    MISMATCHED_PACE("mismatched_pace"),
}
