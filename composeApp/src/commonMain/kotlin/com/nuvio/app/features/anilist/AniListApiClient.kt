package com.nuvio.app.features.anilist

import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.contentOrNull

internal object AniListApiClient {
    private const val API_URL = "https://graphql.anilist.co"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun query(accessToken: String?, graphqlQuery: String, variables: JsonObject? = null): String? {
        val bodyObj = buildJsonObject {
            put("query", JsonPrimitive(graphqlQuery))
            if (variables != null) {
                put("variables", variables)
            }
        }
        val body = json.encodeToString(JsonObject.serializer(), bodyObj)
        
        val headers = mutableMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "Cache-Control" to "no-cache"
        )
        if (!accessToken.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $accessToken"
        }

        return try {
            httpPostJsonWithHeaders(API_URL, body, headers)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchViewer(accessToken: String): AniListViewer? {
        val query = """
            query {
                Viewer {
                    id
                    name
                    mediaListOptions {
                        scoreFormat
                        animeList {
                            advancedScoringEnabled
                        }
                    }
                }
            }
        """.trimIndent()
        
        val response = query(accessToken, query) ?: return null
        return try {
            json.decodeFromString<AniListViewerResponse>(response).data?.viewer
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchMalId(mediaId: Int): Int? {
        val query = """
            query (${'$'}mediaId: Int) {
              Media (id: ${'$'}mediaId) {
                idMal
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("mediaId", JsonPrimitive(mediaId))
        }

        val response = query(null, query, variables) ?: return null
        return try {
            val jsonResponse = json.parseToJsonElement(response).jsonObject
            val dataElement = jsonResponse["data"]
            val data = if (dataElement != null && dataElement !is kotlinx.serialization.json.JsonNull) dataElement.jsonObject else null
            val mediaElement = data?.get("Media")
            val media = if (mediaElement != null && mediaElement !is kotlinx.serialization.json.JsonNull) mediaElement.jsonObject else return null
            media["idMal"]?.jsonPrimitive?.intOrNull
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveProgress(
        accessToken: String,
        mediaId: Int,
        progress: Int,
        status: String? = null,
        score: Double? = null,
        repeat: Int? = null,
        private: Boolean? = null,
        notes: String? = null,
        hiddenFromStatusLists: Boolean? = null,
        startedAt: FuzzyDate? = null,
        completedAt: FuzzyDate? = null,
        clearStartDate: Boolean = false,
        clearFinishDate: Boolean = false,
        advancedScores: List<Double>? = null,
    ): Boolean {
        val mutation = """
            mutation (
                ${'$'}mediaId: Int, 
                ${'$'}progress: Int, 
                ${'$'}status: MediaListStatus, 
                ${'$'}score: Float,
                ${'$'}repeat: Int,
                ${'$'}private: Boolean,
                ${'$'}notes: String,
                ${'$'}hiddenFromStatusLists: Boolean,
                ${'$'}startedAt: FuzzyDateInput,
                ${'$'}completedAt: FuzzyDateInput,
                ${'$'}advancedScores: [Float]
            ) {
              SaveMediaListEntry (
                  mediaId: ${'$'}mediaId, 
                  progress: ${'$'}progress, 
                  status: ${'$'}status, 
                  score: ${'$'}score,
                  repeat: ${'$'}repeat,
                  private: ${'$'}private,
                  notes: ${'$'}notes,
                  hiddenFromStatusLists: ${'$'}hiddenFromStatusLists,
                  startedAt: ${'$'}startedAt,
                  completedAt: ${'$'}completedAt,
                  advancedScores: ${'$'}advancedScores
              ) {
                id
                status
                progress
                score
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("mediaId", JsonPrimitive(mediaId))
            put("progress", JsonPrimitive(progress))
            if (status != null) put("status", JsonPrimitive(status))
            if (score != null) put("score", JsonPrimitive(score))
            if (repeat != null) put("repeat", JsonPrimitive(repeat))
            if (private != null) put("private", JsonPrimitive(private))
            if (notes != null) put("notes", JsonPrimitive(notes))
            if (hiddenFromStatusLists != null) put("hiddenFromStatusLists", JsonPrimitive(hiddenFromStatusLists))
            if (startedAt != null) {
                put("startedAt", buildJsonObject {
                    startedAt.year?.let { put("year", JsonPrimitive(it)) }
                    startedAt.month?.let { put("month", JsonPrimitive(it)) }
                    startedAt.day?.let { put("day", JsonPrimitive(it)) }
                })
            } else if (clearStartDate) {
                put("startedAt", buildJsonObject {
                    put("year", kotlinx.serialization.json.JsonNull)
                    put("month", kotlinx.serialization.json.JsonNull)
                    put("day", kotlinx.serialization.json.JsonNull)
                })
            }
            if (completedAt != null) {
                put("completedAt", buildJsonObject {
                    completedAt.year?.let { put("year", JsonPrimitive(it)) }
                    completedAt.month?.let { put("month", JsonPrimitive(it)) }
                    completedAt.day?.let { put("day", JsonPrimitive(it)) }
                })
            } else if (clearFinishDate) {
                put("completedAt", buildJsonObject {
                    put("year", kotlinx.serialization.json.JsonNull)
                    put("month", kotlinx.serialization.json.JsonNull)
                    put("day", kotlinx.serialization.json.JsonNull)
                })
            }
            if (advancedScores != null) {
                if (advancedScores.all { it == 0.0 }) {
                    put("advancedScores", kotlinx.serialization.json.JsonNull)
                } else {
                    put("advancedScores", kotlinx.serialization.json.JsonArray(advancedScores.map { if (it == 0.0) kotlinx.serialization.json.JsonPrimitive(0) else kotlinx.serialization.json.JsonPrimitive(it) }))
                }
            }
        }

        val response = query(accessToken, mutation, variables)
        if (response == null) {
            // Retry without advanced scores if it failed
            if (advancedScores != null) {
                return saveProgress(
                    accessToken, mediaId, progress, status, score, repeat, private,
                    notes, hiddenFromStatusLists, startedAt, completedAt,
                    clearStartDate, clearFinishDate, null
                )
            }
            return false
        }
        
        return try {
            val jsonResponse = json.parseToJsonElement(response).jsonObject
            val isSuccess = jsonResponse["data"] != null
            if (!isSuccess && advancedScores != null) {
                return saveProgress(
                    accessToken, mediaId, progress, status, score, repeat, private,
                    notes, hiddenFromStatusLists, startedAt, completedAt,
                    clearStartDate, clearFinishDate, null
                )
            }
            isSuccess
        } catch (_: Exception) {
            if (advancedScores != null) {
                return saveProgress(
                    accessToken, mediaId, progress, status, score, repeat, private,
                    notes, hiddenFromStatusLists, startedAt, completedAt,
                    clearStartDate, clearFinishDate, null
                )
            }
            false
        }
    }

    suspend fun deleteEntry(accessToken: String, id: Int): Boolean {
        val mutation = """
            mutation (${'$'}id: Int) {
              DeleteMediaListEntry (id: ${'$'}id) {
                deleted
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("id", JsonPrimitive(id))
        }

        val response = query(accessToken, mutation, variables) ?: return false
        return try {
            val jsonResponse = json.parseToJsonElement(response).jsonObject
            jsonResponse["data"] != null
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchListEntry(
        accessToken: String,
        mediaId: Int,
    ): AniListEntry? {
        val query = """
            query (${'$'}mediaId: Int) {
              Media (id: ${'$'}mediaId) {
                id
                episodes
                title {
                  romaji
                  english
                }
                coverImage {
                  large
                }
                isFavourite
                mediaListEntry {
                  id
                  status
                  score
                  progress
                  repeat
                  notes
                  private
                  hiddenFromStatusLists
                  startedAt { year month day }
                  completedAt { year month day }
                  advancedScores
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("mediaId", JsonPrimitive(mediaId))
        }

        val response = query(accessToken, query, variables) ?: return null
        return try {
            val jsonResponse = json.parseToJsonElement(response).jsonObject
            val dataElement = jsonResponse["data"]
            val data = if (dataElement != null && dataElement !is kotlinx.serialization.json.JsonNull) dataElement.jsonObject else null
            val mediaElement = data?.get("Media")
            val media = if (mediaElement != null && mediaElement !is kotlinx.serialization.json.JsonNull) mediaElement.jsonObject else return null
            val mediaListElement = media["mediaListEntry"]
            val mediaList = if (mediaListElement != null && mediaListElement !is kotlinx.serialization.json.JsonNull) mediaListElement.jsonObject else null
            
            fun parseDate(obj: JsonObject?): FuzzyDate? {
                if (obj == null) return null
                val year = obj["year"]?.jsonPrimitive?.intOrNull
                val month = obj["month"]?.jsonPrimitive?.intOrNull
                val day = obj["day"]?.jsonPrimitive?.intOrNull
                if (year == null && month == null && day == null) return null
                return FuzzyDate(year, month, day)
            }

            AniListEntry(
                id = mediaList?.get("id")?.jsonPrimitive?.intOrNull,
                status = mediaList?.get("status")?.jsonPrimitive?.contentOrNull,
                score = mediaList?.get("score")?.jsonPrimitive?.doubleOrNull,
                progress = mediaList?.get("progress")?.jsonPrimitive?.intOrNull ?: 0,
                repeat = mediaList?.get("repeat")?.jsonPrimitive?.intOrNull,
                notes = mediaList?.get("notes")?.jsonPrimitive?.contentOrNull,
                private = mediaList?.get("private")?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull(),
                hiddenFromStatusLists = mediaList?.get("hiddenFromStatusLists")?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull(),
                startedAt = parseDate(mediaList?.get("startedAt")?.let { if (it is kotlinx.serialization.json.JsonNull) null else it.jsonObject }),
                completedAt = parseDate(mediaList?.get("completedAt")?.let { if (it is kotlinx.serialization.json.JsonNull) null else it.jsonObject }),
                advancedScores = mediaList?.get("advancedScores")?.let { it as? kotlinx.serialization.json.JsonObject }?.values?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull } 
                    ?: mediaList?.get("advancedScores")?.let { it as? kotlinx.serialization.json.JsonArray }?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull },
                title = media["title"]?.let { if (it is kotlinx.serialization.json.JsonNull) null else it.jsonObject }?.get("english")?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.contentOrNull 
                    ?: media["title"]?.let { if (it is kotlinx.serialization.json.JsonNull) null else it.jsonObject }?.get("romaji")?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.contentOrNull,
                imageUrl = media["coverImage"]?.let { if (it is kotlinx.serialization.json.JsonNull) null else it.jsonObject }?.get("large")?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.contentOrNull,
                maxEpisodes = media["episodes"]?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.intOrNull,
                isFavourite = media["isFavourite"]?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.contentOrNull?.toBooleanStrictOrNull() ?: false
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun toggleFavourite(accessToken: String, animeId: Int): Boolean {
        val mutation = """
            mutation (${'$'}animeId: Int) {
              ToggleFavourite(animeId: ${'$'}animeId) {
                anime {
                  pageInfo {
                    total
                  }
                }
              }
            }
        """.trimIndent()

        val variables = kotlinx.serialization.json.buildJsonObject {
            put("animeId", kotlinx.serialization.json.JsonPrimitive(animeId))
        }

        val response = query(accessToken, mutation, variables) ?: return false
        return try {
            val jsonResponse = kotlinx.serialization.json.Json.parseToJsonElement(response).jsonObject
            jsonResponse["data"] != null
        } catch (_: Exception) {
            false
        }
    }

    suspend fun searchAnime(accessToken: String?, queryStr: String): List<SearchResult> {
        val graphqlQuery = """
            query (${'$'}search: String) {
              Page(page: 1, perPage: 15) {
                media(search: ${'$'}search, type: ANIME) {
                  id
                  title {
                    romaji
                    english
                  }
                  coverImage { large }
                  format
                  status
                  startDate { year month day }
                  averageScore
                  description
                }
              }
            }
        """.trimIndent()
        
        val variables = buildJsonObject {
            put("search", JsonPrimitive(queryStr))
        }

        val response = query(accessToken, graphqlQuery, variables) ?: return emptyList()
        return try {
            val jsonResponse = json.parseToJsonElement(response).jsonObject
            val mediaArray = jsonResponse["data"]?.jsonObject?.get("Page")?.jsonObject?.get("media") as? kotlinx.serialization.json.JsonArray ?: return emptyList()
            mediaArray.mapNotNull {
                val obj = it.jsonObject
                val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                val titleObj = obj["title"]?.jsonObject
                val english = titleObj?.get("english")?.jsonPrimitive?.contentOrNull
                val romaji = titleObj?.get("romaji")?.jsonPrimitive?.contentOrNull
                val titleStr = english ?: romaji ?: "Unknown Title"
                val imageUrl = obj["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.contentOrNull
                
                val startDateObj = obj["startDate"]?.jsonObject
                val year = startDateObj?.get("year")?.jsonPrimitive?.intOrNull
                val month = startDateObj?.get("month")?.jsonPrimitive?.intOrNull
                val day = startDateObj?.get("day")?.jsonPrimitive?.intOrNull
                val startDate = if (year != null) "$year-${month?.toString()?.padStart(2, '0') ?: "01"}-${day?.toString()?.padStart(2, '0') ?: "01"}" else null

                val averageScore = obj["averageScore"]?.jsonPrimitive?.intOrNull?.toDouble()
                
                SearchResult(
                    id = id,
                    title = titleStr,
                    imageUrl = imageUrl,
                    type = obj["format"]?.jsonPrimitive?.contentOrNull,
                    startDate = startDate,
                    status = obj["status"]?.jsonPrimitive?.contentOrNull,
                    score = averageScore,
                    description = obj["description"]?.jsonPrimitive?.contentOrNull
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

data class FuzzyDate(
    val year: Int?,
    val month: Int?,
    val day: Int?
)

data class AniListEntry(
    val id: Int?,
    val status: String?,
    val score: Double?,
    val progress: Int?,
    val repeat: Int? = null,
    val notes: String? = null,
    val private: Boolean? = null,
    val hiddenFromStatusLists: Boolean? = null,
    val startedAt: FuzzyDate? = null,
    val completedAt: FuzzyDate? = null,
    val advancedScores: List<Double>? = null,
    val maxEpisodes: Int? = null,
    val title: String? = null,
    val imageUrl: String? = null,
    val isFavourite: Boolean = false
)

data class SearchResult(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val type: String?,
    val startDate: String?,
    val status: String?,
    val score: Double?,
    val description: String?
)
