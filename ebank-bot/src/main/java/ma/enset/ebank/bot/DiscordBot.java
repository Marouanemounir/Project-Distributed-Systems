package ma.enset.ebank.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnExpression("'${discord.bot.token:}' != ''")
class DiscordBot {
    private static final Logger LOG = LoggerFactory.getLogger(DiscordBot.class);
    private static final URI GATEWAY = URI.create("wss://gateway.discord.gg/?v=10&encoding=json");
    private static final int INTENTS = (1 << 0) | (1 << 9) | (1 << 12) | (1 << 15);

    private final EBankAIAgent agent;
    private final ObjectMapper json;
    private final String token;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final RestClient api;
    private final ScheduledExecutorService gatewayWorker = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService messageWorker = Executors.newSingleThreadExecutor();

    DiscordBot(EBankAIAgent agent, ObjectMapper json, @Value("${discord.bot.token}") String token) {
        this.agent = agent;
        this.json = json;
        this.token = token;
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.api = RestClient.builder().baseUrl("https://discord.com/api/v10")
                .defaultHeader("Authorization", "Bot " + token).requestFactory(factory).build();
    }

    @PostConstruct
    void connect() {
        http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                .buildAsync(GATEWAY, new Session())
                .exceptionally(error -> {
                    reconnect();
                    return null;
                });
    }

    @PreDestroy
    void stop() {
        gatewayWorker.shutdownNow();
        messageWorker.shutdownNow();
    }

    private void reconnect() {
        if (!gatewayWorker.isShutdown()) {
            gatewayWorker.schedule(this::connect, 6, TimeUnit.SECONDS);
        }
    }

    private void reply(JsonNode message) {
        if (message.path("author").path("bot").asBoolean()) {
            return;
        }
        String content = message.path("content").asText("").strip();
        if (message.hasNonNull("guild_id")) {
            if (!content.startsWith("!ebank ")) {
                return;
            }
            content = content.substring("!ebank ".length()).strip();
        }
        if (content.isBlank() || content.length() > 4000) {
            return;
        }
        String channel = message.path("channel_id").asText();
        String author = message.path("author").path("id").asText();
        if (!channel.matches("\\d+") || !author.matches("\\d+")) {
            return;
        }
        String response;
        try {
            response = agent.chat(new Prompt(content), "discord:" + channel + ":" + author);
        } catch (RuntimeException error) {
            response = ChatMessages.UNAVAILABLE;
        }
        try {
            for (String part : ChatMessages.split(response, 1950)) {
                api.post().uri("/channels/{channel}/messages", channel)
                        .body(Map.of("content", part, "allowed_mentions", Map.of("parse", List.of())))
                        .retrieve().toBodilessEntity();
            }
        } catch (RuntimeException error) {
            LOG.warn("Discord n’a pas accepté la réponse du bot.");
        }
    }

    private final class Session implements WebSocket.Listener {
        private final StringBuilder frame = new StringBuilder();
        private final java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        private volatile Long sequence;
        private volatile boolean awaitingAck;
        private ScheduledFuture<?> heartbeat;

        @Override
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            frame.append(data);
            if (last) {
                try {
                    dispatch(socket, json.readTree(frame.toString()));
                } catch (IOException | RuntimeException error) {
                    disconnect(socket, true);
                } finally {
                    frame.setLength(0);
                }
            }
            socket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        private void dispatch(WebSocket socket, JsonNode event) throws IOException {
            if (event.hasNonNull("s")) {
                sequence = event.path("s").asLong();
            }
            switch (event.path("op").asInt(-1)) {
                case 10 -> {
                    long interval = event.path("d").path("heartbeat_interval").asLong();
                    if (interval <= 0 || heartbeat != null) {
                        disconnect(socket, true);
                        return;
                    }
                    heartbeat = gatewayWorker.scheduleAtFixedRate(() -> {
                        if (awaitingAck) {
                            disconnect(socket, true);
                        } else {
                            beat(socket);
                        }
                    }, interval / 2, interval, TimeUnit.MILLISECONDS);
                    socket.sendText(json.writeValueAsString(Map.of("op", 2, "d", Map.of(
                            "token", token, "intents", INTENTS, "properties", Map.of(
                                    "os", System.getProperty("os.name"), "browser", "ebank", "device", "ebank")))), true);
                }
                case 0 -> {
                    if ("MESSAGE_CREATE".equals(event.path("t").asText())) {
                        JsonNode message = event.path("d");
                        messageWorker.submit(() -> reply(message));
                    }
                }
                case 1 -> beat(socket);
                case 7, 9 -> disconnect(socket, true);
                case 11 -> awaitingAck = false;
                default -> { }
            }
        }

        private void beat(WebSocket socket) {
            awaitingAck = true;
            socket.sendText("{\"op\":1,\"d\":" + sequence + "}", true)
                    .exceptionally(error -> {
                        disconnect(socket, true);
                        return null;
                    });
        }

        @Override
        public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
            boolean credentialsOrIntentsRejected = statusCode == 4004 || statusCode == 4013 || statusCode == 4014;
            if (credentialsOrIntentsRejected) {
                LOG.warn("Discord a refusé le jeton ou les intents (code {}).", statusCode);
            }
            disconnect(socket, !credentialsOrIntentsRejected);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket socket, Throwable error) {
            disconnect(socket, true);
        }

        private void disconnect(WebSocket socket, boolean retry) {
            if (closed.compareAndSet(false, true)) {
                if (heartbeat != null) {
                    heartbeat.cancel(false);
                }
                socket.abort();
                if (retry) {
                    reconnect();
                }
            }
        }
    }
}
