package com.google.ai.edge.gallery.ui.mcpserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.DataStoreRepository
import com.google.ai.edge.gallery.proto.McpServer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class McpServersViewModel @Inject constructor(
  private val dataStoreRepository: DataStoreRepository,
) : ViewModel() {

  val mcpServers: StateFlow<List<McpServer>> =
    dataStoreRepository.settings
      .map { it.mcpServersList }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
      )

  fun addOrUpdateServer(oldName: String?, newName: String, url: String) {
    viewModelScope.launch {
      dataStoreRepository.updateSettings { currentSettings ->
        val currentServers = currentSettings.mcpServersList.toMutableList()
        if (oldName != null) {
          val index = currentServers.indexOfFirst { it.name == oldName }
          if (index != -1) {
            currentServers[index] = McpServer.newBuilder().setName(newName).setUrl(url).build()
          } else {
            currentServers.add(McpServer.newBuilder().setName(newName).setUrl(url).build())
          }
        } else {
          currentServers.add(McpServer.newBuilder().setName(newName).setUrl(url).build())
        }
        currentSettings.toBuilder()
          .clearMcpServers()
          .addAllMcpServers(currentServers)
          .build()
      }
    }
  }

  fun deleteServer(name: String) {
    viewModelScope.launch {
      dataStoreRepository.updateSettings { currentSettings ->
        val currentServers = currentSettings.mcpServersList.filter { it.name != name }
        currentSettings.toBuilder()
          .clearMcpServers()
          .addAllMcpServers(currentServers)
          .build()
      }
    }
  }
}
