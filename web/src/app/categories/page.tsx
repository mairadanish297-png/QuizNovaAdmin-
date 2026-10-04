"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, doc, setDoc, deleteDoc, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Plus, Trash2, Edit, FolderPlus, Wrench, RefreshCw, Image as ImageIcon, Sparkles } from "lucide-react";

interface Category {
  id: string;
  name: string;
  imageUrl?: string;
  icon?: string;
  description?: string;
  color?: string;
  categoryColor?: string;
  emoji?: string;
  categoryEmoji?: string;
  type?: string;
  quizType?: string;
  mediaType?: string;
}

export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [fixing, setFixing] = useState(false);

  // Add / Edit Category Modal
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [description, setDescription] = useState("");
  const [color, setColor] = useState("#6200EE");
  const [emoji, setEmoji] = useState("📝");
  const [mediaType, setMediaType] = useState("text"); // text, image, audio, video

  useEffect(() => {
    fetchCategories();
  }, []);

  const fetchCategories = async () => {
    setLoading(true);
    try {
      const snap = await getDocs(collection(db, "categories"));
      const list = snap.docs.map(doc => {
        const data = doc.data();
        return {
          id: doc.id,
          name: data.name || doc.id,
          imageUrl: data.imageUrl || data.icon || "",
          icon: data.icon || data.imageUrl || "",
          description: data.description || "",
          color: data.color || data.categoryColor || "#6200EE",
          emoji: data.emoji || data.categoryEmoji || "📝",
          type: data.type || data.quizType || "text quiz",
          quizType: data.quizType || data.type || "text quiz",
          mediaType: data.mediaType || "text",
        } as Category;
      });
      setCategories(list);
    } catch (err) {
      console.error("Error fetching categories:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenAdd = () => {
    setEditingId(null);
    setName("");
    setImageUrl("");
    setDescription("");
    setColor("#6200EE");
    setEmoji("📝");
    setMediaType("text");
    setShowModal(true);
  };

  const handleOpenEdit = (cat: Category) => {
    setEditingId(cat.id);
    setName(cat.name);
    setImageUrl(cat.imageUrl || "");
    setDescription(cat.description || "");
    setColor(cat.color || "#6200EE");
    setEmoji(cat.emoji || "📝");
    setMediaType(cat.mediaType || "text");
    setShowModal(true);
  };

  const handleSaveCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    const quizType = `${mediaType} quiz`;
    const mainId = mediaType === "image" ? 2 : mediaType === "audio" ? 3 : mediaType === "video" ? 4 : 1;
    const mainIdStr = mainId.toString();

    const catData = {
      name,
      imageUrl,
      icon: imageUrl,
      description,
      color,
      categoryColor: color,
      emoji,
      categoryEmoji: emoji,
      type: quizType,
      quizType,
      category_type: quizType,
      quiz_type: mediaType,
      mediaType,
      main_id: mainId,
      mainId,
      type_id: mainIdStr,
      main_id_str: mainIdStr,
      updatedAt: new Date().toISOString(),
    };

    try {
      const docId = editingId || name;
      await setDoc(doc(db, "categories", docId), catData, { merge: true });
      setShowModal(false);
      fetchCategories();
    } catch (err) {
      console.error("Error saving category:", err);
    }
  };

  const handleDelete = async (id: string) => {
    if (confirm(`Are you sure you want to delete category "${id}"?`)) {
      await deleteDoc(doc(db, "categories", id));
      fetchCategories();
    }
  };

  const handleFixCategories = async () => {
    setFixing(true);
    try {
      const snap = await getDocs(collection(db, "categories"));
      for (const catDoc of snap.docs) {
        const d = catDoc.data();
        const raw = d.type || d.mediaType || d.quizType || "text";
        const norm = raw.toLowerCase().replace(" quiz", "");
        const media = norm === "image" ? "image" : norm === "audio" ? "audio" : norm === "video" ? "video" : "text";
        const mainId = media === "image" ? 2 : media === "audio" ? 3 : media === "video" ? 4 : 1;

        await setDoc(doc(db, "categories", catDoc.id), {
          type: `${media} quiz`,
          quizType: `${media} quiz`,
          category_type: `${media} quiz`,
          quiz_type: media,
          mediaType: media,
          main_id: mainId,
          mainId: mainId,
          type_id: mainId.toString(),
          main_id_str: mainId.toString(),
        }, { merge: true });
      }
      alert("Categories schema migrated and fixed successfully!");
      fetchCategories();
    } catch (err) {
      alert("Fix failed: " + err);
    } finally {
      setFixing(false);
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Categories Management</h1>
          <p className="text-slate-500 text-sm mt-1">Manage quiz categories, icons, colors, emojis and quiz types</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleFixCategories}
            disabled={fixing}
            className="bg-slate-800 hover:bg-slate-700 text-white font-medium px-4 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-2 text-sm disabled:opacity-50"
          >
            <Wrench className="w-4 h-4 text-amber-400" />
            {fixing ? "Fixing Schema..." : "Fix Categories Schema"}
          </button>
          <button
            onClick={handleOpenAdd}
            className="bg-indigo-600 hover:bg-indigo-500 text-white font-medium px-4 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-2 text-sm"
          >
            <FolderPlus className="w-4 h-4" />
            Add New Category
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-12 text-slate-400">
          <RefreshCw className="w-6 h-6 animate-spin mr-2" /> Loading categories...
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {categories.map((cat) => (
            <div
              key={cat.id}
              className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm hover:shadow-md transition-all space-y-4 relative overflow-hidden"
            >
              <div
                className="h-2 absolute top-0 left-0 right-0"
                style={{ backgroundColor: cat.color || "#6200EE" }}
              />

              <div className="flex items-start justify-between gap-3 pt-2">
                <div className="flex items-center gap-3">
                  {cat.imageUrl ? (
                    <img
                      src={cat.imageUrl}
                      alt={cat.name}
                      className="w-12 h-12 rounded-xl object-cover border bg-slate-50 shadow-sm"
                      onError={(e) => {
                        (e.target as HTMLElement).style.display = "none";
                      }}
                    />
                  ) : (
                    <div
                      className="w-12 h-12 rounded-xl flex items-center justify-center text-2xl shadow-sm"
                      style={{ backgroundColor: (cat.color || "#6200EE") + "20" }}
                    >
                      {cat.emoji || "📝"}
                    </div>
                  )}
                  <div>
                    <h3 className="font-bold text-slate-900 text-lg flex items-center gap-2">
                      <span>{cat.emoji}</span>
                      <span>{cat.name}</span>
                    </h3>
                    <span className="text-xs bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-md font-semibold border border-indigo-100">
                      {cat.type || "text quiz"}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-1">
                  <button
                    onClick={() => handleOpenEdit(cat)}
                    className="p-1.5 text-slate-400 hover:text-indigo-600 rounded-lg hover:bg-indigo-50"
                  >
                    <Edit className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleDelete(cat.id)}
                    className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {cat.description && (
                <p className="text-xs text-slate-500 line-clamp-2">{cat.description}</p>
              )}

              <div className="flex items-center justify-between text-xs text-slate-400 pt-2 border-t border-slate-100">
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-full inline-block" style={{ backgroundColor: cat.color }} />
                  <span>{cat.color}</span>
                </div>
                <span>Doc ID: {cat.id}</span>
              </div>
            </div>
          ))}

          {categories.length === 0 && (
            <div className="col-span-full py-12 text-center bg-white border border-slate-200 rounded-2xl">
              <p className="text-slate-400">No categories found. Click "Add New Category" to create one.</p>
            </div>
          )}
        </div>
      )}

      {/* Add / Edit Category Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-lg w-full shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <h3 className="text-xl font-bold text-slate-900">
              {editingId ? "Edit Category" : "Add New Category"}
            </h3>

            <form onSubmit={handleSaveCategory} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Category Name</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Science, Biology, General Knowledge"
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">
                  Icon / Image URL
                </label>
                <input
                  type="url"
                  value={imageUrl}
                  onChange={(e) => setImageUrl(e.target.value)}
                  placeholder="https://example.com/icon.png"
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
                {imageUrl && (
                  <div className="mt-2 flex items-center gap-3 bg-slate-50 p-2 rounded-xl border">
                    <img src={imageUrl} alt="Preview" className="w-10 h-10 rounded-lg object-cover" />
                    <span className="text-xs text-slate-500">Image Preview</span>
                  </div>
                )}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Category Color (Hex)</label>
                  <div className="flex items-center gap-2">
                    <input
                      type="color"
                      value={color}
                      onChange={(e) => setColor(e.target.value)}
                      className="w-10 h-10 rounded-xl border cursor-pointer p-0.5"
                    />
                    <input
                      type="text"
                      value={color}
                      onChange={(e) => setColor(e.target.value)}
                      className="flex-1 border border-slate-300 rounded-xl px-3 py-2 text-sm focus:outline-none uppercase font-mono"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Emoji Icon</label>
                  <input
                    type="text"
                    value={emoji}
                    onChange={(e) => setEmoji(e.target.value)}
                    placeholder="📝"
                    className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none text-center text-lg"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Quiz Media Type</label>
                <select
                  value={mediaType}
                  onChange={(e) => setMediaType(e.target.value)}
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none bg-white"
                >
                  <option value="text">Text Quiz (Standard MCQs)</option>
                  <option value="image">Image Quiz (Picture Questions)</option>
                  <option value="audio">Audio Quiz (Sound / Voice Questions)</option>
                  <option value="video">Video Quiz (Video Clip Questions)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Description (Optional)</label>
                <textarea
                  rows={2}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Short description of this category..."
                  className="w-full border border-slate-300 rounded-xl p-3 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
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
    </div>
  );
}
