package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InSufficientFundException;
import com.db.awmd.challenge.exception.InvalidTransferedAmountException;
import com.db.awmd.challenge.service.AccountsService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

	@Autowired
	private AccountsService accountsService;

	@Test
	public void addAccount() throws Exception {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account);

		assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
	}

	@Test
	public void addAccount_failsOnDuplicateId() throws Exception {
		String uniqueId = "Id-" + UUID.randomUUID();
		Account account = new Account(uniqueId);
		this.accountsService.createAccount(account);

		try {
			this.accountsService.createAccount(account);
			fail("Should have failed when adding duplicate account");
		} catch (DuplicateAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
		}
	}

	@Test
	public void transferFund() throws Exception {
		String fromAccountId = "ID-" + UUID.randomUUID();
		Account fromAccount = new Account(fromAccountId);
		fromAccount.setBalance(new BigDecimal(1000));
		String toAccountId = "ID-" + UUID.randomUUID();
		Account toAccount = new Account(toAccountId);
		toAccount.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(fromAccount);
		this.accountsService.createAccount(toAccount);
		BigDecimal transferAmount = new BigDecimal(500);

		this.accountsService.transferFund(fromAccount.getAccountId(), toAccount.getAccountId(), transferAmount);

		assertThat(fromAccount.getBalance()).isEqualTo(new BigDecimal(500));
		assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal(1500));
	}
	
	@Test
	public void transferFund_failsOnInsufficientFund() throws Exception {
		String fromAccountId = "ID-" + UUID.randomUUID();
		Account fromAccount = new Account(fromAccountId);
		fromAccount.setBalance(new BigDecimal(1000));
		String toAccountId = "ID-" + UUID.randomUUID();
		Account toAccount = new Account(toAccountId);
		toAccount.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(fromAccount);
		this.accountsService.createAccount(toAccount);
		try {
			BigDecimal transferAmount = new BigDecimal(1001);
			this.accountsService.transferFund(fromAccount.getAccountId(), toAccount.getAccountId(), transferAmount);
			fail("Should have failed when insufficient funds");
		} catch (InSufficientFundException ex) {
			assertThat(ex.getMessage()).contains("Insufficient balance in account for transfer.");
		}
	}
	
