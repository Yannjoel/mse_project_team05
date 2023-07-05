package de.mse.team5.hibernate;

import de.mse.team5.hibernate.model.Link;
import de.mse.team5.hibernate.model.Website;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.Properties;

/**
 * Based on https://www.javaguides.net/2023/03/hibernate-6-example-tutorial.html
 * See file hinerate.cfg.xml to change connection settings
 */
public class HibernateUtil {
    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        //Set save location of the db file to be contained within the maven project
        Properties props = System.getProperties();
        String dbLocation = props.getProperty("user.dir")+"/db";
        props.setProperty("derby.system.home", dbLocation);

        if (sessionFactory == null) {
            try {
                // Create registry
                registry = new StandardServiceRegistryBuilder().configure().build();

                MetadataSources sources = new MetadataSources(registry);
                //Added new hibernate classes here to enable the creation of the corresponding db- structure
                sources.addAnnotatedClass(Website.class);
                sources.addAnnotatedClass(Link.class);
                // Create Metadata
                Metadata metadata = sources.getMetadataBuilder().build();

                // Create SessionFactory
                sessionFactory = metadata.getSessionFactoryBuilder().build();
                //System.out.println( sessionFactory.getProperties().get("hibernate.connection.url"));
            } catch (Exception e) {
                e.printStackTrace();
                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
            }
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}