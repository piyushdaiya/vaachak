AssemblyAI offers two main options for summarizing audiobooks, each with different capabilities and use cases.

## LLM Gateway (Recommended)

AssemblyAI's LLM Gateway is a unified interface that connects with multiple LLM providers including Claude, GPT, and Gemini [^1]. This approach offers several advantages for audiobook summarization:

- **Flexibility**: Choose from various language models to find the best fit for your content
- **Customization**: Tailor the summarization process to your specific needs
- **High-quality output**: Leverage powerful language models for comprehensive summaries
- **Multiple formats**: Generate different types of summaries (detailed analysis, key points, chapter-by-chapter breakdowns)

## Standard Summarization Feature

The standard Summarization model generates a single abstractive summary of your entire audio file [^2]. For audiobooks, this option provides:

**Configuration Options:**
- **Summary Model**: Choose between `informative` or `conversational` styles [^2]
- **Summary Type**: Select from `paragraph`, `bullets`, or `headline` formats [^2]
- **Cost-effective**: More economical option for straightforward summarization needs [^1]

**Implementation Example:**
```python
import assemblyai as aai

aai.settings.api_key = "<YOUR_API_KEY>"

config = aai.TranscriptionConfig(
  summarization=True,
  summary_model=aai.SummarizationModel.informative,
  summary_type=aai.SummarizationType.bullets
)

transcript = aai.Transcriber().transcribe("audiobook_file.mp3", config)
print(transcript.summary)
```

## Additional Consideration: Auto Chapters

For longer audiobooks, you might also consider the Auto Chapters feature, which automatically segments and summarizes content over time [^3]. Each chapter includes:

- Summary of the section
- One-line gist
- Headline
- Start and end timestamps

**Important Note**: You can only enable either Summarization or Auto Chapters in the same transcription, not both [^2].

## Language Support

Both summarization options currently support English variants including Global English, Australian English, British English, and US English [^2].

The LLM Gateway approach is generally recommended for audiobooks due to its flexibility and ability to handle longer, more complex content with customizable output formats.

[^1]: https://www.assemblyai.com/docs/faq/how-do-your-summarization-models-work
[^2]: https://www.assemblyai.com/docs/speech-understanding/summarization
[^3]: https://www.assemblyai.com/docs/speech-understanding/auto-chapters

Based on the available documentation, AssemblyAI doesn't have a specific "recall" feature designed for audiobook progress tracking and spoiler-free summaries. However, you can implement a custom recall system using AssemblyAI's existing features.

## Implementing a Custom Recall System

### Using Auto Chapters for Progress Tracking

The Auto Chapters feature is particularly well-suited for building a recall system [^1]. Each chapter provides:

- **Summary** of the section content
- **One-line gist** for quick reference
- **Headline** for easy identification
- **Start and end timestamps** for precise tracking

```python
import assemblyai as aai

aai.settings.api_key = "<YOUR_API_KEY>"

config = aai.TranscriptionConfig(auto_chapters=True)
transcript = aai.Transcriber().transcribe("audiobook.mp3", config)

# Store chapters for recall system
for chapter in transcript.chapters:
    print(f"Chapter {chapter.start}-{chapter.end}: {chapter.headline}")
    print(f"Summary: {chapter.summary}")
```

### Building Spoiler-Free Summaries

To create a recall feature without spoilers, you could:

1. **Process the entire audiobook** with Auto Chapters to get all chapter summaries
2. **Track user progress** by timestamp or chapter number
3. **Filter summaries** to only include completed chapters
4. **Generate consolidated summary** using LLM Gateway for previously heard content

### Implementation Approach

```python
def generate_recall_summary(user_progress_timestamp, all_chapters):
    # Filter chapters user has completed
    completed_chapters = [
        chapter for chapter in all_chapters 
        if chapter.end <= user_progress_timestamp
    ]
    
    # Combine summaries of completed chapters
    completed_summaries = [chapter.summary for chapter in completed_chapters]
    
    # Use LLM Gateway to create consolidated recall summary
    # This would require additional LLM processing of the combined summaries
    return completed_summaries
```

## Limitations and Considerations

**Current Constraints:**
- No built-in progress tracking or recall feature
- Auto Chapters and Summarization cannot be used together in the same transcription [^2]
- You'd need to implement the spoiler-prevention logic in your application

**Recommended Architecture:**
1. Pre-process the entire audiobook with Auto Chapters
2. Store chapter data with timestamps in your database
3. Track user listening progress separately
4. Generate recall summaries by filtering completed chapters
5. Optionally use LLM Gateway to create more natural, consolidated summaries

## Alternative Approach with LLM Gateway

You could also use LLM Gateway to create custom prompts that specifically request spoiler-free summaries up to a certain point in the audiobook, giving you more control over the output format and content filtering [^3].

While AssemblyAI provides the foundational transcription and summarization capabilities, you would need to build the recall logic and spoiler prevention as part of your application layer.

[^1]: https://www.assemblyai.com/docs/speech-understanding/auto-chapters
[^2]: https://www.assemblyai.com/docs/speech-understanding/summarization
[^3]: https://www.assemblyai.com/docs/faq/how-do-your-summarization-models-work