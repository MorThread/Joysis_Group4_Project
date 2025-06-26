package com.joysis.library.functions;

import com.joysis.library.util.DbConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp; 
import java.time.LocalDate; 
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; 
import java.time.temporal.ChronoUnit; 


public class BorrowerFunction {

    private final DbConnection dbConnection; 
    private final BookFunction bookFunction; // To update book status


    public BorrowerFunction(DbConnection dbConnection, BookFunction bookFunction) {
        this.dbConnection = dbConnection;
        this.bookFunction = bookFunction;
    }


    public boolean borrowBook(int bookId, String borrowerName, int scheduledReturnDays) {

        if (bookId <= 0) {
            System.out.println("Borrow failed: Invalid Book ID. Must be a positive number.");
            return false;
        }
        if (borrowerName == null || borrowerName.trim().isEmpty()) {
            System.out.println("Borrow failed: Borrower name cannot be empty.");
            return false;
        }
        if (scheduledReturnDays <= 0) {
            System.out.println("Borrow failed: Scheduled return days must be a positive number.");
            return false;
        }

        // Check if the book exists
        String[] bookDetails = bookFunction.getBookDetailsById(bookId);
        if (bookDetails == null) {
            System.out.println("Borrow failed: Book with ID " + bookId + " does not exist.");
            return false;
        }
        
        // Check book status from the retrieved details
        int bookStatus = Integer.parseInt(bookDetails[4]); // Status is at index 4
        if (bookStatus == 1) { // 1 means borrowed
            System.out.println("Borrow failed: Book '" + bookDetails[0] + "' (ID: " + bookId + ") is currently not available (already borrowed).");
            return false;
        }

        String bookTitle = bookDetails[0]; // Title is at index 0

        // Check if there's an active borrowing record for this book
        if (isBookCurrentlyBorrowedInBorrowerList(bookId)) {
            System.out.println("Borrow failed: An active borrowing record for Book ID " + bookId + " already exists. Data might be inconsistent.");
            // Optionally, you might try to set book status to 1 here if it's 0 but an active record exists.
            return false;
        }

        // Get current time for time_in
        LocalDateTime now = LocalDateTime.now();
        Timestamp timeIn = Timestamp.valueOf(now);


        LocalDateTime scheduledReturnDateTime = now.plusDays(scheduledReturnDays);
        Timestamp scheduledReturn = Timestamp.valueOf(scheduledReturnDateTime);

        String query = "INSERT INTO borrower_list (borrower_name, book_id, time_in, scheduled_return) VALUES (?, ?, ?, ?)";

        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {

            prep.setString(1, borrowerName);
            prep.setInt(2, bookId);
            prep.setTimestamp(3, timeIn);
            prep.setTimestamp(4, scheduledReturn);

            int rowsAffected = prep.executeUpdate();
            if (rowsAffected > 0) {

                bookFunction.updateBookStatus(bookId, 1);
                System.out.println("Book '" + bookTitle + "' (ID: " + bookId + ") successfully borrowed by " + borrowerName + ".");
                System.out.println("Scheduled return date: " + scheduledReturnDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n");
                return true;
            } else {
                System.out.println("Borrow failed: Could not record borrowing in the database.");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("Error borrowing book: " + e.getMessage());
            return false;
        }
    }


    public boolean returnBook(int bookId) {
        if (bookId <= 0) {
            System.out.println("Return failed: Invalid Book ID. Must be a positive number.");
            return false;
        }

        // 1. Check if the book exists at all
        if (!bookFunction.bookExists(bookId)) {
            System.out.println("Return failed: Book with ID " + bookId + " does not exist in the library.");
            return false;
        }

        // 2. Check if the book is actually borrowed by looking for an active record in borrower_list
        String checkBorrowedQuery = "SELECT id, borrower_name, scheduled_return FROM borrower_list WHERE book_id = ? AND time_out IS NULL";
        int borrowerListEntryId = -1;
        String borrowerName = null;
        Timestamp scheduledReturnTimestamp = null;

        try (Connection connection = dbConnection.connect();
             PreparedStatement checkPrep = connection.prepareStatement(checkBorrowedQuery)) {

            checkPrep.setInt(1, bookId);
            ResultSet rs = checkPrep.executeQuery();
            if (rs.next()) {
                borrowerListEntryId = rs.getInt("id");
                borrowerName = rs.getString("borrower_name");
                scheduledReturnTimestamp = rs.getTimestamp("scheduled_return");
            } else {
                // If the book status is 1 but no active borrower_list entry, it's inconsistent data
                String[] bookDetails = bookFunction.getBookDetailsById(bookId);
                if (bookDetails != null && Integer.parseInt(bookDetails[4]) == 1) {
                    System.out.println("Book ID " + bookId + " is marked 'Borrowed' but no active borrower record found. Correcting status to 'Available'.");
                    bookFunction.updateBookStatus(bookId, 0); // Correct the status in booklist
                } else {
                    System.out.println("Return failed: Book ID " + bookId + " is not currently marked as borrowed or has already been returned.");
                }
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error checking borrowed status for return: " + e.getMessage());
            return false;
        }

        // Get current time for time_out
        LocalDateTime now = LocalDateTime.now();
        Timestamp timeOut = Timestamp.valueOf(now);

        // Calculate fine
        double fine = 0.0;
        if (scheduledReturnTimestamp != null) {
            fine = calculateFine(scheduledReturnTimestamp.toLocalDateTime().toLocalDate(), now.toLocalDate());
        }

        // 3. Update borrower_list with time_out
        String updateBorrowerQuery = "UPDATE borrower_list SET time_out = ? WHERE id = ?";
        try (Connection connection = dbConnection.connect();
             PreparedStatement updatePrep = connection.prepareStatement(updateBorrowerQuery)) {

            updatePrep.setTimestamp(1, timeOut);
            updatePrep.setInt(2, borrowerListEntryId);

            int rowsAffected = updatePrep.executeUpdate();
            if (rowsAffected > 0) {
                // 4. Update the book status to 'available' (0) in booklist
                bookFunction.updateBookStatus(bookId, 0);

                String bookTitle = bookFunction.getBookTitleById(bookId);
                System.out.println("Book '" + (bookTitle != null ? bookTitle : "ID " + bookId) + "' returned by " + borrowerName + ".");
                if (fine > 0) {
                    System.out.printf("Fine due: $%.2f\n", fine);
                } else {
                    System.out.println("No fine incurred.");
                }
                System.out.println();
                return true;
            } else {
                System.out.println("Return failed: Could not update borrower record.");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("Error returning book: " + e.getMessage());
            return false;
        }
    }

    
    // Displays a list of all currently borrowed books, including borrower details and due dates.
    
    public void displayBorrowedBooks() {
        // Updated query: Replaced bl.author with bl.title in the SELECT statement
        String query = "SELECT bl.id AS book_id, bl.title, bl.isbn, " + // Removed bl.author
                       "br.borrower_name, br.time_in, br.scheduled_return " +
                       "FROM booklist bl " +
                       "INNER JOIN borrower_list br ON bl.id = br.book_id " +
                       "WHERE bl.status = 1 AND br.time_out IS NULL"; 

        try (Connection connection = dbConnection.connect();
             Statement state = connection.createStatement();
             ResultSet result = state.executeQuery(query)) {

            System.out.println("\n--- Currently Borrowed Books ---");
            // Updated header: Changed "AUTHOR" to "TITLE" and adjusted column width if necessary
            System.out.printf("%-5s %-30s %-20s %-20s %-20s %-20s\n", // Adjusted width for TITLE
                              "ID", "TITLE", "BORROWER", "BORROW DATE", "DUE DATE", "OVERDUE FINE"); // Removed "AUTHOR"
            System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------------");

            boolean foundBorrowedBooks = false;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"); // For precise time display if needed

            while (result.next()) {
                foundBorrowedBooks = true;
                int bookId = result.getInt("book_id");
                String title = result.getString("title"); 
                String borrowerName = result.getString("borrower_name");
                Timestamp timeIn = result.getTimestamp("time_in");
                Timestamp scheduledReturn = result.getTimestamp("scheduled_return");

                LocalDate today = LocalDate.now();
                LocalDate scheduledReturnLocalDate = scheduledReturn.toLocalDateTime().toLocalDate();
                double fine = calculateFine(scheduledReturnLocalDate, today);
                String fineDisplay = (fine > 0) ? String.format("$%.2f", fine) : "No Fine";

                String borrowDateStr = timeIn.toLocalDateTime().format(formatter);
                String dueDateStr = scheduledReturn.toLocalDateTime().format(formatter);

                // Updated printf format string and arguments
                System.out.printf("%-5d %-30s %-20s %-20s %-20s %-20s\n",
                                  bookId, title, borrowerName, borrowDateStr, // Removed author from here
                                  dueDateStr, fineDisplay);
            }
            if (!foundBorrowedBooks) {
                System.out.println("No books are currently borrowed.");
            }
            System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------------\n");
        } catch (SQLException e) {
            System.out.println("Error displaying borrowed books: " + e.getMessage());
        }
    }

    // Calculates the fine for an overdue book.
    private double calculateFine(LocalDate scheduledReturnDate, LocalDate actualReturnDate) {
        if (actualReturnDate.isAfter(scheduledReturnDate)) {
            long daysOverdue = ChronoUnit.DAYS.between(scheduledReturnDate, actualReturnDate);
            return daysOverdue * 10.0; // $10 per day
        }
        return 0.0; // No fine if returned on or before due date
    }

    /**
     * Helper method to check if a book has an active borrowing record in borrower_list,
     * regardless of its status in booklist. Useful for consistency checks.
     */
    public boolean isBookCurrentlyBorrowedInBorrowerList(int bookId) {
        String query = "SELECT COUNT(*) FROM borrower_list WHERE book_id = ? AND time_out IS NULL";
        try (Connection connection = dbConnection.connect();
             PreparedStatement prep = connection.prepareStatement(query)) {
            prep.setInt(1, bookId);
            ResultSet rs = prep.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Error checking active borrower record: " + e.getMessage());
        }
        return false;
    }
}