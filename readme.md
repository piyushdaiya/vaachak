# Vaachak (वाचक) 📖🤖

**Vaachak** (derived from the Hindi/Sanskrit word for "Reader") is a next-generation Android EPUB reader specifically optimized for E-Ink displays (like the Onyx Boox, Meebook, and Bigme).

It seamlessly bridges traditional reading with advanced multimodal AI. By leveraging the **Readium 3.1.2** engine and intercepting native Android text selection, Vaachak allows readers to instantly explain complex terms, visualize scenes with generative art, and recall character histories—all without leaving the page.

## ✨ Features

### 📚 Immersive Reading Experience (v2.0)
* **Pro-Level Book Settings:** A completely revamped, book-level settings interface with real-time previews.
  * **Dual-Tab Control:** Separate "Display" and "Layout" tabs for granular control.
  * **Typography:** Support for specialized fonts including **OpenDyslexic**, **Accessible DFA**, **IA Writer Duospace**, Serif, and Sans-Serif.
  * **Granular Layout:** Sliders for Letter Spacing, Paragraph Spacing/Indent, Line Height, and Margins (Side/Top/Bottom).
  * **Real-Time Preview:** Visualize font, size, and layout changes instantly before committing.
  * **Publisher Styles:** Toggle to respect or override the EPUB publisher's original CSS.
* **Custom Immersive UI:** A distraction-free interface replacing default system bars with a Smart Header (TOC, Search, Highlights) and a System Footer (Chapter progress, Battery, Time).
* **In-Book Search:** Full-text search with keyword highlighting and instant navigation.
* **Recursive Table of Contents:** Smart navigation with auto-detection of the active chapter.

### ✒️ E-Ink Optimization
* **E-Ink Optimized UI:** Global bitonal theme, zero-animation transitions, and high-contrast UI components to prevent ghosting.
* **Sharpness Engine:** A custom contrast slider to sharpen secondary text and dividers specifically for e-paper screens.
* **Theme Modes:** Persistent Light, Dark, and Sepia modes optimized for different lighting conditions.

### 🧠 AI Intelligence (Gemini + Cloudflare)
* **Personalized AI Recaps:** "The Story So Far"—one-tap generation of context-aware summaries based on your reading progress.
* **Quick Recall (Bookshelf):** A "Sparkle" icon on book cards provides a 2-sentence briefing on plot tension before you even open the file.
* **Contextual Actions:** Select text to "Explain" terms, "Investigate" characters, or "Visualize" scenes using Generative AI.
* **Knowledge Journaling:** Option to save AI summaries directly to your local Highlights database with a dedicated "Recap" tag.
* **Self-Healing Pipeline:** Automatic fallback to text descriptions if image generation APIs fail.

### 🛡️ Privacy & Offline Focus
* **Offline Mode:** A dedicated toggle to disable all network features. When active, AI buttons are visually hidden to ensure a distraction-free experience.
* **Book-Level Overrides:** You can enable AI features for a specific book even if the global setting is Offline (or vice versa).
* **Local Privacy:** Your library and highlights remain stored locally on your device using Room Database.
* **Secure API Configuration:** **Bring Your Own Keys (BYOK)**. No hardcoded secrets; keys are stored in encrypted DataStore preferences.

## 🏗️ Architecture

Vaachak is built using **Modern Android Development (MAD)** standards and **Clean Architecture** principles, ensuring scalability and testability.

