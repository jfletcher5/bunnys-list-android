package com.bunnyslist.android.data.model

/**
 * Client-side caps that mirror firestore.rules. Keep the two in sync so a
 * normal edit fails with a readable message instead of a permission error.
 */
object ListFieldLimits {
    const val NAME_MAX = 120
    const val DESCRIPTION_MAX = 1000
    const val CATEGORY_NAME_MAX = 80
    const val CATEGORY_REF_MAX = 128
    const val ACTOR_NAME_MAX = 80
    const val HISTORY_LIMIT = 50L
}
