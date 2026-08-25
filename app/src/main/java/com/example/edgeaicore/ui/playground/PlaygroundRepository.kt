package com.example.edgeaicore.ui.playground

import android.content.Context
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.explanation.ExplanationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Local file-backed repository for Playground sessions and messages.
 * Operates completely on-device without cloud egress.
 */
class PlaygroundRepository(private val context: Context) {

    private val storageFile: File by lazy {
        File(context.filesDir, "swayam_playground_sessions.json")
    }

    suspend fun loadSessions(): List<PlaygroundSession> = withContext(Dispatchers.IO) {
        if (!storageFile.exists() || storageFile.length() <= 0) {
            val defaultSession = createDefaultSession()
            saveSessions(listOf(defaultSession))
            return@withContext listOf(defaultSession)
        }

        try {
            val content = storageFile.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(content)
            val list = mutableListOf<PlaygroundSession>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(parseSession(obj))
            }

            if (list.isEmpty()) {
                val defaultSession = createDefaultSession()
                saveSessions(listOf(defaultSession))
                listOf(defaultSession)
            } else {
                list
            }
        } catch (_: Exception) {
            val defaultSession = createDefaultSession()
            listOf(defaultSession)
        }
    }

    suspend fun saveSessions(sessions: List<PlaygroundSession>): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            for (session in sessions) {
                jsonArray.put(serializeSession(session))
            }
            val tmpFile = File(context.filesDir, "swayam_playground_sessions.tmp")
            tmpFile.writeText(jsonArray.toString(2), Charsets.UTF_8)
            if (storageFile.exists()) storageFile.delete()
            tmpFile.renameTo(storageFile)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun createDefaultSession(): PlaygroundSession {
        return PlaygroundSession(
            title = "SWAYAM Primary Workspace",
            mode = PlaygroundMode.GENERAL,
            modelId = "gemma-2b-it-litert",
            messages = listOf(
                PlaygroundMessage(
                    role = MessageRole.ASSISTANT,
                    content = "### ⚡ SWAYAM Sovereign Playground Initialized\n\n" +
                            "Welcome to your private interactive workspace for on-device reasoning, research, document intelligence, memory exploration, and autonomous agents.\n\n" +
                            "```json\n{\n  \"engine\": \"LiteRT-LM On-Device Neural Engine\",\n  \"mode\": \"General / Research / Documents / Memory / Agents\",\n  \"privacy\": \"Zero Cloud Egress\",\n  \"context\": \"Active Context Engine\"\n}\n```\n\n" +
                            "Select a mode above or ask any question to begin.",
                    provider = AIProviderType.LOCAL,
                    runtime = "LiteRT-LM On-Device Neural Engine",
                    backend = ExecutionBackend.GPU,
                    latencyMs = 5,
                    tokensGenerated = 42,
                    tokensPerSecond = 50.0,
                    explanation = ExplanationRecord(
                        featureName = "Playground Initialization",
                        whatHappened = "Bootstrapped local sovereign workspace with verified local engine.",
                        whyReason = "User opened SWAYAM Playground.",
                        confidenceScore = 1.0f,
                        dataSourcesUsed = listOf("SWAYAM Sovereign Core"),
                        wasAiInvolved = true,
                        providerType = AIProviderType.LOCAL
                    )
                )
            )
        )
    }

    private fun serializeSession(session: PlaygroundSession): JSONObject {
        val obj = JSONObject()
        obj.put("id", session.id)
        obj.put("title", session.title)
        obj.put("createdAt", session.createdAt)
        obj.put("updatedAt", session.updatedAt)
        obj.put("mode", session.mode.name)
        obj.put("modelId", session.modelId)

        val msgs = JSONArray()
        for (m in session.messages) {
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("role", m.role.name)
            mObj.put("content", m.content)
            mObj.put("timestamp", m.timestamp)
            mObj.put("model", m.model)
            mObj.put("provider", m.provider.name)
            mObj.put("runtime", m.runtime)
            mObj.put("backend", m.backend.name)
            mObj.put("latencyMs", m.latencyMs)
            mObj.put("tokensGenerated", m.tokensGenerated)
            mObj.put("tokensPerSecond", m.tokensPerSecond)
            mObj.put("networkUsed", m.networkUsed)
            mObj.put("executionMode", m.executionMode.name)
            mObj.put("status", m.status)

            val sourcesArr = JSONArray()
            for (s in m.sources) {
                val sObj = JSONObject()
                sObj.put("title", s.title)
                sObj.put("snippet", s.snippet)
                sObj.put("relevance", s.relevance.toDouble())
                sObj.put("sourceType", s.sourceType)
                sourcesArr.put(sObj)
            }
            mObj.put("sources", sourcesArr)

            val memArr = JSONArray()
            m.memoryUsed.forEach { memArr.put(it) }
            mObj.put("memoryUsed", memArr)

            val docArr = JSONArray()
            m.documentsUsed.forEach { docArr.put(it) }
            mObj.put("documentsUsed", docArr)

            val toolsArr = JSONArray()
            m.toolsUsed.forEach { toolsArr.put(it) }
            mObj.put("toolsUsed", toolsArr)

            val agentArr = JSONArray()
            m.agentUsed.forEach { agentArr.put(it) }
            mObj.put("agentUsed", agentArr)

            msgs.put(mObj)
        }
        obj.put("messages", msgs)

        val docRefs = JSONArray()
        session.documentReferences.forEach { docRefs.put(it) }
        obj.put("documentReferences", docRefs)

        val memRefs = JSONArray()
        session.memoryReferences.forEach { memRefs.put(it) }
        obj.put("memoryReferences", memRefs)

        val agentRefs = JSONArray()
        session.agentReferences.forEach { agentRefs.put(it) }
        obj.put("agentReferences", agentRefs)

        val tagArr = JSONArray()
        session.tags.forEach { tagArr.put(it) }
        obj.put("tags", tagArr)

        return obj
    }

    private fun parseSession(obj: JSONObject): PlaygroundSession {
        val id = obj.optString("id", "")
        val title = obj.optString("title", "Playground Session")
        val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
        val modeStr = obj.optString("mode", PlaygroundMode.GENERAL.name)
        val mode = try { PlaygroundMode.valueOf(modeStr) } catch (_: Exception) { PlaygroundMode.GENERAL }
        val modelId = obj.optString("modelId", "gemma-2b-it-litert")

        val messages = mutableListOf<PlaygroundMessage>()
        val msgsArr = obj.optJSONArray("messages")
        if (msgsArr != null) {
            for (i in 0 until msgsArr.length()) {
                val mObj = msgsArr.getJSONObject(i)
                val mId = mObj.optString("id", "")
                val roleStr = mObj.optString("role", MessageRole.ASSISTANT.name)
                val role = try { MessageRole.valueOf(roleStr) } catch (_: Exception) { MessageRole.ASSISTANT }
                val content = mObj.optString("content", "")
                val timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                val model = mObj.optString("model", "gemma-2b-it-litert")
                val providerStr = mObj.optString("provider", AIProviderType.LOCAL.name)
                val provider = try { AIProviderType.valueOf(providerStr) } catch (_: Exception) { AIProviderType.LOCAL }
                val runtime = mObj.optString("runtime", "LiteRT-LM On-Device")
                val backendStr = mObj.optString("backend", ExecutionBackend.AUTO.name)
                val backend = try { ExecutionBackend.valueOf(backendStr) } catch (_: Exception) { ExecutionBackend.AUTO }
                val latencyMs = mObj.optLong("latencyMs", 0L)
                val tokensGenerated = mObj.optInt("tokensGenerated", 0)
                val tokensPerSecond = mObj.optDouble("tokensPerSecond", 0.0)
                val networkUsed = mObj.optBoolean("networkUsed", false)
                val execModeStr = mObj.optString("executionMode", mode.name)
                val execMode = try { PlaygroundMode.valueOf(execModeStr) } catch (_: Exception) { mode }
                val status = mObj.optString("status", "SUCCESS")

                val sources = mutableListOf<PlaygroundSource>()
                val srcArr = mObj.optJSONArray("sources")
                if (srcArr != null) {
                    for (s in 0 until srcArr.length()) {
                        val sObj = srcArr.getJSONObject(s)
                        sources.add(
                            PlaygroundSource(
                                title = sObj.optString("title", "Document"),
                                snippet = sObj.optString("snippet", ""),
                                relevance = sObj.optDouble("relevance", 0.8).toFloat(),
                                sourceType = sObj.optString("sourceType", "Document")
                            )
                        )
                    }
                }

                val memoryUsed = mutableListOf<String>()
                mObj.optJSONArray("memoryUsed")?.let { arr ->
                    for (j in 0 until arr.length()) memoryUsed.add(arr.getString(j))
                }

                val documentsUsed = mutableListOf<String>()
                mObj.optJSONArray("documentsUsed")?.let { arr ->
                    for (j in 0 until arr.length()) documentsUsed.add(arr.getString(j))
                }

                val toolsUsed = mutableListOf<String>()
                mObj.optJSONArray("toolsUsed")?.let { arr ->
                    for (j in 0 until arr.length()) toolsUsed.add(arr.getString(j))
                }

                val agentUsed = mutableListOf<String>()
                mObj.optJSONArray("agentUsed")?.let { arr ->
                    for (j in 0 until arr.length()) agentUsed.add(arr.getString(j))
                }

                messages.add(
                    PlaygroundMessage(
                        id = mId,
                        role = role,
                        content = content,
                        timestamp = timestamp,
                        model = model,
                        provider = provider,
                        runtime = runtime,
                        backend = backend,
                        latencyMs = latencyMs,
                        tokensGenerated = tokensGenerated,
                        tokensPerSecond = tokensPerSecond,
                        sources = sources,
                        memoryUsed = memoryUsed,
                        documentsUsed = documentsUsed,
                        toolsUsed = toolsUsed,
                        agentUsed = agentUsed,
                        networkUsed = networkUsed,
                        executionMode = execMode,
                        status = status
                    )
                )
            }
        }

        return PlaygroundSession(
            id = id,
            title = title,
            createdAt = createdAt,
            updatedAt = updatedAt,
            mode = mode,
            modelId = modelId,
            messages = messages
        )
    }
}
