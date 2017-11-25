package com.db.awmd.challenge.domain;

import java.math.BigDecimal;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.db.awmd.challenge.exception.InSufficientFundException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Account {

	@NotNull
	@NotEmpty
	private final String accountId;

	@NotNull
	@Min(value = 0, message = "Initial balance must be positive.")
	private BigDecimal balance;

	public Account(String accountId) {
		this.accountId = accountId;
		this.balance = BigDecimal.ZERO;
	}

	@JsonCreator
	public Account(@JsonProperty("accountId") String accountId, @JsonProperty("balance") BigDecimal balance) {
		this.accountId = accountId;
		this.balance = balance;
	}

	public BigDecimal getBalance() {
		return this.balance;
	}

	public void debit(final BigDecimal amount) throws InSufficientFundException {
		synchronized(this) {
			if ((this.balance.subtract(amount)).compareTo(BigDecimal.ZERO) < 0) {
				throw new InSufficientFundException(
						"Insufficient balance in account for transfer. amountTransfer=" + amount);
			}
			this.balance = this.balance.subtract(amount);
		}
	}

	public void credit(final BigDecimal amount) {
		synchronized(this) {
		   this.balance = this.balance.add(amount);
		}
	}

	public String getAccountId() {
		return this.accountId;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

}
