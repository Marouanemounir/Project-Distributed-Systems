package ma.enset.ebank.customer;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
class CustomerController {
    private final CustomerService service;

    CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    List<Customer> list() {
        return service.getAllCustomers();
    }

    @GetMapping("/{id}")
    Customer find(@PathVariable long id) {
        return service.findCustomerById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Customer create(@Valid @RequestBody CustomerRequest request) {
        return service.saveCustomer(request);
    }
}
