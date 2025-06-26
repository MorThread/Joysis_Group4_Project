package com.joysis.library.main;

import com.joysis.library.functions.BookFunction;
import com.joysis.library.functions.BorrowerFunction;
import com.joysis.library.functions.UserFunction;
import com.joysis.library.util.DbConnection;

import java.util.InputMismatchException;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) {

        DbConnection dbConnection = new DbConnection();
        Scanner scanner = new Scanner(System.in);

        UserFunction userFunction = new UserFunction(dbConnection);
        BookFunction bookFunction = new BookFunction(dbConnection);
        BorrowerFunction borrowerFunction = new BorrowerFunction(dbConnection, bookFunction); // BorrowerFunction depends on BookFunction

        boolean loggedIn = false;
        String loggedInUsername = null;

        // --- Login / Registration Loop ---
        while (!loggedIn) {
            System.out.println("\n--- Library Book Management System ---");
            System.out.println("1. Login (Librarian)");
            System.out.println("2. Register New Librarian");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");

            int choice = -1;
            try {
                choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // Consume invalid input
                continue;
            }
            
            
            switch (choice) {
                case 1: // Log-in Function
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine();
                    System.out.print("Enter password: ");
                    String password = scanner.nextLine();

                    if (userFunction.loginUser(username, password)) {
                        loggedIn = true;
                        loggedInUsername = username;
                        System.out.println("Welcome, " + loggedInUsername + "!");
                    }
                    break;
                case 2: //Register Function
                    System.out.print("Enter desired new username for librarian: ");
                    String newUsername = scanner.nextLine();
                    System.out.print("Enter desired password for new librarian: ");
                    String newPassword = scanner.nextLine();

                    userFunction.registerUser(newUsername, newPassword);
                    break;
                case 3:
                    System.out.println("Exiting Library System. Goodbye!");
                    scanner.close();
                    return; // Exit the application
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }

        // --- Main Librarian Menu Loop ---
        while (loggedIn) {
            System.out.println("\n--- Librarian Main Menu ---");
            System.out.println("Logged in as: " + loggedInUsername);
            System.out.println("1. Manage Books (Add, Update, Delete, Search, Display All)");
            System.out.println("2. Borrow Book");
            System.out.println("3. Return Book");
            System.out.println("4. View Currently Borrowed Books");
            System.out.println("5. Logout");
            System.out.print("Enter your choice: ");

            int mainMenuChoice = -1;
            try {
                mainMenuChoice = scanner.nextInt();
                scanner.nextLine(); // Consume newline
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // Consume invalid input
                continue;
            }

            switch (mainMenuChoice) {
                case 1: // Manage Books
                    handleBookManagement(scanner, bookFunction);
                    break;
                case 2: // Borrow Book
                    handleBorrowBook(scanner, borrowerFunction, bookFunction); // Pass bookFunction for title check
                    break;
                case 3: // Return Book
                    handleReturnBook(scanner, borrowerFunction, bookFunction); // Pass bookFunction for title check
                    break;
                case 4: // View Currently Borrowed Books
                    borrowerFunction.displayBorrowedBooks();
                    break;
                case 5: // Logout
                    loggedIn = false;
                    loggedInUsername = null;
                    System.out.println("Logged out successfully.");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }

        scanner.close(); 
    }

    // --- Helper Methods for Menu Choices ---

    private static void handleBookManagement(Scanner scanner, BookFunction bookFunction) {
        boolean managingBooks = true;
        while (managingBooks) {
            System.out.println("\n--- Book Management ---");
            System.out.println("1. Add New Book");
            System.out.println("2. Update Book Details");
            System.out.println("3. Delete Book");
            System.out.println("4. Search Book by Title");
            System.out.println("5. Display All Books");
            System.out.println("6. Back to Main Menu");
            System.out.print("Enter your choice: ");

            int choice = -1;
            try {
                choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // Consume invalid input
                continue;
            }

            switch (choice) {
                case 1: // Add New Book
                    System.out.println("--- Add New Book (Type 'cancel' at any prompt to abort) ---");
                    System.out.print("Enter book title: ");
                    String title = scanner.nextLine();
                    if (title.equalsIgnoreCase("cancel")) {
                        System.out.println("Add book cancelled.");
                        break;
                    }

                    System.out.print("Enter author name: ");
                    String author = scanner.nextLine();
                    if (author.equalsIgnoreCase("cancel")) {
                        System.out.println("Add book cancelled.");
                        break;
                    }

                    int year = 0;
                    while(true) {
                        try {
                            System.out.print("Enter publication year (Type 'cancel' to abort): ");
                            String yearInput = scanner.nextLine();
                            if (yearInput.equalsIgnoreCase("cancel")) {
                                System.out.println("Add book cancelled.");
                                return; 
                            }
                            year = Integer.parseInt(yearInput);
                            break;
                        } catch (InputMismatchException | NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a valid year (number).");
                        }
                    }
                    
                    System.out.print("Enter ISBN (Type 'cancel' to abort): ");
                    String isbn = scanner.nextLine();
                    if (isbn.equalsIgnoreCase("cancel")) {
                        System.out.println("Add book cancelled.");
                        break;
                    }

                    bookFunction.addBook(title, author, year, isbn);
                    break;
                case 2: // Update Book Details
                    int updateId = 0;
                    while(true) {
                        try {
                            System.out.print("Enter Book ID to update (0 to cancel): ");
                            updateId = scanner.nextInt();
                            scanner.nextLine(); // Consume newline
                            if (updateId == 0) {
                                System.out.println("Book update cancelled.");
                                break;
                            }
                            if (!bookFunction.bookExists(updateId)) {
                                System.out.println("Book with ID " + updateId + " does not exist.");
                                continue;
                            }
                            break;
                        } catch (InputMismatchException e) {
                            System.out.println("Invalid input. Please enter a valid Book ID (number).");
                            scanner.nextLine();
                        }
                    }
                    if (updateId == 0) {
                        break;
                    }
                    
                    String[] currentDetails = bookFunction.getBookDetailsById(updateId);
                    String currentTitle = currentDetails != null ? currentDetails[0] : "";
                    String currentAuthor = currentDetails != null ? currentDetails[1] : "";
                    int currentYear = currentDetails != null ? Integer.parseInt(currentDetails[2]) : 0;
                    String currentISBN = currentDetails != null ? currentDetails[3] : "";

                    System.out.print("Enter new title (Current: " + currentTitle + ", press Enter to keep current): ");
                    String newTitle = scanner.nextLine();
                    newTitle = newTitle.isEmpty() ? currentTitle : newTitle;

                    System.out.print("Enter new author (Current: " + currentAuthor + ", press Enter to keep current): ");
                    String newAuthor = scanner.nextLine();
                    newAuthor = newAuthor.isEmpty() ? currentAuthor : newAuthor;

                    int newYear = currentYear;
                    while(true) {
                        System.out.print("Enter new publication year (Current: " + currentYear + ", 0 to keep current, or a new number): ");
                        String yearInput = scanner.nextLine();
                        if (yearInput.isEmpty() || yearInput.equals("0")) {
                            newYear = currentYear;
                            break;
                        } else {
                            try {
                                newYear = Integer.parseInt(yearInput);
                                break;
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input. Please enter a number or 0.");
                            }
                        }
                    }
                    
                    System.out.print("Enter new ISBN (Current: " + currentISBN + ", press Enter to keep current): ");
                    String newISBN = scanner.nextLine();
                    newISBN = newISBN.isEmpty() ? currentISBN : newISBN;
                    
                    bookFunction.updateBook(updateId, newTitle, newAuthor, newYear, newISBN);
                    break;
                case 3: // Delete Book
                    int deleteId = 0;
                    while(true) {
                        try {
                            System.out.print("Enter Book ID to delete (0 to cancel): ");
                            deleteId = scanner.nextInt();
                            scanner.nextLine();
                            if (deleteId == 0) {
                                System.out.println("Book deletion cancelled.");
                                break;
                            }
                            break;
                        } catch (InputMismatchException e) {
                            System.out.println("Invalid input. Please enter a valid Book ID (number).");
                            scanner.nextLine();
                        }
                    }
                    if (deleteId == 0) {
                        break;
                    }
                    bookFunction.deleteBook(deleteId);
                    break;
                case 4: // Search Book
                    System.out.print("Enter keyword to search for book title: ");
                    String searchKeyword = scanner.nextLine();
                    bookFunction.searchBookByTitle(searchKeyword);
                    break;
                case 5: // Display All Books
                    bookFunction.displayAllBooks();
                    break;
                case 6: // Back
                    managingBooks = false;
                    System.out.println("Returning to Main Menu.");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void handleBorrowBook(Scanner scanner, BorrowerFunction borrowerFunction, BookFunction bookFunction) {
        int bookId = 0;
        String bookTitle = null;

        while(true) {
            try {
                System.out.print("Enter Book ID to borrow (Enter 0 to cancel): ");
                bookId = scanner.nextInt();
                scanner.nextLine();

                if (bookId == 0) {
                    System.out.println("Borrow process cancelled.");
                    return;
                }

                bookTitle = bookFunction.getBookTitleById(bookId);
                if (bookTitle == null) {
                    System.out.println("Book with ID " + bookId + " does not exist. Please try again.");
                    continue;
                }
                
                System.out.println("Selected Book: \"" + bookTitle + "\" (ID: " + bookId + ")");
                break;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid Book ID (number).");
                scanner.nextLine();
            }
        }
        
        System.out.print("Enter Borrower's Name (Student Name): ");
        String borrowerName = scanner.nextLine();
        
        if (borrowerName.trim().isEmpty()) {
            System.out.println("Borrow process cancelled: Borrower name cannot be empty.");
            return;
        }

        int scheduledDays = 0;
        while(true) {
            try {
                System.out.print("Enter scheduled return days from today (e.g., 7 for 1 week, Enter 0 to cancel): ");
                scheduledDays = scanner.nextInt();
                scanner.nextLine();
                
                if (scheduledDays == 0) {
                    System.out.println("Borrow process cancelled.");
                    return;
                }

                if (scheduledDays < 0) {
                    System.out.println("Days must be a positive number. Please try again.");
                    continue;
                }
                break;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number for days.");
                scanner.nextLine();
            }
        }
        borrowerFunction.borrowBook(bookId, borrowerName, scheduledDays);
    }

    private static void handleReturnBook(Scanner scanner, BorrowerFunction borrowerFunction, BookFunction bookFunction) {
        int bookId = 0;
        String bookTitle = null;

        while(true) {
            try {
                System.out.print("Enter Book ID to return (Enter 0 to cancel): ");
                bookId = scanner.nextInt();
                scanner.nextLine();

                if (bookId == 0) {
                    System.out.println("Return process cancelled.");
                    return;
                }
                
                bookTitle = bookFunction.getBookTitleById(bookId);
                if (bookTitle == null) {
                    System.out.println("Book with ID " + bookId + " does not exist. Please try again.");
                    continue;
                }
                
                System.out.println("Selected Book: \"" + bookTitle + "\" (ID: " + bookId + ")");
                break;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid Book ID (number).");
                scanner.nextLine();
            }
        }
        borrowerFunction.returnBook(bookId);
    }
}