package ma.enset.ebank.accounts;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.List;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
class BankAccountTools {
    private final EBankService service;
    private final Validator validator;

    BankAccountTools(EBankService service, Validator validator) {
        this.service = service;
        this.validator = validator;
    }

    @Tool(description = "Liste les comptes avec leurs identifiants, types, soldes et identifiants de titulaire.")
    public List<BankAccount> getAllBankAccounts() {
        return service.getAllBankAccounts();
    }

    @Tool(description = "Retourne le détail d’un compte et son titulaire.")
    public BankAccount getBankAccountById(@ToolParam(description = "Identifiant UUID du compte.") String id) {
        return service.getBankAccountById(id);
    }

    @Tool(description = "Crée un compte fictif après demande explicite.")
    public BankAccount saveBankAccount(@ToolParam(description = "Solde initial positif ou nul, type CURRENT_ACCOUNT ou SAVING_ACCOUNT et identifiant numérique du titulaire.") AccountRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return service.saveBankAccount(request);
    }
}

@Configuration(proxyBeanMethods = false)
class BankAccountToolsConfiguration {
    @Bean
    ToolCallbackProvider mcpTools(BankAccountTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
