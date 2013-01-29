
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Christian Beikov
 */
public class OpenMPMatrixVectorDataPreparer {

    private static List<Integer> BLOCK_SIZES = Arrays.asList(1, 5, 10, 25, 50, 100, 250, 500, 1000, 1500);

    static class Data {

        Map<String, Map<String, Double>> matrix = new HashMap<>();
        List<Integer> headerList = new ArrayList<>();

        Data(Data... datas) {
            for (Data d : datas) {
                for (Integer h : d.headerList) {
                    if (!headerList.contains(h)) {
                        headerList.add(h);
                    }
                }
                for (Map.Entry<String, Map<String, Double>> entry : d.matrix.entrySet()) {
                    if (!matrix.containsKey(entry.getKey())) {
                        matrix.put(entry.getKey(), new HashMap<String, Double>());
                    }

                    for (Map.Entry<String, Double> nextEntry : entry.getValue().entrySet()) {
                        if (!matrix.get(entry.getKey()).containsKey(nextEntry.getKey())) {
                            matrix.get(entry.getKey()).put(nextEntry.getKey(), nextEntry.getValue());
                        } else {
                            matrix.get(entry.getKey()).put(nextEntry.getKey(), matrix.get(entry.getKey()).get(nextEntry.getKey()) + nextEntry.getValue());
                        }
                    }
                }
            }

            for (Map.Entry<String, Map<String, Double>> entry : matrix.entrySet()) {

                for (Map.Entry<String, Double> nextEntry : entry.getValue().entrySet()) {
                    nextEntry.setValue(nextEntry.getValue() / datas.length);
                }
            }
        }

        Data(BufferedReader br, int blockSize) throws IOException {
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");

                if (Integer.parseInt(parts[2]) == blockSize) {
                    if (!matrix.containsKey(parts[0])) {
                        matrix.put(parts[0], new HashMap<String, Double>());
                    }

                    matrix.get(parts[0]).put(parts[1], Double.parseDouble(parts[3]));
                }
            }

            for (Map.Entry<String, Map<String, Double>> entry : matrix.entrySet()) {
                headerList.add(Integer.parseInt(entry.getKey()));
            }

            Collections.sort(headerList);
        }
    }

    public static void main(String[] args) {
        DecimalFormat df = new DecimalFormat("#.######");

        for (Integer blockSize : BLOCK_SIZES) {
            try (BufferedReader br0 = new BufferedReader(new InputStreamReader(new FileInputStream("test0.csv")));
                    BufferedReader br1 = new BufferedReader(new InputStreamReader(new FileInputStream("test1.csv")));
                    BufferedReader br2 = new BufferedReader(new InputStreamReader(new FileInputStream("test2.csv")));
                    FileWriter fw = new FileWriter("data-block-" + blockSize + ".csv")) {
                Data d0 = new Data(br0, blockSize);
                Data d1 = new Data(br1, blockSize);
                Data d2 = new Data(br2, blockSize);
                Data d = new Data(d0, d1, d2);

                fw.write(";");

                for (Integer i : d.headerList) {
                    fw.write("" + i);
                    fw.write(";");

                }

                fw.write('\n');

                for (Integer header2 : d.headerList) {
                    fw.write("" + header2 + ";");

                    for (Integer header : d.headerList) {
                        fw.write(df.format(d.matrix.get("" + header).get("" + header2)));
                        fw.write(";");
                    }
                    fw.write('\n');
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }
}
