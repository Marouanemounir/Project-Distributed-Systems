package ma.enset.ebank.customer;

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
class CustomerTools {
    private final CustomerService service;
    private final Validator validator;

    CustomerTools(CustomerService service, Validator validator) {
        this.service = service;
        this.validator = validator;
    }

    @Tool(description = "Liste les clients avec leurs identifiants, noms et e-mails.")
    public List<Customer> getAllCustomers() {
        return service.getAllCustomers();
    }

    @Tool(description = "Recherche un client par identifiant.")
    public Customer findCustomerById(@ToolParam(description = "Identifiant numérique du client.") long id) {
        return service.findCustomerById(id);
    }

    @Tool(description = "Crée un client fictif après demande explicite.")
    public Customer saveCustomer(@ToolParam(description = "Nom et adresse e-mail du client.") CustomerRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return service.saveCustomer(request);
    }
}

@Configuration(proxyBeanMethods = false)
class CustomerToolsConfiguration {
    @Bean
    ToolCallbackProvider mcpTools(CustomerTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
