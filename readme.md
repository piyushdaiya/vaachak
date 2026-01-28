# Vaachak (वाचक) 📖🤖

**Vaachak** (derived from the Hindi/Sanskrit word for "Reader") is a next-generation Android EPUB reader specifically optimized for E-Ink displays (like the Onyx Boox).

It seamlessly bridges traditional reading with advanced multimodal AI. By leveraging the **Readium 3.1.2** engine and intercepting native Android text selection, Vaachak allows readers to instantly explain complex terms, visualize scenes with generative art, and recall character histories—all without leaving the page.

## ✨ Features
* **E-Ink Optimized UI:** Global bitonal theme, zero-animation transitions, and custom high-contrast Indication logic to prevent ghosting.
* **E-ink Sharpness Engine**: A custom contrast slider that allows users to sharpen secondary text and dividers to reduce ghosting on e-paper.
* **Persistent Theme Modes**: Added persistent storage for Light, Dark, and E-ink modes via Jetpack DataStore.
* **Personalized AI Recaps (In-Reader):** AI-powered "Story So Far" feature within the reader.
  * Your Memory, Enhanced: Unlike generic summaries, these recaps use your personal highlights to determine what matters.
  * Spoiler-Free Logic: The AI is strictly bounded to your current progress—it knows where you are and never ruins the ending.
  * Knowledge Journaling: You can now save these AI summaries directly to your highlights list under a new "Recaps" tag.
* **Quick Recall (Bookshelf)** A new Sparkle (AutoAwesome) icon on each book card in the "Continue Reading" list provides a 2-sentence briefing on the plot tension and key characters based on your last read before you even open the file.
* **Global Session Briefing** A dedicated Session Recall dashboard that synthesizes your entire reading life.
  * Parallel Briefing: Parallel Gemini calls generate separate, high-fidelity summaries for your top 5 active books.
  * One-Click Resumption: Read the recap, hit "Resume Reading," and jump straight back into the book.
  * Persistent Recaps: Every time you trigger a Global Recall, the summary is etched into your database.
* **Context-Aware Explanations (Gemini 2.5 Flash):** Understands the exact paragraph context of highlighted text.
* **AI Integration**: Contextual "Explain," "Character Investigation," and "Visualize" actions powered by Gemini and Cloudflare Workers.
* **Self-Healing AI Pipeline:** If the image generation API fails, the app automatically falls back to Gemini to provide a vivid text description instead.
* **Secure API Configuration:** Bring Your Own Keys (BYOK). All API keys and endpoints are configured directly within the app's settings—no hardcoded secrets.
* **Progress Tracking**: Automatic persistence of reading percentage and page numbers.
* **Deep-Link Highlights**: Navigate directly to the exact page of a highlight from a centralized listing using Readium Locators.
  * Highlight Categorization: Add the ability to tag highlights (e.g., "Research," "Quotes," "Characters") and filter the Highlights by these tags.
* **Dual-Section Bookshelf**: A dedicated "Continue Reading" horizontal carousel for active books and a compact, searchable grid for the main library.
* **Local Privacy**: Your library and highlights stay on your device.

## 🏗️ Architecture
Vaachak is built using Modern Android Development (MAD) standards and Clean Architecture principles:
* **UI Layer:** Jetpack Compose, Material 3, Coroutines/Flows.
* **Reading Engine:** Readium 3.1.2 (Kotlin) utilizing the `EpubNavigatorFactory` pattern.
* **Dependency Injection:** Dagger Hilt.
* **Local Storage:** Android DataStore for secure preference persistence.
* **Database**: [Room](https://developer.android.com/training/data-storage/room)
* **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
* **Async**: Kotlin Coroutines & Flow
* **Networking/AI Layer:** Retrofit 2 + OkHttp for dynamic routing between Google Generative AI (Gemini) and Cloudflare Workers.

📂 Project Structure
* **ui/bookshelf:** Dashboard, book grid, and progress indicators.

* **ui/reader:** The core Readium navigator implementation and listeners.

* **ui/highlights:** Management of saved annotations and grouping logic.

* **data/**: Room entities, DAOs, and the AI repository layer.

## 🚀 How to Build and Run

### Prerequisites
* Android Studio (Koala or newer) / IntelliJ IDEA
* An active [Google AI Studio API Key](https://aistudio.google.com/)
* A Cloudflare Worker configured for Image Generation

### Setup Cloudflare Worker (For Images)

To get image generation working, you need to set up a free "Worker" on Cloudflare.

**📺 Video Reference:**
For a visual guide on setting up the Cloudflare environment, refer to **[Code With Nomi's Guide](https://www.youtube.com/watch?v=ZSHEL1EUQuE)**.

**📋 Setup Checklist:**

1. **Create Worker:** Go to Cloudflare Dashboard > Compute (Workers) > Create Application > "Hello World" script. Name it something like `kobo-art`.
2. **Add AI Binding:**

* Go to **Settings > Bindings**.
* Click **Add**.
* Choose **Workers AI**.
* Variable Name: `AI` (Must be uppercase).

3. **Add Secret Key:**

* Go to **Settings > Variables and Secrets**.
* Add a variable named `API_KEY`.
* Value: Create your own password (e.g., `MySuperSecretPassword123`). *You will need this later.*

4. **Paste the Code:**

* Click **Edit Code**.
* Delete the existing code and paste the **Worker Code** below. (This is optimized to accept the specific parameters sent by the Kobo).

**☁️ Cloudflare Worker Code:**

```javascript
export default {
  async fetch(request, env) {
    // 1. Security Check
    const token = request.headers.get("Authorization");
    if (token !== `Bearer ${env.API_KEY}`) {
      return new Response("Unauthorized", { status: 403 });
    }

    // 2. Get Input from Kobo
    // This allows the Kobo to specify width, height, and steps dynamically
    const inputs = await request.json();

    // 3. Run AI Model
    // We use SDXL Lightning for speed. You can also use '@cf/stabilityai/stable-diffusion-xl-base-1.0'
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

5. **Deploy:** Click "Deploy". Copy your Worker URL (e.g., `https://kobo-art.yourname.workers.dev`).

### Setup
1. Clone the repository:
   ```bash
   git clone [https://github.com/yourusername/vaachak.git](https://github.com/yourusername/vaachak.git)

2. Build and Install via Terminal:
   ```bash
   ./gradlew installDebug
3. Open the Vaachak app on your device.

4. Tap the Settings (Gear) Icon in the top right.

5. Enter your Gemini API Key, Cloudflare Worker URL, and Auth Token. (Data is stored securely on your local device).

### 📦 Download APK
Pre-compiled APKs (both Debug and Release versions) can be found in the Releases section of this repository.

### 📄 License
This project is open-source and available under the MIT License.

Built with Google Gemini by Piyush Daiya