package ma.enset.ebank.bot;

import java.util.ArrayList;
import java.util.List;

final class ChatMessages {
    static final String UNAVAILABLE = "L’assistant EBank est momentanément indisponible.";

    private ChatMessages() {}

    static List<String> split(String text, int limit) {
        if (text == null || text.isBlank()) {
            return List.of("Aucune réponse reçue.");
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + limit, text.length());
            if (end < text.length()) {
                int lineBreak = text.lastIndexOf('\n', end - 1);
                if (lineBreak > start + limit / 2) {
                    end = lineBreak + 1;
                } else if (Character.isLowSurrogate(text.charAt(end))) {
                    end--;
                }
            }
            parts.add(text.substring(start, end));
            start = end;
        }
        return parts;
    }
}
