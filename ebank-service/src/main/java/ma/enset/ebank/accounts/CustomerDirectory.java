package ma.enset.ebank.accounts;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class CustomerDirectory {
    private final CustomerRestClient client;

    CustomerDirectory(CustomerRestClient client) {
        this.client = client;
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "unavailableCustomer")
    public Customer findForDisplay(long id) {
        return client.getCustomerById(id);
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "requireAvailableCustomer")
    public Customer require(long id) {
        return client.getCustomerById(id);
    }

    private Customer unavailableCustomer(long id, RuntimeException cause) {
        return new Customer(id, "Not Available", "Not Available");
    }

    private Customer requireAvailableCustomer(long id, RuntimeException cause) {
        if (cause instanceof FeignException.NotFound) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Client introuvable", cause);
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service client indisponible", cause);
    }
}
