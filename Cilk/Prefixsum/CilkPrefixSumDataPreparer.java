
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

/**
 *
 * @author Christian Beikov
 */
public class CilkPrefixSumDataPreparer {

    private static class Data{
        Map<String, Map<String, Double>> recursiveMatrix = new HashMap<>();
        Map<String, Map<String, Double>> iterativeMatrix = new HashMap<>();
        List<Integer> headerList = new ArrayList<>();
        
        Data(BufferedReader br) throws IOException{
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");

                if("Recurisve".equals(parts[0])){
                    if (!recursiveMatrix.containsKey(parts[2])) {
                        recursiveMatrix.put(parts[2], new HashMap<String, Double>());
                    }

                    recursiveMatrix.get(parts[2]).put(parts[1], Double.parseDouble(parts[3]));
                } else {
                    if (!iterativeMatrix.containsKey(parts[2])) {
                        iterativeMatrix.put(parts[2], new HashMap<String, Double>());
                    }

                    iterativeMatrix.get(parts[2]).put(parts[1], Double.parseDouble(parts[3]));
                }
            }

            for (Map.Entry<String, Map<String, Double>> entry : recursiveMatrix.entrySet()) {
                headerList.add(Integer.parseInt(entry.getKey()));
            }

            Collections.sort(headerList);
        }
        
        Data(Data... datas){
            for(Data d : datas){
                for (Integer h : d.headerList) {
                    if (!headerList.contains(h)) {
                        headerList.add(h);
                    }
                }
                for (Map.Entry<String, Map<String, Double>> entry : d.recursiveMatrix.entrySet()) {
                    if (!recursiveMatrix.containsKey(entry.getKey())) {
                        recursiveMatrix.put(entry.getKey(), new HashMap<String, Double>());
                    }

                    for (Map.Entry<String, Double> nextEntry : entry.getValue().entrySet()) {
                        if (!recursiveMatrix.get(entry.getKey()).containsKey(nextEntry.getKey())) {
                            recursiveMatrix.get(entry.getKey()).put(nextEntry.getKey(), nextEntry.getValue());
                        } else {
                            recursiveMatrix.get(entry.getKey()).put(nextEntry.getKey(), recursiveMatrix.get(entry.getKey()).get(nextEntry.getKey()) + nextEntry.getValue());
                        }
                    }
                }
                for (Map.Entry<String, Map<String, Double>> entry : d.iterativeMatrix.entrySet()) {
                    if (!iterativeMatrix.containsKey(entry.getKey())) {
                        iterativeMatrix.put(entry.getKey(), new HashMap<String, Double>());
                    }

                    for (Map.Entry<String, Double> nextEntry : entry.getValue().entrySet()) {
                        if (!iterativeMatrix.get(entry.getKey()).containsKey(nextEntry.getKey())) {
                            iterativeMatrix.get(entry.getKey()).put(nextEntry.getKey(), nextEntry.getValue());
                        } else {
                            iterativeMatrix.get(entry.getKey()).put(nextEntry.getKey(), iterativeMatrix.get(entry.getKey()).get(nextEntry.getKey()) + nextEntry.getValue());
                        }
                    }
                }
            }
            
            for (Map.Entry<String, Map<String, Double>> entry : recursiveMatrix.entrySet()) {

                for (Map.Entry<String, Double> nextEntry : entry.getValue().entrySet()) {
                    nextEntry.setValue(nextEntry.getValue() / datas.length);
                }
            }
            
            for (Map.Entry<String, Map<String, Double>> entry : iterativeMatrix.entrySet()) {

                for (Map.Entry<String, Double> nextEntry : entry.getValue().entrySet()) {
                    nextEntry.setValue(nextEntry.getValue() / datas.length);
                }
            }
        }
    }
    
    public static void main(String[] args) {
        DecimalFormat df = new DecimalFormat("#.######");
        
        try (BufferedReader br0 = new BufferedReader(new InputStreamReader(new FileInputStream("test1.csv")));
                BufferedReader br1 = new BufferedReader(new InputStreamReader(new FileInputStream("test2.csv")));
                BufferedReader br2 = new BufferedReader(new InputStreamReader(new FileInputStream("test3.csv")));
                FileWriter fwRecursive = new FileWriter("recursive.csv");
                FileWriter fwIterative = new FileWriter("iterative.csv")) {
            Data d0 = new Data(br0);
            Data d1 = new Data(br1);
            Data d2 = new Data(br2);
            Data d = new Data(d0, d1, d2);

            // Iterative
            fwIterative.write(";");

            for (Integer i : d.headerList) {
                fwIterative.write("" + i);
                fwIterative.write(";");

            }

            fwIterative.write('\n');

            for (int i = 1; i <= 52; i++) {
                fwIterative.write("" + i + ";");

                for (Integer header : d.headerList) {
                    fwIterative.write(df.format(d.iterativeMatrix.get("" + header).get("" + i)));
                    fwIterative.write(";");
                }

                fwIterative.write('\n');
            }
            
            // Recursive
            fwRecursive.write(";");

            for (Integer i : d.headerList) {
                fwRecursive.write("" + i);
                fwRecursive.write(";");

            }

            fwRecursive.write('\n');

            for (int i = 1; i <= 52; i++) {
                fwRecursive.write("" + i + ";");

                for (Integer header : d.headerList) {
                    fwRecursive.write(df.format(d.recursiveMatrix.get("" + header).get("" + i)));
                    fwRecursive.write(";");
                }

                fwRecursive.write('\n');
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }
}
