/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.data

import androidx.datastore.core.DataStore
import com.google.ai.edge.gallery.proto.AccessTokenData
import com.google.ai.edge.gallery.proto.BenchmarkResult
import com.google.ai.edge.gallery.proto.BenchmarkResults
import com.google.ai.edge.gallery.proto.Cutout
import com.google.ai.edge.gallery.proto.CutoutCollection
import com.google.ai.edge.gallery.proto.ImportedModel
import com.google.ai.edge.gallery.proto.Settings
import com.google.ai.edge.gallery.proto.Skill
import com.google.ai.edge.gallery.proto.Skills
import com.google.ai.edge.gallery.proto.Theme
import com.google.ai.edge.gallery.proto.UserData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// TODO(b/423700720): Change to async (suspend) functions
interface DataStoreRepository {
  suspend fun saveTextInputHistory(history: List<String>)

  suspend fun readTextInputHistory(): List<String>

  suspend fun saveTheme(theme: Theme)

  suspend fun readTheme(): Theme

  suspend fun saveSecret(key: String, value: String)

  suspend fun readSecret(key: String): String?

  suspend fun deleteSecret(key: String)

  suspend fun saveAccessTokenData(accessToken: String, refreshToken: String, expiresAt: Long)

  suspend fun clearAccessTokenData()

  suspend fun readAccessTokenData(): AccessTokenData?

  suspend fun saveImportedModels(importedModels: List<ImportedModel>)

  suspend fun readImportedModels(): List<ImportedModel>

  suspend fun isTosAccepted(): Boolean

  suspend fun acceptTos()

  suspend fun isGemmaTermsOfUseAccepted(): Boolean

  suspend fun acceptGemmaTermsOfUse()

  suspend fun getHasRunTinyGarden(): Boolean

  suspend fun setHasRunTinyGarden(hasRun: Boolean)

  suspend fun addCutout(cutout: Cutout)

  suspend fun getAllCutouts(): List<Cutout>

  suspend fun setCutout(newCutout: Cutout)

  suspend fun setCutouts(cutouts: List<Cutout>)

  suspend fun setHasSeenBenchmarkComparisonHelp(seen: Boolean)

  suspend fun getHasSeenBenchmarkComparisonHelp(): Boolean

  suspend fun addBenchmarkResult(result: BenchmarkResult)

  suspend fun getAllBenchmarkResults(): List<BenchmarkResult>

  suspend fun deleteBenchmarkResult(index: Int)

  suspend fun addSkill(skill: Skill)

  suspend fun setSkills(skills: List<Skill>)

  suspend fun setSkillSelected(skill: Skill, selected: Boolean)

  suspend fun setAllSkillsSelected(selected: Boolean)

  suspend fun getAllSkills(): List<Skill>

  suspend fun deleteSkill(name: String)

  suspend fun deleteSkills(names: Set<String>)

  /** Records that a promo with the specified ID has been viewed. */
  suspend fun addViewedPromoId(promoId: String)

  /** Removes a viewed promo record. */
  suspend fun removeViewedPromoId(promoId: String)

  /** Returns whether a promo with the specified ID has been viewed. */
  suspend fun hasViewedPromo(promoId: String): Boolean
}

