package com.nuvio.app.features.mal

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.httpRequestRaw
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.contentOrNull

internal object MalApiClient {
    private const val API_URL = "https://api.myanimelist.net/v2"
    private const val OAUTH_URL = "https://myanimelist.net/v1/oauth2/token"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun exchangeToken(code: String, codeVerifier: String): MalTokenResponse? {
        val body = "client_id=${MalConfig.CLIENT_ID}&code=$code&code_verifier=$codeVerifier&grant_type=authorization_code"
        
        val headers = mapOf(
            "Content-Type" to "application/x-www-form-urlencoded"
        )
        return try {
            val response = httpRequestRaw("POST", OAUTH_URL, headers, body)
            json.decodeFromString<MalTokenResponse>(response.body)
        } catch (_: Exception) {
            null
        }
    }
    
    suspend fun fetchViewer(accessToken: String): MalUserResponse? {
        val headers = mapOf(
            "Authorization" to "Bearer $accessToken",
            "Accept" to "application/json"
        )
        return try {
            val response = httpRequestRaw("GET", "$API_URL/users/@me", headers, "")
            json.decodeFromString<MalUserResponse>(response.body)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveProgress(
        accessToken: String,
        animeId: Int,
        numWatchedEpisodes: Int,
        status: String? = null,
        score: Double? = null,
        startDate: String? = null,
        finishDate: String? = null,
        numTimesRewatched: Int? = null,
        comments: String? = null,
        priority: Int? = null,
        rewatchValue: Int? = null,
    ): Boolean {
        val url = "$API_URL/anime/$animeId/my_list_status"
        
        val bodyBuilder = StringBuilder("num_watched_episodes=$numWatchedEpisodes")
        if (status != null) {
            bodyBuilder.append("&status=$status")
        }
        if (score != null) {
            bodyBuilder.append("&score=${score.toInt()}")
        }
        if (!startDate.isNullOrBlank()) {
            bodyBuilder.append("&start_date=$startDate")
        }
        if (!finishDate.isNullOrBlank()) {
            bodyBuilder.append("&finish_date=$finishDate")
        }
        if (numTimesRewatched != null) {
            bodyBuilder.append("&num_times_rewatched=$numTimesRewatched")
        }
        if (comments != null) {
            // Encode the string properly in real code, but StringBuilder works for simple text if we URL encode it
            // Let's use simple replacement for basic encoding
            val encodedComments = comments.replace(" ", "%20").replace("\n", "%0A")
            bodyBuilder.append("&comments=$encodedComments")
        }
        if (priority != null) {
            bodyBuilder.append("&priority=$priority")
        }
        if (rewatchValue != null) {
            bodyBuilder.append("&rewatch_value=$rewatchValue")
        }
        val body = bodyBuilder.toString()
        
        val headers = mapOf(
            "Authorization" to "Bearer $accessToken",
            "Content-Type" to "application/x-www-form-urlencoded",
            "Accept" to "application/json"
        )

        return try {
            val response = httpRequestRaw(
                method = "PATCH",
                url = url,
                headers = headers,
                body = body
            )
            response.status in 200..299
        } catch (_: Exception) {
            false
        }
    }
    suspend fun fetchListEntry(
        accessToken: String,
        animeId: Int,
    ): MalListEntry? {
        val url = "$API_URL/anime/$animeId?fields=my_list_status%7Bstatus,score,num_watched_episodes,is_rewatching,start_date,finish_date,priority,num_times_rewatched,rewatch_value,tags,comments,updated_at%7D,num_episodes,main_picture,title"
        val headers = mapOf(
            "Authorization" to "Bearer $accessToken",
            "Accept" to "application/json"
        )
        return try {
            val response = httpRequestRaw("GET", url, headers, "")
            val jsonResponse = json.parseToJsonElement(response.body).jsonObject
            val statusElement = jsonResponse["my_list_status"]
            val status = if (statusElement != null && statusElement !is kotlinx.serialization.json.JsonNull) statusElement.jsonObject else null
            val nodeTitle = jsonResponse["title"]?.jsonPrimitive?.contentOrNull
            val mainPictureElement = jsonResponse["main_picture"]
            val mainPictureObj = if (mainPictureElement != null && mainPictureElement !is kotlinx.serialization.json.JsonNull) mainPictureElement.jsonObject else null
            val nodeImage = mainPictureObj?.get("large")?.jsonPrimitive?.contentOrNull ?: mainPictureObj?.get("medium")?.jsonPrimitive?.contentOrNull
            
            MalListEntry(
                status = status?.get("status")?.jsonPrimitive?.contentOrNull,
                score = status?.get("score")?.jsonPrimitive?.doubleOrNull,
                progress = status?.get("num_episodes_watched")?.jsonPrimitive?.intOrNull,
                startDate = status?.get("start_date")?.jsonPrimitive?.contentOrNull,
                finishDate = status?.get("finish_date")?.jsonPrimitive?.contentOrNull,
                numTimesRewatched = status?.get("num_times_rewatched")?.jsonPrimitive?.intOrNull,
                comments = status?.get("comments")?.jsonPrimitive?.contentOrNull,
                priority = status?.get("priority")?.jsonPrimitive?.intOrNull,
                rewatchValue = status?.get("rewatch_value")?.jsonPrimitive?.intOrNull,
                maxEpisodes = jsonResponse["num_episodes"]?.jsonPrimitive?.intOrNull,
                title = nodeTitle,
                imageUrl = nodeImage
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteEntry(accessToken: String, animeId: Int): Boolean {
        val url = "$API_URL/anime/$animeId/my_list_status"
        val headers = mapOf(
            "Authorization" to "Bearer $accessToken"
        )
        return try {
            val response = httpRequestRaw("DELETE", url, headers, "")
            response.status in 200..299
        } catch (_: Exception) {
            false
        }
    }

    suspend fun searchAnime(accessToken: String, query: String): List<com.nuvio.app.features.anilist.SearchResult> {
        val encodedQuery = query.replace(" ", "%20")
        val url = "$API_URL/anime?q=$encodedQuery&limit=15&fields=main_picture,media_type,start_date,status,mean,synopsis"
        val headers = mapOf(
            "Authorization" to "Bearer $accessToken",
            "Accept" to "application/json"
        )
        return try {
            val response = httpRequestRaw("GET", url, headers, "")
            val jsonResponse = json.parseToJsonElement(response.body).jsonObject
            val dataArray = jsonResponse["data"] as? kotlinx.serialization.json.JsonArray ?: return emptyList()
            dataArray.mapNotNull {
                val node = it.jsonObject["node"]?.jsonObject ?: return@mapNotNull null
                val id = node["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                val title = node["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown Title"
                val imageUrl = node["main_picture"]?.jsonObject?.get("large")?.jsonPrimitive?.contentOrNull ?: node["main_picture"]?.jsonObject?.get("medium")?.jsonPrimitive?.contentOrNull
                
                com.nuvio.app.features.anilist.SearchResult(
                    id = id,
                    title = title,
                    imageUrl = imageUrl,
                    type = node["media_type"]?.jsonPrimitive?.contentOrNull,
                    startDate = node["start_date"]?.jsonPrimitive?.contentOrNull,
                    status = node["status"]?.jsonPrimitive?.contentOrNull,
                    score = node["mean"]?.jsonPrimitive?.doubleOrNull,
                    description = node["synopsis"]?.jsonPrimitive?.contentOrNull
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

data class MalListEntry(
    val status: String?,
    val score: Double?,
    val progress: Int?,
    val startDate: String? = null,
    val finishDate: String? = null,
    val numTimesRewatched: Int? = null,
    val comments: String? = null,
    val priority: Int? = null,
    val rewatchValue: Int? = null,
    val maxEpisodes: Int? = null,
    val title: String? = null,
    val imageUrl: String? = null
)

@Serializable
internal data class MalTokenResponse(
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)
