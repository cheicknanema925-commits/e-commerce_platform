package com.boutique.common;

// Trop de requêtes en peu de temps : traduite en 429 par GlobalExceptionHandler
public class TooManyRequestsException extends RuntimeException {

	public TooManyRequestsException(String message) {
		super(message);
	}

}
