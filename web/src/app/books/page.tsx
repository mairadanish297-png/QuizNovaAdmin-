"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, doc, setDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { BookOpen, FolderTree, Plus, Trash2, Edit, RefreshCw, Layers } from "lucide-react";

interface Category {
  id: string;
  name: string;
}

interface Book {
  id: string;
  title: string;
  coverUrl?: string;
  description?: string;
  order?: number;
}

interface Chapter {
  id: string;
  title: string;
  content_en?: string;
  content_ur?: string;
  order?: number;
}

export default function BooksPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>("");

  const [books, setBooks] = useState<Book[]>([]);
  const [selectedBook, setSelectedBook] = useState<string>("");

  const [chapters, setChapters] = useState<Chapter[]>([]);
  const [loading, setLoading] = useState(false);

  // Book Modal
  const [showBookModal, setShowBookModal] = useState(false);
  const [editingBookId, setEditingBookId] = useState<string | null>(null);
  const [bookTitle, setBookTitle] = useState("");
  const [coverUrl, setCoverUrl] = useState("");
  const [bookDesc, setBookDesc] = useState("");
  const [bookOrder, setBookOrder] = useState(0);

  // Chapter Modal
  const [showChapterModal, setShowChapterModal] = useState(false);
  const [editingChapterId, setEditingChapterId] = useState<string | null>(null);
  const [chapterTitle, setChapterTitle] = useState("");
  const [chapterContentEN, setChapterContentEN] = useState("");
  const [chapterContentUR, setChapterContentUR] = useState("");
  const [chapterOrder, setChapterOrder] = useState(0);

  useEffect(() => {
    fetchCategories();
  }, []);

  useEffect(() => {
    if (selectedCategory) {
      fetchBooks(selectedCategory);
    } else {
      setBooks([]);
      setChapters([]);
    }
  }, [selectedCategory]);

  useEffect(() => {
    if (selectedCategory && selectedBook) {
      fetchChapters(selectedCategory, selectedBook);
    } else {
      setChapters([]);
    }
  }, [selectedBook]);

  const fetchCategories = async () => {
    try {
      const snap = await getDocs(collection(db, "categories"));
      const list = snap.docs.map(d => ({ id: d.id, name: d.data().name || d.id }));
      setCategories(list);
      if (list.length > 0) setSelectedCategory(list[0].name);
    } catch (e) {
      console.error(e);
    }
  };

  const fetchBooks = async (catName: string) => {
    setLoading(true);
    try {
      const snap = await getDocs(collection(db, "questions", catName, "books"));
      const list = snap.docs.map(d => ({ id: d.id, ...d.data() })) as Book[];
      setBooks(list);
      if (list.length > 0) setSelectedBook(list[0].id);
      else setSelectedBook("");
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const fetchChapters = async (catName: string, bookId: string) => {
    try {
      const snap = await getDocs(collection(db, "questions", catName, "books", bookId, "chapters"));
      const list = snap.docs.map(d => ({ id: d.id, ...d.data() })) as Chapter[];
      setChapters(list);
    } catch (e) {
      console.error(e);
    }
  };

  const handleSaveBook = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bookTitle.trim() || !selectedCategory) return;

    try {
      const ref = editingBookId
        ? doc(db, "questions", selectedCategory, "books", editingBookId)
        : doc(collection(db, "questions", selectedCategory, "books"));

      await setDoc(ref, {
        title: bookTitle,
        coverUrl,
        description: bookDesc,
        order: Number(bookOrder),
        updatedAt: new Date().toISOString(),
      }, { merge: true });

      setShowBookModal(false);
      fetchBooks(selectedCategory);
    } catch (e) {
      console.error(e);
    }
  };

  const handleSaveChapter = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!chapterTitle.trim() || !selectedCategory || !selectedBook) return;

    try {
      const ref = editingChapterId
        ? doc(db, "questions", selectedCategory, "books", selectedBook, "chapters", editingChapterId)
        : doc(collection(db, "questions", selectedCategory, "books", selectedBook, "chapters"));

      await setDoc(ref, {
        title: chapterTitle,
        content: chapterContentEN,
        content_en: chapterContentEN,
        content_ur: chapterContentUR,
        order: Number(chapterOrder),
        updatedAt: new Date().toISOString(),
      }, { merge: true });

      setShowChapterModal(false);
      fetchChapters(selectedCategory, selectedBook);
    } catch (e) {
      console.error(e);
    }
  };

  const handleDeleteBook = async (bookId: string) => {
    if (confirm("Delete this book and all its chapters?")) {
      await deleteDoc(doc(db, "questions", selectedCategory, "books", bookId));
      fetchBooks(selectedCategory);
    }
  };

  const handleDeleteChapter = async (chapterId: string) => {
    if (confirm("Delete this chapter?")) {
      await deleteDoc(doc(db, "questions", selectedCategory, "books", selectedBook, "chapters", chapterId));
      fetchChapters(selectedCategory, selectedBook);
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Books & Chapters Manager</h1>
          <p className="text-slate-500 text-sm mt-1">Organize textbooks and study chapters per category</p>
        </div>
      </div>

      {/* Category Selector */}
      <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-sm flex items-center gap-4">
        <FolderTree className="w-5 h-5 text-indigo-600 flex-shrink-0" />
        <span className="text-sm font-semibold text-slate-700">Select Category:</span>
        <select
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value)}
          className="flex-1 border border-slate-200 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white font-medium"
        >
          {categories.map((c) => (
            <option key={c.id} value={c.name}>{c.name}</option>
          ))}
        </select>
      </div>

      {/* Grid: Books & Chapters */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Books List */}
        <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex items-center justify-between border-b pb-3">
            <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
              <BookOpen className="w-5 h-5 text-indigo-600" />
              <span>Books ({books.length})</span>
            </h2>
            <button
              onClick={() => {
                setEditingBookId(null);
                setBookTitle("");
                setCoverUrl("");
                setBookDesc("");
                setBookOrder(books.length + 1);
                setShowBookModal(true);
              }}
              className="bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold px-3 py-2 rounded-xl flex items-center gap-1"
            >
              <Plus className="w-4 h-4" /> Add Book
            </button>
          </div>

          <div className="space-y-3">
            {books.map((b) => (
              <div
                key={b.id}
                onClick={() => setSelectedBook(b.id)}
                className={`p-4 rounded-xl border transition-all cursor-pointer flex items-center justify-between ${
                  selectedBook === b.id
                    ? "bg-indigo-50/60 border-indigo-500 shadow-sm"
                    : "border-slate-200 hover:bg-slate-50"
                }`}
              >
                <div className="flex items-center gap-3">
                  {b.coverUrl ? (
                    <img src={b.coverUrl} alt={b.title} className="w-10 h-12 rounded-lg object-cover border" />
                  ) : (
                    <div className="w-10 h-12 bg-indigo-100 text-indigo-700 rounded-lg flex items-center justify-center font-bold text-sm">
                      📖
                    </div>
                  )}
                  <div>
                    <h4 className="font-bold text-slate-900 text-sm">{b.title}</h4>
                    <p className="text-xs text-slate-400">Order: {b.order ?? 0}</p>
                  </div>
                </div>

                <div className="flex items-center gap-1">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setEditingBookId(b.id);
                      setBookTitle(b.title);
                      setCoverUrl(b.coverUrl || "");
                      setBookDesc(b.description || "");
                      setBookOrder(b.order || 0);
                      setShowBookModal(true);
                    }}
                    className="p-1.5 text-slate-400 hover:text-indigo-600"
                  >
                    <Edit className="w-4 h-4" />
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeleteBook(b.id);
                    }}
                    className="p-1.5 text-slate-400 hover:text-rose-600"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}

            {books.length === 0 && (
              <p className="text-center py-8 text-sm text-slate-400">No books under this category.</p>
            )}
          </div>
        </div>

        {/* Chapters List */}
        <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex items-center justify-between border-b pb-3">
            <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
              <Layers className="w-5 h-5 text-indigo-600" />
              <span>Chapters ({chapters.length})</span>
            </h2>
            <button
              disabled={!selectedBook}
              onClick={() => {
                setEditingChapterId(null);
                setChapterTitle("");
                setChapterContentEN("");
                setChapterContentUR("");
                setChapterOrder(chapters.length + 1);
                setShowChapterModal(true);
              }}
              className="bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold px-3 py-2 rounded-xl flex items-center gap-1 disabled:opacity-50"
            >
              <Plus className="w-4 h-4" /> Add Chapter
            </button>
          </div>

          <div className="space-y-3">
            {chapters.map((ch) => (
              <div
                key={ch.id}
                className="p-4 rounded-xl border border-slate-200 bg-slate-50/50 flex items-center justify-between"
              >
                <div>
                  <h4 className="font-bold text-slate-900 text-sm">{ch.title}</h4>
                  <p className="text-xs text-slate-400">Order: {ch.order ?? 0}</p>
                </div>

                <div className="flex items-center gap-1">
                  <button
                    onClick={() => {
                      setEditingChapterId(ch.id);
                      setChapterTitle(ch.title);
                      setChapterContentEN(ch.content_en || "");
                      setChapterContentUR(ch.content_ur || "");
                      setChapterOrder(ch.order || 0);
                      setShowChapterModal(true);
                    }}
                    className="p-1.5 text-slate-400 hover:text-indigo-600"
                  >
                    <Edit className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleDeleteChapter(ch.id)}
                    className="p-1.5 text-slate-400 hover:text-rose-600"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}

            {chapters.length === 0 && (
              <p className="text-center py-8 text-sm text-slate-400">
                {selectedBook ? "No chapters under this book." : "Select a book to view chapters."}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Book Modal */}
      {showBookModal && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h3 className="text-xl font-bold text-slate-900">
              {editingBookId ? "Edit Book" : "Add Book"}
            </h3>
            <form onSubmit={handleSaveBook} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Book Title</label>
                <input
                  type="text"
                  required
                  value={bookTitle}
                  onChange={(e) => setBookTitle(e.target.value)}
                  placeholder="e.g. Physics Volume 1"
                  className="w-full border rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Cover Image URL</label>
                <input
                  type="url"
                  value={coverUrl}
                  onChange={(e) => setCoverUrl(e.target.value)}
                  placeholder="https://..."
                  className="w-full border rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>
              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowBookModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-sm bg-indigo-600 text-white font-medium rounded-xl shadow-md"
                >
                  Save Book
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Chapter Modal */}
      {showChapterModal && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-lg w-full shadow-2xl space-y-4">
            <h3 className="text-xl font-bold text-slate-900">
              {editingChapterId ? "Edit Chapter" : "Add Chapter"}
            </h3>
            <form onSubmit={handleSaveChapter} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Chapter Title</label>
                <input
                  type="text"
                  required
                  value={chapterTitle}
                  onChange={(e) => setChapterTitle(e.target.value)}
                  placeholder="e.g. Chapter 1: Newton's Laws"
                  className="w-full border rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Content (English)</label>
                <textarea
                  rows={3}
                  value={chapterContentEN}
                  onChange={(e) => setChapterContentEN(e.target.value)}
                  placeholder="English summary/text..."
                  className="w-full border rounded-xl p-3 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Content (Urdu)</label>
                <textarea
                  rows={3}
                  value={chapterContentUR}
                  onChange={(e) => setChapterContentUR(e.target.value)}
                  placeholder="اردو مواد..."
                  className="w-full border rounded-xl p-3 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none text-right font-serif"
                />
              </div>
              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowChapterModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-sm bg-slate-900 text-white font-medium rounded-xl shadow-md"
                >
                  Save Chapter
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
