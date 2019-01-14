import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        System.out.println("PEAKY BLINDERS Business Improver");
        System.out.println("What you want, eh?\n");

        int choice = getMenuChoice(
                "\t1. Show average winnings\n" +
                "\t2. Show possible cheaters\n" +
                "\t3. View popular boxing matches\n");

        switch (choice) {
            case 1:
                System.out.println("Choice is 1");

                int secondChoice = getMenuChoice(
                        "Show average winnings for:\n" +
                                "\t1. Event\n" +
                                "\t2. Day\n" +
                                "\t3. Show overall average\n"
                );

                switch (secondChoice) {
                    case 1:
                        String statement = "SELECT * FROM events;";

                        ArrayList results = doSQLStatement(statement);
                        System.out.println("resultset: " + results.toString());

                        break;
                    case 2:
                        System.out.println("day");
                        break;
                    case 3:
                        System.out.println("Overall average");
                        break;
                }
                break;
            case 2:
                System.out.println("Choice is 2"); break;
            case 3:
                System.out.println("Choice is 3"); break;
        }
    }

    private static ArrayList<String> doSQLStatement(String statement) {
        String dbLocation = "jdbc:sqlite:src/shelby_v1.db";

        try (Connection connection = DriverManager.getConnection(dbLocation);
             Statement stmt = connection.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(statement);
            ArrayList<String> results = new ArrayList<>();

            while (resultSet.next()) {
                results.add(resultSet.getString("event_id"));
            }

            return results;
        } catch (SQLException e) {
            e.printStackTrace();

            return null;
        }
    }

    private static int getMenuChoice(String questions) {
        System.out.println(questions);

        while (true) {
            Scanner reader = new Scanner(System.in);

            System.out.print("Make your choice: ");
            String line = reader.nextLine();

            try {
                return Integer.valueOf(line);
            } catch (NumberFormatException n) {
                System.out.println("\nNot a number, please enter the option number\n");
            }
        }
    }
}
