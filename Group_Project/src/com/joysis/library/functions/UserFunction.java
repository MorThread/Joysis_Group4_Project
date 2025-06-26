package com.joysis.library.functions;

import com.joysis.library.util.DbConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class UserFunction {

    private final DbConnection dbConnection; 

    // constructor injection
    public UserFunction(DbConnection dbConnection) {
        this.dbConnection = dbConnection;
    }


     //Log in user for librarians, student dont have access to this 
 
    public boolean loginUser(String username, String password) {
        String query = "SELECT COUNT(*) FROM user_credentials WHERE username = ? AND password = ?"; // Check for matching credentials

        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {

            prep.setString(1, username);
            prep.setString(2, password);

            ResultSet result = prep.executeQuery();
            if (result.next()) {
                int count = result.getInt(1);
                if (count > 0) {
                    System.out.println("Login successful for user: " + username);
                    return true;
                }
            }
            
            System.out.println("Login failed. Invalid username or password.");
            return false;

        } catch (SQLException e) {
            System.out.println("Error during login: " + e.getMessage());
            return false;
        }
    }
    
    //Sir Darwin eto po yung register function po
    //Register new account for new librarian users
    public boolean registerUser(String username, String password) {

        if (usernameExists(username)) {
            System.out.println("Registration failed: Username '" + username + "' already exists. Please choose a different username.");
            return false;
        }

        String query = "INSERT INTO user_credentials (username, password) VALUES (?, ?)";

        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {

            prep.setString(1, username);
            prep.setString(2, password);

            int rowsAffected = prep.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Librarian '" + username + "' registered successfully!");
                return true;
            } else {
                System.out.println("Registration failed: Could not add user to the database.");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("Error during registration: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to check if a username already exists in the database.
     *
     * @param username The username to check.
     * @return true if the username exists, false otherwise.
     */
    private boolean usernameExists(String username) {
        String query = "SELECT COUNT(*) FROM user_credentials WHERE username = ?";
        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {

            prep.setString(1, username);
            ResultSet result = prep.executeQuery();
            if (result.next()) {
                return result.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Error checking username existence: " + e.getMessage());
        }
        return false;
    }
}
