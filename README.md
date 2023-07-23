# Final Project for the Lecture 'Modern Search Engines'
This is the repository for the final project in 'Modern Search Engines'. The task was to develop a functioning search engine for Tübingen-related english content.

## How to run

### Crawler
Our Crawler is implemented in Java. We are using Maven to download our dependencies.
The following prerequisites are required to run the crawler:
1. Java 20.0.1
2. Maven 3.6.3
3. A working MariaDB installation

In addition to the required software, we need to change the java trust store. The java trust store defines, which root SSL certs are trusted. The default JVM is missing multiple relevant root certificates. We therefor prepared a custom trust store that is based on the certificate list trusted by Firefox\footnote{https://hg.mozilla.org/releases/mozilla-beta/file/tip/security/nss/lib/ckfw/builtins/certdata.txt}. 
Our trust store is available at *mse\_project\_team05/src/java/mozilla\_trustStore.jks* and automatically set via the maven *jvm.config*.

If all prerequisites are met, the following steps can be used to run the crawler.
1. Download and extract the source code
2. Configure the database connection in the file *mse\_project\_team05/src/java/sr/main/resources/hibernate.cfg.xml*
3. Change directory to *mse\_project\_team05/src/java/*
4. Run the commands *mvn compile* and then *mvn exec:java* to start the crawler

We strongly recommend to also run the following sql script in the database to create indices for a speedup of the crawling process:


  CREATE INDEX website_relevantForSearch_IDX 
      USING BTREE ON [DB_NAME].website (relevantForSearch);

  CREATE INDEX website_lastChanged_IDX 
      USING BTREE ON [DB_NAME].website (lastChanged,stagedForCrawling);

  CREATE INDEX website_url_IDX USING HASH ON 
      [DB_NAME].website (url (768));
