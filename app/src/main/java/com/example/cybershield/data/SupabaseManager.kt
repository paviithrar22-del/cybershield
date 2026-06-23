package com.example.cybershield.data

import android.content.Context
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.statement.bodyAsText
import com.example.cybershield.nlp.ClassificationResult
import com.example.cybershield.nlp.Severity
import com.example.cybershield.nlp.BullyingClassifier

@Serializable
data class RemoteIncident(
    @SerialName("user_id") val userId: String,
    val timestamp: Long,
    val sender: String,
    @SerialName("message_content") val messageContent: String,
    @SerialName("source_app") val sourceApp: String,
    val severity: String,
    val reason: String
)

@Serializable
data class EdgeRequest(val text: String)

@Serializable
data class EdgeResponse(val severity: String, val reason: String)

class SupabaseManager private constructor() {

    companion object {
        private const val SUPABASE_URL = "https://rkzrhiwxbypqfttoczzj.supabase.co"
        private const val SUPABASE_ANON_KEY = "sb_publishable_Gk6mjuBLJAwNejBarnDzSw_zT2ITHy5"

        @Volatile
        private var instance: SupabaseManager? = null

        fun getInstance(): SupabaseManager {
            return instance ?: synchronized(this) {
                instance ?: SupabaseManager().also { instance = it }
            }
        }
    }

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Functions)
    }

    val currentUserFlow: Flow<UserInfo?> = client.auth.sessionStatus.map { status ->
        if (status is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
            status.session.user
        } else {
            null
        }
    }

    val currentUserId: String?
        get() = (client.auth.sessionStatus.value as? io.github.jan.supabase.auth.status.SessionStatus.Authenticated)?.session?.user?.id

    suspend fun classifyWithGeminiEdge(text: String): ClassificationResult {
        return try {
            val response = client.functions.invoke(
                function = "classify-message",
                body = EdgeRequest(text = text)
            )
            val responseText = response.bodyAsText()
            val parsed = Json.decodeFromString<EdgeResponse>(responseText)
            
            val severity = try {
                Severity.valueOf(parsed.severity.uppercase())
            } catch (e: Exception) {
                Severity.NONE
            }

            ClassificationResult(
                severity = severity,
                isFlagged = severity.ordinal >= Severity.MEDIUM.ordinal,
                reason = "Gemini Edge: ${parsed.reason}"
            )
        } catch (e: Exception) {
            // Fallback to local classifier if Edge Function call fails
            BullyingClassifier.classify(text, "Medium")
        }
    }

    suspend fun signUp(emailText: String, passwordText: String) {
        client.auth.signUpWith(Email) {
            email = emailText
            password = passwordText
        }
    }

    suspend fun signIn(emailText: String, passwordText: String) {
        client.auth.signInWith(Email) {
            email = emailText
            password = passwordText
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun syncIncident(incident: Incident) {
        val userId = currentUserId ?: return
        val remote = RemoteIncident(
            userId = userId,
            timestamp = incident.timestamp,
            sender = incident.sender,
            messageContent = incident.messageContent,
            sourceApp = incident.sourceApp,
            severity = incident.severity.name,
            reason = incident.reason
        )
        client.postgrest["incidents"].insert(remote)
    }
}
