package ma.enset.ebank.accounts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AccountSeed {
    @Bean
    ApplicationRunner seedAccounts(BankAccountRepository repository) {
        return args -> {
            List<BankAccount> accounts = new ArrayList<>();
            for (long customerId = 1; customerId <= 3; customerId++) {
                for (int index = 0; index < 5; index++) {
                    AccountType type = index % 2 == 0 ? AccountType.CURRENT_ACCOUNT : AccountType.SAVING_ACCOUNT;
                    accounts.add(new BankAccount(BigDecimal.valueOf(1000 * customerId + 350L * index), type, customerId));
                }
            }
            repository.saveAll(accounts);
        };
    }
}
