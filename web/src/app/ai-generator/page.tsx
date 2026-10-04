"use client";

import { useState } from "react";
import { collection, writeBatch, doc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Sparkles, Bot, Check, CheckCircle2, Save, RefreshCw, AlertCircle } from "lucide-react";

interface AIQuestion {
  questionText: string;
  options: string[];
  correctOptionIndex: number;
  explanation: string;
  selected?: boolean;
}

export default function AiGeneratorPage() {
  const [topic, setTopic] = useState("");
  const [count, setCount] = useState(5);
  const [difficulty, setDifficulty] = useState("Medium");
  const [apiKey, setApiKey] = useState(process.env.NEXT_PUBLIC_GEMINI_API_KEY || "");

  const [generating, setGenerating] = useState(false);
  const [generatedQuestions, setGeneratedQuestions] = useState<AIQuestion[]>([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!topic.trim()) return;

    setGenerating(true);
    setError("");
    setSuccessMsg("");

    const prompt = `Generate exactly ${count} multiple choice quiz questions about "${topic}" with difficulty level "${difficulty}".
    Return ONLY a valid JSON array of objects without any markdown formatting or markdown codeblocks. Each object must have:
    - "questionText": string
    - "options": array of exactly 4 strings
    - "correctOptionIndex": integer (0, 1, 2, or 3)
    - "explanation": string explaining the correct option`;

    try {
      // Using Gemini REST API
      const key = apiKey || "AIzaSyAYUjuOpAtKakf6uYEQMt0iMlyvmf6INk4";
      const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${key}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
        }),
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.error?.message || "Failed to generate AI questions");

      let rawText = data.candidates?.[0]?.content?.parts?.[0]?.text || "";
      rawText = rawText.replace(/```json/g, "").replace(/```/g, "").trim();

      const parsed: AIQuestion[] = JSON.parse(rawText);
      setGeneratedQuestions(parsed.map(q => ({ ...q, selected: true })));
    } catch (err: any) {
      setError(err.message || "Failed to generate questions.");
    } finally {
      setGenerating(false);
    }
  };

  const handleSaveToFirestore = async () => {
    const selectedList = generatedQuestions.filter(q => q.selected);
    if (selectedList.length === 0) return;

    setSaving(true);
    try {
      const batch = writeBatch(db);
      const qRef = collection(db, "questions");

      selectedList.forEach(q => {
        const newDoc = doc(qRef);
        batch.set(newDoc, {
          questionText: q.questionText,
          options: q.options,
          correctOptionIndex: q.correctOptionIndex,
          explanation: q.explanation,
          aiGenerated: true,
          topic,
          createdAt: new Date().toISOString(),
        });
      });

      await batch.commit();
      setSuccessMsg(`Successfully saved ${selectedList.length} questions to Firestore!`);
      setGeneratedQuestions([]);
    } catch (err: any) {
      setError(err.message || "Error saving to database.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-8 max-w-5xl">
      <div className="flex items-center gap-3">
        <div className="bg-gradient-to-r from-indigo-600 to-purple-600 text-white p-3 rounded-2xl shadow-lg">
          <Bot className="w-8 h-8" />
        </div>
        <div>
          <h1 className="text-3xl font-bold text-slate-900">AI Question Generator</h1>
          <p className="text-slate-500 text-sm mt-0.5">Generate high-quality MCQs instantly using Gemini AI</p>
        </div>
      </div>

      {/* Generator Form */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-6">
        <form onSubmit={handleGenerate} className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="md:col-span-3">
            <label className="block text-xs font-semibold text-slate-500 uppercase mb-2">Topic or Subject Name</label>
            <input
              type="text"
              required
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
              placeholder="e.g. Quantum Physics, World History, Organic Chemistry"
              className="w-full border border-slate-300 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase mb-2">Number of Questions</label>
            <select
              value={count}
              onChange={(e) => setCount(Number(e.target.value))}
              className="w-full border border-slate-300 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none bg-white"
            >
              <option value={5}>5 Questions</option>
              <option value={10}>10 Questions</option>
              <option value={15}>15 Questions</option>
              <option value={20}>20 Questions</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase mb-2">Difficulty Level</label>
            <select
              value={difficulty}
              onChange={(e) => setDifficulty(e.target.value)}
              className="w-full border border-slate-300 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none bg-white"
            >
              <option value="Easy">Easy</option>
              <option value="Medium">Medium</option>
              <option value="Hard">Hard</option>
            </select>
          </div>

          <div className="flex items-end">
            <button
              type="submit"
              disabled={generating}
              className="w-full bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-medium py-3 rounded-xl transition-all shadow-md flex items-center justify-center gap-2 disabled:opacity-50 text-sm"
            >
              {generating ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" /> Generating AI MCQs...
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" /> Generate Questions
                </>
              )}
            </button>
          </div>
        </form>

        {error && (
          <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 text-sm rounded-xl flex items-center gap-2">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {successMsg && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm rounded-xl flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 flex-shrink-0 text-emerald-600" />
            <span>{successMsg}</span>
          </div>
        )}
      </div>

      {/* Generated Questions Preview */}
      {generatedQuestions.length > 0 && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold text-slate-900">Preview Generated MCQs ({generatedQuestions.length})</h2>
            <button
              onClick={handleSaveToFirestore}
              disabled={saving}
              className="bg-emerald-600 hover:bg-emerald-500 text-white font-medium px-5 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-2 text-sm disabled:opacity-50"
            >
              <Save className="w-4 h-4" />
              {saving ? "Saving to Database..." : "Save Selected to Firestore"}
            </button>
          </div>

          <div className="space-y-4">
            {generatedQuestions.map((q, idx) => (
              <div key={idx} className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
                <div className="flex items-start gap-3">
                  <input
                    type="checkbox"
                    checked={q.selected}
                    onChange={() => {
                      const updated = [...generatedQuestions];
                      updated[idx].selected = !updated[idx].selected;
                      setGeneratedQuestions(updated);
                    }}
                    className="mt-1 w-5 h-5 text-indigo-600 rounded focus:ring-indigo-500"
                  />
                  <div className="flex-1">
                    <span className="text-xs font-bold uppercase tracking-wider text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md mr-2">
                      Q{idx + 1}
                    </span>
                    <span className="font-bold text-slate-900">{q.questionText}</span>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pl-8">
                  {q.options.map((opt, optIdx) => {
                    const isCorrect = optIdx === q.correctOptionIndex;
                    return (
                      <div
                        key={optIdx}
                        className={`p-3 rounded-xl border text-sm font-medium ${
                          isCorrect
                            ? "bg-emerald-50 border-emerald-300 text-emerald-900 flex items-center justify-between"
                            : "bg-slate-50 border-slate-200 text-slate-700"
                        }`}
                      >
                        <span>{String.fromCharCode(65 + optIdx)}. {opt}</span>
                        {isCorrect && <Check className="w-4 h-4 text-emerald-600" />}
                      </div>
                    );
                  })}
                </div>

                {q.explanation && (
                  <p className="text-xs text-slate-500 bg-slate-50 p-3 rounded-xl border border-slate-100 pl-8">
                    <strong className="text-slate-700">Explanation:</strong> {q.explanation}
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
