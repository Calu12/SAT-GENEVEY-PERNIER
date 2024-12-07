package fr.uga.pddl4j.examples.sat;

import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.parser.RequireKey;
import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.plan.SequentialPlan;
import fr.uga.pddl4j.planners.AbstractPlanner;
import fr.uga.pddl4j.problem.DefaultProblem;
import fr.uga.pddl4j.problem.Fluent;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.util.BitVector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.*;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

@CommandLine.Command(name = "SATSolver", version = "Solver 1.0", description = "Solves problems", sortOptions = false, mixinStandardHelpOptions = true, headerHeading = "Usage:%n", synopsisHeading = "%n", descriptionHeading = "%nDescription:%n%n", parameterListHeading = "%nParameters:%n", optionListHeading = "%nOptions:%n")
public class SATSolver extends AbstractPlanner {

    private static final Logger logger = LogManager.getLogger(SATSolver.class.getName());

    private int planMaxSize = 1;
    private String timeFile;
    private String lengthFile;
    private String planFile;

    private void setTimeFile(String resultFile) {
        this.timeFile = resultFile;
    }

    private void setLengthFile(String resultFile) {
        this.lengthFile = resultFile;
    }

    private void setPlanFile(String resultFile) {
        this.planFile = resultFile;
    }

    private void appendToFile(String filePath, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(content);
        }
    }

    /**
     * Écrit le plan dans un fichier texte.
     *
     * @param plan     Le plan à écrire.
     * @param filePath Le chemin du fichier où écrire le plan.
     * @throws IOException Si une erreur d'I/O se produit.
     */
    private void writePlanToFile(String plan, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write(plan);
        }
    }

    // main
    public static void main(String[] args) {

        try {
            final SATSolver solver = new SATSolver();
            if (args.length == 5) {
                solver.setTimeFile(args[2]);
                solver.setLengthFile(args[3]);
                solver.setPlanFile(args[4]);
            }
            CommandLine cmd = new CommandLine(solver);
            cmd.execute(new String[] { args[0], args[1] });
        } catch (IllegalArgumentException e) {
            logger.fatal(e.getMessage());
        }
    }

    @Override
    public Problem instantiate(DefaultParsedProblem problem) {
        final Problem pb = new DefaultProblem(problem);
        pb.instantiate();
        return pb;
    }

    public int generateFluentID(Problem prob, Fluent state, int timeStep) {
        int stateIndex = prob.getFluents().indexOf(state);
        return (prob.getFluents().size() + prob.getActions().size()) * timeStep + 1 + stateIndex;
    }

    public int generateActionID(Problem prob, Action act, int timeStep) {
        int actionIndex = prob.getActions().indexOf(act);
        return (prob.getFluents().size() + prob.getActions().size()) * timeStep + 1 + prob.getFluents().size()
                + actionIndex;
    }

    public Action getActionByID(Problem prob, int actionID) {

        if (actionID <= 0) {
            return null;
        }

        int idx = (actionID - 1) % (prob.getFluents().size() + prob.getActions().size());

        if (idx >= prob.getFluents().size()) {
            return prob.getActions().get(idx - prob.getFluents().size());
        } else {
            return null;
        }
    }

    public Vec<IVecInt> buildInitialStateClauses(final Problem prob, int maxPlanSize) {

        Vec<IVecInt> initialStateClauses = new Vec<IVecInt>();

        BitVector initialState = prob.getInitialState().getPositiveFluents();

        HashSet<Integer> fluentsNotInInitialState = new HashSet<Integer>();
        for (int i = 0; i < prob.getFluents().size(); i++) {
            fluentsNotInInitialState.add(i);
        }

        for (int p = initialState.nextSetBit(0); p >= 0; p = initialState.nextSetBit(p + 1)) {
            Fluent f = prob.getFluents().get(p);

            fluentsNotInInitialState.remove(p);

            int fluentID = generateFluentID(prob, f, 0);
            VecInt clause = new VecInt(new int[] { fluentID });
            initialStateClauses.push(clause);

            initialState.set(p);
        }

        for (Integer missingState : fluentsNotInInitialState) {
            VecInt clause = new VecInt(new int[] { -(missingState + 1) });
            initialStateClauses.push(clause);
        }

        logger.debug("Initial state clauses: {}", initialStateClauses);

        return initialStateClauses;
    }

    public Vec<IVecInt> buildGoalStateClauses(final Problem prob, int maxPlanSize) {

        Vec<IVecInt> goalStateClauses = new Vec<IVecInt>();

        BitVector goalState = prob.getGoal().getPositiveFluents();

        for (int p = goalState.nextSetBit(0); p >= 0; p = goalState.nextSetBit(p + 1)) {
            Fluent f = prob.getFluents().get(p);

            int fluentID = generateFluentID(prob, f, maxPlanSize);
            VecInt clause = new VecInt(new int[] { fluentID });
            goalStateClauses.push(clause);

            goalState.set(p);
        }

        return goalStateClauses;
    }

    public Vec<IVecInt> buildActionClauses(final Problem prob, int maxPlanSize) {

        Vec<IVecInt> actionClauses = new Vec<IVecInt>();

        Fluent fluent;

        for (int timeStep = 0; timeStep < maxPlanSize; timeStep++) {
            for (Action act : prob.getActions()) {

                int actionID = generateActionID(prob, act, timeStep);

                BitVector positivePreconditions = act.getPrecondition().getPositiveFluents();
                for (int p = positivePreconditions.nextSetBit(0); p >= 0; p = positivePreconditions.nextSetBit(p + 1)) {
                    fluent = prob.getFluents().get(p);

                    int fluentID = generateFluentID(prob, fluent, timeStep);
                    VecInt clause = new VecInt(new int[] { -actionID, fluentID });
                    actionClauses.push(clause);
                    positivePreconditions.set(p);
                }

                BitVector negativePreconditions = act.getPrecondition().getNegativeFluents();
                for (int p = negativePreconditions.nextSetBit(0); p >= 0; p = negativePreconditions.nextSetBit(p + 1)) {
                    fluent = prob.getFluents().get(p);

                    int fluentID = generateFluentID(prob, fluent, timeStep);
                    VecInt clause = new VecInt(new int[] { -actionID, -fluentID });
                    actionClauses.push(clause);
                    negativePreconditions.set(p);
                }

                BitVector positiveEffects = act.getUnconditionalEffect().getPositiveFluents();
                for (int p = positiveEffects.nextSetBit(0); p >= 0; p = positiveEffects.nextSetBit(p + 1)) {
                    fluent = prob.getFluents().get(p);

                    int fluentID = generateFluentID(prob, fluent, timeStep + 1);
                    VecInt clause = new VecInt(new int[] { -actionID, fluentID });
                    actionClauses.push(clause);

                    positiveEffects.set(p);
                }

                BitVector negativeEffects = act.getUnconditionalEffect().getNegativeFluents();
                for (int p = negativeEffects.nextSetBit(0); p >= 0; p = negativeEffects.nextSetBit(p + 1)) {
                    fluent = prob.getFluents().get(p);

                    int fluentID = generateFluentID(prob, fluent, timeStep + 1);
                    VecInt clause = new VecInt(new int[] { -actionID, -fluentID });
                    actionClauses.push(clause);

                    negativeEffects.set(p);
                }
            }
        }

        logger.debug("Action clauses: {}", actionClauses);

        return actionClauses;
    }

    public Vec<IVecInt> buildExplanatoryAxioms(final Problem prob, int maxPlanSize) {

        Vec<IVecInt> explanatoryAxioms = new Vec<IVecInt>();
        ArrayList<Action>[] positiveEffects = (ArrayList<Action>[]) new ArrayList[prob.getFluents().size()];
        ArrayList<Action>[] negativeEffects = (ArrayList<Action>[]) new ArrayList[prob.getFluents().size()];

        for (int i = 0; i < prob.getFluents().size(); i++) {
            positiveEffects[i] = new ArrayList<Action>();
            negativeEffects[i] = new ArrayList<Action>();
        }

        for (Action act : prob.getActions()) {
            BitVector effectPos = act.getUnconditionalEffect().getPositiveFluents();

            for (int p = effectPos.nextSetBit(0); p >= 0; p = effectPos.nextSetBit(p + 1)) {
                positiveEffects[p].add(act);
                effectPos.set(p);
            }

            BitVector effectNeg = act.getUnconditionalEffect().getNegativeFluents();

            for (int p = effectNeg.nextSetBit(0); p >= 0; p = effectNeg.nextSetBit(p + 1)) {
                negativeEffects[p].add(act);
                effectNeg.set(p);
            }
        }

        for (int stateIdx = 0; stateIdx < prob.getFluents().size(); stateIdx++) {
            for (int timeStep = 0; timeStep < maxPlanSize; timeStep++) {
                if (positiveEffects[stateIdx].size() != 0) {

                    Fluent fluent = prob.getFluents().get(stateIdx);
                    VecInt clause = new VecInt();

                    clause.push(generateFluentID(prob, fluent, timeStep));
                    clause.push(-generateFluentID(prob, fluent, timeStep + 1));

                    for (Action act : positiveEffects[stateIdx]) {
                        clause.push(generateActionID(prob, act, timeStep));
                    }

                    explanatoryAxioms.push(clause);
                }

                if (negativeEffects[stateIdx].size() != 0) {

                    Fluent fluent = prob.getFluents().get(stateIdx);
                    VecInt clause = new VecInt();

                    clause.push(-generateFluentID(prob, fluent, timeStep));
                    clause.push(generateFluentID(prob, fluent, timeStep + 1));

                    for (Action act : negativeEffects[stateIdx]) {
                        clause.push(generateActionID(prob, act, timeStep));
                    }

                    explanatoryAxioms.push(clause);
                }
            }
        }

        return explanatoryAxioms;
    }

    public Vec<IVecInt> buildExclusionAxioms(final Problem prob, int maxPlanSize) {

        Vec<IVecInt> exclusionAxioms = new Vec<IVecInt>();

        for (int action1Idx = 0; action1Idx < prob.getActions().size(); action1Idx++) {
            for (int action2Idx = 0; action2Idx < action1Idx; action2Idx++) {

                Action action1 = prob.getActions().get(action1Idx);
                Action action2 = prob.getActions().get(action2Idx);

                int action1ID = generateActionID(prob, action1, 0);
                int action2ID = generateActionID(prob, action2, 0);

                int actionOffset = prob.getActions().size() + prob.getFluents().size();

                for (int timeStep = 0; timeStep < maxPlanSize; timeStep++) {

                    int offset = actionOffset * timeStep;
                    VecInt clause = new VecInt(
                            new int[] { -(action1ID + offset), -(action2ID + offset) });
                    exclusionAxioms.push(clause);
                }
            }
        }

        return exclusionAxioms;
    }

    public int[] runSATSolver(Vec<IVecInt> clauses, Problem prob) throws TimeoutException {
        final int MAXVAR = (prob.getFluents().size() + prob.getActions().size()) * this.planMaxSize
                + prob.getFluents().size();

        logger.debug("Total clauses: {}", clauses.size());

        ISolver solver = SolverFactory.newDefault();
        solver.newVar(MAXVAR);
        solver.setExpectedNumberOfClauses(clauses.size());
        solver.setTimeout(600);

        try {
            solver.addAllClauses(clauses);
        } catch (ContradictionException e) {
            return null;
        }

        IProblem problemSAT = solver;
        try {
            if (problemSAT.isSatisfiable()) {
                logger.info("Satisfiable!\n");
                return problemSAT.model();
            } else {
                logger.error("Not satisfiable\n");
                return null;
            }
        } catch (TimeoutException e) {
            logger.error("Timeout\n");
            throw new TimeoutException("Solver timed out while finding a model.");
        }
    }

    public Vec<IVecInt> buildCNFRepresentation(Problem prob, int maxPlanSize) {
        logger.info("Encoding initial state\n");
        Vec<IVecInt> clausesInit = buildInitialStateClauses(prob, maxPlanSize);
        logger.info("Encoding goal state\n");
        Vec<IVecInt> clausesGoal = buildGoalStateClauses(prob, maxPlanSize);
        logger.info("Encoding actions\n");
        Vec<IVecInt> clausesActions = buildActionClauses(prob, maxPlanSize);
        logger.info("Encoding explanatory axioms\n");
        Vec<IVecInt> clausesExplanatoryAxioms = buildExplanatoryAxioms(prob, maxPlanSize);
        logger.info("Encoding exclusion axioms\n");
        Vec<IVecInt> clausesExclusionAxioms = buildExclusionAxioms(prob, maxPlanSize);

        Vec<IVecInt> allClauses = new Vec<IVecInt>(clausesInit.size() + clausesGoal.size()
                + clausesActions.size() + clausesExplanatoryAxioms.size() + clausesExclusionAxioms.size());
        clausesInit.copyTo(allClauses);
        clausesGoal.copyTo(allClauses);
        clausesActions.copyTo(allClauses);
        clausesExplanatoryAxioms.copyTo(allClauses);
        clausesExclusionAxioms.copyTo(allClauses);

        logger.debug("Initial state clauses: {}", clausesInit.size());
        logger.debug("Goal state clauses: {}", clausesGoal.size());
        logger.debug("Action clauses: {}", clausesActions.size());
        logger.debug("Explanatory axioms: {}", clausesExplanatoryAxioms.size());
        logger.debug("Exclusion axioms: {}", clausesExclusionAxioms.size());

        return allClauses;
    }

    public Plan generatePlanFromModel(int[] model, Problem prob) {
        Plan plan = new SequentialPlan();
        int actionIndex = 0;
        for (Integer idx : model) {
            Action action = getActionByID(prob, idx);
            if (action != null) {
                plan.add(actionIndex, action);
                actionIndex++;
            }
        }
        return plan;
    }

    @Override
    public Plan solve(final Problem prob) {

        int[] model;

        while (true) {

            logger.info("Encoding model for plan size: {}\n", this.planMaxSize);

            final long startEncodeTime = System.currentTimeMillis();
            Vec<IVecInt> allClauses = buildCNFRepresentation(prob, this.planMaxSize);
            final long endEncodeTime = System.currentTimeMillis();
            this.getStatistics()
                    .setTimeToEncode(this.getStatistics().getTimeToEncode() + (endEncodeTime - startEncodeTime));

            logger.info("Total clauses: {}\n", allClauses.size());
            final long startSolveTime = System.currentTimeMillis();
            logger.info("Running SAT solver...\n");
            try {
                model = runSATSolver(allClauses, prob);
            } catch (TimeoutException e) {
                final long endSolveTime = System.currentTimeMillis();
                this.getStatistics()
                        .setTimeToSearch(this.getStatistics().getTimeToSearch() + endSolveTime - startSolveTime);
                if (timeFile != null && lengthFile != null) {
                    String timeString = ";";
                    String lengthString = ";";
                    try {
                        appendToFile(timeFile, timeString);
                        appendToFile(lengthFile, lengthString);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                return null;
            }

            final long endSolveTime = System.currentTimeMillis();
            this.getStatistics()
                    .setTimeToSearch(this.getStatistics().getTimeToSearch() + endSolveTime - startSolveTime);

            if (model == null) {
                logger.info(
                        "Failed to find model with max plan size = {}.\n",
                        this.planMaxSize);

                this.planMaxSize *= 2;
            } else {
                break;
            }
        }

        Plan plan = generatePlanFromModel(model, prob);

        if (timeFile != null && lengthFile != null) {
            String timeString = ";" + (this.getStatistics().getTimeToSearch()
                    + this.getStatistics().getTimeToEncode()
                    + this.getStatistics().getTimeToParse());
            String lengthString = ";" + plan.size();

            try {
                appendToFile(timeFile, timeString);
                appendToFile(lengthFile, lengthString);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // écrire le plan dans un fichier texte
        try {
            writePlanToFile(prob.toString(plan), planFile);
        } catch (IOException e) {
            logger.error("Erreur lors de l'écriture du plan dans le fichier texte", e);
        }

        return plan;
    }

    @Override
    public boolean isSupported(Problem prob) {
        return !prob.getRequirements().contains(RequireKey.ACTION_COSTS)
                && !prob.getRequirements().contains(RequireKey.CONSTRAINTS)
                && !prob.getRequirements().contains(RequireKey.CONTINOUS_EFFECTS)
                && !prob.getRequirements().contains(RequireKey.DERIVED_PREDICATES)
                && !prob.getRequirements().contains(RequireKey.DURATIVE_ACTIONS)
                && !prob.getRequirements().contains(RequireKey.DURATION_INEQUALITIES)
                && !prob.getRequirements().contains(RequireKey.FLUENTS)
                && !prob.getRequirements().contains(RequireKey.GOAL_UTILITIES)
                && !prob.getRequirements().contains(RequireKey.METHOD_CONSTRAINTS)
                && !prob.getRequirements().contains(RequireKey.NUMERIC_FLUENTS)
                && !prob.getRequirements().contains(RequireKey.OBJECT_FLUENTS)
                && !prob.getRequirements().contains(RequireKey.PREFERENCES)
                && !prob.getRequirements().contains(RequireKey.TIMED_INITIAL_LITERALS)
                && !prob.getRequirements().contains(RequireKey.HIERARCHY);
    }
}
