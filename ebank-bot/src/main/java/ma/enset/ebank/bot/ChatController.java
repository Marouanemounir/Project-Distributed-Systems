package ma.enset.ebank.bot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ChatController {
    private final EBankAIAgent agent;

    ChatController(EBankAIAgent agent) {
        this.agent = agent;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    String chat(@RequestParam @NotBlank @Size(max = 4000) String query,
                @RequestParam(required = false) UUID conversationId) {
        return agent.chat(new Prompt(query), restConversation(conversationId));
    }

    private String restConversation(UUID conversationId) {
        return "rest:" + (conversationId == null ? UUID.randomUUID() : conversationId);
    }
}
