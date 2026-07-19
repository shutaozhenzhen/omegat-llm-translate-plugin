# LLM Translate (ai4j) — OmegaT Plugin

An OpenAI-compatible machine translation plugin for OmegaT powered by [ai4j](https://github.com/ai4j/ai4j). Supports any OpenAI-compatible API endpoint with customizable prompt templates.

## Features

- **Multi-provider presets**: OpenAI, DeepSeek, Groq, OpenRouter, Ollama, vLLM, Together AI, Azure OpenAI, Custom
- **Customizable prompts**: System/User prompt templates with 14 placeholders (`{text}`, `{glossary}`, `{sourceLang}`, `{fileSegments}`, `{projectPrompt}` etc.)
- **Project-level prompt**: Per-project instruction file stored at `<project>/omegat/llm-prompt.txt`
- **Glossary injection**: Auto-injects glossary entries via `{glossary}` placeholder
- **Configurable cache**: Built-in LRU cache with on/off toggle
- **Test connection**: One-click connectivity test in config panel
- **Verbose logging**: Toggleable detailed logging (Help → Log)

## Build

```bash
.\gradlew fatJar           # Build fat JAR with all dependencies
.\gradlew installPlugin    # Install to OmegaT plugins directory
```

Requires JDK 11. OmegaT JAR at `../omegat/build/libs/OmegaT.jar` (compileOnly).

## Usage

1. Install the plugin JAR to OmegaT's `plugins/` directory
2. Restart OmegaT
3. Go to **Options → Preferences → Machine Translation**
4. Enable **LLM Translate (ai4j)**
5. Click **Configure** — select provider, enter API key, adjust prompts
6. Click a segment to translate

## License

MIT
