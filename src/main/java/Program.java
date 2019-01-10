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

    @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
    public static void createTables(String dbLocation) {

        String gamblerTable = "CREATE TABLE IF NOT EXISTS gamblers(" +
                "gambler_id VARCHAR(255) NOT NULL," +
                "name VARCHAR(255) NOT NULL," +
                "address VARCHAR(255) NOT NULL," +
                "PRIMARY KEY(gambler_id)" +
                ");";

        String wagerTable = "CREATE TABLE IF NOT EXISTS wagers(" +
                "wager_id VARCHAR(255) NOT NULL," +
                "event_id VARCHAR(255) NOT NULL," +
                "gambler_id VARCHAR(255) NOT NULL," +
                "odds VARCHAR(255) NOT NULL," +
                "selection INTEGER NOT NULL," +
                "stake INTEGER NOT NULL," +
                "time_stamp TIMESTAMP NOT NULL," +
                "PRIMARY KEY(wager_id)," +
                "FOREIGN KEY (event_id) REFERENCES events(event_id)" +
                "FOREIGN KEY (gambler_id) REFERENCES gamblers(gambler_id)" +
                ");";

        String eventTable = "CREATE TABLE IF NOT EXISTS events(" +
                "event_id VARCHAR(255) NOT NULL," +
                "outcome VARCHAR(255) NOT NULL," +
                "current_odds_contender_1 VARCHAR(255) NOT NULL," +
                "current_odds_contender_2 VARCHAR(255) NOT NULL," +
                "contender_1_id VARCHAR(255) NOT NULL," +
                "contender_2_id VARCHAR(255) NOT NULL," +
                "PRIMARY KEY(event_id)" +
                "FOREIGN KEY (contender_1_id) REFERENCES contenders(contender_id)" +
                "FOREIGN KEY (contender_2_id) REFERENCES contenders(contender_id)" +
                ");";

        String contenderTable = "CREATE TABLE IF NOT EXISTS contenders(" +
                "contender_id VARCHAR(255) NOT NULL," +
                "name VARCHAR(255) NOT NULL, " +
                "confidence INTEGER NOT NULL," +
                "PRIMARY KEY (contender_id)," +
                ");";

        try (Connection conn = DriverManager.getConnection(dbLocation);
             Statement stmt = conn.createStatement()) {

            // todo: creates a new table
            stmt.execute(gamblerTable);
            stmt.execute(wagerTable);
            stmt.execute(eventTable);
            stmt.execute(contenderTable);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

//    private static void parseJsonToDB(String dbLocation, InputStream inputStream) throws SQLException, IOException {
//        final Connection conn = DriverManager.getConnection(dbLocation);
//        Objects.requireNonNull(inputStream, "InputStream cannot be null");
//        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
//
//        long currentTime = System.currentTimeMillis();
//
////        the following GREATLY speeds up importing the data: 23 seconds against 120 minutes
//        conn.prepareStatement("PRAGMA synchronous = OFF").execute();
////        conn.prepareStatement("PRAGMA journal_mode = WAL").execute();
//        conn.setAutoCommit(false);
//
//        String line = null;
//        JSONObject jsonObject = null;
//        int i = 0;
//
//        String subsSql = "INSERT OR IGNORE INTO subs(subreddit_id, subreddit) VALUES(?,?)";
//        String usersSql = "INSERT OR IGNORE INTO users(id, author) VALUES(?,?)";
//        String postsSql = "INSERT OR IGNORE INTO posts(parent_id, score, created_utc, link_id, body, name, author, subreddit) VALUES(?,?,?,?,?,?,?,?)";
//
//        PreparedStatement ppstmtSubs = conn.prepareStatement(subsSql);
//        PreparedStatement ppstmtUsers = conn.prepareStatement(usersSql);
//        PreparedStatement ppstmtPosts = conn.prepareStatement(postsSql);
//
//        while ((line = bufferedReader.readLine()) != null) {
//            jsonObject = new JSONObject(line);
//
//            ppstmtSubs.setString(1, jsonObject.getString("subreddit_id"));
//            ppstmtSubs.setString(2, jsonObject.getString("subreddit"));
//
//            ppstmtUsers.setString(1, jsonObject.getString("id"));
//            ppstmtUsers.setString(2, jsonObject.getString("author"));
//
//            ppstmtPosts.setString(1, jsonObject.getString("parent_id"));
//            ppstmtPosts.setInt(2, jsonObject.getInt("score"));
//            ppstmtPosts.setInt(3, jsonObject.getInt("created_utc"));
//            ppstmtPosts.setString(4, jsonObject.getString("link_id"));
//            ppstmtPosts.setString(5, jsonObject.getString("body"));
//            ppstmtPosts.setString(6, jsonObject.getString("name"));
//            ppstmtPosts.setString(7, jsonObject.getString("author"));
//            ppstmtPosts.setString(8, jsonObject.getString("subreddit"));
//
//            ppstmtSubs .addBatch();
//            ppstmtUsers.addBatch();
//            ppstmtPosts.addBatch();
//
//            if (i++ % 10000 == 0) {
//                ppstmtSubs.executeBatch();
//                ppstmtUsers.executeBatch();
//                ppstmtPosts.executeBatch();
//                if (i % 100000 == 0)
//                    conn.commit();
//                System.out.printf("\nbatch %d executed.", (i / 10000));
//            }
//        }
//
//        if (i % 10000 != 0) {
//            ppstmtSubs.executeBatch();
//            ppstmtUsers.executeBatch();
//            ppstmtPosts.executeBatch();
//            conn.commit();
//        }
//
//        conn.commit();
//
//        System.out.println("\nTotal time in seconds: " + (System.currentTimeMillis() - currentTime) / 1000);
//    }

    public static void main(String[] args) {
        String tableName = "shelby.db";
//
        String dbLocation = "jdbc:sqlite:/home/n41r0j/" + tableName;

        // TODO: UNCOMMENT THIS to create a new database:
        createNewDatabase(dbLocation);
        createTables(dbLocation);
//        try {
//            parseJsonToDB(dbLocation, new FileInputStream(new File("/home/n41r0j/Downloads/RC_2012-12")));
////            parseJsonToDB(dbLocation, new FileInputStream(new File("/home/n41r0j/RC_2007-10")));
//        } catch (SQLException | IOException e) { e.printStackTrace(); }
    }
}