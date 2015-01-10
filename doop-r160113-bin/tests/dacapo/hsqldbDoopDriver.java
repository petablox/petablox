/*
   I had to write this to get hsqldb started from the right point
   even when reflection is not used. Configuring a dynamic forName
   target and enabling full reflection introduced so much imprecision
   that even an insensitive analysis wouldn't run.

   @author Yannis Smaragdakis
*/

package org.hsqldb;


import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import org.hsqldb.jdbc.jdbcConnection;
import dacapo.hsqldb.*;

public class hsqldbDoopDriver {
    public static void main(String[] args) {
        try {
	    DriverManager.registerDriver(new jdbcDriver());
	    new PseudoJDBCBench("", "", "", false);
	} catch (Exception e) {}
    }
}