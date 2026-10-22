"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, doc, setDoc, deleteDoc, writeBatch } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Search, Plus, Upload, Trash2, Edit, HelpCircle, CheckCircle2, FileText, RefreshCw, AlertTriangle } from "lucide-react";

interface QuestionLang {
  question: string;
  options: string[];
  explanation: string;
}

interface QuestionData {
  id: string;
  category: string;
  difficulty: string;
  type: string;
  mediaUrl?: string;
  correct: number;
  isActive: boolean;
  en: QuestionLang;
  ur: QuestionLang;
  roman: QuestionLang;
}

const asRecord = (value: unknown): Record<string, unknown> =>
  value !== null && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown>
    : {};

const asText = (value: unknown, fallback = ""): string =>
  typeof value === "string" ? value : fallback;

const parseOptions = (value: unknown, fallback: unknown[]): string[] => {
  if (Array.isArray(value) && value.length >= 4) {
    return value.map(option => asText(option));
  }
  return fallback.map(option => asText(option));
};

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
      const catNames = catSnap.docs
        .map(d => asText(d.data()?.name, d.id))
        .filter(Boolean);
      setCategories(catNames);
      if (catNames.length > 0) {
        if (!category) setCategory(catNames[0]);
        if (!bulkCategory) setBulkCategory(catNames[0]);
      }

      const allList: QuestionData[] = [];

      for (const catName of catNames) {
        if (!catName) continue;
        try {
          const qSnap = await getDocs(collection(db, "questions", catName, "items"));
          qSnap.docs.forEach((docSnap) => {
            const data = asRecord(docSnap.data());
            const enObj = asRecord(data.en);
            const urObj = asRecord(data.ur);
            const romanObj = asRecord(data.roman);
            const legacyOptions = [
              data.optionA || data.opt1 || "",
              data.optionB || data.opt2 || "",
              data.optionC || data.opt3 || "",
              data.optionD || data.opt4 || "",
            ];
            const correctValue = Number(data.correct ?? data.correctOptionIndex ?? 0);

            allList.push({
              id: docSnap.id,
              category: asText(data.category, catName) || catName,
              difficulty: asText(data.difficulty, "easy"),
              type: asText(data.type, "text"),
              mediaUrl: asText(data.mediaUrl || data.imageUrl || data.audioUrl || data.videoUrl),
              correct: Number.isInteger(correctValue) && correctValue >= 0 ? correctValue : 0,
              isActive: typeof data.isActive === "boolean" ? data.isActive : true,
              en: {
                question: asText(enObj.question) || asText(data.questionText) || asText(data.question),
                options: parseOptions(enObj.options || data.options, legacyOptions),
                explanation: asText(enObj.explanation) || asText(data.explanation),
              },
              ur: {
                question: asText(urObj.question),
                options: parseOptions(urObj.options, ["", "", "", ""]),
                explanation: asText(urObj.explanation),
              },
              roman: {
                question: asText(romanObj.question),
                options: parseOptions(romanObj.options, ["", "", "", ""]),
                explanation: asText(romanObj.explanation),
              },
            });
          });
        } catch (e) {
          console.error(`Error loading questions for ${catName}:`, e);
        }
      }

      setQuestions(allList);
    } catch (err) {
      console.error("Error in loadCategoriesAndQuestions:", err);
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
    setCategory(q.category || categories[0] || "General");
    setDifficulty(q.difficulty || "easy");
    setType(q.type || "text");
    setMediaUrl(q.mediaUrl || "");
    setCorrect(q.correct ?? 0);

    setEnQuestion(q.en?.question || "");
    setEnOptions(Array.isArray(q.en?.options) ? [...q.en.options] : ["", "", "", ""]);
    setEnExplanation(q.en?.explanation || "");

    setUrQuestion(q.ur?.question || "");
    setUrOptions(Array.isArray(q.ur?.options) ? [...q.ur.options] : ["", "", "", ""]);
    setUrExplanation(q.ur?.explanation || "");

    setRomanQuestion(q.roman?.question || "");
    setRomanOptions(Array.isArray(q.roman?.options) ? [...q.roman.options] : ["", "", "", ""]);
    setRomanExplanation(q.roman?.explanation || "");

    setShowAddModal(true);
  };

  const handleSaveQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!category) {
      alert("Please select a category.");
      return;
    }

    try {
      const docRef = editingId
        ? doc(db, "questions", category, "items", editingId)
        : doc(collection(db, "questions", category, "items"));

      const data = {
        category,
        difficulty,
        type,
        mediaUrl,
        correct: Number(correct),
        isActive: true,
        en: { question: enQuestion, options: enOptions, explanation: enExplanation },
        ur: { question: urQuestion, options: urOptions, explanation: urExplanation },
        roman: { question: romanQuestion, options: romanOptions, explanation: romanExplanation },
        questionText: enQuestion,
        options: enOptions,
        correctOptionIndex: Number(correct),
        explanation: enExplanation,
        updatedAt: new Date().toISOString(),
      };

      await setDoc(docRef, data, { merge: true });
      setShowAddModal(false);
      loadCategoriesAndQuestions();
    } catch (err: any) {
      alert("Error saving question: " + err.message);
    }
  };

  const handleDeleteSingle = async (q: QuestionData) => {
    const qText = q.en?.question || q.ur?.question || "Question";
    if (confirm(`Delete question: "${qText.slice(0, 60)}..."?`)) {
      try {
        await deleteDoc(doc(db, "questions", q.category, "items", q.id));
        loadCategoriesAndQuestions();
      } catch (err: any) {
        alert("Delete failed: " + err.message);
      }
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
      } catch (err: any) {
        alert("Error deleting questions: " + err.message);
        setLoading(false);
      }
    }
  };

  const handleBulkUpload = async () => {
    if (!bulkFile) {
      alert("Please select a JSON or CSV file first.");
      return;
    }
    const targetCat = bulkCategory || categories[0] || "General";
    setUploading(true);

    try {
      const text = await bulkFile.text();

      if (bulkFile.name.endsWith(".json")) {
        const parsed = JSON.parse(text);
        const jsonArray = Array.isArray(parsed) ? parsed : [parsed];
        const batch = writeBatch(db);

        jsonArray.forEach((q: any) => {
          const cat = asText(q.category, targetCat) || targetCat;
          const docRef = doc(collection(db, "questions", cat, "items"));
          const enObj = asRecord(q.en);
          const enQ = asText(enObj.question || q.questionText || q.question);
          const opts = parseOptions(enObj.options || q.options, [q.optionA, q.optionB, q.optionC, q.optionD]);
          const corr = Number(q.correct ?? q.correctOptionIndex ?? 0);
          const diff = asText(q.difficulty, "easy");
          const qType = asText(q.type, "text");
          const imgUrl = asText(q.imageUrl || q.mediaUrl);

          batch.set(docRef, {
            category: cat,
            difficulty: diff,
            type: qType,
            mediaUrl: imgUrl,
            correct: corr,
            isActive: true,
            en: {
              question: enQ,
              options: opts,
              explanation: asText(enObj.explanation || q.explanation),
            },
            ur: {
              question: asText(q.ur?.question),
              options: parseOptions(q.ur?.options, ["", "", "", ""]),
              explanation: asText(q.ur?.explanation),
            },
            roman: {
              question: asText(q.roman?.question),
              options: parseOptions(q.roman?.options, ["", "", "", ""]),
              explanation: asText(q.roman?.explanation),
            },
            questionText: enQ,
            options: opts,
            correctOptionIndex: corr,
            explanation: asText(enObj.explanation || q.explanation),
            imageUrl: imgUrl,
          });
        });

        await batch.commit();
        alert(`Successfully imported ${jsonArray.length} questions into category "${targetCat}"!`);
      } else if (bulkFile.name.endsWith(".csv")) {
        const lines = text.split(/\r?\n/).filter(line => line.trim().length > 0);
        if (lines.length > 1) {
          const headers = lines[0].split(",").map(h => h.trim().replace(/^"(.*)"$/, "$1"));
          const batch = writeBatch(db);

          for (let i = 1; i < lines.length; i++) {
            const rowValues = lines[i].split(",").map(v => v.trim().replace(/^"(.*)"$/, "$1"));
            const rowObj: Record<string, string> = {};
            headers.forEach((h, idx) => {
              rowObj[h] = rowValues[idx] || "";
            });

            const enQ = rowObj["questionText"] || rowObj["question"] || "";
            if (enQ) {
              const cat = rowObj["category"] || targetCat;
              const docRef = doc(collection(db, "questions", cat, "items"));
              const opts = [
                rowObj["optionA"] || rowObj["opt1"] || "",
                rowObj["optionB"] || rowObj["opt2"] || "",
                rowObj["optionC"] || rowObj["opt3"] || "",
                rowObj["optionD"] || rowObj["opt4"] || "",
              ];
              const corr = parseInt(rowObj["correct"] || rowObj["correctOptionIndex"] || "0", 10);

              batch.set(docRef, {
                category: cat,
                difficulty: rowObj["difficulty"] || "easy",
                type: rowObj["type"] || "text",
                mediaUrl: rowObj["mediaUrl"] || rowObj["imageUrl"] || "",
                correct: isNaN(corr) ? 0 : corr,
                isActive: true,
                en: { question: enQ, options: opts, explanation: rowObj["explanation"] || "" },
                ur: { question: "", options: ["", "", "", ""], explanation: "" },
                roman: { question: "", options: ["", "", "", ""], explanation: "" },
                questionText: enQ,
                options: opts,
                correctOptionIndex: isNaN(corr) ? 0 : corr,
                explanation: rowObj["explanation"] || "",
              });
            }
          }
          await batch.commit();
          alert(`CSV imported successfully into category "${targetCat}"!`);
        }
      }
      setShowBulkModal(false);
      setBulkFile(null);
      loadCategoriesAndQuestions();
    } catch (err: any) {
      alert("Error during bulk upload: " + err.message);
    } finally {
      setUploading(false);
    }
  };

  const filteredQuestions = (questions || []).filter(q => {
    if (!q) return false;
    const matchesCat = filterCategory === "All" || q.category === filterCategory;
    const matchesDiff = filterDifficulty === "All" || q.difficulty === filterDifficulty;
    const matchesType = filterType === "All" || q.type === filterType;
    const qText = (q.en?.question || q.ur?.question || "").toLowerCase();
    const matchesSearch = qText.includes(searchTerm.toLowerCase());
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
            onClick={() => {
              if (categories.length > 0 && !bulkCategory) setBulkCategory(categories[0]);
              setShowBulkModal(true);
            }}
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
          {(categories || []).map((c) => (
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
            <div key={q.id || idx} className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
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
                {q.en?.question || q.ur?.question || "No question text"}
              </h3>

              {q.mediaUrl && (
                <div className="bg-slate-50 p-2 rounded-xl border max-w-sm">
                  {q.type === "image" && <img src={q.mediaUrl} alt="Media" className="max-h-40 rounded-lg object-contain" />}
                  {q.type === "audio" && <audio controls src={q.mediaUrl} className="w-full" />}
                  {q.type === "video" && <video controls src={q.mediaUrl} className="max-h-40 w-full rounded-lg" />}
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-2">
                {(q.en?.options || ["", "", "", ""]).map((opt, optIdx) => {
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

              {q.en?.explanation && (
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
                    {(categories || []).map((c) => (
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
                {(categories || []).map((c) => (
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
              <p className="text-[11px] text-slate-400 mt-2">
                Supported JSON format: Array of objects with &quot;en&quot; (question, options, explanation), &quot;correct&quot;, &quot;difficulty&quot;, etc.
              </p>
            </div>

            <div className="flex items-center justify-end pt-2 gap-3">
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
