package com.quiznova.admin.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.quiznova.admin.models.BookData
import com.quiznova.admin.models.ChapterData

object BookHelper {

    private val db = FirebaseFirestore.getInstance()

    // ═══════════════════════════════════
    //  BOOKS
    // ═══════════════════════════════════

    fun getBooks(categoryName: String, callback: (List<BookData>) -> Unit) {
        db.collection("questions")
            .document(categoryName)
            .collection("books")
            .orderBy("order")
            .get()
            .addOnSuccessListener { snapshot ->
                val books = snapshot.documents.map { doc ->
                    BookData(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        coverUrl = doc.getString("coverUrl") ?: "",
                        description = doc.getString("description") ?: "",
                        order = (doc.getLong("order") ?: 0).toInt(),
                        chapterCount = 0
                    )
                }
                callback(books)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun saveBook(
        categoryName: String,
        bookId: String?,
        title: String,
        coverUrl: String,
        description: String,
        order: Int,
        callback: (Boolean, String?) -> Unit  // success, newDocId
    ) {
        val data = hashMapOf(
            "title" to title,
            "coverUrl" to coverUrl,
            "description" to description,
            "order" to order,
            "updatedAt" to Timestamp.now()
        )

        if (bookId.isNullOrEmpty()) {
            // New book
            data["createdAt"] = Timestamp.now()
            db.collection("questions")
                .document(categoryName)
                .collection("books")
                .add(data)
                .addOnSuccessListener { docRef ->
                    callback(true, docRef.id)
                }
                .addOnFailureListener { e ->
                    callback(false, null)
                }
        } else {
            // Update existing
            db.collection("questions")
                .document(categoryName)
                .collection("books")
                .document(bookId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    callback(true, bookId)
                }
                .addOnFailureListener { e ->
                    callback(false, null)
                }
        }
    }

    fun deleteBook(categoryName: String, bookId: String, callback: (Boolean) -> Unit) {
        // Delete all chapters first
        db.collection("questions")
            .document(categoryName)
            .collection("books")
            .document(bookId)
            .collection("chapters")
            .get()
            .addOnSuccessListener { chapters ->
                val batch = db.batch()
                for (ch in chapters.documents) {
                    batch.delete(ch.reference)
                }
                // Delete the book itself
                batch.delete(
                    db.collection("questions")
                        .document(categoryName)
                        .collection("books")
                        .document(bookId)
                )
                batch.commit()
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { callback(false) }
            }
            .addOnFailureListener { callback(false) }
    }

    // ═══════════════════════════════════
    //  CHAPTERS
    // ═══════════════════════════════════

    fun getChapters(
        categoryName: String,
        bookId: String,
        callback: (List<ChapterData>) -> Unit
    ) {
        db.collection("questions")
            .document(categoryName)
            .collection("books")
            .document(bookId)
            .collection("chapters")
            .orderBy("order")
            .get()
            .addOnSuccessListener { snapshot ->
                val chapters = snapshot.documents.map { doc ->
                    ChapterData(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        content_en = doc.getString("content_en") ?: doc.getString("content") ?: "",
                        content_ur = doc.getString("content_ur") ?: "",
                        order = (doc.getLong("order") ?: 0).toInt()
                    )
                }
                callback(chapters)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun saveChapter(
        categoryName: String,
        bookId: String,
        chapterId: String?,
        title: String,
        contentEN: String,
        contentUR: String,
        order: Int,
        callback: (Boolean) -> Unit
    ) {
        val data = hashMapOf(
            "title" to title,
            "content" to contentEN,
            "content_en" to contentEN,
            "content_ur" to contentUR,
            "order" to order,
            "updatedAt" to Timestamp.now()
        )

        if (chapterId.isNullOrEmpty()) {
            data["createdAt"] = Timestamp.now()
            db.collection("questions")
                .document(categoryName)
                .collection("books")
                .document(bookId)
                .collection("chapters")
                .add(data)
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { callback(false) }
        } else {
            db.collection("questions")
                .document(categoryName)
                .collection("books")
                .document(bookId)
                .collection("chapters")
                .document(chapterId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { callback(false) }
        }
    }

    fun deleteChapter(
        categoryName: String,
        bookId: String,
        chapterId: String,
        callback: (Boolean) -> Unit
    ) {
        db.collection("questions")
            .document(categoryName)
            .collection("books")
            .document(bookId)
            .collection("chapters")
            .document(chapterId)
            .delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}