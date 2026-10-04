"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, addDoc, doc, deleteDoc, writeBatch } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Search, Plus, Upload, Trash2, HelpCircle, CheckCircle2, FileText, RefreshCw } from "lucide-react";
import Papa from "papaparse";

interface Question {
  id: string;
  questionText: string;
  options: string[];
  correctOptionIndex: number;
  explanation?: string;
  bookId?: string;
  categoryId?: string;
}

export default function QuestionsPage() {
  const [questions, setQuestions] = useState<Question[]>([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);

  // Add Question Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [qText, setQText] = useState("");
  const [options, setOptions] = useState(["", "", "", ""]);
  const [correctIdx, setCorrectIdx] = useState(0);
  const [explanation, setExplanation] = useState("");

  // Bulk Upload Modal
  const [showBulkModal, setShowBulkModal] = useState(false);
  const [bulkFile, setBulkFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    fetchQuestions();
  }, []);

  const fetchQuestions = async () => {
    setLoading(true);
    try {
      const snap = await getDocs(collection(db, "questions"));
      const list = snap.docs.map(doc => ({ id: doc.id, ...doc.data() })) as Question[];
      setQuestions(list);
    } catch (err) {
      console.error("Error fetching questions:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleAddQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!qText.trim() || options.some(o => !o.trim())) {
      alert("Please enter question text and all 4 options.");
      return;
    }

    try {
      await addDoc(collection(db, "questions"), {
        questionText: qText,
        options,
        correctOptionIndex: Number(correctIdx),
        explanation,
        createdAt: new Date().toISOString(),
      });
      setQText("");
      setOptions(["", "", "", ""]);
      setCorrectIdx(0);
      setExplanation("");
      setShowAddModal(false);
      fetchQuestions();
    } catch (err) {
      console.error("Error adding question:", err);
    }
  };

  const handleDelete = async (id: string) => {
    if (confirm("Delete this question?")) {
      await deleteDoc(doc(db, "questions", id));
      fetchQuestions();
    }
  };

  const handleBulkUpload = async () => {
    if (!bulkFile) return;
    setUploading(true);

    try {
      if (bulkFile.name.endsWith(".json")) {
        const text = await bulkFile.text();
        const data = JSON.parse(text);
        const batch = writeBatch(db);
        const qRef = collection(db, "questions");

        data.forEach((q: any) => {
          const newDoc = doc(qRef);
          batch.set(newDoc, {
            questionText: q.questionText || q.question,
            options: q.options || [q.optionA, q.optionB, q.optionC, q.optionD],
            correctOptionIndex: q.correctOptionIndex ?? q.correctAnswer ?? 0,
            explanation: q.explanation || "",
            createdAt: new Date().toISOString(),
          });
        });
        await batch.commit();
      } else if (bulkFile.name.endsWith(".csv")) {
        Papa.parse(bulkFile, {
          header: true,
          complete: async (results) => {
            const batch = writeBatch(db);
            const qRef = collection(db, "questions");

            results.data.forEach((row: any) => {
              if (row.questionText || row.question) {
                const newDoc = doc(qRef);
                batch.set(newDoc, {
                  questionText: row.questionText || row.question,
                  options: [row.optionA || row.opt1, row.optionB || row.opt2, row.optionC || row.opt3, row.optionD || row.opt4],
                  correctOptionIndex: parseInt(row.correctOptionIndex || row.answerIndex || "0", 10),
                  explanation: row.explanation || "",
                  createdAt: new Date().toISOString(),
                });
              }
            });
            await batch.commit();
            setShowBulkModal(false);
            fetchQuestions();
          }
        });
      }
      setShowBulkModal(false);
      fetchQuestions();
    } catch (err) {
      alert("Error uploading file: " + err);
    } finally {
      setUploading(false);
    }
  };

  const filteredQuestions = questions.filter(q =>
    q.questionText?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Questions Manager</h1>
          <p className="text-slate-500 text-sm mt-1">Add, edit, or bulk upload quiz questions</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowBulkModal(true)}
            className="bg-emerald-600 hover:bg-emerald-500 text-white font-medium px-4 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-2 text-sm"
          >
            <Upload className="w-4 h-4" />
            Bulk Upload (JSON/CSV)
          </button>
          <button
            onClick={() => setShowAddModal(true)}
            className="bg-indigo-600 hover:bg-indigo-500 text-white font-medium px-4 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-2 text-sm"
          >
            <Plus className="w-4 h-4" />
            Add Single Question
          </button>
        </div>
      </div>

      {/* Search Input */}
      <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-sm">
        <div className="relative">
          <Search className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search questions by keyword..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-11 pr-4 py-2.5 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-indigo-500"
          />
        </div>
      </div>

      {/* Questions List */}
      {loading ? (
        <div className="flex items-center justify-center p-12 text-slate-400">
          <RefreshCw className="w-6 h-6 animate-spin mr-2" /> Loading questions...
        </div>
      ) : (
        <div className="space-y-4">
          {filteredQuestions.map((q, idx) => (
            <div key={q.id} className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-3">
                  <span className="bg-indigo-50 text-indigo-700 font-bold text-xs px-2.5 py-1 rounded-lg border border-indigo-100">
                    Q{idx + 1}
                  </span>
                  <h3 className="font-bold text-slate-900 text-base">{q.questionText}</h3>
                </div>
                <button
                  onClick={() => handleDelete(q.id)}
                  className="text-slate-400 hover:text-rose-500 p-1 rounded-lg transition-colors"
                >
                  <Trash2 className="w-5 h-5" />
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-2">
                {q.options?.map((opt, optIdx) => {
                  const isCorrect = optIdx === q.correctOptionIndex;
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

              {q.explanation && (
                <p className="text-xs text-slate-500 bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <strong className="text-slate-700">Explanation:</strong> {q.explanation}
                </p>
              )}
            </div>
          ))}

          {filteredQuestions.length === 0 && (
            <div className="text-center py-12 bg-white border border-slate-200 rounded-2xl">
              <HelpCircle className="w-12 h-12 text-slate-300 mx-auto mb-3" />
              <p className="text-slate-500 font-medium">No questions found</p>
            </div>
          )}
        </div>
      )}

      {/* Add Question Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-xl w-full shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <h3 className="text-xl font-bold text-slate-900">Add Question</h3>
            <form onSubmit={handleAddQuestion} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Question Statement</label>
                <textarea
                  required
                  rows={3}
                  value={qText}
                  onChange={(e) => setQText(e.target.value)}
                  placeholder="Enter full question statement..."
                  className="w-full border border-slate-300 rounded-xl p-3 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div className="space-y-3">
                <label className="block text-xs font-semibold text-slate-500 uppercase">Answer Options & Correct Select</label>
                {options.map((opt, i) => (
                  <div key={i} className="flex items-center gap-3">
                    <input
                      type="radio"
                      name="correctOption"
                      checked={correctIdx === i}
                      onChange={() => setCorrectIdx(i)}
                      className="w-4 h-4 text-indigo-600 focus:ring-indigo-500"
                    />
                    <input
                      type="text"
                      required
                      placeholder={`Option ${String.fromCharCode(65 + i)}`}
                      value={opt}
                      onChange={(e) => {
                        const newOpts = [...options];
                        newOpts[i] = e.target.value;
                        setOptions(newOpts);
                      }}
                      className="flex-1 border border-slate-300 rounded-xl px-4 py-2 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                    />
                  </div>
                ))}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Explanation (Optional)</label>
                <input
                  type="text"
                  value={explanation}
                  onChange={(e) => setExplanation(e.target.value)}
                  placeholder="Explain why the answer is correct..."
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3">
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
            <p className="text-xs text-slate-500">
              Select a JSON or CSV file containing question objects (questionText, options array or optionA-D, correctOptionIndex).
            </p>
            <div className="border-2 border-dashed border-slate-300 rounded-2xl p-6 text-center hover:border-emerald-500 transition-colors">
              <FileText className="w-10 h-10 text-slate-400 mx-auto mb-2" />
              <input
                type="file"
                accept=".json,.csv"
                onChange={(e) => setBulkFile(e.target.files?.[0] || null)}
                className="block w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-emerald-50 file:text-emerald-700 hover:file:bg-emerald-100"
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
                className="px-5 py-2 text-sm bg-emerald-600 text-white font-medium rounded-xl hover:bg-emerald-500 shadow-md disabled:opacity-50"
              >
                {uploading ? "Uploading..." : "Start Import"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
