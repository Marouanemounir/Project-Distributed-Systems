package ma.enset.ebank.accounts;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
public class BankAccount {
    @Id
    private String id;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false)
    private long customerId;

    @Transient
    private Customer customer;

    protected BankAccount() {}

    BankAccount(BigDecimal balance, AccountType type, long customerId) {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.balance = balance;
        this.type = type;
        this.customerId = customerId;
    }

    public String getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public BigDecimal getBalance() { return balance; }
    public AccountType getType() { return type; }
    public long getCustomerId() { return customerId; }
    public Customer getCustomer() { return customer; }

    void attachCustomer(Customer customer) {
        this.customer = customer;
    }
}
