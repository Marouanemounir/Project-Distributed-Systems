package ma.enset.ebank.bot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

@RestController
class ChatController {
    private final EBankAIAgent agent;
    private final ObjectMapper json;

    ChatController(EBankAIAgent agent, ObjectMapper json) {
        this.agent = agent;
        this.json = json;
    }

    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(
            @RequestParam @NotBlank @Size(max = 4000) String query,
            @RequestParam(required = false) UUID conversationId) {
        var emitter = new SseEmitter(120_000L);
        var subscription = new AtomicReference<Disposable>();
        emitter.onCompletion(() -> {
            var running = subscription.get();
            if (running != null) {
                running.dispose();
            }
        });
        emitter.onTimeout(emitter::complete);
        subscription.set(agent.chatStream(new Prompt(query), restConversation(conversationId))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .subscribe(token -> emit(emitter, "chunk", token),
                        error -> {
                            emit(emitter, "failure", ChatMessages.UNAVAILABLE);
                            emitter.complete();
                        },
                        () -> {
                            emit(emitter, "done", "");
                            emitter.complete();
                        }));
        return emitter;
    }

    private void emit(SseEmitter emitter, String event, String text) {
        try {
            emitter.send(SseEmitter.event().name(event).data(json.writeValueAsString(Map.of("text", text))));
        } catch (IOException error) {
            emitter.completeWithError(error);
        }
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
