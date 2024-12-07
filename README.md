Binome : Alexandre PERNIER & Coemgen GENEVEY

<h1>Installation</h1>

Java, Git et Python doivent être installé sur la machine.

D'abord on ouvre une invite de commande dans le répertoire ou on soihaite installer le projet puis un execute la commande suivante :
```
git clone https://github.com/Calu12/SAT-GENEVEY-PERNIER.git
 ```

Puis placez-vous dans le projet avec la commande
```
cd SAT-PERNIER-GENEVEY
 ```

Dans votre explorateur de fichier, vérifier que le dossier "classes\fr\uga\pddl4j\examples\sat" dans "SAT-PERNIER-GENEVEY" contient bien les fichiers suivant : 
-EvaluationSingle.class
-HSP.class
-SATSolver.class
-ScriptEvaluation.class

Si certains fichier manque vous pouvez les générer avec les commandes suivantes (à exécuter dans "SAT-PERNIER-GENEVEY") :
```
javac -d classes -cp lib/pddl4j-4.0.0.jar;lib/org.sat4j.core.jar;classes src/fr/uga/pddl4j/examples/sat/EvaluationSingle.java
 ```
```
javac -d classes -cp lib/pddl4j-4.0.0.jar;lib/org.sat4j.core.jar;classes src/fr/uga/pddl4j/examples/sat/HSP.java
 ```
```
javac -d classes -cp lib/pddl4j-4.0.0.jar;lib/org.sat4j.core.jar;classes src/fr/uga/pddl4j/examples/sat/SATSolver.java
 ```
```
javac -d classes -cp lib/pddl4j-4.0.0.jar;lib/org.sat4j.core.jar;classes src/fr/uga/pddl4j/examples/sat/ScriptEvaluation.java
 ```

Pour l'affichage des graphiques, il est nécessaire d'avoir la bibliothèque python matplotlib, pour l'installer il faut exécuter la commande suivante dans un invité de commande : 
```
pip install matplotlib
 ```

 Cependant les graphiques sont déjà présents dans le document Rapport-TP-SAT.pdf .


<h1>Graphiques</h1>

Dans le fichier "graphs.ipynb" se trouve des graphes de comparaison entre les performances des planners HSP et SAT.
C'est un NoteBook python qui peut être ouvert et exécuté dans l'éditeur VSCode avec les extensions : Jupyter, Jupyter Cell Tags, Jupyter Keymap et Jupyter Slide Show.

<h1>Exécuter les classes</h1>

Les commandes ci-dessous doivent être exécutées dans "SAT-PERNIER-GENEVEY" et les éléments "\<domain>" et \<probleme> doivent être remplacés par les chemain d'accès aux fichier de domain et de problème ".pddl" souhaités.

Pour exécuter le planner HSP : 
```
java -cp classes;lib/org.sat4j.core.jar;lib/pddl4j-4.0.0.jar fr.uga.pddl4j.examples.sat.HSP <domain> <probleme>
 ```

Pour exécuter le planner SAT : 
```
java -cp classes;lib/org.sat4j.core.jar;lib/pddl4j-4.0.0.jar fr.uga.pddl4j.examples.sat.SATSolver <domain> <probleme>
 ```

<strong>Je ne recommande pas d'utiliser les 2 commandes suivantes dans la mesure où la première peut prendre plusieurs heures d'exécution et où les deux vont modifier les fichiers text de données servants à afficher les graphiques dans le notebook "graphs.ipynb" !</strong>


Pour exécuter les planners HSP et SAT pour touts les problèmes des dossiers "gripper", "blocks", "logistics" et "depots" et ajoute les résultats de temps et de longueur de la solution dans les documents text à la base du projet :
```
java -cp classes;lib/org.sat4j.core.jar;lib/pddl4j-4.0.0.jar fr.uga.pddl4j.examples.sat.ScriptEvaluation
 ```

Pour exécuter les planners HSP et SAT pour un problème et ajoute les résultats de temps et de longueur de la solution dans les documents text à la base du projet :
```
java -cp classes;lib/org.sat4j.core.jar;lib/pddl4j-4.0.0.jar fr.uga.pddl4j.examples.sat.EvaluationSingle
 ```
Remarque : On peut changer le nom du fichier de problème et de son dossier dans le fichier "src\fr\uga\pddl4j\examples\sat\EvaluationSingle.java". Il faut ensuite exécuter :
```
javac -d classes -cp lib/pddl4j-4.0.0.jar;lib/org.sat4j.core.jar;classes src/fr/uga/pddl4j/examples/sat/EvaluationSingle.java
 ```
Pour recompiler la classe avant de l'exécuter.


<h1>Stockage des plans</h1>

Quand le planificateur SAT réussi à trouver un plan, il l'enregistre dans un fichier intitulé "res"+\<le nom du fichier problème>.
Ces plan sont stockés dans les dossiers planBlocks, planDepots, planGripper et planLogistics pour pouvoir vérifier leur validité.

Cependant le script validate.py, sensé vérifier automatiquement la validité de tout les plans, n'est pas encore fonctionnel. 