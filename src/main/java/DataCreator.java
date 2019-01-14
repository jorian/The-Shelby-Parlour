import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.Random;

public class DataCreator {

    // Todo:when contender 1 has high odds, contender 2 has to have lower odds.

    public static void createEvents() throws IOException {
        File contenders = new File("src/contenders.json");
        JsonArray jsonArray = new Gson().fromJson(new JsonReader(new FileReader(contenders)), JsonArray.class);
        Random rand = new Random();

        for (int i = 0; i < 25; i++) {
            // there are 29 contenders
            int selection1 = rand.nextInt(28);
            int selection2;

            // 2nd contender cannot be the same as first contender:
            do {
                selection2 = rand.nextInt(28);
            } while (selection2 == selection1);

            // get them as JSON objects
            JsonObject contender1 = (JsonObject) jsonArray.get(selection1);
            JsonObject contender2 = (JsonObject) jsonArray.get(selection2);

            // odds need to make sense: when contender 1 has high odds, contender 2 needs to have lower odds
            int odds1 = rand.nextInt(6);
            int odds2 = rand.nextInt(6);

            // now create an event in JSON
            JsonObject event = new JsonObject();
            event.addProperty("event_id", "ev00" + i);
            event.addProperty("contender_1_id", contender1.get("contender_id").getAsString());
            event.addProperty("current_odds_contender_1", odds1);
            event.addProperty("contender_2_id", contender2.get("contender_id").getAsString());
            event.addProperty("current_odds_contender_2", odds2);
            event.addProperty("outcome", rand.nextInt(2) + 1);

            Files.write(Paths.get("src/events.json"), (event.toString() + "\n").getBytes(), StandardOpenOption.APPEND);
        }
    }
    public static void createWagers() throws IOException {
        File wagers = new File("src/wagers.json");
        Random rand = new Random();
        for (int i = 19; i < 40; i++) {
            JsonObject bet = new JsonObject();
            bet.addProperty("wager_id", "w" + i);
            bet.addProperty("event_id", "ev00"+(rand.nextInt(10)+3));
            bet.addProperty("gambler_id", "g00000"+rand.nextInt(32));
            bet.addProperty("odds", rand.nextInt(6)+1);
            bet.addProperty("selection", rand.nextInt(2)+1);
            bet.addProperty("stake", rand.nextInt(10000) + 5);
            bet.addProperty("date_of_wager", String.format("%tF", new Date()));

            Files.write(Paths.get("src/wagers.json"), (bet.toString() + "\n").getBytes(), StandardOpenOption.APPEND);
        }
    }
}