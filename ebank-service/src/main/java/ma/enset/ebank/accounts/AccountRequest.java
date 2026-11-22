package ma.enset.ebank.accounts;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record AccountRequest(
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal balance,
        @NotNull AccountType type,
        @NotNull @Positive Long customerId) {}
