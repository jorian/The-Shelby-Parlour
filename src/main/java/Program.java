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
        } catch (SQLException e) { System.out.println(e.getMessage()); }
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
                "time_stamp INTEGER NOT NULL," +
                "PRIMARY KEY (wager_id)," +
                "FOREIGN KEY (event_id) REFERENCES events(event_id)," +
                "FOREIGN KEY (gambler_id) REFERENCES gamblers(gambler_id)" +
                ");";

        try (Connection conn = DriverManager.getConnection(dbLocation);
             Statement stmt = conn.createStatement()) {

            stmt.execute(gamblerTable);
            stmt.execute(wagerTable);
            stmt.execute(eventTable);
            stmt.execute(contenderTable);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    private static void parseJsonToDB(String dbLocation, InputStream inputStream) throws SQLException, IOException {
        final Connection conn = DriverManager.getConnection(dbLocation);
        Objects.requireNonNull(inputStream, "InputStream cannot be null");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        long currentTime = System.currentTimeMillis();

//        the following GREATLY speeds up importing the data: 23 seconds against 120 minutes
//        conn.prepareStatement("PRAGMA synchronous = OFF").execute();
        conn.setAutoCommit(false);

        String line = null;
        JSONObject jsonObject = null;
        int i = 0;

        String sqlContender = "INSERT INTO contenders(contender_id, name, confidence) VALUES(?,?,?)";
//        String sqlEvent = "INSERT OR IGNORE INTO events(event_id, outcome, current_odds_contender_1, current_odds_contender_2, contender_1_id, contender_2_id)VALUES(?,?,?,?,?,?)";
//        String sqlGambler = "INSERT OR IGNORE INTO gamblers(gambler_id, name, address) VALUES(?,?,?)";
//        String sqlWager = "INSERT OR IGNORE INTO wagers(wager_id, event_id,gambler_id,odds,selection,stake,time_stamp)VALUES(?,?,?,?,?,?,?)";

        PreparedStatement ppstmtContender = conn.prepareStatement(sqlContender);
//        PreparedStatement ppstmtEvent = conn.prepareStatement(sqlEvent);
//        PreparedStatement ppstmtGambler = conn.prepareStatement(sqlGambler);
//        PreparedStatement ppstmtWager = conn.prepareStatement(sqlWager);

        while ((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
            jsonObject = new JSONObject(line);

            // TODO JSON imports
            ppstmtContender.setString(1, jsonObject.getString("contender_id"));
            System.out.println(jsonObject.getString("contender_id"));
            ppstmtContender.setString(2, jsonObject.getString("name"));
            ppstmtContender.setInt(3, jsonObject.getInt("confidence"));
            // ... and more

            ppstmtContender.addBatch();
//            ppstmtEvent.addBatch();
//            ppstmtGambler.addBatch();
//            ppstmtWager.addBatch();

//            if (i++ % 10000 == 0) {
//                ppstmtContender.addBatch();
////                ppstmtEvent.addBatch();
////                ppstmtGambler.addBatch();
////                ppstmtWager.addBatch();
//
//                if (i % 100000 == 0)
//                    conn.commit();
//                System.out.printf("\nbatch %d executed.", (i / 10000));
//            }
        }

//        if (i % 10000 != 0) {
//            ppstmtContender.addBatch();
////            ppstmtEvent.addBatch();
////            ppstmtGambler.addBatch();
////            ppstmtWager.addBatch();
//        }

        ppstmtContender.executeBatch();
//        ppstmtEvent.executeBatch();
//        ppstmtGambler.executeBatch();
//        ppstmtWager.executeBatch();
        conn.commit();
        conn.close();
        System.out.println("\nTotal time in seconds: " + (System.currentTimeMillis() - currentTime) / 1000);
    }

    public static void main(String[] args) {
        String tableName = "shelby_v1.db";
        String dbLocation = "jdbc:sqlite:src/" + tableName;

        createNewDatabase(dbLocation);
        createTables(dbLocation);

        try {
            parseJsonToDB(dbLocation, new FileInputStream(new File("src/contenders.json")));
//            parseJsonToDB(dbLocation, new FileInputStream(new File("src/events.json")));
//            parseJsonToDB(dbLocation, new FileInputStream(new File("src/gamblers.json")));
//            parseJsonToDB(dbLocation, new FileInputStream(new File("src/wagers.json")));
        } catch (SQLException | IOException e) { e.printStackTrace(); }
    }
}