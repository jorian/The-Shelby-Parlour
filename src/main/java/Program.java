import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Objects;

public class Program {

    private static void createNewDatabase(String dbLocation) {
        try (Connection conn = DriverManager.getConnection(dbLocation)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void createTables(String dbLocation) {
        String contenderTable = "CREATE TABLE IF NOT EXISTS contenders(" +
                "contender_id VARCHAR(255) NOT NULL," +
                "name VARCHAR(255) NOT NULL, " +
                "confidence INTEGER NOT NULL," +
                "PRIMARY KEY (contender_id)" +
                ");";

        String eventTable = "CREATE TABLE IF NOT EXISTS events(" +
                "event_id VARCHAR(255) NOT NULL," +
                "outcome INTEGER NOT NULL," +
                "current_odds_contender_1 INTEGER NOT NULL," +
                "current_odds_contender_2 INTEGER NOT NULL," +
                "contender_1_id VARCHAR(255) NOT NULL," +
                "contender_2_id VARCHAR(255) NOT NULL," +
                "PRIMARY KEY(event_id)," +
                "FOREIGN KEY (contender_1_id) REFERENCES contenders(contender_id)," +
                "FOREIGN KEY (contender_2_id) REFERENCES contenders(contender_id)" +
                ");";

        String gamblerTable = "CREATE TABLE IF NOT EXISTS gamblers(" +
                "gambler_id VARCHAR(255) NOT NULL," +
                "name VARCHAR(255) NOT NULL," +
                "address VARCHAR(255) NOT NULL," +
                "PRIMARY KEY(gambler_id)" +
                ");";

        // TODO timestamps are weird, because thomas shelby lives in 1919 where no computers exists
        String wagerTable = "CREATE TABLE IF NOT EXISTS wagers(" +
                "wager_id VARCHAR(255) NOT NULL," +
                "event_id VARCHAR(255) NOT NULL," +
                "gambler_id VARCHAR(255) NOT NULL," +
                "odds INTEGER(255) NOT NULL," +
                "selection INTEGER NOT NULL," +
                "stake INTEGER NOT NULL," +
                "date_of_wager INTEGER NOT NULL," +
                "PRIMARY KEY (wager_id)," +
                "FOREIGN KEY (event_id) REFERENCES events(event_id)," +
                "FOREIGN KEY (gambler_id) REFERENCES gamblers(gambler_id)" +
                ");";

        try (Connection conn = DriverManager.getConnection(dbLocation); Statement stmt = conn.createStatement()) {
            stmt.execute(gamblerTable);
            stmt.execute(wagerTable);
            stmt.execute(eventTable);
            stmt.execute(contenderTable);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static int EventWins(String dbLocation, String eventId) {
        try (Connection conn = DriverManager.getConnection(dbLocation); Statement stmt = conn.createStatement();) {
            System.out.println("EVENT WINS: HOUSE WINNINGS");
            String strSelect;
            if (eventId != null)
                strSelect =
                        "SELECT wager_id, odds, gambler_id, stake " +
                                "FROM wagers " +
                                "WHERE event_id = \'" + eventId + "\' AND selection = " +
                                "(  SELECT selection " +
                                "FROM wagers AS w1, events " +
                                "WHERE events.event_id =  \'" + eventId + "\'   AND wagers.selection != events.outcome )" +
                                "ORDER BY stake; ";

            else strSelect =
                    "SELECT wager_id, odds, gambler_id, stake " +
                            "FROM wagers " +
                            "WHERE selection = " +
                            "(  SELECT selection " +
                            "FROM wagers AS w1, events AS e " +
                            "WHERE e.event_id =  w1.event_id  AND wagers.selection != e.outcome )" +
                            "ORDER BY stake; ";
            System.out.println("\nThe SQL query is: " + strSelect + "\n"); // Echo For debugging

            ResultSet rset = stmt.executeQuery(strSelect);
            //Process the ResultSet by scrolling the cursor forward via next().
            //For each row, retrieve the contents of the cells with getXxx(columnName).
            System.out.println("Wagers Dropped:");
            int MulaMade = 0;
            while (rset.next()) {   // Move the cursor to the next row, return false if no more row
                System.out.print("Booked Wager Nr°: " + rset.getString("wager_id"));
                System.out.print(", Player id: " + rset.getString("gambler_id"));
                MulaMade += rset.getInt("stake");
                System.out.println(", odds when bet was made by player: " + rset.getInt("odds"));
                System.out.println(", Stake due from player: " + rset.getInt("stake"));
            }
            System.out.println("\nTotal House earnings from event = " + MulaMade);
            return MulaMade;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    private static int EventLosses(String dbLocation, String eventId) {
        try (Connection conn = DriverManager.getConnection(dbLocation); Statement stmt = conn.createStatement();) {
            System.out.println("EVENT LOSSES: HOUSE PAYMENTS");
            String strSelect;
            if (eventId != null)
                strSelect =
                        "SELECT wager_id, odds, gambler_id, stake " +
                                "FROM wagers " +
                                "WHERE event_id = \'" + eventId + "\' AND selection = " +
                                "( SELECT selection " +
                                "FROM wagers , events " +
                                "WHERE events.event_id =  \'" + eventId + "\'   AND wagers.selection = events.outcome )" +
                                "ORDER BY stake; ";

            else strSelect =
                    "SELECT wager_id, odds, gambler_id, stake " +
                            "FROM wagers " +
                            "WHERE selection = " +
                            "( SELECT selection " +
                            "FROM wagers AS w1, events AS e " +
                            "WHERE e.event_id =  w1.event_id  AND w1.selection = e.outcome )" +
                            "ORDER BY stake; ";
            System.out.println("\nThe SQL query is: " + strSelect + "\n"); // Echo For debugging
            ResultSet rset = stmt.executeQuery(strSelect);
            System.out.println("Wagers to be fulfilled:");
            int loss = 0;
            while (rset.next()) {   // Move the cursor to the next row, return false if no more row
                System.out.print("Booked Wager Nr°: " + rset.getString("wager_id"));
                System.out.print(", Player id: " + rset.getString("gambler_id"));
                loss += rset.getInt("odds") * rset.getInt("stake");
                System.out.println(", odds when bet was made by player: " + rset.getInt("odds"));
                System.out.println(", AMount placed by player * odds " + rset.getInt("stake"));

            }
            System.out.println("\nTotal House earnings from event = " + loss);
            return loss;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    private static void parseJsonToDB(String dbLocation) throws SQLException, IOException {
        Connection conn = DriverManager.getConnection(dbLocation);
        conn.setAutoCommit(false); // disables the automatic updating and commits to the database so we can batch statements and significantly improve speed

        String sqlContender = "INSERT OR IGNORE INTO contenders(contender_id, name, confidence) VALUES(?,?,?)";
        PreparedStatement ppstmtContender = conn.prepareStatement(sqlContender);
        File contenders = new File("src/contenders.json");
        JsonArray jsonArray = new Gson().fromJson(new JsonReader(new FileReader(contenders)), JsonArray.class);
        for (JsonElement jsonElement : jsonArray) {
            JsonObject contender = (JsonObject) jsonElement;
            ppstmtContender.setString(1, contender.get("contender_id").getAsString());
            ppstmtContender.setString(2, contender.get("name").getAsString());
            ppstmtContender.setInt(3, contender.get("confidence").getAsInt());
            ppstmtContender.addBatch();
        }
        ppstmtContender.executeBatch();
        conn.commit();

        String line;
        JSONObject jsonObject;
        /*Insert into the events Table*/
        InputStream inputStream = new FileInputStream(new File("src/events.json"));
        Objects.requireNonNull(inputStream, "InputStream cannot be null");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String sqlEvent = "INSERT OR IGNORE INTO events(event_id, outcome, current_odds_contender_1, current_odds_contender_2, contender_1_id, contender_2_id)VALUES(?,?,?,?,?,?)";
        PreparedStatement ppstmtEvent = conn.prepareStatement(sqlEvent);
        while ((line = bufferedReader.readLine()) != null) {
            //System.out.println(line);
            jsonObject = new JSONObject(line);
            ppstmtEvent.setString(1, jsonObject.getString("event_id"));
            ppstmtEvent.setInt(2, jsonObject.getInt("outcome"));
            ppstmtEvent.setString(3, jsonObject.getString("contender_1_id"));
            ppstmtEvent.setInt(4, jsonObject.getInt("current_odds_contender_1"));
            ppstmtEvent.setString(5, jsonObject.getString("contender_2_id"));
            ppstmtEvent.setInt(6, jsonObject.getInt("current_odds_contender_2"));
            ppstmtEvent.addBatch();
        }
        ppstmtEvent.executeBatch();
        conn.commit();
        /*Insert into the gamblers Table*/
        inputStream = new FileInputStream(new File("src/gamblers.json"));
        Objects.requireNonNull(inputStream, "InputStream cannot be null");
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String sqlGambler = "INSERT OR IGNORE INTO gamblers(gambler_id, name, address) VALUES(?,?,?)";
        PreparedStatement ppstmtGambler = conn.prepareStatement(sqlGambler);
        while ((line = bufferedReader.readLine()) != null) {
            //System.out.println(line);
            jsonObject = new JSONObject(line);
            ppstmtGambler.setString(1, jsonObject.getString("gambler_id"));
            ppstmtGambler.setString(2, jsonObject.getString("name"));
            ppstmtGambler.setString(3, jsonObject.getString("address"));
            ppstmtGambler.addBatch();
        }
        ppstmtGambler.executeBatch();
        conn.commit();
        /*Insert into the wagers Table*/
        inputStream = new FileInputStream(new File("src/wagers.json"));
        Objects.requireNonNull(inputStream, "InputStream cannot be null");
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String sqlWager = "INSERT OR IGNORE INTO wagers(wager_id, event_id,gambler_id,odds,selection,stake,date_of_wager)VALUES(?,?,?,?,?,?,?)";
        PreparedStatement ppstmtWager = conn.prepareStatement(sqlWager);
        while ((line = bufferedReader.readLine()) != null) {
            //System.out.println(line);
            jsonObject = new JSONObject(line);
            ppstmtWager.setString(1, jsonObject.getString("wager_id"));
            ppstmtWager.setString(2, jsonObject.getString("event_id"));
            ppstmtWager.setString(3, jsonObject.getString("gambler_id"));
            ppstmtWager.setInt(4, jsonObject.getInt("odds"));
            ppstmtWager.setInt(5, jsonObject.getInt("selection"));
            ppstmtWager.setInt(6, jsonObject.getInt("stake"));
            ppstmtWager.setString(7, jsonObject.getString("date_of_wager"));
            ppstmtWager.addBatch();
        }
        ppstmtWager.executeBatch();
        conn.commit();
        conn.close();
    }

    private static String getAddress(String dbLocation, String gambler_id) {
        //query gambler table for address of id.
        return "";
    }

    private static void investigate(String dbLocation) {
        try (Connection conn = DriverManager.getConnection(dbLocation); Statement stmt = conn.createStatement();) {
            String strSelect =
                    "SELECT gambler_id " +
                            "FROM wagers " +
                            "WHERE selection = " +
                            "( SELECT selection " +
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
            System.out.println(myMap.toString());
            for (String k : myMap.keySet())
                if (myMap.get(k) > 3) {
                    System.out.println("CHEATER ID: " + k);
                    System.out.println("CHEATER INFO: " + getCredentials(dbLocation, k));
                }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    private static String getCredentials(String dbLocation, String k) {
        try (Connection conn = DriverManager.getConnection(dbLocation); Statement stmt = conn.createStatement();) {
            String strSelect = "SELECT name , address FROM gamblers WHERE gambler_id = \'" + k + "\' ;";
            ResultSet x = stmt.executeQuery(strSelect);
            while (x.next()) {
                System.out.println("NAME: " + x.getString("name"));
                System.out.println("ADDRESS: " + x.getString("address"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void main(String[] args) {
        try {
            //try { DataCreator.createWagers(); } catch (IOException e) { e.printStackTrace(); }
            String tableName = "shelby_v1.db";
            String dbLocation = "jdbc:sqlite:src/" + tableName;
            createNewDatabase(dbLocation);
            createTables(dbLocation);

            parseJsonToDB(dbLocation);

            int pureTokyo = EventWins(dbLocation, null) - EventLosses(dbLocation, null);
            System.out.println("=======================================");
            System.out.println("The Garrison pub's Output: ==> $$ " + pureTokyo);
            System.out.println("=======================================");
            if (pureTokyo <= 0) {
                System.err.println("=====================================");
                System.err.println("The Garrison pub's Output is negative ");
                System.err.println("FIX a match to recover losses ");
                System.err.println("=====================================");
            }
            System.out.println("==============================================================================");
            System.out.println("                 SUSPECTED CHEATERS ==> $$ send arthur to collect");
            System.out.println("==============================================================================");
            investigate(dbLocation);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}