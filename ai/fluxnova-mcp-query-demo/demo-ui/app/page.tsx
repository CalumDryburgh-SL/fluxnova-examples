"use client";

import Image from "next/image";
import { FormEvent, useState, useRef, useEffect } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

interface Message {
  id: number;
  question: string;
  answer: string;
}

export default function Home() {
  const [question, setQuestion] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");
  const [messages, setMessages] = useState<Message[]>([]);
  const [chatId, setChatId] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  // Generate and persist chat ID on component mount
  useEffect(() => {
    if (!chatId) {
      setChatId(generateChatId());
    }
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, isSubmitting]);

  function generateChatId(): string {
    return crypto.randomUUID();
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmedQuestion = question.trim();

    if (!trimmedQuestion || isSubmitting) {
      return;
    }

    setIsSubmitting(true);
    setStatusMessage("");
    const currentQuestion = trimmedQuestion;
    setQuestion("");

    // Real mode - call backend
    try {
      const response = await fetch("http://localhost:8083/chat", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          chatId: chatId,
          question: currentQuestion,
        }),
      });

      if (!response.ok) {
        throw new Error("Failed to submit question");
      }

      const responseText = await response.text();
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now(),
          question: currentQuestion,
          answer: responseText,
        },
      ]);
    } catch {
      setStatusMessage("Unable to submit question. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="chat-screen">
      <div className="chat-container">
        <header className="chat-header">
          <div className="header-content">
            <img src="/logo.png" alt="FlowSight" className="header-logo" />
            <div className="header-text">
              <p className="header-subtitle">
                Real-time insights into your business workflows
              </p>
            </div>
          </div>
        </header>

        <div className="messages-container">
          {messages.length === 0 && !isSubmitting && (
            <div className="empty-state">
              <p>👋 Welcome to FlowSight!</p>
              <p>
                Ask me anything about your workflows and I&apos;ll help you find
                the answer.
              </p>
            </div>
          )}

          {messages.map((message) => (
            <div key={message.id} className="message-group">
              <div className="message user-message">
                <div className="message-content">{message.question}</div>
              </div>
              <div className="message bot-message">
                <div className="message-content">
                  <ReactMarkdown
                    className="qa-answer-markdown"
                    remarkPlugins={[remarkGfm]}
                  >
                    {message.answer}
                  </ReactMarkdown>
                </div>
              </div>
            </div>
          ))}

          {isSubmitting && (
            <div className="loading-container">
              <div className="loading-dots">
                <div className="loading-dot"></div>
                <div className="loading-dot"></div>
                <div className="loading-dot"></div>
              </div>
              <span className="loading-text">
                FlowSight is analyzing your request...
              </span>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        <div className="input-container">
          {statusMessage && <p className="submit-status">{statusMessage}</p>}
          <form className="chat-form" onSubmit={handleSubmit}>
            <input
              id="chat-question"
              type="text"
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              placeholder="Type your question here..."
              disabled={isSubmitting}
              className="chat-input"
            />
            <button
              type="submit"
              disabled={isSubmitting}
              className="send-button"
            >
              Send
            </button>
          </form>
        </div>
      </div>
    </main>
  );
}
