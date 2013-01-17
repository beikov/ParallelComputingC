
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Christian Beikov
 */
public class PrefixSumDataPreparer {

    public static void main(String[] args) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("data.csv")));
                FileWriter fw = new FileWriter("newData.csv")) {
            String line;
            Map<String, Map<String, String>> matrix = new HashMap<>();
            List<Integer> headerList = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");

                if (!matrix.containsKey(parts[0])) {
                    matrix.put(parts[0], new HashMap<String, String>());
                }

                matrix.get(parts[0]).put(parts[1], parts[2]);
            }

            fw.write(";");

            for (Map.Entry<String, Map<String, String>> entry : matrix.entrySet()) {
                headerList.add(Integer.parseInt(entry.getKey()));
            }

            Collections.sort(headerList);

            for (Integer i : headerList) {
                fw.write("" + i);
                fw.write(";");

            }

            fw.write('\n');

            for (int i = 1; i <= 54; i++) {
                fw.write("" + i + ";");

                for (Integer header : headerList) {
                    fw.write(matrix.get("" + header).get("" + i));
                    fw.write(";");
                }

                fw.write('\n');
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }
}
