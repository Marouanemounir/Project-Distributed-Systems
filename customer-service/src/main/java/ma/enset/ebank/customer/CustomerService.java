package ma.enset.ebank.customer;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerService {
    private final CustomerRepository customers;

    CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    public List<Customer> getAllCustomers() {
        return customers.findAll(Sort.by("id"));
    }

    public Customer findCustomerById(long id) {
        return customers.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable"));
    }

    public Customer saveCustomer(CustomerRequest request) {
        return customers.save(new Customer(request.name().strip(), request.email().strip()));
    }
}
