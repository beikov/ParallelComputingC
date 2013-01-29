
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
public class MPIMatrixVectorDataPreparer {

    private static class Data {

        private static final Pattern p = Pattern.compile("\\d+");
        Map<String, Map<String, Map<String, Map<String, Double>>>> matrix = new HashMap<>();
        List<Integer> headerList = new ArrayList<>();
        List<Integer> nodeList = new ArrayList<>();
        List<Integer> processList = new ArrayList<>();

        Data(BufferedReader br) throws IOException {
            String line;
            int nodes = 0, processes = 0, rowVal, colVal, target;

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
                        matrix.put("" + nodes, new HashMap<String, Map<String, Map<String, Double>>>());
                    }
                    if (!matrix.get("" + nodes).containsKey("" + processes)) {
                        matrix.get("" + nodes).put("" + processes, new HashMap<String, Map<String, Double>>());
                    }
                } else {
                    rowVal = Integer.parseInt(parts[1]);
                    
                    if(rowVal == 0){
                        continue;
                    }

                    if(rowVal <= 100){
                        target = 100;
                    } else if(rowVal <= 500){
                        target = 500;
                    } else if(rowVal <= 1000){
                        target = 1000;
                    } else {
                        target = 5000;
                    }
                    
                    if (rowVal % target != 0) {
                        rowVal += target - rowVal % target;
                    }
                    colVal = Integer.parseInt(parts[2]);
                    
                    if(colVal == 0){
                        continue;
                    }

                    if(colVal <= 100){
                        target = 100;
                    } else if(colVal <= 500){
                        target = 500;
                    } else if(colVal <= 1000){
                        target = 1000;
                    } else {
                        target = 5000;
                    }
                    
                    if (colVal % target != 0) {
                        colVal += target - colVal % target;
                    }

                    if (!matrix.get("" + nodes).get("" + processes).containsKey("" + rowVal)) {
                        matrix.get("" + nodes).get("" + processes).put("" + rowVal, new HashMap<String, Double>());
                    }

                    matrix.get("" + nodes).get("" + processes).get("" + rowVal).put("" + colVal, Double.parseDouble(parts[3]));

                    if (!headerList.contains(rowVal)) {
                        headerList.add(rowVal);
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

                for (Map.Entry<String, Map<String, Map<String, Map<String, Double>>>> entry : d.matrix.entrySet()) {
                    if (!matrix.containsKey(entry.getKey())) {
                        matrix.put(entry.getKey(), new HashMap<String, Map<String, Map<String, Double>>>());
                    }

                    for (Map.Entry<String, Map<String, Map<String, Double>>> nextEntry : entry.getValue().entrySet()) {
                        if (!matrix.get(entry.getKey()).containsKey(nextEntry.getKey())) {
                            matrix.get(entry.getKey()).put(nextEntry.getKey(), new HashMap<String, Map<String, Double>>());
                        }

                        for (Map.Entry<String, Map<String, Double>> lastEntry : nextEntry.getValue().entrySet()) {
                            if (!matrix.get(entry.getKey()).get(nextEntry.getKey()).containsKey(lastEntry.getKey())) {
                                matrix.get(entry.getKey()).get(nextEntry.getKey()).put(lastEntry.getKey(), lastEntry.getValue());
                            }

                            for (Map.Entry<String, Double> veryLastEntry : lastEntry.getValue().entrySet()) {
                                if (!matrix.get(entry.getKey()).get(nextEntry.getKey()).get(lastEntry.getKey()).containsKey(veryLastEntry.getKey())) {
                                    matrix.get(entry.getKey()).get(nextEntry.getKey()).get(lastEntry.getKey()).put(lastEntry.getKey(), veryLastEntry.getValue());
                                } else {
                                    matrix.get(entry.getKey()).get(nextEntry.getKey()).get(lastEntry.getKey()).put(veryLastEntry.getKey(), matrix.get(entry.getKey()).get(nextEntry.getKey()).get(lastEntry.getKey()).get(veryLastEntry.getKey()) + veryLastEntry.getValue());
                                }
                            }
                        }
                    }
                }
            }

            Collections.sort(headerList);
            Collections.sort(nodeList);
            Collections.sort(processList);

            for (Map.Entry<String, Map<String, Map<String, Map<String, Double>>>> entry : matrix.entrySet()) {
                for (Map.Entry<String, Map<String, Map<String, Double>>> nextEntry : entry.getValue().entrySet()) {
                    for (Map.Entry<String, Map<String, Double>> lastEntry : nextEntry.getValue().entrySet()) {
                        for (Map.Entry<String, Double> veryLastEntry : lastEntry.getValue().entrySet()) {
                            veryLastEntry.setValue(veryLastEntry.getValue() / datas.length);
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        DecimalFormat df = new DecimalFormat("#.######");

        try (BufferedReader br0 = new BufferedReader(new InputStreamReader(new FileInputStream("data-gather1.csv")));
                BufferedReader br1 = new BufferedReader(new InputStreamReader(new FileInputStream("data-gather2.csv")));
                BufferedReader br2 = new BufferedReader(new InputStreamReader(new FileInputStream("data-gather3.csv")));
                BufferedReader br3 = new BufferedReader(new InputStreamReader(new FileInputStream("data-scatter1.csv")));
                BufferedReader br4 = new BufferedReader(new InputStreamReader(new FileInputStream("data-scatter2.csv")));
                BufferedReader br5 = new BufferedReader(new InputStreamReader(new FileInputStream("data-scatter3.csv")));
                FileWriter fw0 = new FileWriter("data-gather.csv");
                FileWriter fw1 = new FileWriter("data-scatter.csv")) {
            //Gather
            Data d0 = new Data(br0);
            Data d1 = new Data(br1);
            Data d2 = new Data(br2);
            Data d = new Data(d0, d1, d2);
            Map<String, Double> m;

            fw0.write("Nodes;Processes;;;");

            for (Integer i : d.headerList) {
                fw0.write("" + i);
                fw0.write(";");
            }

            fw0.write('\n');

            for (Integer nodeHeader : d.nodeList) {
                for (Integer processHeader : d.processList) {
                    for (Integer header2 : d.headerList) {
                        fw0.write(nodeHeader + ";" + processHeader + ";" + nodeHeader * processHeader + ";" + header2 + ";");

                        for (Integer header : d.headerList) {
                            m = d.matrix.get("" + nodeHeader).get("" + processHeader).get("" + header);
                            
                            if(m != null && m.get("" + header2) != null){
                                fw0.write(df.format(m.get("" + header2)));
                            }
                            
                            fw0.write(";");
                        }
                        fw0.write('\n');
                    }
                }

            }

            // Scatter
            Data d3 = new Data(br3);
            Data d4 = new Data(br4);
            Data d5 = new Data(br5);
            d = new Data(d3, d4, d5);

            fw1.write("Nodes;Processes;;;");

            for (Integer i : d.headerList) {
                fw1.write("" + i);
                fw1.write(";");
            }

            fw1.write('\n');

            for (Integer nodeHeader : d.nodeList) {
                for (Integer processHeader : d.processList) {
                    for (Integer header2 : d.headerList) {
                        fw1.write(nodeHeader + ";" + processHeader + ";" + nodeHeader * processHeader + ";" + header2 + ";");

                        for (Integer header : d.headerList) {
                            m = d.matrix.get("" + nodeHeader).get("" + processHeader).get("" + header);
                            
                            if(m != null && m.get("" + header2) != null){
                                fw1.write(df.format(m.get("" + header2)));
                            }
                            
                            fw1.write(";");
                        }
                        fw1.write('\n');
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }
}
