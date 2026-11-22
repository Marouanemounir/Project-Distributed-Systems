package ma.enset.ebank.bot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
class EBankAIAgent {
    private static final String INSTRUCTIONS = """
            Tu es l'assistant du projet EBank. Les données sont fictives.
            Réponds dans la langue de la demande. Utilise les outils MCP pour toute donnée métier.
            Si tu ne peux pas vérifier l'information, indique-le clairement.
            Effectue une création uniquement si l'utilisateur le demande explicitement
            et fournit les informations requises. N'invente ni identifiant ni solde.
            Pour une liste tabulaire, utilise Markdown.
            """;

    private final ChatClient client;

    EBankAIAgent(ChatClient.Builder builder, ChatMemory memory, ToolCallbackProvider tools) {
        this.client = builder.defaultSystem(INSTRUCTIONS)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .defaultToolCallbacks(tools)
                .build();
    }

    String chat(Prompt prompt, String conversationId) {
        return request(prompt, conversationId).call().content();
    }

    private ChatClient.ChatClientRequestSpec request(Prompt prompt, String conversationId) {
        return client.prompt(prompt).advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId));
    }
}
