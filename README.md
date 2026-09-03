# 🤖 AI-Powered Java Interview Assistant

An AI-powered interview preparation web application built using **Java, Spring Boot, JavaScript, and Gemini API**.

The application helps users prepare for technical interviews by generating interview questions, providing structured AI-generated answers, and conducting interactive mock interviews.

---

## 🚀 Features

- 🤖 AI-generated interview answers using Gemini API
- 🎯 Category-based interview preparation
- 🟢 Easy, 🟡 Medium, and 🔴 Hard difficulty levels
- 🎤 Interactive Mock Interview mode
- ✅ AI-based answer evaluation and feedback
- 📊 Interview scoring
- 📚 Interview question history
- 🔽 Collapse / expand previous answers
- 🗑️ Delete individual questions or clear history
- 💾 Persistent history using LocalStorage
- 💻 Syntax highlighting for Java code
- 📋 One-click code copy functionality
- 🔐 Login & authentication system *(under development)*

---

## 🛠️ Tech Stack

### Backend
- Java 21
- Spring Boot
- Spring MVC
- REST API
- Spring Security

### Frontend
- HTML5
- CSS3
- JavaScript
- Marked.js
- Highlight.js

### AI Integration
- Google Gemini API

### Database
- PostgreSQL *(being integrated for user authentication)*

---

## ⚙️ How It Works

1. User selects an interview **category**.
2. User selects the **difficulty level**.
3. User can ask a technical interview question.
4. The Spring Boot backend sends the request to the Gemini API.
5. Gemini generates a structured interview-ready response.
6. The response is rendered on the frontend using Markdown.
7. Previous questions and answers are stored in interview history.

### Mock Interview Flow

```text
Start Mock Interview
        ↓
AI Generates Question
        ↓
Candidate Submits Answer
        ↓
AI Evaluates Answer
        ↓
Score /10
        ↓
Good Points
        ↓
Areas for Improvement
        ↓
Suggested Answer
        ↓
Next Question
```

---

## 📂 Project Structure

```text
src/
└── main/
    ├── java/
    │   └── Java/Interview/Assistant/
    │       ├── JavaInterviewAssistantApplication.java
    │       ├── InterviewController.java
    │       ├── SecurityConfig.java
    │       └── service/
    │           └── ClaudeService.java
    │
    └── resources/
        ├── static/
        │   └── index.html
        └── application.properties
```

---

## 🔑 Environment Variables

The application requires a Gemini API key.

Set the following environment variable before running the project:

```text
GEMINI_API_KEY=your_api_key_here
```

> ⚠️ Never commit your actual API key to GitHub.

---

## ▶️ Running the Project

### 1. Clone the repository

```bash
git clone YOUR_REPOSITORY_URL
```

### 2. Open the project

Open the project in **IntelliJ IDEA**.

### 3. Configure Gemini API Key

Add the `GEMINI_API_KEY` environment variable to your run configuration.

### 4. Run Spring Boot

Run:

```text
JavaInterviewAssistantApplication.java
```

### 5. Open the application

Open the application in your browser at:

```text
http://localhost:8080
```

---

## 🔮 Future Enhancements

- Complete user registration and login system
- PostgreSQL-based user profiles
- User-specific interview history
- Performance analytics dashboard
- Topic-wise performance tracking
- Interview session reports
- Voice-based interview mode
- Resume-based interview question generation

---

## 🎓 Project Purpose

This project is being developed as a **Final Year Project** with the objective of combining Java backend development, REST APIs, artificial intelligence, and interview preparation into a practical web application.

---

## 👨‍💻 Author

**Sambhav Jain**

Computer Science Engineering Student

---

## ⭐ Support

If you find this project useful, consider giving the repository a ⭐.
