## 🗺️ Future Roadmap

### Phase 1: Library & Audiobook Expansion
* **Audiobook Engine:** Implement the `readium-kotlin` audiobook module to support M4B and multi-part MP3 formats.
* **Readium LCP Integration:** Add support for Readium LCP DRM to allow seamless borrowing from public libraries (standard in EU and growing in US).
* **OPDS 2.0 Support:** Build a catalog browser to connect to services like Feedbooks and public library manifests.
* **Calibre Integration:** Wireless syncing with local Calibre libraries.
### Phase 2: AI-Powered "Active Listening"
* **Speech-to-Summary:** Integrate Whisper/Gemini Multimodal to generate transcripts for audiobooks.
* **Conversational Recall:** A "Chat with your Book" feature using Gemini 1.5 Pro's 2M context window, allowing users to ask questions like "Who was the minor character mentioned in Chapter 3?"
* **Libby/Hoopla Bridge:** Explore deep-linking or "Share-to-Vaachak" workflows for borrowed EPUBs.
* **Advanced AI Insights:** "Character Maps" or "Plot Recaps" powered by Gemini for long-form fiction.
* **Custom E-Ink Drivers:** Deeper integration with specific vendor SDKs for zero-latency refreshing.

🛠️ Implementing Library & Audiobook Support
1. Integration with Libby (OverDrive) and Hoopla
   These services generally do not provide open APIs for third-party apps to stream content directly. To support them in Vaachak, you have two main paths:
   * Adobe Vendor ID / LCP DRM: Most library books use Adobe DRM or the newer, open Readium LCP. Readium has native support for LCP. By implementing the Readium LCP library, users can "side-load" .lcpl (license) files they download from library websites. 
   * OPDS Catalogs: Libby and many public libraries support OPDS (Open Publication Distribution System). You can build an OPDS browser in Vaachak to allow users to sign in and browse their library's XML feed, though the actual "borrow" button often redirects to a web view.

2. Audiobooks via Readium
   Readium has a specific Audiobook Toolkit (part of the kotlin-toolkit you are likely using). 
   * The Format: Readium uses the Web Publication Manifest for audiobooks. It treats a folder of MP3s/AACs + a JSON manifest as a single "book."
   * Audible Challenge: Audible is strictly proprietary. Unless you use a library like libaudible (which is unofficial and legally grey), direct integration is nearly impossible. Most developers focus on supporting DRM-free M4B files or LCP-protected audiobooks which are becoming the standard for library lending.

🧠 LLM Support for Audiobook Summary & Recall
To summarize an audiobook, the LLM needs text. Since audiobooks don't always come with transcripts, the workflow is Audio → Transcription → LLM.

Which LLM to use?
* Google Gemini 1.5 Pro (Recommended): Since you are already using Gemini, this is your best bet. It has a 2-million token context window. You can feed it a massive transcript of a 10-hour book (roughly 150k words) in one go, and it won't "forget" the beginning.

* OpenAI Whisper + GPT-4o: Use Whisper (locally on a server or via API) to generate the transcript, then feed chunks to GPT-4o. However, for "Recall" (asking questions about the whole book), Gemini’s long context window is superior.

* AssemblyAI: If you don't want to build the pipeline yourself, AssemblyAI offers an "LLM Gateway" that transcribes and summarizes audio in one API call.