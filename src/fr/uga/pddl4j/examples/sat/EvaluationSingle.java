package fr.uga.pddl4j.examples.sat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class EvaluationSingle {
    public static void main(String[] args) {
        
        String file = "p09.pddl";
        String dossier="gripper";

        
        /*try (BufferedWriter writer = new BufferedWriter(new FileWriter(dossier+"Time.txt", true))) {
            writer.write("\n" + file+";");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dossier+"Length.txt", true))) {
            writer.write("\n" + file + ";");
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        SATSolver.main(new String[] { ".\\"+dossier+"\\domain.pddl", ".\\"+dossier+"\\" + file, dossier+"Time.txt",
                dossier+"Length.txt" });
        
    }
}