//	@Test
//	public void transferFund_failsOnException() throws Exception {
//		String fromAccountId = "ID-" + UUID.randomUUID();
//		Account fromAccount = new Account(fromAccountId);
//		fromAccount.setBalance(new BigDecimal(1000));
//		String toAccountId = "ID-" + UUID.randomUUID();
//		Account toAccount = new Account(toAccountId);
//		toAccount.setBalance(new BigDecimal(1000));
//		this.accountsService.createAccount(fromAccount);
//		this.accountsService.createAccount(toAccount);
//		try {
//			BigDecimal transferAmount = new BigDecimal(100);
//			this.accountsService.transferFund(fromAccount.getAccountId(), toAccount.getAccountId(), transferAmount);
//			fail("Should have failed when FundTransferException occurs");
//		} catch (FundTransferException ex) {
//			assertThat(ex.getMessage()).contains("Funds Transfer Exception.");
//			assertThat(fromAccount.getBalance()).isEqualTo(new BigDecimal(1000));
//			assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal(1000));
//		}
//	}
	
	@Test
	public void transferFund_failsOnNegativeOrZeroTransferAmount() throws Exception {
		String fromAccountId = "ID-" + UUID.randomUUID();
		Account fromAccount = new Account(fromAccountId);
		fromAccount.setBalance(new BigDecimal(1000));
		String toAccountId = "ID-" + UUID.randomUUID();
		Account toAccount = new Account(toAccountId);
		toAccount.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(fromAccount);
		this.accountsService.createAccount(toAccount);
		try {
			BigDecimal transferAmount = new BigDecimal(0);
			this.accountsService.transferFund(fromAccount.getAccountId(), toAccount.getAccountId(), transferAmount);
			fail("Should have failed when insufficient funds");
		} catch (InvalidTransferedAmountException ex) {
			assertThat(ex.getMessage()).contains("Invalid transfer amount.");
		}
	}
	
	@Test
	public void transferFund_failsOnNegativeOrZeroBalanceInDebitAccount() throws Exception {
		String fromAccountId = "ID-" + UUID.randomUUID();
		Account fromAccount = new Account(fromAccountId);
		fromAccount.setBalance(new BigDecimal(-1));
		String toAccountId = "ID-" + UUID.randomUUID();
		Account toAccount = new Account(toAccountId);
		toAccount.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(fromAccount);
		this.accountsService.createAccount(toAccount);
		try {
			BigDecimal transferAmount = new BigDecimal(1001);
			this.accountsService.transferFund(fromAccount.getAccountId(), toAccount.getAccountId(), transferAmount);
			fail("Should have failed when insufficient funds");
		} catch (InSufficientFundException ex) {
			assertThat(ex.getMessage()).contains("Insufficient balance in account for transfer.");
		}
	}

	@Test
	public void transferFundWithMultipleThreadsSequentialTransfer() throws Exception {
		String fromAccountId = "ID-" + UUID.randomUUID();
		Account fromAccount = new Account(fromAccountId);
		fromAccount.setBalance(new BigDecimal(1000));
		String toAccountId = "ID-" + UUID.randomUUID();
		Account toAccount = new Account(toAccountId);
		toAccount.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(fromAccount);
		this.accountsService.createAccount(toAccount);

		Runnable worker = new Worker(fromAccount, toAccount);

		Thread thread1 = new Thread(worker);
		thread1.start();

		Thread.sleep(2000);

		assertThat(fromAccount.getBalance()).isEqualTo(new BigDecimal(900));
		assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal(1100));

		Thread thread2 = new Thread(worker);
		thread2.start();

		Thread.sleep(2000);

		assertThat(fromAccount.getBalance()).isEqualTo(new BigDecimal(800));
		assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal(1200));

		Thread thread3 = new Thread(worker);
		thread3.start();

		Thread.sleep(2000);

		assertThat(fromAccount.getBalance()).isEqualTo(new BigDecimal(700));
		assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal(1300));

		Thread thread4 = new Thread(worker);
		thread4.start();

		Thread.sleep(2000);

		assertThat(fromAccount.getBalance()).isEqualTo(new BigDecimal(600));
		assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal(1400));

	}

	@Test
	public void transferFundWithMultipleThreadParallelTransfer_FixedTransferAmount() throws Exception {
		String fromAccountId = "ID-" + UUID.randomUUID();
		Account fromAccount = new Account(fromAccountId);
		fromAccount.setBalance(new BigDecimal(1000));
		String toAccountId = "ID-" + UUID.randomUUID();
		Account toAccount = new Account(toAccountId);
		toAccount.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(fromAccount);
		this.accountsService.createAccount(toAccount);

		Runnable worker = new Worker(fromAccount, toAccount);

		Thread thread1 = new Thread(worker);
		thread1.start();

		Thread thread2 = new Thread(worker);
		thread2.start();

		Thread thread3 = new Thread(worker);
		thread3.start();

		Thread thread4 = new Thread(worker);
		thread4.start();

		Thread.sleep(4000);

		assertThat(fromAccount.getBalance()).isEqualTo(new BigDecimal(600));
		assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal(1400));

	}

	@Test
	public void transferFundWithMultipleThreads_ParallelTransfer_VariableAmount() throws Exception {
		String fromAccountId = "ID-" + UUID.randomUUID();
		Account fromAccount = new Account(fromAccountId);
		fromAccount.setBalance(new BigDecimal(1000));
		String toAccountId = "ID-" + UUID.randomUUID();
		Account toAccount = new Account(toAccountId);
		toAccount.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(fromAccount);
		this.accountsService.createAccount(toAccount);

		Runnable worker1 = new Worker(fromAccount, toAccount, new BigDecimal(100));

		Thread thread1 = new Thread(worker1);
		thread1.start();

		Runnable worker2 = new Worker(fromAccount, toAccount, new BigDecimal(400));
		Thread thread2 = new Thread(worker2);
		thread2.start();

		Runnable worker3 = new Worker(fromAccount, toAccount, new BigDecimal(200));
		Thread thread3 = new Thread(worker3);
		thread3.start();

		Runnable worker4 = new Worker(fromAccount, toAccount, new BigDecimal(300));
		Thread thread4 = new Thread(worker4);
		thread4.start();

		Thread.sleep(4000);

		assertThat(fromAccount.getBalance()).isEqualTo(new BigDecimal(0));
		assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal(2000));

	}

	@Test
	public void transferFunds_regressionTest() throws Exception {
		String fromAccountId = "ID-" + UUID.randomUUID();
		Account fromAccount = new Account(fromAccountId);
		fromAccount.setBalance(new BigDecimal(100000));
		String toAccountId = "ID-" + UUID.randomUUID();
		Account toAccount = new Account(toAccountId);
		toAccount.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(fromAccount);
		this.accountsService.createAccount(toAccount);

		Runnable worker = new Worker(fromAccount, toAccount, new BigDecimal(1));

		for (int index = 0; index < 100000; index++) {
			Thread thread = new Thread(worker, "Thread-" + index);
			thread.start();
		}

		Thread.sleep(60000); // It may vary on machine to machine. I have kept
								// this interval high enough to complete all
								// threads

		assertThat(fromAccount.getBalance()).isEqualTo(new BigDecimal(0));
		assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal(101000));

	}

	private class Worker implements Runnable {

		private Account fromAccount;
		private Account toAccount;
		private BigDecimal transferAmount;

		public Worker(Account fromAccount, Account toAccount) {
			this.toAccount = toAccount;
			this.fromAccount = fromAccount;
		}

		public Worker(Account fromAccount, Account toAccount, BigDecimal transferAmount) {
			this(fromAccount, toAccount);
			this.transferAmount = transferAmount;
		}

		@Override
		public void run() {
			if (transferAmount == null) {
				transferAmount = new BigDecimal(100);
			}
			accountsService.transferFund(fromAccount.getAccountId(), toAccount.getAccountId(), transferAmount);
		}
	}

}
