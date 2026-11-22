package ma.enset.ebank.bot;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnExpression("'${telegram.bot.token:}' != ''")
class TelegramBot {
    private static final Logger LOG = LoggerFactory.getLogger(TelegramBot.class);
    private final RestClient api;
    private final String token;
    private final EBankAIAgent agent;
    private final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();

    TelegramBot(EBankAIAgent agent, @Value("${telegram.bot.token}") String token) {
        this.agent = agent;
        this.token = token;
        var requests = new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10)).build());
        requests.setReadTimeout(java.time.Duration.ofSeconds(35));
        this.api = RestClient.builder().baseUrl("https://api.telegram.org").requestFactory(requests).build();
    }

    @PostConstruct
    void start() {
        worker.submit(this::poll);
    }

    @PreDestroy
    void stop() {
        worker.shutdownNow();
    }

    private void poll() {
        long offset = 0;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                var updates = call("getUpdates", Map.of("offset", offset, "timeout", 25,
                        "allowed_updates", List.of("message")));
                for (JsonNode update : updates) {
                    offset = update.path("update_id").asLong() + 1;
                    handle(update.path("message"));
                }
            } catch (RuntimeException error) {
                LOG.warn("Telegram indisponible, nouvelle tentative dans 5 secondes.");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void handle(JsonNode message) {
        if (!message.path("chat").path("type").asText().equals("private") || message.path("from").path("is_bot").asBoolean()) {
            return;
        }
        String text = message.path("text").asText(message.path("caption").asText(""));
        JsonNode photos = message.path("photo");
        if (text.isBlank() && photos.isEmpty()) {
            return;
        }
        long chatId = message.path("chat").path("id").asLong();
        try {
            call("sendChatAction", Map.of("chat_id", chatId, "action", "typing"));
            Prompt prompt = photos.isEmpty() ? new Prompt(text) : photoPrompt(text, photos.get(photos.size() - 1));
            ChatMessages.split(agent.chat(prompt, "telegram:" + chatId), 4000)
                    .forEach(part -> send(chatId, part));
        } catch (RuntimeException error) {
            LOG.warn("Échec du traitement d'un message Telegram.");
            send(chatId, ChatMessages.UNAVAILABLE);
        }
    }

    private Prompt photoPrompt(String text, JsonNode photo) {
        String path = call("getFile", Map.of("file_id", photo.path("file_id").asText()))
                .path("file_path").asText();
        if (!path.matches("[a-zA-Z0-9_./-]+") || path.contains("..")) {
            throw new IllegalArgumentException("Chemin de photo invalide");
        }
        byte[] bytes = api.get().uri("/file/bot" + token + "/" + path).retrieve().body(byte[].class);
        if (bytes == null || bytes.length > 10_000_000) {
            throw new IllegalArgumentException("Photo indisponible ou trop volumineuse");
        }
        var image = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(bytes));
        var message = UserMessage.builder().text(text.isBlank() ? "Décris cette image." : text).media(image).build();
        return new Prompt(message);
    }

    private JsonNode call(String method, Map<String, Object> data) {
        JsonNode response = api.post().uri("/bot" + token + "/" + method).body(data)
                .retrieve().body(JsonNode.class);
        if (response == null || !response.path("ok").asBoolean()) {
            throw new IllegalStateException("Requête Telegram refusée");
        }
        return response.path("result");
    }

    private void send(long chatId, String text) {
        call("sendMessage", Map.of("chat_id", chatId, "text", text));
    }
}
