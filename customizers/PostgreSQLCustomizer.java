// camel-k: dependency=camel:jdbc dependency=mvn:org.postgresql:postgresql:jar:42.2.13 dependency=mvn:org.apache.commons:commons-dbcp2:jar:2.7.0
package customizers;

import org.apache.camel.BindToRegistry;
import org.apache.camel.PropertyInject;
import org.apache.commons.dbcp2.BasicDataSource;
import java.sql.DriverManager;
import javax.sql.DataSource;
import java.sql.Driver;

public class PostgreSQLCustomizer {

    @BindToRegistry("postgresBean")
    public DataSource initializePostgresDataSource(
        @PropertyInject("postgresql.user") String user,
        @PropertyInject("postgresql.password") String password,
        @PropertyInject("postgresql.host") String host,
        @PropertyInject("postgresql.database") String database) throws Exception {

        DriverManager.registerDriver((Driver) Class.forName("org.postgresql.Driver").newInstance());
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(String.format("jdbc:postgresql://%s:5432/%s", host, database));
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setValidationQuery("SELECT 1");
        
        return dataSource;
    }
    
}
