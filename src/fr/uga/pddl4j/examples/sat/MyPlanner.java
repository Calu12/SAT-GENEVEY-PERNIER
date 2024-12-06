package fr.uga.pddl4j.examples.sat;

import fr.uga.pddl4j.heuristics.state.StateHeuristic;
import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.parser.RequireKey;
import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.plan.SequentialPlan;
import fr.uga.pddl4j.planners.AbstractPlanner;
import fr.uga.pddl4j.planners.SearchStrategy;
import fr.uga.pddl4j.planners.statespace.search.StateSpaceSearch;
import fr.uga.pddl4j.problem.DefaultProblem;
import fr.uga.pddl4j.problem.Fluent;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.problem.operator.Condition;
import fr.uga.pddl4j.problem.operator.Effect;
import fr.uga.pddl4j.problem.operator.ConditionalEffect;
import fr.uga.pddl4j.util.BitVector;
import fr.uga.pddl4j.problem.State;
import fr.uga.pddl4j.problem.operator.Action;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;
import org.sat4j.core.VecInt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

/**
 * The class is an example. It shows how to create a PRW search planner
 * able to
 * solve an ADL problem by choosing the heuristic to used and its weight.
 *
 * @author D. Pellier
 * @version 4.0 - 30.11.2021
 */
@CommandLine.Command(name = "ASP", version = "ASP 1.0", description = "Solves a specified planning problem using A* search strategy.", sortOptions = false, mixinStandardHelpOptions = true, headerHeading = "Usage:%n", synopsisHeading = "%n", descriptionHeading = "%nDescription:%n%n", parameterListHeading = "%nParameters:%n", optionListHeading = "%nOptions:%n")
// Credit Alexandre Pernier le goat
public class MyPlanner extends AbstractPlanner {

    private double heuristicWeight;
    private StateHeuristic.Name heuristic;
    private final Map<Action, Integer> actionIds = new HashMap<>();
    private int nextActionId = 1; // Identifiant à attribuer à la prochaine action
    private String timeFile;
    private String lengthFile;
    /**
     * The class logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(MyPlanner.class.getName());

    private void setTimeFile(String resultFile) {
        this.timeFile = resultFile;
    }

    private void setLengthFile(String resultFile) {
        this.lengthFile = resultFile;
    }

    private void appendToFile(String filePath, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(content);
        }
    }
    /**
     * Instantiates the planning problem from a parsed problem.
     *
     * @param problem the problem to instantiate.
     * @return the instantiated planning problem or null if the problem cannot be
     *         instantiated.
     */
    @Override
    public Problem instantiate(DefaultParsedProblem problem) {
        final Problem pb = new DefaultProblem(problem);
        pb.instantiate();
        return pb;
    }

    private int getActionId(Action action) {
        return actionIds.computeIfAbsent(action, a -> nextActionId++);
    }

    @Override
    public Plan solve(final Problem problem) {
        int actions = problem.getActions().size();
        int fluents = problem.getFluents().size();

        // Calculer un facteur optimal pour fluentToVar
        int optimalFactor = calculateOptimalFactor(fluents, actions);
        LOGGER.info("Optimal factor for SAT variable encoding: " + optimalFactor);

        // Log the beginning of the SAT solver
        LOGGER.info("* Starting SAT-based search \n");

        // Step 1: Create a SAT solver instance
        ISolver solver = SolverFactory.newDefault();
        solver.newVar(actions * fluents); // Estimate the number of variables
        solver.setTimeout(60); // Set a timeout in seconds

        // Step 2: Generate SAT clauses
        try {
            // Encode initial state as SAT clauses
            encodeInitialState(problem, solver, optimalFactor);

            // Encode goal state as SAT clauses
            encodeGoalState(problem, solver, optimalFactor);

            // Encode action preconditions and effects
            encodeActions(problem, solver, optimalFactor);

            // Step 3: Solve the SAT problem
            if (solver.isSatisfiable()) {
                LOGGER.info("* SAT search succeeded\n");

                // Decode solution into a plan
                Plan plan = decodeSolution(solver.model(), problem);
                this.getStatistics().setTimeToSearch(solver.getTimeout());
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
                return plan;
            } else {
                LOGGER.info("* SAT search failed\n");
                return null;
            }
        } catch (ContradictionException e) {
            LOGGER.error("SAT problem has a contradiction!", e);
            return null;
        } catch (OutOfMemoryError e) {
            LOGGER.error(
                    "Problème trop grand pour la mémoire actuelle.");
            System.err.println(
                    "Erreur de mémoire.");
            return null;
        } catch (TimeoutException e) {
            LOGGER.error("SAT solver timeout exceeded!", e);
            return null;
        }
    }

