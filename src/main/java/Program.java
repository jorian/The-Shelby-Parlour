import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
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

    private static void fetchQueries(String dbLocation) {
        try (Connection conn = DriverManager.getConnection(dbLocation); Statement stmt = conn.createStatement();) {
            String strSelect =
                    "SELECT wager_id " +
                    "FROM wagers As w " +
                    "WHERE (w.event_id = 'ev001' AND w.selection <> " +
                            "(SELECT outcome " +
                            "FROM events " +
                            "WHERE events.event_id = w.event_id)); ";
            System.out.println("\nThe SQL query is: " + strSelect + "\n"); // Echo For debugging

            ResultSet rset = stmt.executeQuery(strSelect);
            //Process the ResultSet by scrolling the cursor forward via next().
            //For each row, retrieve the contents of the cells with getXxx(columnName).
            System.out.println("The wagers that lost:");
            int rowCount = 0;
            while (rset.next()) {   // Move the cursor to the next row, return false if no more row
                String id = rset.getString("wager_id");
                System.out.println(id);
                ++rowCount;
            }
            System.out.println("Total number of wagers = " + rowCount);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /*
    For every json, a new inputStream needs to be created. Then, based on that inputstream, a BufferedReader is created, to
    be able to read the file line by line.
    In the while loop, all data from a json file gets converted to a PreparedStatement, which is then batched en masse into the specified tables.
    Since the schema is different for each table, we need 4 different while loops.
     */
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
            System.out.println(line);
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
            System.out.println(line);
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
            System.out.println(line);
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

    public static void main(String[] args) {
//         try {
//             DataCreator.createEvents();
//         } catch (IOException e) {
//             e.printStackTrace();
//         }

        String tableName = "shelby_v1.db";
        String dbLocation = "jdbc:sqlite:src/" + tableName;

        createNewDatabase(dbLocation);
        createTables(dbLocation);
        try {
            parseJsonToDB(dbLocation);
            fetchQueries(dbLocation);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

    }
}