package com.db.awmd.challenge.exception;

public class InvalidTransferedAmountException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = -2963368686833502204L;

	public InvalidTransferedAmountException(String msg) {
        super(msg);
    }
}
