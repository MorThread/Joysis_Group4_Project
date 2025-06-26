package com.joysis.library.functions;

import com.joysis.library.util.DbConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author user
 */
public class BookFunction {

    private final DbConnection dbConnection; // composition

    // constructor injection
    public BookFunction(DbConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    // --- CRUD Operations for Books ---

    // 1. Create Operation (Add Book)
    public void addBook(String title, String author, int year, String isbn) {
        String query = "INSERT INTO booklist (title, author, year, isbn, status) VALUES (?, ?, ?, ?, 0)"; // Default status 0 (available)

        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {

            prep.setString(1, title);
            prep.setString(2, author);
            prep.setInt(3, year);
            prep.setString(4, isbn);

            prep.executeUpdate();
            System.out.println("Book '" + title + "' added successfully!\n");
        } catch (SQLException e) {
            System.out.println("Error adding book: " + e.getMessage());
        }
    }

    // 2. Read Operation (Display All Books)
    public void displayAllBooks() {
        String query = "SELECT bl.id, bl.title, bl.author, bl.year, bl.isbn, bl.status, " +
                       " br.borrower_name AS current_borrower, " +
                       " br.time_in AS borrow_date, " +
                       " br.scheduled_return AS scheduled_return_date " +
                       " FROM booklist bl " +
                       " LEFT JOIN borrower_list br ON bl.id = br.book_id AND br.time_out IS NULL"; // Join to get active borrowed info

        try (Connection connection = dbConnection.connect();
             Statement state = connection.createStatement();
             ResultSet result = state.executeQuery(query)) {

            System.out.println("\n--- Current Book List ---");
            System.out.printf("%-5s %-30s %-20s %-5s %-15s %-10s %-20s %-20s %-20s\n",
                              "ID", "TITLE", "AUTHOR", "YEAR", "ISBN", "STATUS", "BORROWED BY", "BORROW DATE", "DUE DATE");
            System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------");

            boolean foundBooks = false;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            while (result.next()) {
                foundBooks = true;
                int id = result.getInt("id");
                String title = result.getString("title");
                String author = result.getString("author");
                int year = result.getInt("year");
                String isbn = result.getString("isbn");
                int status = result.getInt("status");

                String statusText = (status == 1) ? "Borrowed" : "Available";

                String currentBorrower;
                String borrowDateStr;
                String scheduledReturnDateStr;

                if (statusText.equals("Borrowed")) {
                    currentBorrower = result.getString("current_borrower");
                    Timestamp borrowTimestamp = result.getTimestamp("borrow_date");
                    Timestamp scheduledReturnTimestamp = result.getTimestamp("scheduled_return_date");

                    borrowDateStr = (borrowTimestamp != null) ? borrowTimestamp.toLocalDateTime().format(formatter) : "N/A";
                    scheduledReturnDateStr = (scheduledReturnTimestamp != null) ? scheduledReturnTimestamp.toLocalDateTime().format(formatter) : "N/A";
                    
                    // Small consistency check for cases where book is marked borrowed but no active record is joined
                    if (currentBorrower == null) {
                         currentBorrower = "N/A (Data Inconsistent)"; 
                         borrowDateStr = "N/A";
                         scheduledReturnDateStr = "N/A";
                    }

                } else {
                    currentBorrower = "N/A";
                    borrowDateStr = "N/A";
                    scheduledReturnDateStr = "N/A";
                }

                System.out.printf("%-5d %-30s %-20s %-5d %-15s %-10s %-20s %-20s %-20s\n",
                                  id, title, author, year, isbn, statusText, currentBorrower, borrowDateStr, scheduledReturnDateStr);
            }
            if (!foundBooks) {
                System.out.println("No books found in the library.");
            }
            System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        } catch (SQLException e) {
            System.out.println("Error displaying books: " + e.getMessage());
        }
    }

    // 3. Read Operation (Search Book by Title)
    public void searchBookByTitle(String keyword) {
        String query = "SELECT bl.id, bl.title, bl.author, bl.year, bl.isbn, bl.status, " +
                       " br.borrower_name AS current_borrower, " +
                       " br.time_in AS borrow_date, " +
                       " br.scheduled_return AS scheduled_return_date " +
                       " FROM booklist bl " +
                       " LEFT JOIN borrower_list br ON bl.id = br.book_id AND br.time_out IS NULL " +
                       " WHERE bl.title LIKE ?";

        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {

            prep.setString(1, "%" + keyword + "%");

            ResultSet result = prep.executeQuery();

            System.out.println("\n--- Search Results for '" + keyword + "' ---");
            System.out.printf("%-5s %-30s %-20s %-5s %-15s %-10s %-20s %-20s %-20s\n",
                              "ID", "TITLE", "AUTHOR", "YEAR", "ISBN", "STATUS", "BORROWED BY", "BORROW DATE", "DUE DATE");
            System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------");

            boolean foundBooks = false;
            DateTimeFormatter foatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            while (result.next()) {
                foundBooks = true;
                int id = result.getInt("id");
                String title = result.getString("title");
                String author = result.getString("author");
                int year = result.getInt("year");
                String isbn = result.getString("isbn");
                int status = result.getInt("status");

                String statusText = (status == 1) ? "Borrowed" : "Available";

                String currentBorrower;
                String borrowDateStr;
                String scheduledReturnDateStr;

                if (statusText.equals("Borrowed")) {
                    currentBorrower = result.getString("current_borrower");
                    Timestamp borrowTimestamp = result.getTimestamp("borrow_date");
                    Timestamp scheduledReturnTimestamp = result.getTimestamp("scheduled_return_date");

                    borrowDateStr = (borrowTimestamp != null) ? borrowTimestamp.toLocalDateTime().format(formatter) : "N/A";
                    scheduledReturnDateStr = (scheduledReturnTimestamp != null) ? scheduledReturnTimestamp.toLocalDateTime().format(formatter) : "N/A";
                    
                     if (currentBorrower == null) {
                         currentBorrower = "N/A (Data Inconsistent)";
                         borrowDateStr = "N/A";
                         scheduledReturnDateStr = "N/A";
                    }

                } else {
                    currentBorrower = "N/A";
                    borrowDateStr = "N/A";
                    scheduledReturnDateStr = "N/A";
                }

                System.out.printf("%-5d %-30s %-20s %-5d %-15s %-10s %-20s %-20s %-20s\n",
                                  id, title, author, year, isbn, statusText, currentBorrower, borrowDateStr, scheduledReturnDateStr);
            }
            if (!foundBooks) {
                System.out.println("No books found matching your search.");
            }
            System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        } catch (SQLException e) {
            System.out.println("Error searching books: " + e.getMessage());
        }
    }

    // 4. Update Operation (Update Book Details)
    public void updateBook(int bookId, String newTitle, String newAuthor, int newYear, String newISBN) {
        String query = "UPDATE booklist SET title = ?, author = ?, year = ?, isbn = ? WHERE id = ?";

        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {

            prep.setString(1, newTitle);
            prep.setString(2, newAuthor);
            prep.setInt(3, newYear);
            prep.setString(4, newISBN);
            prep.setInt(5, bookId);

            int rowsAffected = prep.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Book ID " + bookId + " updated successfully!\n");
            } else {
                System.out.println("Book ID " + bookId + " not found or no changes made.\n");
            }
        } catch (SQLException e) {
            System.out.println("Error updating book: " + e.getMessage());
        }
    }

