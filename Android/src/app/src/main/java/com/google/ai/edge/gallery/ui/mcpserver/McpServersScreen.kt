package com.google.ai.edge.gallery.ui.mcpserver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.proto.McpServer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpServersScreen(
  navigateUp: () -> Unit,
  viewModel: McpServersViewModel = hiltViewModel(),
) {
  val servers by viewModel.mcpServers.collectAsState()
  var showAddEditDialog by remember { mutableStateOf(false) }
  var serverToEdit by remember { mutableStateOf<McpServer?>(null) }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("MCP Servers") },
        navigationIcon = {
          IconButton(onClick = navigateUp) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          serverToEdit = null
          showAddEditDialog = true
        }
      ) {
        Icon(Icons.Rounded.Add, contentDescription = "Add Server")
      }
    }
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      if (servers.isEmpty()) {
        Text(
          "No MCP servers configured.",
          modifier = Modifier.align(Alignment.Center),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(servers) { server ->
            McpServerItem(
              server = server,
              onEdit = {
                serverToEdit = server
                showAddEditDialog = true
              },
              onDelete = { viewModel.deleteServer(server.name) },
            )
          }
        }
      }
    }
  }

  if (showAddEditDialog) {
    McpServerDialog(
      server = serverToEdit,
      onDismiss = { showAddEditDialog = false },
      onConfirm = { oldName, newName, url ->
        viewModel.addOrUpdateServer(oldName, newName, url)
        showAddEditDialog = false
      }
    )
  }
}

@Composable
fun McpServerItem(
  server: McpServer,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onEdit() }
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(server.name, style = MaterialTheme.typography.titleMedium)
      Text(server.url, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    IconButton(onClick = onEdit) {
      Icon(Icons.Rounded.Edit, contentDescription = "Edit")
    }
    IconButton(onClick = onDelete) {
      Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
    }
  }
}

@Composable
fun McpServerDialog(
  server: McpServer?,
  onDismiss: () -> Unit,
  onConfirm: (String?, String, String) -> Unit,
) {
  var name by remember { mutableStateOf(server?.name ?: "") }
  var url by remember { mutableStateOf(server?.url ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (server == null) "Add MCP Server" else "Edit MCP Server") },
    text = {
      Column {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Name") },
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = url,
          onValueChange = { url = it },
          label = { Text("URL") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onConfirm(server?.name, name, url) },
        enabled = name.isNotBlank() && url.isNotBlank()
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
