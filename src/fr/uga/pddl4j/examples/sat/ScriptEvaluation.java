package fr.uga.pddl4j.examples.sat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import fr.uga.pddl4j.examples.sat.HSP;
import fr.uga.pddl4j.examples.sat.SATSolver;


public class ScriptEvaluation {

    public static void main(String[] args) {
        
         
        File dir = new File(".\\blocks");
        File[] liste = dir.listFiles(); 
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("blocksTime.txt", true))) {
            writer.write("probleme;HSPtemps;SATtemps");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("blocksLength.txt", true))) {
            writer.write("probleme;HSPlength;SATlength");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (File item : liste) {
            if (!item.getName().toString().equals("domain.pddl")) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("blocksTime.txt", true))) {
                        writer.write("\n"+item.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("blocksLength.txt", true))) {
                    writer.write("\n"+item.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                HSP.main(new String[] { ".\\blocks\\domain.pddl",".\\blocks\\"+item.getName(),"blocksTime.txt","blocksLength.txt"});
                SATSolver.main(new String[] { ".\\blocks\\domain.pddl",".\\blocks\\"+item.getName(),"blocksTime.txt","blocksLength.txt","planBlocks\\res"+item.getName() });
            }
        }
        
        dir = new File(".\\depots");
        liste = dir.listFiles();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("depotsTime.txt", true))) {
            writer.write("probleme;HSPtemps;SATtemps");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("depotsLength.txt", true))) {
            writer.write("probleme;HSPlength;SATlength");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (File item : liste) {
            if (!item.getName().toString().equals("domain.pddl")) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("depotsTime.txt", true))) {
                        writer.write("\n"+item.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("depotsLength.txt", true))) {
                    writer.write("\n"+item.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                HSP.main(new String[] { ".\\depots\\domain.pddl",".\\depots\\"+item.getName(),"depotsTime.txt","depotsLength.txt" });
                SATSolver.main(new String[] { ".\\depots\\domain.pddl",".\\depots\\"+item.getName(),"depotsTime.txt","depotsLength.txt","planDepots\\res"+item.getName() });
            }
        }
        
        dir = new File(".\\gripper");
        liste = dir.listFiles();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("gripperTime.txt", true))) {
            writer.write("probleme;HSPtemps;SATtemps");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("gripperLength.txt", true))) {
            writer.write("probleme;HSPlength;SATlength");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (File item : liste) {
            if (!item.getName().toString().equals("domain.pddl")) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("gripperTime.txt", true))) {
                        writer.write("\n"+item.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("gripperLength.txt", true))) {
                    writer.write("\n"+item.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                HSP.main(new String[] { ".\\gripper\\domain.pddl",".\\gripper\\"+item.getName(),"gripperTime.txt","gripperLength.txt" });
                SATSolver.main(new String[] { ".\\gripper\\domain.pddl",".\\gripper\\"+item.getName(),"gripperTime.txt","gripperLength.txt","planGripper\\res"+item.getName() });
            }
        }
        
        
        dir = new File(".\\logistics");
        liste = dir.listFiles();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("logisticsTime.txt", true))) {
            writer.write("probleme;HSPtemps;SATtemps");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("logisticsLength.txt", true))) {
            writer.write("probleme;HSPlength;SATlength");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (File item : liste) {
            if (!item.getName().toString().equals("domain.pddl")) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("logisticsTime.txt", true))) {
                        writer.write("\n"+item.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("logisticsLength.txt", true))) {
                    writer.write("\n"+item.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                HSP.main(new String[] { ".\\logistics\\domain.pddl",".\\logistics\\"+item.getName(),"logisticsTime.txt","logisticsLength.txt" });
                SATSolver.main(new String[] { ".\\logistics\\domain.pddl",".\\logistics\\"+item.getName(),"logisticsTime.txt","logisticsLength.txt","planLogistics\\res"+item.getName() });
            }
        }
        
        

    }
}
