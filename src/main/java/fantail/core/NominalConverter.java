package fantail.core;

import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

/**
 * Created by Microsoft on 14. 1. 14.
 */
public class NominalConverter {

    public static Instances covertNominalToNumeric(Instances data) {

        Instances newData = null;

        try {
            String userDIR = System.getProperty("user.dir");
            String randFileName = Long.toString(System.nanoTime()).substring(10) + ".temp.arff";
            String path_separator = System.getProperty("file.separator");
            String arffPath = userDIR + path_separator + randFileName;
            //System.out.println(m_xarffPath);
            int numObjects = Tools.getNumberTargets(data);

            StringBuilder sb = new StringBuilder();
            sb.append("@relation tmp").append(System.getProperty("line.separator"));
            for (int i = 0; i < data.numAttributes() - 1; i++) {
                sb.append("@attribute ");
                sb.append(data.attribute(i).name());
                sb.append(" numeric").append(System.getProperty("line.separator"));
            }
            sb.append("\n@attribute targets relational\n");

            for (int i = 0; i < numObjects; i++) {
                sb.append("@attribute T" + (i) + " numeric");
                sb.append(System.getProperty("line.separator"));
            }
            sb.append("@end targets\n\n");

            sb.append("@data ").append(System.getProperty("line.separator"));

            for (int i = 0; i < data.numInstances(); i++) {
                Instance inst = data.instance(i);
                String attr_set_str = "";
                for (int j = 0; j < data.numAttributes() - 1; j++) {
                    attr_set_str += (inst.value(j) + ",");
                }

                String cls_set_str = "'";
                double ranks[] = Tools.getTargetVector(inst);
                for (int j = 0; j < ranks.length; j++) {
                    if (j == ranks.length-1) cls_set_str += ((int) ranks[j]);
                    else cls_set_str += ((int) ranks[j] + ",");
                }
                cls_set_str += "'";

                sb.append(attr_set_str + cls_set_str + "\n");
            }

            File file = new File(arffPath);
            Writer output = new BufferedWriter(new FileWriter(file));
            output.write(sb.toString());
            output.close();

            //System.out.println(file.getAbsoluteFile());

            newData = Tools.loadFantailARFFInstances(arffPath);
            File tmpxarffFile = new File(arffPath);
            if (tmpxarffFile.exists()) {
                tmpxarffFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return newData;
    }
}
