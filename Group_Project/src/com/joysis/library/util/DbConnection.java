package com.joysis.library.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


//May problem ata po ako sa databse ko po kaya gawa nalang po kayo new database nyu po from your own
//Po post ko po sa github yung SQL query po ng github



public class DbConnection {

    //Kurt Kenji's Databse URL
    //private static final String URL = "jdbc:mysql://localhost:3306/group4?useTimezone=true&serverTimezone=UTC";
    
    //Lagay nyu nalang URL ng sarili nyu pong databse nyu dto
    private static final String URL = " ";
    private static final String USERNAME = "root"; 
    private static final String PASSWORD = ""; 
    private static final String DRIVER = "com.mysql.jdbc.Driver";

    static {
        try {
            // Load the JDBC driver
            Class.forName(DRIVER);
            System.out.println("JDBC Driver loaded successfully."); 
        } catch (ClassNotFoundException e) {
            System.out.println("Failed to load JDBC Driver: " + e.getMessage()); 
            e.printStackTrace(); 
        }
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}