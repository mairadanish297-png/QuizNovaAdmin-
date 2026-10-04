"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, doc, setDoc, deleteDoc, writeBatch, collectionGroup } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Search, Plus, Upload, Trash2, Edit, HelpCircle, CheckCircle2, FileText, RefreshCw, AlertTriangle } from "lucide-react";
import Papa from "papaparse";

interface QuestionLang {
  question: string;
  options: string[];
  explanation: string;
}

interface QuestionData {
  id: string;
  category: string;
  difficulty: string; // easy, medium, hard
  type: string; // text, image, audio, video
  mediaUrl?: string;
  correct: number; // 0, 1, 2, 3
  isActive: boolean;
  en: QuestionLang;
  ur: QuestionLang;
  roman: QuestionLang;
}

export default function QuestionsPage() {
  const [categories, setCategories] = useState<string[]>([]);
  const [questions, setQuestions] = useState<QuestionData[]>([]);
  const [loading, setLoading] = useState(true);

  // Filters
  const [filterCategory, setFilterCategory] = useState("All");
  const [filterDifficulty, setFilterDifficulty] = useState("All");
  const [filterType, setFilterType] = useState("All");
  const [searchTerm, setSearchTerm] = useState("");

  // Add / Edit Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const [category, setCategory] = useState("");
  const [difficulty, setDifficulty] = useState("easy");
  const [type, setType] = useState("text");
  const [mediaUrl, setMediaUrl] = useState("");
  const [correct, setCorrect] = useState(0);

  // Language fields
  const [activeLangTab, setActiveLangTab] = useState<"en" | "ur" | "roman">("en");
  const [enQuestion, setEnQuestion] = useState("");
  const [enOptions, setEnOptions] = useState(["", "", "", ""]);
  const [enExplanation, setEnExplanation] = useState("");

  const [urQuestion, setUrQuestion] = useState("");
  const [urOptions, setUrOptions] = useState(["", "", "", ""]);
  const [urExplanation, setUrExplanation] = useState("");

  const [romanQuestion, setRomanQuestion] = useState("");
  const [romanOptions, setRomanOptions] = useState(["", "", "", ""]);
  const [romanExplanation, setRomanExplanation] = useState("");

  // Bulk Upload Modal
  const [showBulkModal, setShowBulkModal] = useState(false);
  const [bulkCategory, setBulkCategory] = useState("");
  const [bulkFile, setBulkFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    loadCategoriesAndQuestions();
  }, []);

  const loadCategoriesAndQuestions = async () => {
    setLoading(true);
    try {
      const catSnap = await getDocs(collection(db, "categories"));
      const catNames = catSnap.docs.map(d => d.data().name || d.id);
      setCategories(catNames);
      if (catNames.length > 0) {
        setCategory(catNames[0]);
        setBulkCategory(catNames[0]);
      }

      const allList: QuestionData[] = [];

      // Query questions/{cat}/items for each category
      await Promise.all(catNames.map(async (catName) => {
        try {
          const qSnap = await getDocs(collection(db, "questions", catName, "items"));
          qSnap.docs.forEach((docSnap) => {
            const data = docSnap.data();
            const enObj = data.en || {};
            const urObj = data.ur || {};
            const romanObj = data.roman || {};

            const parseOptions = (arr: any) => Array.isArray(arr) && arr.length >= 4 ? arr : [
              data.optionA || data.opt1 || "",
              data.optionB || data.opt2 || "",
              data.optionC || data.opt3 || "",
              data.optionD || data.opt4 || "",
            ];

            allList.push({
              id: docSnap.id,
              category: data.category || catName,
              difficulty: data.difficulty || "easy",
              type: data.type || "text",
              mediaUrl: data.mediaUrl || data.imageUrl || data.audioUrl || data.videoUrl || "",
              correct: data.correct ?? data.correctOptionIndex ?? 0,
              isActive: data.isActive ?? true,
              en: {
                question: enObj.question || data.questionText || data.question || "",
                options: parseOptions(enObj.options || data.options),
                explanation: enObj.explanation || data.explanation || "",
              },
              ur: {
                question: urObj.question || "",
                options: Array.isArray(urObj.options) ? urObj.options : ["", "", "", ""],
                explanation: urObj.explanation || "",
              },
              roman: {
                question: romanObj.question || "",
                options: Array.isArray(romanObj.options) ? romanObj.options : ["", "", "", ""],
                explanation: romanObj.explanation || "",
              },
            });
          });
        } catch (e) {
          console.error(`Error loading questions for ${catName}:`, e);
        }
      }));

      // Fallback query via collectionGroup if empty
      if (allList.length === 0) {
        try {
          const groupSnap = await getDocs(collectionGroup(db, "items"));
          groupSnap.docs.forEach(docSnap => {
            const data = docSnap.data();
            allList.push({
              id: docSnap.id,
              category: data.category || "General",
              difficulty: data.difficulty || "easy",
              type: data.type || "text",
              mediaUrl: data.mediaUrl || "",
              correct: data.correct ?? 0,
              isActive: true,
              en: {
                question: data.en?.question || data.questionText || data.question || "",
                options: data.en?.options || data.options || ["", "", "", ""],
                explanation: data.en?.explanation || data.explanation || "",
              },
              ur: { question: "", options: ["", "", "", ""], explanation: "" },
              roman: { question: "", options: ["", "", "", ""], explanation: "" },
            });
          });
        } catch (e) {
          console.error("CollectionGroup fallback failed:", e);
        }
      }

      setQuestions(allList);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenAdd = () => {
    setEditingId(null);
    setDifficulty("easy");
    setType("text");
    setMediaUrl("");
    setCorrect(0);
    setEnQuestion("");
    setEnOptions(["", "", "", ""]);
    setEnExplanation("");
    setUrQuestion("");
    setUrOptions(["", "", "", ""]);
    setUrExplanation("");
    setRomanQuestion("");
    setRomanOptions(["", "", "", ""]);
    setRomanExplanation("");
    setShowAddModal(true);
  };

  const handleOpenEdit = (q: QuestionData) => {
    setEditingId(q.id);
    setCategory(q.category);
    setDifficulty(q.difficulty);
    setType(q.type);
    setMediaUrl(q.mediaUrl || "");
    setCorrect(q.correct);

    setEnQuestion(q.en.question);
    setEnOptions([...q.en.options]);
    setEnExplanation(q.en.explanation);

    setUrQuestion(q.ur.question);
    setUrOptions([...q.ur.options]);
    setUrExplanation(q.ur.explanation);

    setRomanQuestion(q.roman.question);
    setRomanOptions([...q.roman.options]);
    setRomanExplanation(q.roman.explanation);

    setShowAddModal(true);
  };

  const handleSaveQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!category) {
      alert("Please select a category.");
      return;
    }

    const docId = editingId || doc(collection(db, "questions", category, "items")).id;

    const data = {
      category,
      difficulty,
      type,
      mediaUrl,
      correct: Number(correct),
      isActive: true,
      en: {
        question: enQuestion,
        options: enOptions,
        explanation: enExplanation,
      },
      ur: {
        question: urQuestion,
        options: urOptions,
        explanation: urExplanation,
      },
      roman: {
        question: romanQuestion,
        options: romanOptions,
        explanation: romanExplanation,
      },
      questionText: enQuestion,
      options: enOptions,
      correctOptionIndex: Number(correct),
      explanation: enExplanation,
      updatedAt: new Date().toISOString(),
    };

    try {
      await setDoc(doc(db, "questions", category, "items", docId), data, { merge: true });
      setShowAddModal(false);
      loadCategoriesAndQuestions();
    } catch (err) {
      console.error("Error saving question:", err);
    }
  };

  const handleDeleteSingle = async (q: QuestionData) => {
    if (confirm(`Delete question: "${q.en.question.slice(0, 60)}..."?`)) {
      await deleteDoc(doc(db, "questions", q.category, "items", q.id));
      loadCategoriesAndQuestions();
    }
  };

  const handleDeleteAllFiltered = async () => {
    if (filteredQuestions.length === 0) return;

    if (confirm(`DANGER: Are you sure you want to delete ALL ${filteredQuestions.length} filtered questions?`)) {
      setLoading(true);
      try {
        const batch = writeBatch(db);
        filteredQuestions.forEach(q => {
          batch.delete(doc(db, "questions", q.category, "items", q.id));
        });
        await batch.commit();
        alert(`Deleted ${filteredQuestions.length} questions successfully.`);
        loadCategoriesAndQuestions();
      } catch (err) {
        alert("Error deleting questions: " + err);
        setLoading(false);
      }
    }
  };

  const handleBulkUpload = async () => {
    if (!bulkFile || !bulkCategory) return;
    setUploading(true);

    try {
      if (bulkFile.name.endsWith(".json")) {
        const text = await bulkFile.text();
        const jsonArray = JSON.parse(text);
        const batch = writeBatch(db);

        jsonArray.forEach((q: any) => {
          const docRef = doc(collection(db, "questions", bulkCategory, "items"));
          const enQ = q.questionText || q.question || q.en?.question || "";
          const opts = q.options || q.en?.options || [q.optionA, q.optionB, q.optionC, q.optionD];
          const corr = q.correct ?? q.correctOptionIndex ?? 0;

          batch.set(docRef, {
            category: bulkCategory,
            difficulty: q.difficulty || "easy",
            type: q.type || "text",
            mediaUrl: q.mediaUrl || "",
            correct: Number(corr),
            isActive: true,
            en: { question: enQ, options: opts, explanation: q.explanation || "" },
            ur: { question: q.ur?.question || "", options: q.ur?.options || ["", "", "", ""], explanation: "" },
            roman: { question: q.roman?.question || "", options: q.roman?.options || ["", "", "", ""], explanation: "" },
            questionText: enQ,
            options: opts,
            correctOptionIndex: Number(corr),
            explanation: q.explanation || "",
          });
        });

        await batch.commit();
      } else if (bulkFile.name.endsWith(".csv")) {
        Papa.parse(bulkFile, {
          header: true,
          complete: async (results) => {
            const batch = writeBatch(db);
            results.data.forEach((row: any) => {
              if (row.question || row.questionText) {
                const docRef = doc(collection(db, "questions", bulkCategory, "items"));
                const enQ = row.questionText || row.question;
                const opts = [row.optionA || row.opt1, row.optionB || row.opt2, row.optionC || row.opt3, row.optionD || row.opt4];
                const corr = parseInt(row.correct || row.correctOptionIndex || "0", 10);

                batch.set(docRef, {
                  category: bulkCategory,
                  difficulty: row.difficulty || "easy",
                  type: row.type || "text",
                  mediaUrl: row.mediaUrl || "",
                  correct: corr,
                  isActive: true,
                  en: { question: enQ, options: opts, explanation: row.explanation || "" },
                  ur: { question: "", options: ["", "", "", ""], explanation: "" },
                  roman: { question: "", options: ["", "", "", ""], explanation: "" },
                  questionText: enQ,
                  options: opts,
                  correctOptionIndex: corr,
                  explanation: row.explanation || "",
                });
              }
            });
            await batch.commit();
            setShowBulkModal(false);
            loadCategoriesAndQuestions();
          }
        });
      }
      setShowBulkModal(false);
      loadCategoriesAndQuestions();
    } catch (err) {
      alert("Error: " + err);
    } finally {
      setUploading(false);
    }
  };

  const filteredQuestions = questions.filter(q => {
    const matchesCat = filterCategory === "All" || q.category === filterCategory;
    const matchesDiff = filterDifficulty === "All" || q.difficulty === filterDifficulty;
    const matchesType = filterType === "All" || q.type === filterType;
    const matchesSearch = q.en.question.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          q.ur.question.toLowerCase().includes(searchTerm.toLowerCase());
    return matchesCat && matchesDiff && matchesType && matchesSearch;
  });

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Questions Manager</h1>
          <p className="text-slate-500 text-sm mt-1">
            Showing {filteredQuestions.length} of {questions.length} total quiz questions across categories
          </p>
        </div>
        <div className="flex items-center gap-3">
          {filteredQuestions.length > 0 && (
            <button
              onClick={handleDeleteAllFiltered}
              className="bg-rose-900 hover:bg-rose-800 text-white font-medium px-4 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-2 text-sm"
            >
              <AlertTriangle className="w-4 h-4 text-rose-300" />
              Delete Filtered ({filteredQuestions.length})
            </button>
          )}
          <button
            onClick={() => setShowBulkModal(true)}
            className="bg-emerald-600 hover:bg-emerald-500 text-white font-medium px-4 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-2 text-sm"
          >
            <Upload className="w-4 h-4" />
            Bulk Upload (JSON/CSV)
          </button>
          <button
            onClick={handleOpenAdd}
            className="bg-indigo-600 hover:bg-indigo-500 text-white font-medium px-4 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-2 text-sm"
          >
            <Plus className="w-4 h-4" />
            Add Single Question
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-sm grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="relative">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search questions..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-2 border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        <select
          value={filterCategory}
          onChange={(e) => setFilterCategory(e.target.value)}
          className="border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
        >
          <option value="All">All Categories</option>
          {categories.map((c) => (
            <option key={c} value={c}>{c}</option>
          ))}
        </select>

        <select
          value={filterDifficulty}
          onChange={(e) => setFilterDifficulty(e.target.value)}
          className="border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
        >
          <option value="All">All Difficulties</option>
          <option value="easy">Easy</option>
          <option value="medium">Medium</option>
          <option value="hard">Hard</option>
        </select>

        <select
          value={filterType}
          onChange={(e) => setFilterType(e.target.value)}
          className="border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
        >
          <option value="All">All Types</option>
          <option value="text">Text Quiz</option>
          <option value="image">Image Quiz</option>
          <option value="audio">Audio Quiz</option>
          <option value="video">Video Quiz</option>
        </select>
      </div>

      {/* Questions List */}
      {loading ? (
        <div className="flex items-center justify-center p-12 text-slate-400">
          <RefreshCw className="w-6 h-6 animate-spin mr-2" /> Loading all questions...
        </div>
      ) : (
        <div className="space-y-4">
          {filteredQuestions.map((q, idx) => (
            <div key={q.id} className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-center gap-3">
                  <span className="bg-indigo-50 text-indigo-700 font-bold text-xs px-2.5 py-1 rounded-lg border border-indigo-100">
                    Q{idx + 1}
                  </span>
                  <span className="text-xs bg-slate-100 text-slate-700 px-2.5 py-0.5 rounded-md font-semibold">
                    {q.category}
                  </span>
                  <span className={`text-xs px-2.5 py-0.5 rounded-md font-semibold uppercase ${
                    q.difficulty === "easy" ? "bg-emerald-100 text-emerald-800" :
                    q.difficulty === "medium" ? "bg-amber-100 text-amber-800" : "bg-rose-100 text-rose-800"
                  }`}>
                    {q.difficulty}
                  </span>
                  <span className="text-xs bg-purple-50 text-purple-700 px-2 py-0.5 rounded-md font-semibold border border-purple-100">
                    {q.type}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleOpenEdit(q)}
                    className="p-1.5 text-slate-400 hover:text-indigo-600 rounded-lg hover:bg-indigo-50"
                  >
                    <Edit className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleDeleteSingle(q)}
                    className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              <h3 className="font-bold text-slate-900 text-base">
                {q.en.question || q.ur.question || "No question text"}
              </h3>

              {q.mediaUrl && (
                <div className="bg-slate-50 p-2 rounded-xl border max-w-sm">
                  {q.type === "image" && <img src={q.mediaUrl} alt="Media" className="max-h-40 rounded-lg object-contain" />}
                  {q.type === "audio" && <audio controls src={q.mediaUrl} className="w-full" />}
                  {q.type === "video" && <video controls src={q.mediaUrl} className="max-h-40 w-full rounded-lg" />}
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-2">
                {q.en.options.map((opt, optIdx) => {
                  const isCorrect = optIdx === q.correct;
                  return (
                    <div
                      key={optIdx}
                      className={`p-3 rounded-xl border text-sm font-medium flex items-center justify-between ${
                        isCorrect
                          ? "bg-emerald-50 border-emerald-300 text-emerald-900"
                          : "bg-slate-50 border-slate-200 text-slate-700"
                      }`}
                    >
                      <span>{String.fromCharCode(65 + optIdx)}. {opt}</span>
                      {isCorrect && <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />}
                    </div>
                  );
                })}
              </div>

              {q.en.explanation && (
                <p className="text-xs text-slate-500 bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <strong className="text-slate-700">Explanation:</strong> {q.en.explanation}
                </p>
              )}
            </div>
          ))}

          {filteredQuestions.length === 0 && (
            <div className="text-center py-12 bg-white border border-slate-200 rounded-2xl">
              <HelpCircle className="w-12 h-12 text-slate-300 mx-auto mb-3" />
              <p className="text-slate-500 font-medium">No questions found matching selected filters.</p>
            </div>
          )}
        </div>
      )}

      {/* Add / Edit Question Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-2xl w-full shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <h3 className="text-xl font-bold text-slate-900">
              {editingId ? "Edit Question" : "Add Question"}
            </h3>

            <form onSubmit={handleSaveQuestion} className="space-y-4">
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Category</label>
                  <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    className="w-full border rounded-xl px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 bg-white"
                  >
                    {categories.map((c) => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Difficulty</label>
                  <select
                    value={difficulty}
                    onChange={(e) => setDifficulty(e.target.value)}
                    className="w-full border rounded-xl px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 bg-white"
                  >
                    <option value="easy">Easy</option>
                    <option value="medium">Medium</option>
                    <option value="hard">Hard</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Quiz Type</label>
                  <select
                    value={type}
                    onChange={(e) => setType(e.target.value)}
                    className="w-full border rounded-xl px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 bg-white"
                  >
                    <option value="text">Text Quiz</option>
                    <option value="image">Image Quiz</option>
                    <option value="audio">Audio Quiz</option>
                    <option value="video">Video Quiz</option>
                  </select>
                </div>
              </div>

              {type !== "text" && (
                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Media URL ({type})</label>
                  <input
                    type="url"
                    value={mediaUrl}
                    onChange={(e) => setMediaUrl(e.target.value)}
                    placeholder="https://..."
                    className="w-full border rounded-xl px-4 py-2 text-sm focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              )}

              {/* Language Tabs */}
              <div className="border-b flex items-center gap-4 text-sm font-semibold">
                <button
                  type="button"
                  onClick={() => setActiveLangTab("en")}
                  className={`pb-2 border-b-2 ${activeLangTab === "en" ? "border-indigo-600 text-indigo-600" : "border-transparent text-slate-400"}`}
                >
                  English
                </button>
                <button
                  type="button"
                  onClick={() => setActiveLangTab("ur")}
                  className={`pb-2 border-b-2 ${activeLangTab === "ur" ? "border-indigo-600 text-indigo-600" : "border-transparent text-slate-400"}`}
                >
                  Urdu (اردو)
                </button>
                <button
                  type="button"
                  onClick={() => setActiveLangTab("roman")}
                  className={`pb-2 border-b-2 ${activeLangTab === "roman" ? "border-indigo-600 text-indigo-600" : "border-transparent text-slate-400"}`}
                >
                  Roman Urdu
                </button>
              </div>

              {/* Active Language Form */}
              {activeLangTab === "en" && (
                <div className="space-y-3">
                  <textarea
                    rows={2}
                    required
                    value={enQuestion}
                    onChange={(e) => setEnQuestion(e.target.value)}
                    placeholder="Question statement (English)..."
                    className="w-full border rounded-xl p-3 text-sm focus:ring-2 focus:ring-indigo-500"
                  />
                  {enOptions.map((opt, i) => (
                    <div key={i} className="flex items-center gap-2">
                      <input
                        type="radio"
                        name="correctOpt"
                        checked={correct === i}
                        onChange={() => setCorrect(i)}
                      />
                      <input
                        type="text"
                        required
                        value={opt}
                        onChange={(e) => {
                          const copy = [...enOptions];
                          copy[i] = e.target.value;
                          setEnOptions(copy);
                        }}
                        placeholder={`Option ${String.fromCharCode(65 + i)}`}
                        className="flex-1 border rounded-xl px-3 py-1.5 text-sm"
                      />
                    </div>
                  ))}
                  <input
                    type="text"
                    value={enExplanation}
                    onChange={(e) => setEnExplanation(e.target.value)}
                    placeholder="Explanation (English)..."
                    className="w-full border rounded-xl px-3 py-2 text-sm"
                  />
                </div>
              )}

              {activeLangTab === "ur" && (
                <div className="space-y-3 text-right font-serif">
                  <textarea
                    rows={2}
                    value={urQuestion}
                    onChange={(e) => setUrQuestion(e.target.value)}
                    placeholder="سوال بیان (اردو)..."
                    className="w-full border rounded-xl p-3 text-sm focus:ring-2 focus:ring-indigo-500 text-right"
                  />
                  {urOptions.map((opt, i) => (
                    <input
                      key={i}
                      type="text"
                      value={opt}
                      onChange={(e) => {
                        const copy = [...urOptions];
                        copy[i] = e.target.value;
                        setUrOptions(copy);
                      }}
                      placeholder={`آپشن ${i + 1}`}
                      className="w-full border rounded-xl px-3 py-1.5 text-sm text-right"
                    />
                  ))}
                </div>
              )}

              {activeLangTab === "roman" && (
                <div className="space-y-3">
                  <textarea
                    rows={2}
                    value={romanQuestion}
                    onChange={(e) => setRomanQuestion(e.target.value)}
                    placeholder="Sawal (Roman Urdu)..."
                    className="w-full border rounded-xl p-3 text-sm focus:ring-2 focus:ring-indigo-500"
                  />
                  {romanOptions.map((opt, i) => (
                    <input
                      key={i}
                      type="text"
                      value={opt}
                      onChange={(e) => {
                        const copy = [...romanOptions];
                        copy[i] = e.target.value;
                        setRomanOptions(copy);
                      }}
                      placeholder={`Option ${String.fromCharCode(65 + i)}`}
                      className="w-full border rounded-xl px-3 py-1.5 text-sm"
                    />
                  ))}
                </div>
              )}

              <div className="flex items-center justify-end gap-3 pt-3 border-t">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-sm bg-indigo-600 text-white font-medium rounded-xl hover:bg-indigo-500 shadow-md"
                >
                  Save Question
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Bulk Upload Modal */}
      {showBulkModal && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h3 className="text-xl font-bold text-slate-900">Bulk Upload Questions</h3>
            <div>
              <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Target Category</label>
              <select
                value={bulkCategory}
                onChange={(e) => setBulkCategory(e.target.value)}
                className="w-full border rounded-xl px-4 py-2.5 text-sm bg-white"
              >
                {categories.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            <div className="border-2 border-dashed border-slate-300 rounded-2xl p-6 text-center">
              <FileText className="w-10 h-10 text-slate-400 mx-auto mb-2" />
              <input
                type="file"
                accept=".json,.csv"
                onChange={(e) => setBulkFile(e.target.files?.[0] || null)}
                className="block w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-emerald-50 file:text-emerald-700"
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowBulkModal(false)}
                className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancel
              </button>
              <button
                disabled={!bulkFile || uploading}
                onClick={handleBulkUpload}
                className="px-5 py-2 text-sm bg-emerald-600 text-white font-medium rounded-xl shadow-md disabled:opacity-50"
              >
                {uploading ? "Importing..." : "Start Import"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
