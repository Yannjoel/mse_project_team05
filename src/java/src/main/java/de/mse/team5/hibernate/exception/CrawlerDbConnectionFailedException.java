package de.mse.team5.hibernate.exception;

public class CrawlerDbConnectionFailedException extends RuntimeException {
    public CrawlerDbConnectionFailedException(String message) {
        super(message);
    }
}
