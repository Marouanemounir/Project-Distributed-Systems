package ma.enset.ebank.accounts;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EBankService {
    private final BankAccountRepository accounts;
    private final CustomerDirectory customers;

    EBankService(BankAccountRepository accounts, CustomerDirectory customers) {
        this.accounts = accounts;
        this.customers = customers;
    }

    public List<BankAccount> getAllBankAccounts() {
        return accounts.findAll(Sort.by("createdAt", "id"));
    }

    public BankAccount getBankAccountById(String id) {
        BankAccount account = accounts.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable"));
        account.attachCustomer(customers.findForDisplay(account.getCustomerId()));
        return account;
    }

    public BankAccount saveBankAccount(AccountRequest request) {
        Customer customer = customers.require(request.customerId());
        BankAccount account = accounts.save(new BankAccount(request.balance(), request.type(), customer.id()));
        account.attachCustomer(customer);
        return account;
    }
}
