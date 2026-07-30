# README Diagrams — Mermaid & Structurizr Preview

**Render ```mermaid and ```structurizr fenced code blocks from Markdown files in a live, offline preview tool window.**

[![Version](https://img.shields.io/badge/Version-2026.1.0-green.svg)](https://plugins.jetbrains.com)

## Features

- **Mermaid** — all diagram types supported by bundled Mermaid 11 (flowchart, sequence, class,
  state, ER, gantt, pie, C4, …). Rendered offline via JCEF; no cloud calls.
- **Structurizr DSL** — a practical subset (person, softwareSystem, container, component,
  relationships, groups) transpiled to Mermaid C4 diagrams. Unsupported DSL lines are listed
  as warnings under the diagram instead of failing silently.
- **Live preview** — re-renders as you type (500 ms debounce) and when you switch editors.
- **Theme-aware** — follows the IDE dark/light theme.

## Usage

1. Open a Markdown file with a diagram block:

   ~~~markdown
   ```mermaid
   graph TD
     A[Start] --> B{OK?}
   ```

   ```structurizr
   workspace {
     model {
       u = person "User"
       s = softwareSystem "Shop" {
         web = container "Web App" "Storefront" "React"
       }
       u -> web "Browses"
     }
   }
   ```
   ~~~

2. Open the **README Diagrams** tool window (right sidebar), or right-click the editor →
   **Preview README Diagrams**.

## Structurizr support boundaries

The transpiler targets Mermaid C4 output, so it renders the **model**, not Structurizr views:
`views`, `styles`, `configuration`, and deployment blocks are intentionally skipped and
Mermaid's automatic C4 layout is used. Diagram level is chosen from the deepest element
present (components → C4Component, containers → C4Container, otherwise C4Context).

## Requirements

- Any IntelliJ-based IDE 2023.2+ on the JetBrains Runtime (JCEF available by default)

## Development

```bash
./gradlew test          # unit tests (extractor, transpiler, HTML builder)
./gradlew runIde        # sandbox IDE
./gradlew buildPlugin   # build/distributions/*.zip
```

## License

See [LICENSE](LICENSE) for details.

---

**© 2026 Jakub Jirák. All rights reserved.**
