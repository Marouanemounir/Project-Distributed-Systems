package ma.enset.ebank.accounts;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
class AccountController {
    private final EBankService service;

    AccountController(EBankService service) {
        this.service = service;
    }

    @GetMapping
    List<BankAccount> list() {
        return service.getAllBankAccounts();
    }

    @GetMapping("/{id}")
    BankAccount find(@PathVariable String id) {
        return service.getBankAccountById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BankAccount create(@Valid @RequestBody AccountRequest request) {
        return service.saveBankAccount(request);
    }
}
