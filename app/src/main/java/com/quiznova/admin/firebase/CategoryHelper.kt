package com.quiznova.admin.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.models.CategoryData

object CategoryHelper {

    private val db = FirebaseFirestore.getInstance()
    private var cachedNames: List<String>? = null
    private var cachedCategories: List<CategoryData>? = null

    fun clearCache() {
        cachedNames = null
        cachedCategories = null
    }

    // ═══ Existing method -- kept for backward compatibility ═══
    fun getCategoryNames(callback: (List<String>) -> Unit) {
        cachedNames?.let {
            callback(it)
            return
        }

        db.collection("categories")
            .get()
            .addOnSuccessListener { snapshot ->
                val names = snapshot.documents.mapNotNull { doc ->
                    doc.getString("name") ?: doc.id
                }
                cachedNames = names
                callback(names)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    // ═══ New method -- returns full CategoryData with imageUrl ═══
    fun getCategoriesWithDetails(callback: (List<CategoryData>) -> Unit) {
        cachedCategories?.let {
            callback(it)
            return
        }

        db.collection("categories")
            .get()
            .addOnSuccessListener { snapshot ->
                val categories = snapshot.documents.map { doc ->
                    val img = doc.getString("imageUrl") ?: ""
                    val ic = doc.getString("icon") ?: ""
                    val type = doc.getString("type") ?: "text"
                    val qType = doc.getString("quizType") ?: "${type} quiz"
                    val emoji = doc.getString("emoji") ?: doc.getString("categoryEmoji") ?: "📝"
                    val color = doc.getString("color") ?: doc.getString("categoryColor") ?: "#6200EE"

                    CategoryData(
                        id = doc.id,
                        name = doc.getString("name") ?: doc.id,
                        imageUrl = img,
                        icon = ic.ifEmpty { img },
                        description = doc.getString("description") ?: "",
                        questionCount = 0,
                        color = color,
                        emoji = emoji,
                        type = type,
                        quizType = qType,
                        categoryEmoji = emoji,
                        categoryColor = color
                    )
                }
                cachedCategories = categories
                callback(categories)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    // ═══ Save / Update category ═══
    fun saveCategory(
        name: String,
        imageUrl: String,
        description: String,
        color: String = "#6200EE",
        emoji: String = "📝",
        type: String = "text",
        callback: (Boolean) -> Unit
    ) {
        val normalizedType = type.lowercase().trim().replace(" quiz", "").replace("_quiz", "")
        val (quizType, mainId) = when (normalizedType) {
            "image", "2" -> "image quiz" to 2
            "audio", "3" -> "audio quiz" to 3
            "video", "4" -> "video quiz" to 4
            else -> "text quiz" to 1
        }

        val mainIdStr = mainId.toString()
        val mediaType = when (mainId) {
            2 -> "image"
            3 -> "audio"
            4 -> "video"
            else -> "text"
        }

        val data = hashMapOf(
            "name" to name,
            "imageUrl" to imageUrl,
            "icon" to imageUrl,
            "description" to description,
            "color" to color,
            "categoryColor" to color,
            "emoji" to emoji,
            "categoryEmoji" to emoji,
            "type" to quizType,
            "quizType" to quizType,
            "category_type" to quizType,
            "quiz_type" to mediaType,
            "mediaType" to mediaType,
            "main_id" to mainId,
            "mainId" to mainId,
            "type_id" to mainIdStr,
            "main_id_str" to mainIdStr,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("categories")
            .document(name)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                clearCache()
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    // ═══ Auto-fix/Migrate existing categories in Firestore ═══
    fun fixExistingCategories(callback: ((Int) -> Unit)? = null) {
        db.collection("categories").get()
            .addOnSuccessListener { snapshot ->
                var updatedCount = 0
                val totalDocs = snapshot.documents.size
                if (totalDocs == 0) {
                    callback?.invoke(0)
                    return@addOnSuccessListener
                }

                var processed = 0
                for (doc in snapshot.documents) {
                    val rawType = doc.getString("type")
                        ?: doc.getString("mediaType")
                        ?: doc.getString("quizType")
                        ?: doc.getString("category_type")
                        ?: doc.get("main_id")?.toString()
                        ?: "text"

                    val normalized = rawType.lowercase().trim().replace(" quiz", "").replace("_quiz", "")
                    val (quizType, mainId) = when (normalized) {
                        "image", "2" -> "image quiz" to 2
                        "audio", "3" -> "audio quiz" to 3
                        "video", "4" -> "video quiz" to 4
                        else -> "text quiz" to 1
                    }

                    val mainIdStr = mainId.toString()
                    val mediaType = when (mainId) {
                        2 -> "image"
                        3 -> "audio"
                        4 -> "video"
                        else -> "text"
                    }

                    // Update if missing proper schema fields
                    val updateMap = hashMapOf<String, Any>(
                        "type" to quizType,
                        "quizType" to quizType,
                        "category_type" to quizType,
                        "quiz_type" to mediaType,
                        "mediaType" to mediaType,
                        "main_id" to mainId,
                        "mainId" to mainId,
                        "type_id" to mainIdStr,
                        "main_id_str" to mainIdStr
                    )

                    doc.reference.set(updateMap, com.google.firebase.firestore.SetOptions.merge())
                        .addOnCompleteListener {
                            processed++
                            updatedCount++
                            if (processed >= totalDocs) {
                                clearCache()
                                callback?.invoke(updatedCount)
                            }
                        }
                }
            }
            .addOnFailureListener {
                callback?.invoke(0)
            }
    }

    // ═══ Delete category ═══
    fun deleteCategory(id: String, callback: (Boolean) -> Unit) {
        db.collection("categories")
            .document(id)
            .delete()
            .addOnSuccessListener {
                clearCache()
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }
}