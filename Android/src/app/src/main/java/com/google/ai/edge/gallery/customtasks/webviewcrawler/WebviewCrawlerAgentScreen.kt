package com.google.ai.edge.gallery.customtasks.webviewcrawler

import android.graphics.Bitmap
import android.util.Base64
import android.webkit.WebView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.GalleryWebView
import com.google.ai.edge.gallery.ui.llmchat.LlmChatScreen
import com.google.ai.edge.gallery.ui.llmchat.LlmChatViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CompletableDeferred
import kotlin.math.min

@Composable
fun WebviewCrawlerAgentScreen(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  tools: WebviewCrawlerAgentTools,
  viewModel: LlmChatViewModel = hiltViewModel(),
) {
  var isWebviewMaximized by remember { mutableStateOf(false) }
  var webviewUrl by remember { mutableStateOf("https://www.google.com") }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var approvalPlan by remember { mutableStateOf<String?>(null) }
  var approvalDeferred by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(tools) {
    tools.onNavigateUrl = { url ->
      webviewUrl = url
      webViewRef?.loadUrl(url)
    }

    tools.onGetDomAndScreenshot = { deferred ->
      val view = webViewRef
      if (view != null) {
        view.evaluateJavascript(
          "(function() { return document.documentElement.outerHTML; })();"
        ) { domResult ->
          val unescapedDom = domResult?.replace("\\\\u003C", "<") ?: ""

          // Simplistic screenshot capture, scaling down
          val originalWidth = view.width.takeIf { it > 0 } ?: 100
          val originalHeight = view.height.takeIf { it > 0 } ?: 100

          val bitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888)
          val canvas = android.graphics.Canvas(bitmap)
          view.draw(canvas)

          // Max dimension 512px
          val maxDim = 512f
          val scale = min(maxDim / originalWidth, maxDim / originalHeight).coerceAtMost(1f)
          val scaledWidth = (originalWidth * scale).toInt()
          val scaledHeight = (originalHeight * scale).toInt()

          val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

          val stream = ByteArrayOutputStream()
          scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
          val base64Screenshot = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

          deferred.complete(
            WebviewCrawlerAgentTools.DomAndScreenshotResult(
              dom = unescapedDom,
              screenshotBase64 = base64Screenshot
            )
          )
        }
      } else {
        deferred.complete(
          WebviewCrawlerAgentTools.DomAndScreenshotResult(
            dom = "Error: WebView not initialized",
            screenshotBase64 = ""
          )
        )
      }
    }

    tools.onRunInternalCommand = { command, deferred ->
      webViewRef?.evaluateJavascript(command) { result ->
        deferred.complete(result ?: "executed")
      } ?: deferred.complete("Error: WebView not initialized")
    }

    tools.onRequestApproval = { plan, deferred ->
      isWebviewMaximized = false // Hide/minimize when asking for approval
      approvalPlan = plan
      approvalDeferred = deferred
    }
  }

  val webViewWeight by animateFloatAsState(targetValue = if (isWebviewMaximized) 1f else 0.0001f, label = "webviewWeight")

  Column(modifier = Modifier.fillMaxSize()) {
    // Top part: WebView (animates weight)
    Box(modifier = Modifier.weight(webViewWeight)) {
      GalleryWebView(
        modifier = Modifier.fillMaxSize(),
        initialUrl = webviewUrl,
        preventParentScrolling = true,
        allowRequestPermission = true,
        onWebViewCreated = { webViewRef = it }
      )

      if (isWebviewMaximized) {
        IconButton(
          onClick = { isWebviewMaximized = false },
          modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
          Icon(Icons.Outlined.CloseFullscreen, contentDescription = "Minimize")
        }
      }
    }

    // Toggle button if minimized
    if (!isWebviewMaximized) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(onClick = { isWebviewMaximized = true }) {
          Icon(Icons.Outlined.OpenInFull, contentDescription = "Maximize Webview")
          Text(" Maximize Webview", modifier = Modifier.padding(start = 4.dp))
        }
      }
    }

    // Bottom part: Chat View
    Box(modifier = Modifier.weight(if (isWebviewMaximized) 1f else 2f)) {
      LlmChatScreen(
        modelManagerViewModel = modelManagerViewModel,
        taskId = BuiltInTaskId.LLM_WEBVIEW_AGENT,
        navigateUp = navigateUp,
        allowEditingSystemPrompt = true,
        curSystemPrompt = task.defaultSystemPrompt,
        onSystemPromptChanged = {}
      )
    }
  }

  // Approval Dialog
  if (approvalPlan != null && approvalDeferred != null) {
    AlertDialog(
      onDismissRequest = {
        approvalDeferred?.complete(false)
        approvalPlan = null
        approvalDeferred = null
      },
      title = { Text("Approve Action") },
      text = { Text(approvalPlan!!) },
      confirmButton = {
        Button(
          onClick = {
            isWebviewMaximized = true // Maximize when approved
            approvalDeferred?.complete(true)
            approvalPlan = null
            approvalDeferred = null
          }
        ) {
          Text("Approve")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            approvalDeferred?.complete(false)
            approvalPlan = null
            approvalDeferred = null
          }
        ) {
          Text("Reject")
        }
      }
    )
  }
}
