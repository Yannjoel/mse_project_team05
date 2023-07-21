package de.mse.team5.hibernate;

import de.mse.team5.hibernate.exception.CrawlerDbConnectionFailedException;
import de.mse.team5.hibernate.model.Website;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * See file hibernate.cfg.xml to change connection settings
 */
public class HibernateUtil {

    private static final HibernateUtil hibernateUtil = new HibernateUtil();

    private static final Logger LOG = LogManager.getLogger(HibernateUtil.class);

    private StandardServiceRegistry registry;
    private SessionFactory sessionFactory;

    public HibernateUtil(){
       //prevent initialization
    }

    public static HibernateUtil getSingletonInstance(){
        return hibernateUtil;
    }

    public static StatelessSession openStatelessSession() {
        return getSingletonInstance().getSessionFactory().openStatelessSession();
    }

    public SessionFactory getSessionFactory() {
        if(sessionFactory == null){
            try {
                // Create registry
                registry = new StandardServiceRegistryBuilder().configure().build();

                MetadataSources sources = new MetadataSources(registry);
                //Added new hibernate classes here to enable the creation of the corresponding db- structure
                sources.addAnnotatedClass(Website.class);
                // Create Metadata
                Metadata metadata = sources.getMetadataBuilder().build();

                // Create SessionFactory
                sessionFactory = metadata.getSessionFactoryBuilder().build();
            } catch (Exception e) {
                LOG.warn("Failed to create DB connection due to ", e);
                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
                throw new CrawlerDbConnectionFailedException("Failed to initialize db connection - terminating");
            }
        }
        return sessionFactory;
    }
    public void shutdown() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}