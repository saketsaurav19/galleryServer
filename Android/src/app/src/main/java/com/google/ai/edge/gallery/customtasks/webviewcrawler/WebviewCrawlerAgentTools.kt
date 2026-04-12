package com.google.ai.edge.gallery.customtasks.webviewcrawler

import android.util.Log
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

private const val TAG = "WebviewCrawlerTools"

class WebviewCrawlerAgentTools : ToolSet {
  // Callback handlers for UI integration
  var onNavigateUrl: ((String) -> Unit)? = null
  var onGetDomAndScreenshot: ((CompletableDeferred<DomAndScreenshotResult>) -> Unit)? = null
  var onRunInternalCommand: ((String, CompletableDeferred<String>) -> Unit)? = null
  var onRequestApproval: ((String, CompletableDeferred<Boolean>) -> Unit)? = null

  data class DomAndScreenshotResult(
    val dom: String,
    val screenshotBase64: String
  )

  @Tool(description = "Navigates the webview to the specified URL.")
  fun loadUrl(
    @ToolParam(description = "The full URL to load (e.g., https://example.com).") url: String
  ): Map<String, String> {
    return runBlocking(Dispatchers.Main) {
      Log.d(TAG, "Tool loadUrl called: $url")
      onNavigateUrl?.invoke(url)
      mapOf("status" to "success", "message" to "Started loading $url")
    }
  }

  @Tool(description = "Gets the DOM content and a screenshot of the currently loaded webpage.")
  fun getDomAndScreenshot(): Map<String, String> {
    return runBlocking(Dispatchers.Main) {
      Log.d(TAG, "Tool getDomAndScreenshot called")
      val deferred = CompletableDeferred<DomAndScreenshotResult>()
      onGetDomAndScreenshot?.invoke(deferred)
      val result = deferred.await()
      mapOf(
        "dom" to result.dom,
        "screenshot_base64" to result.screenshotBase64,
        "status" to "success"
      )
    }
  }

  @Tool(description = "Runs an internal JavaScript command on the currently loaded webpage.")
  fun runInternalCommand(
    @ToolParam(description = "The JavaScript command to run (e.g., document.querySelector('button').click()).") command: String
  ): Map<String, String> {
    return runBlocking(Dispatchers.Main) {
      Log.d(TAG, "Tool runInternalCommand called: $command")
      val deferred = CompletableDeferred<String>()
      onRunInternalCommand?.invoke(command, deferred)
      val result = deferred.await()
      mapOf("status" to "success", "result" to result)
    }
  }

  @Tool(description = "Asks the user to approve a plan before executing destructive or significant actions. Hides the webview while waiting.")
  fun askUserApproval(
    @ToolParam(description = "The plan or action description you want the user to approve.") plan: String
  ): Map<String, String> {
    return runBlocking(Dispatchers.Main) {
      Log.d(TAG, "Tool askUserApproval called for plan: $plan")
      val deferred = CompletableDeferred<Boolean>()
      onRequestApproval?.invoke(plan, deferred)
      val approved = deferred.await()
      if (approved) {
        mapOf("status" to "approved")
      } else {
        mapOf("status" to "rejected")
      }
    }
  }
}
