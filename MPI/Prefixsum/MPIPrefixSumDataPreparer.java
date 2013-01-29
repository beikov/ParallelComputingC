
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Christian Beikov
 */
public class MPIPrefixSumDataPreparer {

    private static class Data {

        private static final Pattern p = Pattern.compile("\\d+");
        Map<String, Map<String, Map<String, Double>>> matrix = new HashMap<>();
        List<Integer> headerList = new ArrayList<>();
        List<Integer> nodeList = new ArrayList<>();
        List<Integer> processList = new ArrayList<>();

        Data(BufferedReader br) throws IOException {
            String line;
            int nodes = 0, processes = 0, headerVal;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");

                if (parts.length == 1) {
                    Matcher m = p.matcher(line);
                    m.find();
                    nodes = Integer.parseInt(m.group());
                    m.find();
                    processes = Integer.parseInt(m.group());

                    if (!nodeList.contains(nodes)) {
                        nodeList.add(nodes);
                    }
                    if (!processList.contains(processes)) {
                        processList.add(processes);
                    }

                    if (!matrix.containsKey("" + nodes)) {
                        matrix.put("" + nodes, new HashMap<String, Map<String, Double>>());
                    }
                    if (!matrix.get("" + nodes).containsKey("" + processes)) {
                        matrix.get("" + nodes).put("" + processes, new HashMap<String, Double>());
                    }
                } else {
                    headerVal = Integer.parseInt(parts[1]);

                    if (headerVal % 1000000 != 0) {
                        headerVal += 1000000 - headerVal % 1000000;
                    }

                    matrix.get("" + nodes).get("" + processes).put("" + headerVal, Double.parseDouble(parts[2]));

                    if (!headerList.contains(headerVal)) {
                        headerList.add(headerVal);
                    }
                }

            }

            Collections.sort(headerList);
            Collections.sort(nodeList);
            Collections.sort(processList);
        }

        Data(Data... datas) {
            for (Data d : datas) {
                for (Integer h : d.headerList) {
                    if (!headerList.contains(h)) {
                        headerList.add(h);
                    }
                }
                for (Integer h : d.nodeList) {
                    if (!nodeList.contains(h)) {
                        nodeList.add(h);
                    }
                }
                for (Integer h : d.processList) {
                    if (!processList.contains(h)) {
                        processList.add(h);
                    }
                }

                for (Map.Entry<String, Map<String, Map<String, Double>>> entry : d.matrix.entrySet()) {
                    if (!matrix.containsKey(entry.getKey())) {
                        matrix.put(entry.getKey(), new HashMap<String, Map<String, Double>>());
                    }

                    for (Map.Entry<String, Map<String, Double>> nextEntry : entry.getValue().entrySet()) {
                        if (!matrix.get(entry.getKey()).containsKey(nextEntry.getKey())) {
                            matrix.get(entry.getKey()).put(nextEntry.getKey(), new HashMap<String, Double>());
                        }

                        for (Map.Entry<String, Double> lastEntry : nextEntry.getValue().entrySet()) {
                            if (!matrix.get(entry.getKey()).get(nextEntry.getKey()).containsKey(lastEntry.getKey())) {
                                matrix.get(entry.getKey()).get(nextEntry.getKey()).put(lastEntry.getKey(), lastEntry.getValue());
                            } else {
                                matrix.get(entry.getKey()).get(nextEntry.getKey()).put(lastEntry.getKey(), matrix.get(entry.getKey()).get(nextEntry.getKey()).get(lastEntry.getKey()) + lastEntry.getValue());
                            }
                        }
                    }
                }
            }

            Collections.sort(headerList);
            Collections.sort(nodeList);
            Collections.sort(processList);

            for (Map.Entry<String, Map<String, Map<String, Double>>> entry : matrix.entrySet()) {

                for (Map.Entry<String, Map<String, Double>> nextEntry : entry.getValue().entrySet()) {
                    for (Map.Entry<String, Double> lastEntry : nextEntry.getValue().entrySet()) {
                        lastEntry.setValue(lastEntry.getValue() / datas.length);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        DecimalFormat df = new DecimalFormat("#.######");

        try (BufferedReader br0 = new BufferedReader(new InputStreamReader(new FileInputStream("test1.csv")));
                BufferedReader br1 = new BufferedReader(new InputStreamReader(new FileInputStream("test2.csv")));
                BufferedReader br2 = new BufferedReader(new InputStreamReader(new FileInputStream("test3.csv")));
                FileWriter fw = new FileWriter("data.csv")) {
            Data d0 = new Data(br0);
            Data d1 = new Data(br1);
            Data d2 = new Data(br2);
            Data d = new Data(d0, d1, d2);

            fw.write("Nodes;Processes;");

            for (Integer i : d.headerList) {
                fw.write("" + i);
                fw.write(";");
            }

            fw.write('\n');

            for (Integer nodeHeader : d.nodeList) {
                for (Integer processHeader : d.processList) {
                    fw.write(nodeHeader + ";" + processHeader + ";");

                    for (Integer header : d.headerList) {
                        fw.write(df.format(d.matrix.get("" + nodeHeader).get("" + processHeader).get("" + header)));
                        fw.write(";");
                    }

                    fw.write('\n');
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }
}
