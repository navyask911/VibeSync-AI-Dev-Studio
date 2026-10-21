package com.vibesync.promptstudio.presentation.resources

import androidx.annotation.StringRes
import com.vibesync.promptstudio.R

enum class AndroidResourceCategory(@StringRes val titleRes: Int) {
    OFFICIAL_CORE(R.string.android_resource_category_official),
    NEWSLETTERS_BLOGS(R.string.android_resource_category_news),
    UTILITIES_PRODUCTIVITY(R.string.android_resource_category_utilities),
    COMMUNITY_DISCUSSION(R.string.android_resource_category_community)
}

data class AndroidDeveloperResource(
    val id: String,
    @StringRes val titleRes: Int,
    val url: String,
    @StringRes val descriptionRes: Int,
    val category: AndroidResourceCategory
)

/** Curated Android resources; presentation copy is localized through Android string resources. */
object AndroidDeveloperResourceCatalog {
    val resources: List<AndroidDeveloperResource> = listOf(
        AndroidDeveloperResource("android-developers", R.string.android_resource_android_developers_title, "https://developer.android.com", R.string.android_resource_android_developers_description, AndroidResourceCategory.OFFICIAL_CORE),
        AndroidDeveloperResource("material-design", R.string.android_resource_material_design_title, "https://material.io", R.string.android_resource_material_design_description, AndroidResourceCategory.OFFICIAL_CORE),
        AndroidDeveloperResource("android-weekly", R.string.android_resource_android_weekly_title, "https://androidweekly.net", R.string.android_resource_android_weekly_description, AndroidResourceCategory.NEWSLETTERS_BLOGS),
        AndroidDeveloperResource("proandroiddev", R.string.android_resource_proandroiddev_title, "https://proandroiddev.com", R.string.android_resource_proandroiddev_description, AndroidResourceCategory.NEWSLETTERS_BLOGS),
        AndroidDeveloperResource("styling-android", R.string.android_resource_styling_android_title, "https://stylingandroid.com", R.string.android_resource_styling_android_description, AndroidResourceCategory.NEWSLETTERS_BLOGS),
        AndroidDeveloperResource("dan-lew-codes", R.string.android_resource_dan_lew_codes_title, "https://blog.danlew.net", R.string.android_resource_dan_lew_codes_description, AndroidResourceCategory.NEWSLETTERS_BLOGS),
        AndroidDeveloperResource("android-arsenal", R.string.android_resource_android_arsenal_title, "https://android-arsenal.com", R.string.android_resource_android_arsenal_description, AndroidResourceCategory.UTILITIES_PRODUCTIVITY),
        AndroidDeveloperResource("icon-kitchen", R.string.android_resource_icon_kitchen_title, "https://iconkitchen.com", R.string.android_resource_icon_kitchen_description, AndroidResourceCategory.UTILITIES_PRODUCTIVITY),
        AndroidDeveloperResource("kotlin-playground", R.string.android_resource_kotlin_playground_title, "https://play.kotlinlang.org", R.string.android_resource_kotlin_playground_description, AndroidResourceCategory.UTILITIES_PRODUCTIVITY),
        AndroidDeveloperResource("perfetto-ui", R.string.android_resource_perfetto_ui_title, "https://ui.perfetto.dev", R.string.android_resource_perfetto_ui_description, AndroidResourceCategory.UTILITIES_PRODUCTIVITY),
        AndroidDeveloperResource("reddit-androiddev", R.string.android_resource_reddit_androiddev_title, "https://www.reddit.com/r/androiddev", R.string.android_resource_reddit_androiddev_description, AndroidResourceCategory.COMMUNITY_DISCUSSION),
        AndroidDeveloperResource("stackoverflow-android", R.string.android_resource_stackoverflow_android_title, "https://stackoverflow.com/questions/tagged/android", R.string.android_resource_stackoverflow_android_description, AndroidResourceCategory.COMMUNITY_DISCUSSION)
    )
}