    // 5. Delete Operation (Delete Book - Hard Delete)
    public void deleteBook(int bookId) {
        String query = "DELETE FROM booklist WHERE id = ?";

        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {

            prep.setInt(1, bookId);

            int rowsAffected = prep.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Book ID " + bookId + " deleted successfully!\n");
            } else {
                System.out.println("Book ID " + bookId + " not found.\n");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting book: " + e.getMessage());
            // Provide a more user-friendly message if it's a foreign key constraint
            if (e.getMessage().contains("a foreign key constraint fails")) {
                 System.out.println("Cannot delete book: It might be currently borrowed or referenced in another table.");
            }
        }
    }

    // --- Helper Method for Internal Use (e.g., by BorrowingFunction) ---
    // This method is called by BorrowerFunction to change a book's status (available/borrowed)
    public void updateBookStatus(int bookId, int newStatus) {
        String query = "UPDATE booklist SET status = ? WHERE id = ?";
        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {
            prep.setInt(1, newStatus);
            prep.setInt(2, bookId);
            prep.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating book status for ID " + bookId + ": " + e.getMessage());
        }
    }

    // New: Check if a book ID exists and is available
    public boolean isBookAvailable(int bookId) {
        String query = "SELECT status FROM booklist WHERE id = ?";
        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {
            prep.setInt(1, bookId);
            ResultSet result = prep.executeQuery();
            if (result.next()) {
                return result.getInt("status") == 0; // 0 means available
            }
        } catch (SQLException e) {
            System.out.println("Error checking book availability: " + e.getMessage());
        }
        return false; // Book not found or error
    }

    // New: Get book title by ID (used for display in Main before borrowing/returning confirmation)
    public String getBookTitleById(int bookId) {
        String query = "SELECT title FROM booklist WHERE id = ?";
        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {
            prep.setInt(1, bookId);
            ResultSet result = prep.executeQuery();
            if (result.next()) {
                return result.getString("title");
            }
        } catch (SQLException e) {
            System.out.println("Error getting book title: " + e.getMessage());
        }
        return null; // Book not found or error
    }

    // New: Check if a book ID exists at all (used in Main for update/delete checks)
    public boolean bookExists(int bookId) {
        String query = "SELECT COUNT(*) FROM booklist WHERE id = ?";
        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {
            prep.setInt(1, bookId);
            ResultSet result = prep.executeQuery();
            if (result.next()) {
                return result.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Error checking if book exists: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Retrieves all details of a book by its ID.
     * Returns a String array: {title, author, year, isbn, status} or null if not found.
     * Status is 0 for available, 1 for borrowed.
     */
    public String[] getBookDetailsById(int bookId) {
        String query = "SELECT title, author, year, isbn, status FROM booklist WHERE id = ?";
        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {
            prep.setInt(1, bookId);
            ResultSet result = prep.executeQuery();
            if (result.next()) {
                String title = result.getString("title");
                String author = result.getString("author");
                int year = result.getInt("year");
                String isbn = result.getString("isbn");
                int status = result.getInt("status");
                return new String[]{title, author, String.valueOf(year), isbn, String.valueOf(status)};
            }
        } catch (SQLException e) {
            System.out.println("Error getting book details: " + e.getMessage());
        }
        return null; // Book not found or error
    }
}