/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fantail.core;

import edu.monash.infotech.r2o2.evaluation.ReasonerComponent;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.util.*;

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public class WekaLRHelper {

    public enum DATA_TYPE {
        TRAIN, TEST
    }

    public static Instances covertArff2Xarff(Instances data) {

        Instances xarffData = null;

        try {
            String userDIR = System.getProperty("user.dir");
            String randFileName = Long.toString(System.nanoTime()).substring(10) + ".fantail.algorithms.LRT.temp.xarff";
            String path_separator = System.getProperty("file.separator");
            String xarffPath = userDIR + path_separator + randFileName;
            //System.out.println(m_xarffPath);
            int numObjects = Tools.getNumberTargets(data);

            StringBuilder sb = new StringBuilder();
            sb.append("@relation arff2xarff").append(System.getProperty("line.separator"));
            for (int i = 0; i < data.numAttributes() - 1; i++) {
                sb.append("@attribute ");
                sb.append(data.attribute(i).name());
                sb.append(" numeric").append(System.getProperty("line.separator"));
            }
            sb.append("@attribute L RANKING {");
            for (int i = 0; i < numObjects; i++) {
                String spr = ",";
                if (i == numObjects - 1) {
                    spr = "";
                }
                String targetName = "T" + (i);
                sb.append(targetName).append(spr);
            }
            sb.append("}").append(System.getProperty("line.separator"));
            sb.append("@data ").append(System.getProperty("line.separator"));

            for (int i = 0; i < data.numInstances(); i++) {
                Instance inst = data.instance(i);
                for (int j = 0; j < data.numAttributes() - 1; j++) {
                    sb.append(inst.value(j)).append(",");
                }
                for (int x = 1; x <= numObjects; x++) {
                    int rank = x;

                    String[] names = Tools.getTargetNames(inst);
                    String algo = getName(rank, Tools.getTargetVector(inst), names);

                    System.out.println("\t algo: " + algo + ". rank: " + rank +
                            ", Tools.getTargetVector(inst):" + Arrays.toString(Tools.getTargetVector(inst)) + ", " +
                            "names:" + Arrays.toString(names));

                    String sprr = ">";
                    if (x == names.length) {
                        sprr = "";
                    }
                    sb.append(algo).append(sprr);
                }
                sb.append(System.getProperty("line.separator"));
            }

            File file = new File(xarffPath);
            Writer output = new BufferedWriter(new FileWriter(file));
            output.write(sb.toString());
            output.close();

            System.out.println(file.getAbsoluteFile());

            weka.core.converters.XArffLoader xarffLoader = new weka.core.converters.XArffLoader();
            xarffLoader.setSource(new File(xarffPath));
            xarffData = xarffLoader.getDataSet();
            //
            File tmpxarffFile = new File(xarffPath);
            if (tmpxarffFile.exists()) {
                tmpxarffFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return xarffData;
    }

    // data_type is
    public static Instances covertArff2Xarff2(DATA_TYPE data_type, Instances data) {

        Instances xarffData = null;

        try {
            String userDIR = System.getProperty("user.dir");
            //String randFileName = Long.toString(System.nanoTime()).substring(10) + ".LRT.temp.xarff";
            String randFileName = UUID.randomUUID().toString() + ".LRT.temp.xarff";
            String path_separator = System.getProperty("file.separator");
            String xarffPath = userDIR + path_separator + randFileName;
            //System.out.println(m_xarffPath);
            int numObjects = Tools.getNumberTargets(data);

            StringBuilder sb = new StringBuilder();
            sb.append("@relation arff2xarff").append(System.getProperty("line.separator"));
            for (int i = 0; i < data.numAttributes() - 1; i++) {
                sb.append("@attribute ");
                sb.append(data.attribute(i).name());
                sb.append(" numeric").append(System.getProperty("line.separator"));
            }
            sb.append("@attribute L RANKING {");
            for (int i = 0; i < numObjects; i++) {
                String spr = ",";
                if (i == numObjects - 1) {
                    spr = "";
                }
                String targetName = "T" + (i);
                sb.append(targetName).append(spr);
            }
            sb.append("}").append(System.getProperty("line.separator"));
            sb.append("@data ").append(System.getProperty("line.separator"));

            for (int i = 0; i < data.numInstances(); i++) {
                Instance inst = data.instance(i);

                // determine a ranking of the class labels
                String ranking_result = determineRanking(Tools.getTargetObjects(inst));
                if (ranking_result == null) continue;
                //System.out.println("\t ranking_result:" + ranking_result);

                // looking at a>b>c, if the 'a' part consists of more than two partial relations, we need to split them.
                List<ArrayList<String>> label_collection = new ArrayList<ArrayList<String>>();

                // generate feature string
                String attr_set_str = "";
                for (int j = 0; j < data.numAttributes() - 1; j++) {
                    attr_set_str += (inst.value(j) + ",");
                }

                // split label string via ">"
                String items[] = ranking_result.split(">");
                for (int j = 0; j < items.length; j++) {

                    String labels[] = items[j].split("\\|");

                    // if the first label has more than or equal to 2 partial relations, we split it.
                    ArrayList<String> label_list = new ArrayList<String>();
                    if (j == 0) {
                        if (labels.length >= 2) {
                            for (int k = 0; k < labels.length; k++) {
                                label_list.add(labels[k]);
                            }
                        } else {
                            label_list.add(items[j]);
                        }
                        label_collection.add(label_list);
                    } else {
                        if (labels.length >= 3) {
                            for (int k = 0; k < labels.length; k++) {
                                label_list.add(labels[k]);
                            }

                        } else {
                            label_list.add(items[j]);
                        }
                        label_collection.add(label_list);
                    }
                }

                List<String> prev_items_in_label_collection = new ArrayList<String>();
                for (int j = 0; j < label_collection.size(); j++) {
                    List<String> items_in_label_collection = new ArrayList<String>();
                    if (j == 0) {
                        for (int k = 0; k < label_collection.get(j).size(); k++) {
                            items_in_label_collection.add(label_collection.get(j).get(k));
                        }
                    } else {
                        for (int k = 0; k < label_collection.get(j).size(); k++) {
                            for (int l = 0; l < prev_items_in_label_collection.size(); l++) {
                                items_in_label_collection.add(prev_items_in_label_collection.get(l) + ">" + label_collection.get(j).get(k));
                            }
                        }
                    }
                    prev_items_in_label_collection = items_in_label_collection;
                }

                for (int j = 0; j < prev_items_in_label_collection.size(); j++) {
                    //System.out.println("\t\t line:" + prev_items_in_label_collection.get(j));
                    sb.append(attr_set_str + prev_items_in_label_collection.get(j) + "\n");
                }

                InputStream is = new ByteArrayInputStream(sb.toString().getBytes());
                weka.core.converters.XArffLoader xarffLoader = new weka.core.converters.XArffLoader();
                xarffLoader.setSource(is);
                xarffData = xarffLoader.getDataSet();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return xarffData;
    }

    private static String getName(int rank, double[] rankings, String[] names) {
        String n = "NaN";
        for (int i = 0; i < rankings.length; i++) {

            if (rank == rankings[i]) {
                return names[i];
            }
        }
        return n;
    }

    public void sort(double[] v) {
        // check for empty or null array
        if (v ==null || v.length==0){
            return;
        }
        quicksort(v, 0, v.length-1);
    }

    private void quicksort(double v[], int low, int high) {

        int i = low, j = high;
        // Get the pivot element from the middle of the list
        double pivot = v[low + (high-low)/2];

        // Divide into two lists
        while (i <= j) {
            // If the current value from the left list is smaller then the pivot
            // element then get the next element from the left list
            while (v[i] < pivot) {
                i++;
            }
            // If the current value from the right list is larger then the pivot
            // element then get the next element from the right list
            while (v[j] > pivot) {
                j--;
            }

            // If we have found a values in the left list which is larger then
            // the pivot element and if we have found a value in the right list
            // which is smaller then the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                exchange(v, i, j);
                i++;
                j--;
            }
        }
        // Recursion
        if (low < j)
            quicksort(v, low, j);
        if (i < high)
            quicksort(v, i, high);
    }

    private void exchange(double v[], int i, int j) {
        double temp = v[i];
        v[i] = v[j];
        v[j] = temp;
    }

    private Double[] convertDouble(double[] arr) {

        Double res[] = new Double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            Double v = new Double(arr[i]);
            res[i] = v;
        }

        return res;
    }

    private static String determineRanking(ReasonerComponent ranked_list[]) {

        StringBuilder res = new StringBuilder();

        try {
            // Sort first
            List<ReasonerComponent> target_list = Arrays.asList(ranked_list);
            Collections.sort(target_list);
            Collections.reverse(target_list);

            int rank = 0;
            double last_compared_item = 0;

            for (int i = 0; i < target_list.size(); i++) {
                ReasonerComponent target = target_list.get(i);
                double v = target.getRank();

                if (v != last_compared_item) {
                    rank += 1;

                    if (i == 0)
                        res.append(target.getIndex());
                    else
                        res.append(">" + target.getIndex());
                } else {
                    if (i == 0)
                        res.append(target.getIndex());
                    else
                        res.append("|" + target.getIndex());
                }
                last_compared_item = v;
            }

            // if all class labels have the same rank, then we return null. Thus, we don't include the instance in the training set.
            if (rank == 1) return null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res.toString();
    }

}
