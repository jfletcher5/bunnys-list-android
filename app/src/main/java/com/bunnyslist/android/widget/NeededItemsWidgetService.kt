package com.bunnyslist.android.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.bunnyslist.android.R
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * RemoteViews service that backs the widget's scrollable needed-items list.
 */
class NeededItemsWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NeededItemsRemoteViewsFactory(
            packageName = packageName,
            signedOutText = getString(R.string.widget_signed_out_text),
        )
    }
}

private class NeededItemsRemoteViewsFactory(
    private val packageName: String,
    private val signedOutText: String,
) : RemoteViewsService.RemoteViewsFactory {
    private val neededItems = mutableListOf<String>()
    private var hasLoadError = false
    private var signedOut = false

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        // Blocking fetch is expected here because this callback already runs off the UI thread.
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            neededItems.clear()
            hasLoadError = false
            signedOut = true
            return
        }

        runCatching {
            val snapshot = Tasks.await(
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("items")
                    .whereEqualTo("need", true)
                    .get(),
            )
            val names = snapshot.documents
                .mapNotNull { doc -> doc.get("name")?.toString()?.trim() }
                .filter { it.isNotBlank() }
                .sortedBy { it.lowercase() }

            neededItems.clear()
            neededItems.addAll(names)
            hasLoadError = false
            signedOut = false
        }.onFailure {
            neededItems.clear()
            hasLoadError = true
            signedOut = false
        }
    }

    override fun onDestroy() {
        neededItems.clear()
    }

    override fun getCount(): Int {
        // Keep one row for user feedback even if load failed.
        return when {
            hasLoadError || signedOut -> 1
            else -> neededItems.size
        }
    }

    override fun getViewAt(position: Int): RemoteViews {
        val row = RemoteViews(packageName, R.layout.widget_needed_item_row)
        val text = when {
            signedOut -> signedOutText
            hasLoadError -> "Could not load items. Tap Refresh."
            else -> neededItems.getOrNull(position)?.let { "\u2022 $it" }.orEmpty()
        }
        row.setTextViewText(R.id.widget_item_text, text)
        return row
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