    // Helper methods to encode problem into SAT and decode the solution

    private void encodeInitialState(Problem problem, ISolver solver, int optimalFactor) throws ContradictionException {
        BitVector initialState = problem.getInitialState().getPositiveFluents();
        for (int i = 0; i < initialState.size(); i++) {
            if (initialState.get(i)) { // Check if the fluent is true
                int var = fluentToVar(i, 0, optimalFactor); // Use optimalFactor
                solver.addClause(new VecInt(new int[] { var }));
            }
        }
    }

    private void encodeActions(Problem problem, ISolver solver, int optimalFactor) throws ContradictionException {
        for (Action action : problem.getActions()) {
            int actionId = getActionId(action);

            // Encode les préconditions
            if (action.getPrecondition() != null) {
                BitVector preconditions = action.getPrecondition().getPositiveFluents();
                for (int i = 0; i < preconditions.size(); i++) {
                    if (preconditions.get(i)) {
                        int var = fluentToVar(i, actionId, optimalFactor);
                        solver.addClause(new VecInt(new int[] { -actionId, var })); // action implique précondition
                    }
                }
            }

            // Encode uniquement les effets nécessaires
            for (ConditionalEffect condEffect : action.getConditionalEffects()) {
                Effect effect = condEffect.getEffect();
                BitVector positiveEffects = effect.getPositiveFluents();
                for (int i = 0; i < positiveEffects.size(); i++) {
                    if (positiveEffects.get(i)) {
                        // On encode uniquement si le fluent est utilisé dans l'état final ou un
                        // prérequis pour économiser la mémoire
                        if (problem.getGoal().getPositiveFluents().get(i)) {
                            int var = fluentToVar(i, actionId + 1, optimalFactor);
                            solver.addClause(new VecInt(new int[] { -actionId, var })); // action implique effet
                        }
                    }
                }
            }
        }
    }

    private void encodeGoalState(Problem problem, ISolver solver, int optimalFactor) throws ContradictionException {
        BitVector goalState = problem.getGoal().getPositiveFluents();
        for (int i = 0; i < goalState.size(); i++) {
            if (goalState.get(i)) {
                int var = fluentToVar(i, Integer.MAX_VALUE, optimalFactor); // Use optimalFactor
                solver.addClause(new VecInt(new int[] { var }));
            }
        }
    }

    // Decodes the solution from the SAT solver
    private Plan decodeSolution(int[] model, Problem problem) {
        Plan plan = new SequentialPlan();
        for (int var : model) {
            if (var > 0) { // Positive variables indicate satisfied fluents/actions
                Action action = getActionByVar(var);
                if (action != null) {
                    plan.add(0, action);
                }
            }
        }
        return plan;
    }

    // Utility to convert fluent ID to SAT variable
    private int fluentToVar(int fluentId, int timeStep, int optimalFactor) {
        // fluentId >= 0 pour éviter un identifiant SAT invalide
        if (fluentId < 0 || timeStep < 0) {
            throw new IllegalArgumentException("fluentId ou timeStep est négatif");
        }
        // résultat est strictement positif
        return 1 + fluentId + timeStep * optimalFactor; // +1 pour pas que ça puisse = 0
    }