* **UI Layer:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3) with unidirectional data flow (UDF).
* **Architecture Pattern:** MVVM (Model-View-ViewModel) with Kotlin Flows connecting layers.
* **Reading Engine:** [Readium Kotlin Toolkit 3.1.2](https://github.com/readium/kotlin-toolkit). Uses `EpubNavigatorFragment` wrapped in AndroidView for Compose interoperability.
* **Dependency Injection:** [Dagger Hilt](https://dagger.dev/hilt/).
* **Persistence:**
  * **Settings:** Jetpack DataStore (Proto/Preferences) for secure key storage.
  * **Data:** [Room Database](https://developer.android.com/training/data-storage/room) for Books, Highlights, and Reading Progress.
* **Networking:** Retrofit 2 + OkHttp for AI API communication.
* **AI Integration:**
  * **Text/Logic:** Google Gemini 1.5 Flash (via Gemini API).
  * **Image Generation:** Cloudflare Workers (Stable Diffusion XL Lightning).

### 📂 Project Structure
* `ui/bookshelf`: Dashboard, library grid, and file import logic.
* `ui/reader`: The core reading activity.
  * `components/`: ReaderSettingsSheet, TopBar, BottomSheets, and Overlays.
* `ui/highlights`: Management of saved annotations and filtering logic.
* `ui/settings`: Global app configuration (API keys, Offline mode).
* `data/`: Room Entities, DAOs, Repositories (AiRepository, SettingsRepository), and API interfaces.

## 🚀 How to Build and Run

### 1. Prerequisites
* **Android Studio:** Koala Feature Drop or newer (recommended).
* **JDK:** Java 17.
* **Android Device:** Minimum SDK 26 (Android 8.0). E-Ink device recommended but not required.

### 2. Setup AI Services

#### A. Setup Google Gemini (For Text/Recall)
1.  Go to [Google AI Studio](https://aistudio.google.com/).
2.  Click **"Get API key"**.
3.  Copy the key. You will enter this in the app settings later.

#### B. Setup Cloudflare Worker (For Visualize)
To enable the "Visualize" feature, you need a free Cloudflare Worker to act as a proxy for the Stable Diffusion model.

1.  **Create Worker:** Log in to [Cloudflare Dashboard](https://dash.cloudflare.com/) > Compute (Workers) > Create Application > "Hello World" script. Name it `vaachak-art`.
2.  **Add AI Binding:**
  * Go to **Settings > Bindings**.
  * Click **Add** > **Workers AI**.
  * Variable Name: `AI` (Must be uppercase).
3.  **Set Secret Token:**
  * Go to **Settings > Variables and Secrets**.
  * Add a variable named `API_KEY`.
  * Value: Create a strong password (e.g., `VaachakSecret123`).
4.  **Deploy Code:**
  * Click **Edit Code**, delete everything, and paste the following:

```javascript
export default {
  async fetch(request, env) {
    // 1. Security Check
    const token = request.headers.get("Authorization");
    if (token !== `Bearer ${env.API_KEY}`) {
      return new Response("Unauthorized", { status: 403 });
    }

    // 2. Parse Input (Prompt)
    const inputs = await request.json();

    // 3. Run AI Model (SDXL Lightning for speed)
    const response = await env.AI.run(
      "@cf/bytedance/stable-diffusion-xl-base-1.0",
      inputs
    );

    // 4. Return Image
    return new Response(response, {
      headers: { "content-type": "image/png" },
    });
  },
};
```
5.  **Save URL:** Click Deploy and copy the Worker URL (e.g., `https://vaachak-art.yourname.workers.dev`).

### 3. Build & Install
1.  Clone the repository:
    ```bash
    git clone [https://github.com/piyushdaiya/vaachak.git](https://github.com/piyushdaiya/vaachak.git)
    ```
2.  Open in Android Studio and sync Gradle.
3.  Build and Run:
    ```bash
    ./gradlew installDebug
    ```
4.  **Configure App:**
  * Open Vaachak on your device.
  * Tap the **Settings (Gear)** icon in the Bookshelf or Reader.
  * Enter your **Gemini API Key**, **Cloudflare URL**, and **Auth Token**.

## 📦 Download APK
Pre-compiled APKs for the **v2.0 Release** can be downloaded directly from GitHub:

👉 **[Download Vaachak v2.0](https://github.com/piyushdaiya/vaachak/releases/tag/v2.0)**

## 📄 License & Attribution

**Vaachak** is open-source software licensed under the **MIT License**.

### Open Source Technologies Used:
This project gratefully utilizes the following open-source libraries:

* **Readium Kotlin Toolkit** (BSD 3-Clause): The core EPUB rendering engine. [Readium on GitHub](https://github.com/readium/kotlin-toolkit).
* **Jetpack Compose** (Apache 2.0): Android's modern toolkit for building native UI.
* **Retrofit & OkHttp** (Apache 2.0): Type-safe HTTP client for Android.
* **Coil** (Apache 2.0): Image loading backed by Kotlin Coroutines.
* **Dagger Hilt** (Apache 2.0): Dependency injection for Android.
* **Room Database** (Apache 2.0): SQLite object mapping library.

---
*Built with  🤖 by Piyush Daiya*