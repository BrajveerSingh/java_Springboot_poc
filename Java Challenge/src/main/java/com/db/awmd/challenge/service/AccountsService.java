package com.db.awmd.challenge.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.FundTransferException;
import com.db.awmd.challenge.exception.InSufficientFundException;
import com.db.awmd.challenge.exception.InvalidTransferedAmountException;
import com.db.awmd.challenge.repository.AccountsRepository;

import lombok.Getter;

@Service
public class AccountsService {
	final static Logger log = LoggerFactory.getLogger(AccountsService.class);
	@Getter
	private final AccountsRepository accountsRepository;

	@Autowired
	NotificationService emailNotificationService;

	@Autowired
	public AccountsService(AccountsRepository accountsRepository) {
		this.accountsRepository = accountsRepository;
	}

	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		return this.accountsRepository.getAccount(accountId);
	}

	/**
	 * @param fromAccountId
	 *            account id from amount to be debited
	 * @param toAccountId
	 *            account id into which amount to be credited
	 * @param amountTransfer
	 *            amount to be transfered
	 */
	//This method should run in transactional context. Since there is no db 
	//hence handling transaction in application logic. Notification may be separate feature and can 
	//be send asynchronously.
	public void transferFund(String fromAccountId, String toAccountId, BigDecimal amountTransfer) {
		BigDecimal fromAccountBalance = null;
		BigDecimal toAccountBalance = null;
		Account fromAccount = null;
		Account toAccount = null;
		
		try {
			fromAccount = getAccount(fromAccountId);
			toAccount = getAccount(toAccountId);
			
			fromAccountBalance = fromAccount.getBalance();
			toAccountBalance = toAccount.getBalance();
			
			doTransfer(fromAccount, toAccount, amountTransfer);
			
		} catch (InvalidTransferedAmountException e) {
			log.error("Error in fund transfer. fromAccountId=" + fromAccountId + "  amountTransfer=" + amountTransfer);
			throw new InvalidTransferedAmountException("Error: " + e.getMessage());
		} catch (InSufficientFundException e) {
			log.error("Insuffiecient fund in account. fromAccountId=" + fromAccountId + "  amountTransfer="
					+ amountTransfer);
			throw new InSufficientFundException("Error: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Technical Error in fund transfer. fromAccountId=" + fromAccountId + "  amountTransfer="
					+ amountTransfer);
			fromAccount.setBalance(fromAccountBalance);
			toAccount.setBalance(toAccountBalance);
			throw new FundTransferException("Error: Funds Transfer Exception." + e.getMessage());
		} 
	}


	/**
	 * 
	 * @param fromAccount
	 * @param toAccount
	 * @param amountTransfer
	 * @throws FundTransferException
	 * @throws InSufficientFundException
	 */
	private void doTransfer(final Account fromAccount, final Account toAccount, final BigDecimal amountTransfer)
			throws FundTransferException, InSufficientFundException {
		synchronized (this) { // This is a syncronized block
			validate(fromAccount.getBalance(), amountTransfer);
			fromAccount.debit(amountTransfer);
			toAccount.credit(amountTransfer);
			sendNotification(fromAccount, toAccount, amountTransfer);
		}
	}

	/**
	 * @param fromAccount
	 * @param toAccount
	 * @param amountTransfer
	 */
	//This method can be made async as per design requirement
	private void sendNotification(final Account fromAccount, final Account toAccount, BigDecimal amountTransfer) {
		BigDecimal remainingBalance = fromAccount.getBalance();
		StringBuffer fromAccountTransferDescription = new StringBuffer();
		fromAccountTransferDescription.append("An amount of $ ").append(amountTransfer.doubleValue())
				.append(" is debited from your account ").append(fromAccount.getAccountId())
				.append(" your current account balance is ").append(remainingBalance);
		emailNotificationService.notifyAboutTransfer(fromAccount, fromAccountTransferDescription.toString());

		BigDecimal newBalance = toAccount.getBalance();
		StringBuffer toAccountTransferDescription = new StringBuffer();
		toAccountTransferDescription.append("An amount of $ ").append(amountTransfer.doubleValue())
				.append(" is credited in your account ").append(toAccount.getAccountId())
				.append(" your current account balance is ").append(newBalance);
		emailNotificationService.notifyAboutTransfer(toAccount, toAccountTransferDescription.toString());
	}

	private void validate(final BigDecimal fromAccountBalance, final BigDecimal amountTransfer) {

		if (amountTransfer.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidTransferedAmountException(
					"Invalid transfer amount. Amount to be transfered must be greater than zero.");
		}

	}

	public AccountsRepository getAccountsRepository() {
		return this.accountsRepository;
	}

}
