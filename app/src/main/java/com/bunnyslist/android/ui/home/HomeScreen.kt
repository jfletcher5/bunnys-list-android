package com.bunnyslist.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bunnyslist.android.R
import com.bunnyslist.android.data.model.GroceryCategory
import com.bunnyslist.android.data.model.GroceryItem
import java.text.DateFormat
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Nav host wrapper so the app can grow beyond the single-screen MVP cleanly.
 */
@Composable
fun BunnysListApp(openAddDialogSignal: Int = 0) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(viewModel) {
                viewModel.messages.collect { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }

            HomeScreen(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                openAddDialogSignal = openAddDialogSignal,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onFilterSelected = viewModel::updateFilter,
                onToggleNeeded = viewModel::toggleNeeded,
                onAddItem = viewModel::addItem,
                onUpdateItem = viewModel::updateItem,
                onDeleteItem = viewModel::deleteItem,
                onUndoDelete = viewModel::undoDelete,
                onAddCategory = viewModel::addCategory,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    openAddDialogSignal: Int,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (HomeFilter) -> Unit,
    onToggleNeeded: (GroceryItem) -> Unit,
    onAddItem: (String, String, String, Boolean) -> Unit,
    onUpdateItem: (GroceryItem, String, String, String) -> Unit,
    onDeleteItem: suspend (GroceryItem) -> GroceryItem?,
    onUndoDelete: (GroceryItem) -> Unit,
    onAddCategory: suspend (String) -> GroceryCategory?,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var itemBeingEdited by remember { mutableStateOf<GroceryItem?>(null) }
    var itemPendingDelete by remember { mutableStateOf<GroceryItem?>(null) }
    val expandedItems = remember { mutableStateMapOf<String, Boolean>() }
    val coroutineScope = rememberCoroutineScope()

    // External entry points (like widgets) can bump this signal to open Add Item immediately.
    LaunchedEffect(openAddDialogSignal) {
        if (openAddDialogSignal > 0) {
            showAddDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_bunnys_logo),
                            contentDescription = null,
                            modifier = Modifier.height(32.dp),
                        )
                        Column {
                            Text(
                                text = "Bunny's Grocery List",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "Hop through your list faster",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .navigationBarsPadding()
                    .offset(y = 8.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Soft gradient background gives the home screen more visual depth.
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ),
                )
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            AnimatedVisibility(visible = !uiState.isOnline) {
                OfflineBanner()
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Search items") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    ),
                    singleLine = true,
                )

                // Needed toggle is high-contrast when selected and quiet otherwise.
                FilledTonalIconButton(
                    onClick = {
                        onFilterSelected(
                            if (uiState.selectedFilter == HomeFilter.Needed) {
                                HomeFilter.All
                            } else {
                                HomeFilter.Needed
                            },
                        )
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (uiState.selectedFilter == HomeFilter.Needed) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = if (uiState.selectedFilter == HomeFilter.Needed) {
                            "Show all items"
                        } else {
                            "Show needed items only"
                        },
                    )
                }
            }

            when {
                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null -> ErrorState(uiState.errorMessage)
                uiState.filteredItems.isEmpty() -> EmptyState(uiState.emptyMessage)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = uiState.filteredItems,
                        key = { it.firestoreId },
                    ) { item ->
                        GroceryItemCard(
                            item = item,
                            category = uiState.categoryFor(item),
                            expanded = expandedItems[item.firestoreId] == true,
                            onExpandedChange = { expanded ->
                                expandedItems[item.firestoreId] = expanded
                            },
                            onToggleNeeded = { onToggleNeeded(item) },
                            onEditClick = { itemBeingEdited = item },
                            onDeleteClick = { itemPendingDelete = item },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ItemEditorDialog(
            title = "Add Item",
            confirmLabel = "Add",
            categories = uiState.categories,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, description, categoryId, needed ->
                onAddItem(name, description, categoryId, needed)
                showAddDialog = false
            },
            onAddCategory = onAddCategory,
        )
    }

    itemBeingEdited?.let { item ->
        ItemEditorDialog(
            title = "Edit Item",
            confirmLabel = "Save",
            initialName = item.name,
            initialDescription = item.description,
            initialCategoryId = item.itemCategory,
            initialNeeded = item.need,
            categories = uiState.categories,
            onDismiss = { itemBeingEdited = null },
            onConfirm = { name, description, categoryId, _ ->
                onUpdateItem(item, name, description, categoryId)
                itemBeingEdited = null
            },
            onAddCategory = onAddCategory,
            showNeededToggle = false,
        )
    }

    itemPendingDelete?.let { item ->
        ConfirmDeleteDialog(
            item = item,
            onDismiss = { itemPendingDelete = null },
            onConfirm = {
                itemPendingDelete = null
                coroutineScope.launch {
                    val deleted = onDeleteItem(item) ?: return@launch
                    val result = snackbarHostState.showSnackbar(
                        message = "${deleted.name} deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onUndoDelete(deleted)
                    }
                }
            },
        )
    }
}

@Composable
private fun OfflineBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.WifiOff, contentDescription = null)
            Text("You're offline. Changes will sync when connectivity returns.")
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(4) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp),
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        )
        {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        )
        {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun GroceryItemCard(
    item: GroceryItem,
    category: GroceryCategory?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggleNeeded: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (item.need) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!expanded) },
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (!expanded) {
                        Text(
                            text = category?.itemCategory ?: "Uncategorized",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                AssistChip(
                    onClick = onToggleNeeded,
                    label = { Text(if (item.need) "Needed" else "Got it") },
                    leadingIcon = if (item.need) {
                        { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                    } else {
                        null
                    },
                    colors = if (item.need) {
                        androidx.compose.material3.AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        androidx.compose.material3.AssistChipDefaults.assistChipColors()
                    },
                    border = androidx.compose.material3.AssistChipDefaults.assistChipBorder(
                        enabled = true,
                        borderColor = if (item.need) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        },
                    ),
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse item" else "Expand item",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(category?.itemCategory ?: "Uncategorized") },
                    )

                    Text(
                        text = if (item.description.isBlank()) {
                            "No description"
                        } else {
                            item.description
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Text(
                        text = "Updated ${DateFormat.getDateInstance().format(item.lastUpdated)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        OutlinedButton(onClick = onEditClick) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(onClick = onDeleteClick) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    item: GroceryItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Item") },
        text = { Text("Delete ${item.name}? You can undo from the snackbar.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ItemEditorDialog(
    title: String,
    confirmLabel: String,
    categories: List<GroceryCategory>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean) -> Unit,
    onAddCategory: suspend (String) -> GroceryCategory?,
    initialName: String = "",
    initialDescription: String = "",
    initialCategoryId: String = "",
    initialNeeded: Boolean = true,
    showNeededToggle: Boolean = true,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    var selectedCategoryId by remember(initialCategoryId, categories) {
        mutableStateOf(
            initialCategoryId.ifBlank { categories.firstOrNull()?.id?.toString().orEmpty() },
        )
    }
    var needed by remember(initialNeeded) { mutableStateOf(initialNeeded) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = if (title == "Add Item") {
                        "Capture it quickly and keep your list organized."
                    } else {
                        "Update details and save your changes."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Core input group for item details.
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Description (optional)") },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelLarge,
                        )

                        if (categories.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "No categories yet",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Create a category before saving this item so the list stays organized.",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    TextButton(onClick = { showCategoryDialog = true }) {
                                        Text("Add Category")
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { categoryMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    categories.firstOrNull { it.id.toString() == selectedCategoryId }?.itemCategory
                                        ?: "Select a category",
                                )
                            }

                            androidx.compose.material3.DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false },
                            ) {
                                categories.forEach { category ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(category.itemCategory) },
                                        onClick = {
                                            selectedCategoryId = category.id.toString()
                                            categoryMenuExpanded = false
                                        },
                                    )
                                }
                            }

                            TextButton(onClick = { showCategoryDialog = true }) {
                                Text("Add Category")
                            }
                        }
                    }

                    if (showNeededToggle) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            onClick = { needed = !needed },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = "Mark as needed",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Text(
                                        text = if (needed) "Item will appear in needed list" else "Item will be in 'Got it' state",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    )
                                }
                                Switch(
                                    checked = needed,
                                    onCheckedChange = { needed = it },
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            onConfirm(name, description, selectedCategoryId, needed)
                        },
                        enabled = name.isNotBlank() && selectedCategoryId.isNotBlank(),
                    ) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showCategoryDialog = false },
            onConfirm = { categoryName ->
                coroutineScope.launch {
                    val createdCategory = onAddCategory(categoryName) ?: return@launch
                    selectedCategoryId = createdCategory.id.toString()
                    showCategoryDialog = false
                }
            },
        )
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Category") },
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Category name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(categoryName.trim()) },
                enabled = categoryName.isNotBlank(),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
