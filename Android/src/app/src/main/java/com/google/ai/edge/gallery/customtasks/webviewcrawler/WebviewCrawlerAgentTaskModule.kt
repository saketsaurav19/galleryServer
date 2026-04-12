package com.google.ai.edge.gallery.customtasks.webviewcrawler

import com.google.ai.edge.gallery.customtasks.common.CustomTask
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal object WebviewCrawlerAgentTaskModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return WebviewCrawlerAgentTask()
  }
}