    // Calculate optimal factor based on problem size
    private int calculateOptimalFactor(int numFluents, int numActions) {
        int maxSafeOverlap = 20; // Adjust this as needed
        return Math.max(1000, (int) Math.ceil((double) (numFluents + numActions) / maxSafeOverlap));
    }

    // Helper to get action by SAT variable
    private Action getActionByVar(int var) {
        for (Map.Entry<Action, Integer> entry : actionIds.entrySet()) {
            if (entry.getValue().equals(var)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Returns if a specified problem is supported by the planner. Just ADL problem
     * can be solved by this planner.
     *
     * @param problem the problem to test.
     * @return <code>true</code> if the problem is supported <code>false</code>
     *         otherwise.
     */
    @Override
    public boolean isSupported(Problem problem) {
        return !problem.getRequirements().contains(RequireKey.ACTION_COSTS)
                && !problem.getRequirements().contains(RequireKey.CONSTRAINTS)
                && !problem.getRequirements().contains(RequireKey.CONTINOUS_EFFECTS)
                && !problem.getRequirements().contains(RequireKey.DERIVED_PREDICATES)
                && !problem.getRequirements().contains(RequireKey.DURATIVE_ACTIONS)
                && !problem.getRequirements().contains(RequireKey.DURATION_INEQUALITIES)
                && !problem.getRequirements().contains(RequireKey.FLUENTS)
                && !problem.getRequirements().contains(RequireKey.GOAL_UTILITIES)
                && !problem.getRequirements().contains(RequireKey.METHOD_CONSTRAINTS)
                && !problem.getRequirements().contains(RequireKey.NUMERIC_FLUENTS)
                && !problem.getRequirements().contains(RequireKey.OBJECT_FLUENTS)
                && !problem.getRequirements().contains(RequireKey.PREFERENCES)
                && !problem.getRequirements().contains(RequireKey.TIMED_INITIAL_LITERALS)
                && !problem.getRequirements().contains(RequireKey.HIERARCHY);
    }

    /**
     * The main method of the <code>ASP</code> planner.
     *
     * @param args the arguments of the command line.
     */
    public static void main(String[] args) {
        try {
            final MyPlanner planner = new MyPlanner();
            if (args.length == 4) {
                planner.setTimeFile(args[2]);
                planner.setLengthFile(args[3]);
            }
            CommandLine cmd = new CommandLine(planner);
            cmd.execute(args);
        } catch (IllegalArgumentException e) {
            LOGGER.fatal(e.getMessage());
        }
    }

    /**
     * Returns the name of the heuristic used by the planner to solve a planning
     * problem.
     *
     * @return the name of the heuristic used by the planner to solve a planning
     *         problem.
     */
    public final StateHeuristic.Name getHeuristic() {
        return this.heuristic;
    }

    /**
     * Returns the weight of the heuristic.
     *
     * @return the weight of the heuristic.
     */
    public final double getHeuristicWeight() {
        return this.heuristicWeight;
    }

    /**
     * Sets the weight of the heuristic.
     *
     * @param weight the weight of the heuristic. The weight must be greater than 0.
     * @throws IllegalArgumentException if the weight is strictly less than 0.
     */
    @CommandLine.Option(names = { "-w",
            "--weight" }, defaultValue = "1.0", paramLabel = "<weight>", description = "Set the weight of the heuristic (preset 1.0).")
    public void setHeuristicWeight(final double weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight <= 0");
        }
        this.heuristicWeight = weight;
    }

    /**
     * Set the name of heuristic used by the planner to the solve a planning
     * problem.
     *
     * @param heuristic the name of the heuristic.
     */
    @CommandLine.Option(names = { "-e",
            "--heuristic" }, defaultValue = "FAST_FORWARD", description = "Set the heuristic : AJUSTED_SUM, AJUSTED_SUM2, AJUSTED_SUM2M, COMBO, "
                    + "MAX, FAST_FORWARD SET_LEVEL, SUM, SUM_MUTEX (preset: FAST_FORWARD)")
    public void setHeuristic(StateHeuristic.Name heuristic) {
        this.heuristic = heuristic;
    }
}
