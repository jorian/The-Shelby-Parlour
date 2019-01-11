import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.util.Random;

public class DataCreator {
    public static void createEvents() throws IOException {
        File contenders = new File("src/contenders.json");
        File events = new File("src/events.json");

        JsonArray jsonArray = new Gson().fromJson(new JsonReader(new FileReader(contenders)), JsonArray.class);

        Random rand = new Random();

        FileWriter fw = new FileWriter("src/events.json", true);
        BufferedWriter bfw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bfw);

        for (int i = 0; i < 25; i++) {
            int selection1 = rand.nextInt(28);
            int selection2;

            // 2nd contender cannot be the same as first contender:
            do {
                selection2 = rand.nextInt(28);

            } while (selection2 == selection1);

            // get them as JSON objects
            JsonObject contender1 = (JsonObject) jsonArray.get(selection1);
            JsonObject contender2 = (JsonObject) jsonArray.get(selection2);

            // now create an event in JSON
            JsonObject event = new JsonObject();
            event.addProperty("event_id", "ev00" + i);
            event.addProperty("contender_1_id", contender1.get("contender_id").getAsString());
            event.addProperty("current_odds_contender_1", rand.nextInt(10) + 2);
            event.addProperty("contender_2_id", contender2.get("contender_id").getAsString());
            event.addProperty("current_odds_contender_2", rand.nextInt(10) + 2);
            event.addProperty("outcome", rand.nextInt(1) + 1);

            out.println(event.toString());
        }
    }
}
