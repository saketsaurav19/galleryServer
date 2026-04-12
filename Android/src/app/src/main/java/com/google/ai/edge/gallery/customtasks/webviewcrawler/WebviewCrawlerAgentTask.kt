package com.google.ai.edge.gallery.customtasks.webviewcrawler

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.Composable
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskDataForBuiltinTask
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.litertlm.tool
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

class WebviewCrawlerAgentTask @Inject constructor() : CustomTask {
  private val tools = WebviewCrawlerAgentTools()

  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_WEBVIEW_AGENT,
      label = "Web Crawler Agent",
      category = Category.EXPERIMENTAL,
      icon = Icons.Outlined.Language,
      newFeature = true,
      models = mutableListOf(),
      description = "An AI agent that can crawl, analyze, and interact with web pages",
      shortDescription = "Crawl and interact with websites",
      docUrl = "https://github.com/google-ai-edge/LiteRT-LM/blob/main/kotlin/README.md",
      sourceCodeUrl =
        "https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/webviewcrawler/",
      defaultSystemPrompt =
        """
        You are a Web Crawler Agent. Your job is to help users navigate and interact with websites. You have tools available to load URLs, get the DOM of the loaded webpage, and run internal JavaScript commands (like clicking, scrolling, or filling inputs).

        When a user asks you to interact with a page, think about the steps needed, load the page, inspect the DOM, and formulate internal commands to execute actions. Provide concise summaries to the user.
        """.trimIndent(),
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    LlmChatModelHelper.initialize(
      context = context,
      model = model,
      supportImage = true,
      supportAudio = false,
      onDone = onDone,
      systemInstruction = com.google.ai.edge.litertlm.Contents.of(task.defaultSystemPrompt),
      tools = listOf(tool(tools)),
      enableConversationConstrainedDecoding = true,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    WebviewCrawlerAgentScreen(
      task = task,
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
      tools = tools,
    )
  }
}
