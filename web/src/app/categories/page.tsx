"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, addDoc, doc, updateDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Plus, Trash2, Edit, FolderPlus, BookPlus, RefreshCw } from "lucide-react";

interface Category {
  id: string;
  name: string;
  iconUrl?: string;
  order?: number;
}

interface Book {
  id: string;
  categoryId: string;
  title: string;
  coverUrl?: string;
  author?: string;
}

export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);

  // Modals state
  const [showCatModal, setShowCatModal] = useState(false);
  const [catName, setCatName] = useState("");
  const [catIcon, setCatIcon] = useState("");

  const [showBookModal, setShowBookModal] = useState(false);
  const [selectedCatId, setSelectedCatId] = useState("");
  const [bookTitle, setBookTitle] = useState("");
  const [bookCover, setBookCover] = useState("");

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const catSnap = await getDocs(collection(db, "categories"));
      const bookSnap = await getDocs(collection(db, "books"));

      const catsList = catSnap.docs.map(doc => ({ id: doc.id, ...doc.data() })) as Category[];
      const booksList = bookSnap.docs.map(doc => ({ id: doc.id, ...doc.data() })) as Book[];

      setCategories(catsList);
      setBooks(booksList);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleAddCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!catName.trim()) return;

    try {
      await addDoc(collection(db, "categories"), {
        name: catName,
        iconUrl: catIcon || "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=200",
        createdAt: new Date().toISOString(),
      });
      setCatName("");
      setCatIcon("");
      setShowCatModal(false);
      fetchData();
    } catch (err) {
      console.error("Error adding category:", err);
    }
  };

  const handleAddBook = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bookTitle.trim() || !selectedCatId) return;

    try {
      await addDoc(collection(db, "books"), {
        categoryId: selectedCatId,
        title: bookTitle,
        coverUrl: bookCover || "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=200",
        createdAt: new Date().toISOString(),
      });
      setBookTitle("");
      setBookCover("");
      setShowBookModal(false);
      fetchData();
    } catch (err) {
      console.error("Error adding book:", err);
    }
  };

  const handleDeleteCategory = async (id: string) => {
    if (confirm("Are you sure you want to delete this category?")) {
      await deleteDoc(doc(db, "categories", id));
      fetchData();
    }
  };

  const handleDeleteBook = async (id: string) => {
    if (confirm("Are you sure you want to delete this book?")) {
      await deleteDoc(doc(db, "books", id));
      fetchData();
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Categories & Books</h1>
          <p className="text-slate-500 text-sm mt-1">Organize quiz topics and underlying books</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowCatModal(true)}
            className="bg-indigo-600 hover:bg-indigo-500 text-white font-medium px-4 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-2 text-sm"
          >
            <FolderPlus className="w-4 h-4" />
            Add Category
          </button>
          <button
            onClick={() => setShowBookModal(true)}
            className="bg-slate-900 hover:bg-slate-800 text-white font-medium px-4 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-2 text-sm"
          >
            <BookPlus className="w-4 h-4" />
            Add Book
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-12 text-slate-400">
          <RefreshCw className="w-6 h-6 animate-spin mr-2" /> Loading data...
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Categories List */}
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
            <h2 className="text-lg font-bold text-slate-900 flex items-center justify-between">
              <span>Categories ({categories.length})</span>
            </h2>
            <div className="divide-y divide-slate-100">
              {categories.map((cat) => (
                <div key={cat.id} className="py-3 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <img src={cat.iconUrl} alt={cat.name} className="w-10 h-10 rounded-xl object-cover bg-slate-100 border" />
                    <div>
                      <p className="font-semibold text-slate-800">{cat.name}</p>
                      <p className="text-xs text-slate-400">ID: {cat.id}</p>
                    </div>
                  </div>
                  <button
                    onClick={() => handleDeleteCategory(cat.id)}
                    className="p-2 text-rose-500 hover:bg-rose-50 rounded-lg transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
              {categories.length === 0 && (
                <p className="text-slate-400 text-sm py-4 text-center">No categories found.</p>
              )}
            </div>
          </div>

          {/* Books List */}
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
            <h2 className="text-lg font-bold text-slate-900 flex items-center justify-between">
              <span>Books ({books.length})</span>
            </h2>
            <div className="divide-y divide-slate-100">
              {books.map((b) => {
                const cat = categories.find((c) => c.id === b.categoryId);
                return (
                  <div key={b.id} className="py-3 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <img src={b.coverUrl} alt={b.title} className="w-10 h-12 rounded-lg object-cover bg-slate-100 border" />
                      <div>
                        <p className="font-semibold text-slate-800">{b.title}</p>
                        <span className="text-xs bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-md font-medium">
                          {cat?.name || "Uncategorized"}
                        </span>
                      </div>
                    </div>
                    <button
                      onClick={() => handleDeleteBook(b.id)}
                      className="p-2 text-rose-500 hover:bg-rose-50 rounded-lg transition-colors"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                );
              })}
              {books.length === 0 && (
                <p className="text-slate-400 text-sm py-4 text-center">No books found.</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Add Category Modal */}
      {showCatModal && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h3 className="text-xl font-bold text-slate-900">Add New Category</h3>
            <form onSubmit={handleAddCategory} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Category Name</label>
                <input
                  type="text"
                  required
                  value={catName}
                  onChange={(e) => setCatName(e.target.value)}
                  placeholder="e.g. Science & Tech"
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Icon URL (Optional)</label>
                <input
                  type="url"
                  value={catIcon}
                  onChange={(e) => setCatIcon(e.target.value)}
                  placeholder="https://..."
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>
              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCatModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-sm bg-indigo-600 text-white font-medium rounded-xl hover:bg-indigo-500 shadow-md"
                >
                  Save Category
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Book Modal */}
      {showBookModal && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h3 className="text-xl font-bold text-slate-900">Add New Book</h3>
            <form onSubmit={handleAddBook} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Select Category</label>
                <select
                  required
                  value={selectedCatId}
                  onChange={(e) => setSelectedCatId(e.target.value)}
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none bg-white"
                >
                  <option value="">-- Choose Category --</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Book Title</label>
                <input
                  type="text"
                  required
                  value={bookTitle}
                  onChange={(e) => setBookTitle(e.target.value)}
                  placeholder="e.g. General Physics 101"
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Cover Image URL (Optional)</label>
                <input
                  type="url"
                  value={bookCover}
                  onChange={(e) => setBookCover(e.target.value)}
                  placeholder="https://..."
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
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
                  className="px-5 py-2 text-sm bg-slate-900 text-white font-medium rounded-xl hover:bg-slate-800 shadow-md"
                >
                  Save Book
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
