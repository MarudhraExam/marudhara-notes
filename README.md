# Marudhara Exam — Standalone Result Search & Compiler System

This is a modern, high-performance, serverless **Result Search & Compiler System** built using React, Vite, and Tailwind CSS. It is fully compatible with static hosting platforms like GitHub Pages.

---

## 🚀 Key Features

- **Serverless Search**: Queries thousands of results in milliseconds without any database server, using an intelligent, partitioned JSON key-value store.
- **Excel Database Compiler**: Imports exam results from standard Excel files (`.xlsx`) completely in-browser, and compiles them into a partitioned JSON database ready for deployment.
- **Responsive UI**: A fully optimized search portal and administration console with a clean, high-contrast interface styled with Tailwind CSS.
- **Zero-Cost Deployment**: Designed specifically to build static files and deploy straight to GitHub Pages.

---

## 📂 Project Structure

```text
result-system/
├── .github/
│   └── workflows/
│       └── deploy.yml          # GitHub Actions deployment to Pages
├── public/
│   └── results/                # Target folder for partitioned database JSON chunks
├── src/
│   ├── components/
│   │   ├── DesktopGenerator.tsx # The browser-based Database Compiler
│   │   └── SearchPortal.tsx     # The search and result inquiries view
│   ├── utils/
│   │   └── indexer.ts           # Partitioning, searching, and indexing utils
│   ├── App.tsx                  # Application entry shell and tab layout
│   ├── main.tsx                 # React app launcher
│   ├── types.ts                 # Type definitions
│   └── index.css                # Global Tailwind CSS styles
├── scripts/
│   └── post_build.js            # Copies configuration files post-compilation
├── package.json                 # Scripts and package dependencies
├── tsconfig.json                # TypeScript configurations
├── vite.config.ts               # Vite bundler options
└── index.html                   # Core single page application index
```

---

## 🛠️ Local Development

### 1. Install Dependencies
```bash
npm install
```

### 2. Start Development Server
```bash
npm run dev
```
The app will be available locally.

### 3. Build and Compile for Production
```bash
npm run build
```
This compiles the application and outputs fully optimized static assets inside `/dist`, ready to be copied or hosted anywhere.
