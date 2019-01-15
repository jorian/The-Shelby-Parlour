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
                                            "AND e.outcome <> w.selection;", eventID
                            );

                            int payouts = getSum(statement);

                            System.out.println("All payouts: "+ payouts);

                            System.out.println("Profits: " + (allStakes - payouts));

                            break;
                        }
                        case 2: {
                            // Probably overkill, but this way we can check for a correct date:
                            LocalDate date = getDateInput();

                            String strdate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

                            // 1. get all stakes for that day
                            String statement = String.format(
                                    "SELECT stake " +
                                            "FROM wagers " +
                                            "WHERE date_of_wager = '%s';", strdate
                            );

                            int stake = sumOfStake(statement);

                            // 2. get all payouts for that day
                            statement = "SELECT stake " +
                                    "FROM wagers " +
                                    "WHERE " +
                                    "selection = (SELECT selection " +
                                    "FROM wagers AS w1, events AS e " +
                                    "WHERE e.event_id = w1.event_id AND w1.selection = e.outcome) " +
                                    "AND date_of_wager = '" + strdate + "';";

                            System.out.println(statement);

                            int payouts = sumOfStake(statement);

                            System.out.println("Profit: " + (stake - payouts));

                            break;
                        }
                        case 3: {

                            String statement =
                                    "SELECT stake " +
                                            "FROM wagers;";

                            int totalStake = sumOfStake(statement);

                            statement = "SELECT stake " +
                                    "FROM wagers " +
                                    "WHERE " +
                                    "selection = (SELECT selection " +
                                    "FROM wagers AS w1, events AS e " +
                                    "WHERE e.event_id = w1.event_id AND w1.selection = e.outcome)" +
                                    ";";

                            int payouts = sumOfStake(statement);

                            System.out.println("Total profits: " + (totalStake - payouts));
                            break;
                        }
                    }
                    break;
                case 2:
                    System.out.println("Find cheaters!");

                    try (Connection conn = DriverManager.getConnection(dbLocation); Statement stmt = conn.createStatement()) {
                        String strSelect =
                                "SELECT gambler_id " +
                                        "FROM wagers " +
                                        "WHERE " +
                                        "selection = (SELECT selection " +
                                        "FROM wagers AS w1, events AS e " +
                                        "WHERE e.event_id =  w1.event_id  AND w1.selection = e.outcome ) " +
                                        "ORDER BY stake; ";

                        ResultSet x = stmt.executeQuery(strSelect);
                        HashMap<String, Integer> myMap = new HashMap<String, Integer>();

                        while (x.next()) {
                            int curVal;
                            if (myMap.containsKey(x.getString("gambler_id"))) {
                                curVal = myMap.get(x.getString("gambler_id"));
                                myMap.put(x.getString("gambler_id"), curVal + 1);
                            } else
                                myMap.put(x.getString("gambler_id"), 1);
                        }

                        for (String k : myMap.keySet())
                            if (myMap.get(k) >= 3) {
//                            System.out.println("CHEATER ID: " + k);

                                strSelect =
                                        "SELECT name, address FROM gamblers WHERE gambler_id = \'" + k + "\';";
                                ResultSet addresses = stmt.executeQuery(strSelect);
                                while (addresses.next()) {
                                    System.out.println(String.format("CHEATER ID: %s, NAME: %s, %s", k, addresses.getString("name"), addresses.getString("address")));
//                                System.out.println("CHEATER ADDRESS "+ addresses.getString("address"));
                                }
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
            Scanner reader = new Scanner(System.in);

            System.out.print("Make your choice: ");
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

    private static int getSum(String statement) {
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
