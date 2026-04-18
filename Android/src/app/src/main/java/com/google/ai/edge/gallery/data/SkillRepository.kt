/*
 * Copyright 2026 Google LLC
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

import android.content.Context
import android.util.Log
import com.google.ai.edge.gallery.common.LOCAL_URL_BASE
import com.google.ai.edge.gallery.proto.Skill
import com.google.ai.edge.litertlm.Contents
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AGSkillRepository"

/** Repository for managing Agent Skills, including loading from assets and persistence. */
@Singleton
class SkillRepository
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val dataStoreRepository: DataStoreRepository,
) {
  private var skillLoaded = false
  private var cachedSkills: List<Skill> = emptyList()

  suspend fun loadSkills(): List<Skill> {
    if (skillLoaded) return cachedSkills

    return withContext(Dispatchers.IO) {
      Log.d(TAG, "Loading skills index...")

      // 1. Load all skills from DataStore.
      val allDataStoreSkills = dataStoreRepository.getAllSkills()
      val dataStoreBuiltInSkills = allDataStoreSkills.filter { it.builtIn }
      val dataStoreCustomSkills = allDataStoreSkills.filter { !it.builtIn }

      // 2. Keep track of the selection state of existing built-in skills.
      val builtInSelectionMap = dataStoreBuiltInSkills.associate { it.name to it.selected }

      // 3. Read and parse SKILL.md files from assets/skills directories.
      val builtInSkills = mutableListOf<Skill>()
      try {
        val skillAssetDirs = context.assets.list("skills") ?: emptyArray()
        for (dirName in skillAssetDirs) {
          val skillMdPath = "skills/$dirName/SKILL.md"
          try {
            context.assets.open(skillMdPath).use { inputStream ->
              val mdContent = inputStream.bufferedReader().use { it.readText() }
              val (skillProto, errors) =
                convertSkillMdToProto(
                  mdContent,
                  builtIn = true,
                  selected = true,
                  importDir = "assets/skills/$dirName",
                )
              if (errors.isNotEmpty()) {
                Log.w(TAG, "Error parsing asset skill $dirName: ${errors.joinToString(", ")}")
              } else {
                skillProto?.let {
                  // Apply the previous selection state if it exists, otherwise default to true.
                  val selectedState = builtInSelectionMap[it.name] ?: true
                  builtInSkills.add(it.toBuilder().setSelected(selectedState).build())
                }
              }
            }
          } catch (e: Exception) {
            Log.w(TAG, "SKILL.md not found or error reading for asset skill $dirName", e)
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error listing assets/skills", e)
      }

      // 4. Combine with custom skills.
      val finalSkills = builtInSkills.toMutableList()
      for (customSkill in dataStoreCustomSkills) {
        if (!finalSkills.any { it.name == customSkill.name }) {
          finalSkills.add(customSkill)
        }
      }

      // 5. Update DataStore and Cache.
      dataStoreRepository.setSkills(finalSkills)
      cachedSkills = finalSkills
      skillLoaded = true
    finalSkills
    }
  }

  fun getSkill(name: String): Skill? {
    return cachedSkills.find { it.name == name }
  }

  fun getJsSkillUrl(skillName: String, scriptName: String): String? {
    val skill = getSkill(name = skillName) ?: return null
    var baseUrl = ""
    // Construct a local URL for imported skill and built-in skills.
    if (skill.importDirName.isNotEmpty()) {
      baseUrl = "$LOCAL_URL_BASE/${skill.importDirName}"
    }
    // Use skill.skillUrl if set.
    else if (skill.skillUrl.isNotEmpty()) {
      baseUrl = skill.skillUrl
    }
    if (baseUrl.isEmpty()) {
      return null
    }
    return "$baseUrl/scripts/$scriptName"
  }

  fun getJsSkillWebviewUrl(skillName: String, url: String): String {
    val skill = getSkill(name = skillName) ?: return url

    // Return the url if it is an absolute url.
    if (url.startsWith("http")) {
      return url
    }

    var baseUrl = ""
    // Construct a local URL for imported skill.
    if (skill.importDirName.isNotEmpty()) {
      baseUrl = "$LOCAL_URL_BASE/${skill.importDirName}"
    }
    // Use skill.skillUrl if set.
    else if (skill.skillUrl.isNotEmpty()) {
      baseUrl = skill.skillUrl
    }
    if (baseUrl.isEmpty()) {
      return url
    }
    return "$baseUrl/assets/$url"
  }

  fun getSelectedSkills(): List<Skill> {
    return cachedSkills.filter { it.selected }
  }

  fun getSystemPrompt(baseSystemPrompt: String): Contents {
    val selectedSkillsStr = getSelectedSkills().joinToString("\n") { skill ->
      "- ${skill.name}: ${skill.description}"
    }
    val systemPrompt = baseSystemPrompt.replace("___SKILLS___", selectedSkillsStr)
    return Contents.of(systemPrompt)
  }

  /** Simplified conversion logic extracted from ViewModel. */
  private fun convertSkillMdToProto(
    mdContent: String,
    builtIn: Boolean,
    selected: Boolean,
    importDir: String = "",
  ): Pair<Skill?, List<String>> {
    val parts = mdContent.split("---")
    val errors = mutableListOf<String>()

    if (parts.size < 3) {
      errors.add("Invalid format: Expected at least two '---' sections.")
      return Pair(null, errors)
    }

    val header = parts[1].trim()
    var name: String? = null
    var description: String? = null

    for (line in header.lines()) {
      val trimmedLine = line.trim()
      when {
        trimmedLine.startsWith("name:") -> name = trimmedLine.substringAfter("name:").trim()
        trimmedLine.startsWith("description:") ->
          description = trimmedLine.substringAfter("description:").trim()
      }
    }

    if (name == null || description == null) {
      errors.add("Missing name or description in SKILL.md header.")
      return Pair(null, errors)
    }

    val instruction = parts.subList(2, parts.size).joinToString("---").trim()
    val skill =
      Skill.newBuilder()
        .setName(name)
        .setDescription(description)
        .setInstructions(instruction)
        .setBuiltIn(builtIn)
        .setSelected(selected)
        .setImportDirName(importDir)
        .build()

    return Pair(skill, errors)
  }
}
