package com.contenthelper.draft;

import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Renders the three drafts from a source text. Canned structure in v1 on
 * purpose: issue 2 puts these templates behind the AI chat client.
 */
@Service
public class DraftTemplates {

  public String linkedin(String source) {
    return "# LinkedIn draft\n\n"
        + "What I built this week:\n\n"
        + source.trim()
        + "\n\n---\n"
        + "Takeaway: ship small, write it down, repeat.\n"
        + "\n#buildinpublic #java #springboot\n";
  }

  public String x(String source) {
    String firstLine = source.trim().lines().findFirst().orElse("").trim();
    if (firstLine.length() > 200) {
      firstLine = firstLine.substring(0, 200).trim() + "...";
    }
    return firstLine + "\n\nShipped today. Notes below.\n";
  }

  public String blog(String source, String slug) {
    return "---\n"
        + "title: \"" + slug + "\"\n"
        + "date: " + LocalDate.now() + "\n"
        + "---\n\n"
        + "# " + slug + "\n\n"
        + source.trim()
        + "\n\n## Notes\n\nWrite the longer story here.\n";
  }
}