/** Repository for managing data using Proto DataStore. */
class DefaultDataStoreRepository(
  private val dataStore: DataStore<Settings>,
  private val userDataDataStore: DataStore<UserData>,
  private val cutoutDataStore: DataStore<CutoutCollection>,
  private val benchmarkResultsDataStore: DataStore<BenchmarkResults>,
  private val skillsDataStore: DataStore<Skills>,
) : DataStoreRepository {
  override suspend fun saveTextInputHistory(history: List<String>) {
    dataStore.updateData { settings ->
      settings.toBuilder().clearTextInputHistory().addAllTextInputHistory(history).build()
    }
  }

  override suspend fun readTextInputHistory(): List<String> {
    val settings = dataStore.data.first()
    return settings.textInputHistoryList
  }

  override suspend fun saveTheme(theme: Theme) {
    dataStore.updateData { settings -> settings.toBuilder().setTheme(theme).build() }
  }

  override suspend fun readTheme(): Theme {
    val settings = dataStore.data.first()
    val curTheme = settings.theme
    // Use "auto" as the default theme.
    return if (curTheme == Theme.THEME_UNSPECIFIED) Theme.THEME_AUTO else curTheme
  }

  override suspend fun saveSecret(key: String, value: String) {
    userDataDataStore.updateData { userData ->
      userData.toBuilder().putSecrets(key, value).build()
    }
  }

  override suspend fun readSecret(key: String): String? {
    return userDataDataStore.data.first().secretsMap[key]
  }

  override suspend fun deleteSecret(key: String) {
    userDataDataStore.updateData { userData -> userData.toBuilder().removeSecrets(key).build() }
  }

  override suspend fun saveAccessTokenData(accessToken: String, refreshToken: String, expiresAt: Long) {
    // Clear the entry in old data store.
    dataStore.updateData { settings ->
      settings.toBuilder().setAccessTokenData(AccessTokenData.getDefaultInstance()).build()
    }

    userDataDataStore.updateData { userData ->
      userData
        .toBuilder()
        .setAccessTokenData(
          AccessTokenData.newBuilder()
            .setAccessToken(accessToken)
            .setRefreshToken(refreshToken)
            .setExpiresAtMs(expiresAt)
            .build()
        )
        .build()
    }
  }

  override suspend fun clearAccessTokenData() {
    dataStore.updateData { settings -> settings.toBuilder().clearAccessTokenData().build() }
    userDataDataStore.updateData { userData -> userData.toBuilder().clearAccessTokenData().build() }
  }

  override suspend fun readAccessTokenData(): AccessTokenData? {
    val userData = userDataDataStore.data.first()
    return userData.accessTokenData
  }

  override suspend fun saveImportedModels(importedModels: List<ImportedModel>) {
    dataStore.updateData { settings ->
      settings.toBuilder().clearImportedModel().addAllImportedModel(importedModels).build()
    }
  }

  override suspend fun readImportedModels(): List<ImportedModel> {
    val settings = dataStore.data.first()
    return settings.importedModelList
  }

  override suspend fun isTosAccepted(): Boolean {
    val settings = dataStore.data.first()
    return settings.isTosAccepted
  }

  override suspend fun acceptTos() {
    dataStore.updateData { settings -> settings.toBuilder().setIsTosAccepted(true).build() }
  }

  override suspend fun isGemmaTermsOfUseAccepted(): Boolean {
    val settings = dataStore.data.first()
    return settings.isGemmaTermsAccepted
  }

  override suspend fun acceptGemmaTermsOfUse() {
    dataStore.updateData { settings -> settings.toBuilder().setIsGemmaTermsAccepted(true).build() }
  }

  override suspend fun getHasRunTinyGarden(): Boolean {
    val settings = dataStore.data.first()
    return settings.hasRunTinyGarden
  }

  override suspend fun setHasRunTinyGarden(hasRun: Boolean) {
    dataStore.updateData { settings -> settings.toBuilder().setHasRunTinyGarden(hasRun).build() }
  }

  override suspend fun addCutout(cutout: Cutout) {
    cutoutDataStore.updateData { cutouts -> cutouts.toBuilder().addCutout(cutout).build() }
  }

  override suspend fun getAllCutouts(): List<Cutout> {
    return cutoutDataStore.data.first().cutoutList
  }

  override suspend fun setCutout(newCutout: Cutout) {
    cutoutDataStore.updateData { cutouts ->
      var index = -1
      for (i in 0..<cutouts.cutoutCount) {
        val cutout = cutouts.cutoutList.get(i)
        if (cutout.id == newCutout.id) {
          index = i
          break
        }
      }
      if (index >= 0) {
        cutouts.toBuilder().setCutout(index, newCutout).build()
      } else {
        cutouts
      }
    }
  }

  override suspend fun setCutouts(cutouts: List<Cutout>) {
    cutoutDataStore.updateData { CutoutCollection.newBuilder().addAllCutout(cutouts).build() }
  }

  override suspend fun setHasSeenBenchmarkComparisonHelp(seen: Boolean) {
    dataStore.updateData { settings ->
      settings.toBuilder().setHasSeenBenchmarkComparisonHelp(seen).build()
    }
  }

  override suspend fun getHasSeenBenchmarkComparisonHelp(): Boolean {
    val settings = dataStore.data.first()
    return settings.hasSeenBenchmarkComparisonHelp
  }

  override suspend fun addBenchmarkResult(result: BenchmarkResult) {
    benchmarkResultsDataStore.updateData { results ->
      results.toBuilder().addResult(0, result).build()
    }
  }

  override suspend fun getAllBenchmarkResults(): List<BenchmarkResult> {
    return benchmarkResultsDataStore.data.first().resultList
  }

  override suspend fun deleteBenchmarkResult(index: Int) {
    benchmarkResultsDataStore.updateData { results ->
      results.toBuilder().removeResult(index).build()
    }
  }

  override suspend fun addSkill(skill: Skill) {
    skillsDataStore.updateData { skills ->
      val newSkills = buildList {
        add(skill)
        addAll(skills.skillList)
      }
      skills.toBuilder().clearSkill().addAllSkill(newSkills).build()
    }
  }

  override suspend fun setSkills(skills: List<Skill>) {
    skillsDataStore.updateData { curSkills ->
      curSkills.toBuilder().clearSkill().addAllSkill(skills).build()
    }
  }

  override suspend fun setSkillSelected(skill: Skill, selected: Boolean) {
    skillsDataStore.updateData { skills ->
      val newSkills = mutableListOf<Skill>()
      for (curSkill in skills.skillList) {
        if (curSkill.name == skill.name) {
          newSkills.add(curSkill.toBuilder().setSelected(selected).build())
        } else {
          newSkills.add(curSkill)
        }
      }
      Skills.newBuilder().addAllSkill(newSkills).build()
    }
  }

  override suspend fun setAllSkillsSelected(selected: Boolean) {
    skillsDataStore.updateData { skills ->
      val newSkills = mutableListOf<Skill>()
      for (curSkill in skills.skillList) {
        newSkills.add(curSkill.toBuilder().setSelected(selected).build())
      }
      Skills.newBuilder().addAllSkill(newSkills).build()
    }
  }

  override suspend fun getAllSkills(): List<Skill> {
    return skillsDataStore.data.first().skillList
  }

  override suspend fun deleteSkill(name: String) {
    skillsDataStore.updateData { skills ->
      val newSkills = mutableListOf<Skill>()
      for (skill in skills.skillList) {
        if (skill.name != name) {
          newSkills.add(skill)
        }
      }
      Skills.newBuilder().addAllSkill(newSkills).build()
    }
  }

  override suspend fun deleteSkills(names: Set<String>) {
    skillsDataStore.updateData { skills ->
      val newSkills = skills.skillList.filter { it.name !in names }
      skills.toBuilder().clearSkill().addAllSkill(newSkills).build()
    }
  }

  override suspend fun addViewedPromoId(promoId: String) {
    dataStore.updateData { settings ->
      if (settings.viewedPromoIdList.contains(promoId)) {
        settings
      } else {
        settings.toBuilder().addViewedPromoId(promoId).build()
      }
    }
  }

  override suspend fun removeViewedPromoId(promoId: String) {
    dataStore.updateData { settings ->
      val newList = settings.viewedPromoIdList.filter { it != promoId }
      settings.toBuilder().clearViewedPromoId().addAllViewedPromoId(newList).build()
    }
  }

  override suspend fun hasViewedPromo(promoId: String): Boolean {
    val settings = dataStore.data.first()
    return settings.viewedPromoIdList.contains(promoId)
  }
}
