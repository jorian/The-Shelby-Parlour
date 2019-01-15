import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class App {
    @SuppressWarnings("Duplicates")

    private final static String dbLocation = "jdbc:sqlite:src/shelby_v1.db";

    public static void main(String[] args) {
        System.out.println("PEAKY BLINDERS Business Improver");
        System.out.println("What you want, eh?\n");

        while (true) {
            int choice = getMenuChoice(
                    "\n\n\n\n>>>>>> Main menu:\n\n" +
                            "\t1. Show profits\n" +
                            "\t2. Show possible cheaters\n" +
                            "\t3. View popular boxing matches\n");

            switch (choice) {

                // Average winnings
                // for an Event, Day or just overall average.
                case 1:
                    int secondChoice = getMenuChoice(
                            "\nShow profit for:\n" +
                                    "\t1. Event\n" +
                                    "\t2. Day\n" +
                                    "\t3. Show overall profit\n"
                    );

                    switch (secondChoice) {
                        case 1: {
                            String statement = "SELECT * FROM events;";
                            ArrayList results = doSQLStatement(statement);
                            String eventID = getEventIDInput("Enter an event ID: ", results);

                            // 1. get all stakes
                            statement = String.format(
                                    "SELECT stake " +
                                            "FROM wagers " +
                                            "WHERE event_id = '%s';", eventID
                            );

                            int allStakes = sumOfStake(statement);
                            System.out.println("All stakes: " + allStakes);

                            // 2. get all payouts
                            statement = String.format(
                                    "SELECT sum(stake) " +
                                            "FROM wagers w " +
                                            "INNER JOIN events e " +
                                            "ON w.event_id = e.event_id " +
                                            "AND e.event_id = '%s'" +
                                            "AND e.outcome = w.selection;", eventID
                            );

                            int payouts = aggregatedSumOfStake(statement);

                            System.out.println("All payouts: "+ payouts);
                            System.out.println("Profits: " + (allStakes - payouts));

                            break;
                        }
                        case 2: {
                            // Probably overkill, but this way we can check for a correct date:
                            LocalDate date = getDateInput();

                            String strdate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

                            // 1. get all stakes for that day
                            String statement = String.format("SELECT stake " +
                                    "FROM wagers " +
                                    "WHERE date_of_wager = '%s';", strdate
                            );

                            int stake = sumOfStake(statement);

                            // 2. get all payouts for that day
                            statement = String.format("SELECT sum(stake) " +
                                    "FROM wagers w " +
                                    "INNER JOIN events e " +
                                    "ON w.event_id = e.event_id " +
                                    "AND e.outcome = w.selection " +
                                    "AND w.date_of_wager = '%s';", strdate
                            );

                            int payouts = aggregatedSumOfStake(statement);

                            System.out.println("Profit: " + (stake - payouts));

                            break;
                        }
                        case 3: {

                            // 1. get all stakes
                            String statement = "SELECT stake FROM wagers;";

                            int totalStake = sumOfStake(statement);

                            // 2. get all payouts
                            statement = "SELECT sum(stake) " +
                                    "FROM wagers w " +
                                    "INNER JOIN events e " +
                                    "ON w.event_id = e.event_id " +
                                    "AND e.outcome = w.selection;";

                            int payouts = aggregatedSumOfStake(statement);

                            System.out.println("Total profits: " + (totalStake - payouts));
                            break;
                        }
                    }
                    break;
                case 2:
                    System.out.println("Find cheaters!");

                    try (Connection conn = DriverManager.getConnection(dbLocation); Statement stmt = conn.createStatement()) {
                        String statement = String.format("SELECT gambler_id " +
                                "FROM (" +
                                    "SELECT gambler_id, COUNT(gambler_id) AS count " +
                                    "FROM wagers w " +
                                    "INNER JOIN events e " +
                                    "ON w.event_id = e.event_id " +
                                    "AND e.outcome = w.selection " +
                                    "AND w.odds > 4 " +
                                    "GROUP BY gambler_id)" +
                                "WHERE count > 3;"
                        );

                        ResultSet resultSet = stmt.executeQuery(statement);

                        while (resultSet.next()) {
                            System.out.println("CHEATER ID: " + resultSet.getString("gambler_id"));
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }

                    break;
                case 3:
                    System.out.println("Popularity of matches");
                    break;
            }
        }
    }

    private static ArrayList<String> doSQLStatement(String statement) {
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

    private static String getEventIDInput(String questions, ArrayList<String> options) {
        System.out.println(options);

        while (true) {
            System.out.print("Make your choice: ");
            Scanner reader = new Scanner(System.in);
            String line = reader.nextLine();

            if (options.contains(line)) {

                return line;
            } else
                System.out.println("That event ID does not exist");
        }
    }

    private static LocalDate getDateInput() {
        while (true) {
            Scanner reader = new Scanner(System.in);

            System.out.print("\nEnter the date YYYY-MM-DD: ");
            String line = reader.nextLine();

            try {
                LocalDate date = LocalDate.parse(line);

                return date;
            } catch (DateTimeParseException dtpe) {
                System.out.println("Date not entered correctly, please try again and enter the date as YYYY-MM-DD");
            }
        }
    }

    private static int sumOfStake(String statement) {
        try (Connection connection = DriverManager.getConnection(dbLocation);
             Statement stmt = connection.createStatement()) {
            System.out.println(statement);

            ResultSet resultSet = stmt.executeQuery(statement);
            int sum = 0;
            int count = 0;
            while (resultSet.next()) {
                int stake = resultSet.getInt("stake");
                sum += stake;
                count++;
            }

            return sum;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int aggregatedSumOfStake(String statement) {
        try (Connection connection = DriverManager.getConnection(dbLocation);
             Statement stmt = connection.createStatement()) {
            System.out.println(statement);

            ResultSet resultSet = stmt.executeQuery(statement);

            return resultSet.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();

            return 0;
        }
    }
}