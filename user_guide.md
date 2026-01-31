# 📖 Vaachak User Guide

Welcome to **Vaachak**, your distraction-free, AI-powered E-Ink reader. This guide will help you configure the app, customize your reading experience, and master the AI features.

---

## 🚀 1. Getting Started

### Initial Setup (API Keys)
Vaachak follows a **"Bring Your Own Key" (BYOK)** privacy model. Your keys are stored securely on your device.

1.  Open the app and tap the **Settings (Gear Icon)** in the top-right corner of the Bookshelf.
2.  **Gemini API Key:** Required for Summaries, Recaps, and Text Explanations.
    * *Don't have one?* Get it for free at [Google AI Studio](https://aistudio.google.com/).
3.  **Cloudflare URL & Token:** Required *only* if you want to use the "Visualize" (Image Generation) feature.
4.  Tap **Save Connection**.

> **Note:** The app will validate your keys immediately. If the "AI Status" shows green, you are ready to go.

![Screenshot of Global Settings Screen showing API Input fields]

---

## 📚 2. The Bookshelf

### Importing Books
1.  Tap the **Floating Action Button (+)** at the bottom right.
2.  Select an `.epub` file from your device storage.
3.  The book will appear in your "Library" grid.

### Quick Recall (Sparkle)
Before opening a book, get a quick reminder of the plot tension.
* Look for the **Sparkle (✨)** icon on any book card in the "Continue Reading" row.
* Tap it to see a 2-sentence briefing of what's happening in that book.

![Screenshot of Bookshelf with Sparkle Icon highlighted]

---

## 👓 3. Reading Experience

### Reader Controls
Tap the **center of the screen** to toggle the reading controls:
* **Top Bar:** Table of Contents, Search, Highlights, and Settings.
* **Bottom Bar:** Chapter Progress, Battery %, and Clock (optimized for E-Ink).

### Customizing the View (v2.0)
Vaachak offers professional-grade typography controls.
1.  Tap center screen > **Settings (Gear Icon)**.
2.  **Display Tab:**
    * **Theme:** Switch between Light, Dark, or Sepia (E-Ink optimized).
    * **Font:** Choose from specialized fonts like *OpenDyslexic*, *IA Writer*, or *Literata*.
    * **Size:** Use the slider to adjust text size.
3.  **Layout Tab:**
    * **Publisher Styles:** Turn **OFF** to unlock advanced controls.
    * **Spacing:** Adjust Line Height, Paragraph Gap, and Letter Spacing.
    * **Margins:** customize Side, Top, and Bottom margins independently.
4.  **Real-Time Preview:** Watch the sample text update instantly as you move sliders.
5.  Tap **Save** to apply changes.

![Screenshot of Reader Settings Sheet - Display Tab]
![Screenshot of Reader Settings Sheet - Layout Tab]

---

## 🤖 4. AI Features

### "The Story So Far" (Recap)
Lost track of the plot?
1.  Tap the **Recap Icon** (next to the gear) in the top bar.
2.  Vaachak generates a spoiler-free summary based *only* on the chapters you have read.
3.  **Save to Highlights:** Tap this button to keep the summary permanently in your notes.

### Contextual AI
Select text (long-press) to reveal the AI menu:
1.  **Explain:** Clarifies complex terms or archaic words.
2.  **Investigate:** deep-dive into a character's history using knowledge from previous chapters.
3.  **Visualize:** Generates a line-art illustration of the selected scene (requires Cloudflare setup).

![Screenshot of Text Selection Menu with AI options]

---

## 🛡️ 5. Offline Mode

For a pure, distraction-free experience or to save battery:
1.  Go to **Global Settings**.
2.  Toggle **Offline Mode** to **ON**.
3.  **Result:** All AI buttons (Sparkle, Recap, Ask AI) will be hidden from the interface.

> **Pro Tip:** You can override this per-book inside the Reader Settings > Display Tab if you need AI for just one specific difficult text.

---

## 📂 6. Highlights & Notes

1.  Tap the **Highlighter Icon** in the top bar.
2.  View all your saved highlights, including AI Recaps.
3.  Tap any highlight to jump instantly to that page.
4.  **Filter:** Use chips to filter by "General," "Characters," or "Recaps."

![Screenshot of Highlights Overlay]