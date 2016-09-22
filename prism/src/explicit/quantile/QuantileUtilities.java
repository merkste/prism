package explicit.quantile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import common.BitSetTools;
import common.functions.Relation;
import common.iterable.Support;
import common.iterable.collections.SetFactory;
import parser.ast.Expression;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionQuantileHelpers;
import parser.ast.ExpressionQuantileProb;
import parser.ast.ExpressionQuantileProbNormalForm;
import parser.ast.ExpressionTemporal;
import parser.ast.RelOp;
import parser.ast.TemporalOperatorBound;
import parser.ast.TemporalOperatorBounds;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import prism.ModelType;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLangException;
import prism.PrismLog;
import prism.PrismSettings;
import prism.PrismUtils;
import explicit.BasicModelTransformation;
import explicit.CTMC;
import explicit.CTMCModelChecker;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker;
import explicit.ExportIterations;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MinMax;
import explicit.Model;
import explicit.ModelCheckerResult;
import explicit.ModelExplicit;
import explicit.ProbModelChecker;
import explicit.StateValues;
import explicit.ProbModelChecker.TermCrit;
import explicit.quantile.context.Context;
import explicit.quantile.context.Context4ExpressionQuantileProb;
import explicit.quantile.context.Context4ExpressionQuantileProbLowerRewardBound;
import explicit.quantile.context.Context4ExpressionQuantileProbUpperRewardBound;
import explicit.quantile.context.helpers.PrecomputationHelper;
import explicit.quantile.context.helpers.multiStateSolutions.LinearProgramComputer;
import explicit.quantile.context.helpers.multiStateSolutions.MultiStateSolutionMethod;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.Pair;
import explicit.quantile.dataStructure.RewardWrapper;
import explicit.quantile.dataStructure.RewardWrapperMDP;
import explicit.quantile.dataStructure.Triplet;
import explicit.quantile.topologicalSorting.TopologicalSorting;
import explicit.quantile.topologicalSorting.TopologicalSorting.QuantileSccMethod;
import explicit.rewards.Rewards;

/**
 * provides utilities and methods to calculate quantiles
 * @author mdaum
 */
public class QuantileUtilities
{
	//XXX: das Quantil beliebiger Zustaende berechnen, nicht nur von den Initialzustaenden
	//XXX:
	//XXX: am Ende ist es eine schoene Sache, wenn alle Methoden nur eine Unterscheidung zwischen einem increasing oder decreasing quantile machen
	//XXX: auf dieser Entscheidung werden dann die passenden Methoden aufgerufen
	//XXX:
	//XXX:

	//XXX:
	//XXX: !!! Reachability gesondert implementieren !!!
	//XXX:
	private final ProbModelChecker probModelChecker;
	private final PrismLog log;

	private List<Double> thresholds;
	private Map<Double, BitSet> finiteQuantileStates;
	private Map<Double, Map<Integer, Double>> quantileValues;

	private SetFactory setFactory;

	private final double termCritParam;
	private final boolean termCritAbsolute;

	public QuantileUtilities(ProbModelChecker theProbModelChecker)
	{
		probModelChecker = theProbModelChecker;
		log = theProbModelChecker.getLog();

		termCritParam = probModelChecker.getTermCritParam();
		termCritAbsolute = (probModelChecker.getTermCrit() == TermCrit.ABSOLUTE);
	}

	public PrismLog getLog()
	{
		return log;
	}

	public ProbModelChecker getProbModelChecker()
	{
		return probModelChecker;
	}

	public int getDebugLevel()
	{
		return probModelChecker.getDebugLevel();
	}

	public boolean calculatePositiveRewardStatesInParallel()
	{
		return probModelChecker.calculatePositiveRewardStatesInParallel();
	}

	public boolean calculateZeroRewardStatesInParallel()
	{
		return probModelChecker.calculateZeroRewardStatesInParallel();
	}

	public QuantileSccMethod getSccMethod()
	{
		return probModelChecker.getQuantileSccMethod();
	}

	public SetFactory getSetFactory()
	{
		return setFactory;
	}

	public boolean getTermCritAbsolute()
	{
		return termCritAbsolute;
	}

	public double getTermCritParam()
	{
		return termCritParam;
	}

	public int getMaxIters()
	{
		return probModelChecker.getMaxIters();
	}

	public MultiStateSolutionMethod getMultiStateSolutionMethod()
	{
		return probModelChecker.getMultiStateSolutionMethod();
	}

	public List<Double> getThresholds()
	{
		return thresholds;
	}

	private List<Set<Integer>> zeroRewardOrderIfSequential;
	private List<Set<Set<Integer>>> zeroRewardOrderIfParallel;

	public ForkJoinPool getWorkerPoolForPositiveRewardStates()
	{
		if (calculatePositiveRewardStatesInParallel()){
			return probModelChecker.getWorkerPool();
		}
		return null;
	}

	public ForkJoinPool getWorkerPoolForZeroRewardStates()
	{
		if (calculateZeroRewardStatesInParallel()){
			return probModelChecker.getWorkerPool();
		}
		return null;
	}

	public void shutdownWorkerPool()
	{
		ForkJoinPool workerPool = probModelChecker.getWorkerPool();
		if (workerPool != null){
			assert (! workerPool.hasQueuedSubmissions());
			workerPool.shutdownNow();
		}
	}

	private void buildQuantileValuesMap(BitSet statesOfInterest)
	{
		quantileValues = new HashMap<Double, Map<Integer, Double>>();
		for (Double threshold : thresholds) {
			Map<Integer, Double> initialValues = new HashMap<Integer, Double>();
			for (int state = statesOfInterest.nextSetBit(0); state >= 0; state = statesOfInterest.nextSetBit(state+1)){
				initialValues.put(state, Double.POSITIVE_INFINITY);
			}
			quantileValues.put(threshold, initialValues);
		}
	}

	private BasicModelTransformation<? extends MDP, ? extends MDP> getTransformation(RewardWrapper model)
	{
		if (model instanceof RewardWrapperMDP){
			return ((RewardWrapperMDP) model).getModelTransformation();
		}
		return null;
	}

	private StateValues getTransformedValues(StateValues stateValues, BasicModelTransformation<? extends MDP,? extends MDP> transformation) throws PrismException
	{
		if (transformation != null){
			return transformation.projectToOriginalModel(stateValues);
		}
		return stateValues;
	}

	private void logQuantileValues(Model model, BasicModelTransformation<? extends MDP,? extends MDP> transformation) throws PrismException
	{
		List<Double> sortedKeys = new ArrayList<Double>(quantileValues.keySet());
		Collections.sort(sortedKeys);
		StateValues result;
		for (Double threshold : sortedKeys) {
			getLog().printSeparator();
			getLog().println("\nResults for threshold " + threshold + ":");
			result = new StateValues(TypeDouble.getInstance(), model);
			for (Map.Entry<Integer, Double> stateValue : quantileValues.get(threshold).entrySet())
				result.setDoubleValue(stateValue.getKey(), stateValue.getValue());
			result = getTransformedValues(result, transformation);
			result.print(getLog(), true, false, false, true);
			result = null;
		}
		getLog().printSeparator();
	}

	private boolean isQuantileValueReached(final double value, final RelOp relOp, final double threshold)
	{
		switch (relOp) {
		case GT:
			return value > threshold;
		case GEQ:
			return (value >= threshold || PrismUtils.doublesAreClose(value, threshold, getTermCritParam(), getTermCritAbsolute()));
		case LT:
			return value < threshold;
		case LEQ:
			return (value <= threshold || PrismUtils.doublesAreClose(value, threshold, getTermCritParam(), getTermCritAbsolute()));
		default:
			return false;
		}
	}

	public void setQuantileForReward(final int reward, final Context context, final double[] values, final long timer)
	{
		final RelOp relOp = context.getRelationOperator();
		for (Double threshold : thresholds) {
			final BitSet statesOfInterest = finiteQuantileStates.get(threshold);
			for (int state = statesOfInterest.nextSetBit(0); state >= 0; state = statesOfInterest.nextSetBit(state+1)){
				final Map<Integer, Double> qValues = quantileValues.get(threshold);
				if (qValues.get(state) == Double.POSITIVE_INFINITY) {
					//the value for the state has not yet been computed
					if (isQuantileValueReached(values[state], relOp, threshold)){
						double quantileValue = reward + context.getResultAdjustment();
						if (quantileValue < 0){
							quantileValue = Double.NaN;
						}
						qValues.put(state, quantileValue);
						statesOfInterest.clear(state);
						if (getDebugLevel() > 0) {
							getLog().print("\nfyi: state " + state + ": quantile is " + quantileValue + " for threshold " + threshold + " (after " + (System.currentTimeMillis()-timer)/1000.0 + " secs)");
						}
					}
				}
			}
		}
	}

	public boolean finiteQuantileStatesAreDetermined()
	{
		for (Double threshold : finiteQuantileStates.keySet()) {
			if (!finiteQuantileStates.get(threshold).isEmpty())
				return false;
		}
		return true;
	}

	private void calculateCurrentRewardBound(Context context, CalculatedValues calculated, int rewardStep) throws PrismException
	{
		//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		context.calculateDerivableStates(calculated, rewardStep);
		if (context.getNumberOfDerivableStates() == context.getModel().getNumStates()) {
			assert (calculated.allStatesAreDefined()) : "The derivable states are not correct!";
			return;
		}
		//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		if (probModelChecker.getQuantileSccMethod() != QuantileSccMethod.NONE) {
			if (calculateZeroRewardStatesInParallel()){
				context.calculateZeroRewardStatesParallel(calculated, zeroRewardOrderIfParallel, rewardStep, getWorkerPoolForZeroRewardStates());
			} else {
				context.calculateZeroRewardStatesSequential(calculated, zeroRewardOrderIfSequential, rewardStep);
			}
			return;
		}
		//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		//only accessible when topological sorting is deactivated
		if (probModelChecker.useZeroRewardTryAndSet())
			context.calculateZeroRewardTransitionsForZeroStateRewardStatesTryAndSet(calculated, zeroRewardOrderIfSequential, rewardStep);
		Set<Integer> undefinedStates = calculated.getUndefinedStates();
		if (undefinedStates.isEmpty())
			return;
		if (rewardStep == 0 && getDebugLevel() >= 2)
			getLog().println(undefinedStates.size() + " states are in a zero-reward cycle!");
		final Pair<double[], Map<Integer, Integer>> result = context.calculateSet(undefinedStates, calculated, rewardStep);
		final double[] calculatedValues = result.getFirst();
		final Map<Integer, Integer> stateToIndex = result.getSecond();
		for (int state : undefinedStates){
			calculated.setCurrentValue(state, calculatedValues[stateToIndex.get(state)]);
		}
	}

	public Set<Integer> zeroRewardStatesWithPositiveProbability(Context4ExpressionQuantileProb context)
	{
		Set<Integer> zeroRewardStates = setFactory.getSet();
		zeroRewardStates.addAll(context.getInvariantStates());
		if (context instanceof Context4ExpressionQuantileProbLowerRewardBound)
			zeroRewardStates.addAll(context.getGoalStates());
		zeroRewardStates.retainAll(context.getZeroStateRewardStatesWithZeroRewardTransition());
		zeroRewardStates.removeAll(context.getZeroValueStates());
		if (context instanceof Context4ExpressionQuantileProbUpperRewardBound)
			zeroRewardStates.removeAll(context.getGoalStates());
		return zeroRewardStates;
	}

	private void doTopologicalSorting(Context context)
	{
		Set<Integer> zeroRewardStates = zeroRewardStatesWithPositiveProbability((Context4ExpressionQuantileProb) context);
		final int numZeroRewardStates = zeroRewardStates.size();
		assert (numZeroRewardStates + context.getNumberOfDerivableStates() == context.getModel().getNumStates()) : "You are trying to topologically sort the wrong states.";
		getLog().print("Using " + getSccMethod() + " for topological SCC-sorting of " + numZeroRewardStates + " zero-reward states ... ");
		long startTime = System.currentTimeMillis();
		final TopologicalSorting topologicalSorter = new TopologicalSorting(getSccMethod(), context.getModel(), setFactory, zeroRewardStates);
		final List<Set<Integer>> sccs = topologicalSorter.doTopologicalSorting4sequentialComputation();
		final int numSccs = sccs.size();
		getLog().println("found " + numSccs + " SCCs in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds " + ((numSccs > 0) ? " (~ " + (numZeroRewardStates*1.0/numSccs) + " states per SCC)" : ""));
		if (getDebugLevel() >= 4)
			getLog().println("found SCCs: " + sccs);
		if (! calculateZeroRewardStatesInParallel()){
			zeroRewardOrderIfSequential = sccs;
			return;
		}
		getLog().print("Analysing DAG of zero-reward components for parallel zero-reward computation ... ");
		startTime = System.currentTimeMillis();
		final List<Set<Set<Integer>>> parallelSccOrder = topologicalSorter.doTopologicalSorting4parallelComputation(sccs);
		final int numParallelTiers = parallelSccOrder.size();
		getLog().println("found " + numParallelTiers + " tiers in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds  (average degree of parallelism: " + (numSccs*1.0 / numParallelTiers) + "x)");
		zeroRewardOrderIfParallel = parallelSccOrder;
	}

	private void initialiseCalculations(Context context)
	{
		getLog().println("Calculating state-probabilities ...");
		if (calculatePositiveRewardStatesInParallel()) {
			getLog().print("\t* parallel (" + getWorkerPoolForPositiveRewardStates().getParallelism() + "x) ");
		} else {
			getLog().print("\t* sequential ");
		}
		getLog().println("computation of " + context.getPositiveRewardStates().size() + " positive reward states");
		getLog().println("\t* storage for previously calculated values: " + probModelChecker.getSettings().getString(PrismSettings.QUANTILE_VALUES_STORAGE));
		if (probModelChecker.getQuantileSccMethod() != QuantileSccMethod.NONE) {
			getLog().println("\t* topological zero-reward sorting: " + probModelChecker.getQuantileSccMethod());
			if (calculateZeroRewardStatesInParallel()) {
				getLog().print("\t* parallel (" + getWorkerPoolForZeroRewardStates().getParallelism() + "x) ");
			} else {
				getLog().print("\t* sequential ");
			}
			getLog().println("computation of zero reward states");
			getLog().println("\t* " + probModelChecker.getMultiStateSolutionMethod());
			doTopologicalSorting(context);
		} else {
			getLog().println("\t* NO topological zero-reward sorting");
			getLog().println("\t* " + (probModelChecker.useZeroRewardTryAndSet() ? "" : "NO ") + "tryAndSet");
			getLog().println("\t* " + probModelChecker.getMultiStateSolutionMethod());
			zeroRewardOrderIfSequential = new ArrayList<Set<Integer>>();
		}
	}

	public ModelCheckerResult prepareResults(int states, long timer, int iterations)
	{
		return prepareResults(states, timer, iterations, null, null);
	}
	private ModelCheckerResult prepareResults(int states, long timer, int iterations, Map<Integer, Double> currentValues,
			Map<Integer, Double> valuesForVeryLastIteration)
	{
		assert ((currentValues == null & valuesForVeryLastIteration == null) | (currentValues != null & valuesForVeryLastIteration != null));
		timer = System.currentTimeMillis() - timer;
		getLog().println("\nQuantile calculations finished for all states of interest in " + iterations + " iterations.");

		if (currentValues != null){
			getLog().println("\ncomputed values for the states of interest within the last iteration:");
			for (Integer state : valuesForVeryLastIteration.keySet())
				getLog().println("state " + state + ": " + valuesForVeryLastIteration.get(state));
			getLog().println("\ncomputed values for the states of interest:");
			for (Integer state : currentValues.keySet())
				getLog().println("state " + state + ": " + currentValues.get(state));
		}

		//the values for the quantile
		double[] soln = new double[states];
		if (quantileValues == null){
			Arrays.fill(soln, Double.POSITIVE_INFINITY);
		} else {
			Map<Integer, Double> quantilesForLastThreshold = quantileValues.get(thresholds.get(thresholds.size() - 1));
			for (Map.Entry<Integer, Double> entry : quantilesForLastThreshold.entrySet()) {
				soln[entry.getKey()] = entry.getValue();
			}
		}
		return returnResults(timer, iterations, soln);
	}
	private static ModelCheckerResult returnResults(long timer, int iterations, double[] calculatedValues)
	{
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = calculatedValues;
		res.numIters = iterations;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	private static Map<Integer, Double> initialiseValues4Logging(BitSet statesOfInterest)
	{
		Map<Integer, Double> values = new HashMap<Integer, Double>();
		for (int state = statesOfInterest.nextSetBit(0); state >= 0; state = statesOfInterest.nextSetBit(state+1)){
			values.put(state, (double) CalculatedValues.UNDEFINED);
		}
		return values;
	}

	private static void updateValues4Logging(Map<Integer, Double> values, BitSet statesOfInterest, double[] currentValues)
	{
		values.clear();
		for (int state = statesOfInterest.nextSetBit(0); state >= 0; state = statesOfInterest.nextSetBit(state+1)){
			values.put(state, currentValues[state]);
		}
		return;
	}

	private static void updateValues4Logging(Map<Integer, Double> values2update, Map<Integer, Double> values)
	{
		values2update.clear();
		for (Integer state : values.keySet())
			values2update.put(state, values.get(state));
		return;
	}

	private void calculateRewardBoundZero(Context context, CalculatedValues values) throws PrismException
	{
		if (context instanceof Context4ExpressionQuantileProbLowerRewardBound) {
			values.replaceCurrentValues(((Context4ExpressionQuantileProbLowerRewardBound) context).getExtremalProbabilities());
			return;
		}
		//the quantile is upper reward bounded
		calculateCurrentRewardBound(context, values, 0);
	}

	private ModelCheckerResult computeQuantileValues(Context context) throws PrismException
	{
		final long timer = System.currentTimeMillis();
		initialiseCalculations(context);
		CalculatedValues values = new CalculatedValues(context.getModel(), probModelChecker.getSettings().getString(PrismSettings.QUANTILE_VALUES_STORAGE), calculateZeroRewardStatesInParallel());
		ExportIterations iterationsExport = null;
		if (probModelChecker.getSettings().getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS)) {
			iterationsExport = new ExportIterations("Quantile (explicit)", PrismFileLog.create("quantile.html"));
		}
		getLog().println("\n" + values.previousValuesInfoString());
		int currentReward = 0;
		//just for logging reasons
		Map<Integer, Double> valuesForCurrentIteration = initialiseValues4Logging(context.getStatesOfInterest());
		Map<Integer, Double> valuesForPreviousIteration = initialiseValues4Logging(context.getStatesOfInterest());
		while (true){
			if (getDebugLevel() >= 6)
				getLog().println("Calculating reward step " + currentReward + ":");
			updateValues4Logging(valuesForPreviousIteration, valuesForCurrentIteration);
			if (currentReward == 0)
				calculateRewardBoundZero(context, values);
			else
				calculateCurrentRewardBound(context, values, currentReward);
			if (getDebugLevel() >= 6) {
				for (int state = 0, states = context.getModel().getNumStates(); state < states; state++)
					getLog().println("s_" + state + " " + context.getModel().getModel().getStatesList().get(state) + " -> " + values.getCurrentValue(state));
				getLog().println();
			}
			if (iterationsExport != null) {
				values.exportCurrentValues(iterationsExport);
			}
			assert (values.allStatesAreDefined()) : "NOT all states' values have been computed!";
			updateValues4Logging(valuesForCurrentIteration, context.getStatesOfInterest(), values.getCurrentValues());
			setQuantileForReward(currentReward, context, values.getCurrentValues(), timer);
			if (finiteQuantileStatesAreDetermined()) {
				if (getDebugLevel() >= 4)
					getLog().println("result = " + Arrays.toString(values.getCurrentValues()));
				break;
			}
			//go to the next iteration
			values.mergeCurrentValuesIntoPreviousValues(currentReward);
			currentReward++;
		}

		if (iterationsExport != null) {
			iterationsExport.close();
		}

		return prepareResults(context.getModel().getNumStates(), timer, currentReward+1, valuesForCurrentIteration, valuesForPreviousIteration);
	}

	private ModelCheckerResult computeRewardBoundedReachability(final Context4ExpressionQuantileProb context, final int bound, final boolean negateResult) throws PrismException
	{
		if (bound < 0){
			throw new PrismException("Negative bound: " + bound);
		}
		final long timer = System.currentTimeMillis();
		initialiseCalculations(context);
		CalculatedValues values = new CalculatedValues(context.getModel(), probModelChecker.getSettings().getString(PrismSettings.QUANTILE_VALUES_STORAGE), calculateZeroRewardStatesInParallel());
		getLog().println("\n" + values.previousValuesInfoString());
		for (int currentReward = 0; currentReward <= bound; currentReward++){
			if (currentReward == 0)
				calculateRewardBoundZero(context, values);
			else
				calculateCurrentRewardBound(context, values, currentReward);
			assert (values.allStatesAreDefined()) : "NOT all states' values have been computed!";
			if (currentReward < bound){
				values.mergeCurrentValuesIntoPreviousValues(currentReward);
			}
		}
		final double[] result = values.getCurrentValues();
		if (negateResult){
			for (int state = 0; state < result.length; state++){
				result[state] = 1.0 - result[state];
			}
		}
		return returnResults(timer, bound+1, result);
	}

	private void restrictToStatesOfInterest(final BitSet statesOfInterest)
	{
		for (Double threshold : thresholds) {
			BitSet states = finiteQuantileStates.get(threshold);
			states.and(statesOfInterest);
		}
	}

	private StateValues solveQualitativeQuantile(RewardWrapper model, ExpressionQuantileProbNormalForm expressionQuantile, BitSet statesOfInterest) throws PrismException
	{
		assert (ExpressionQuantileProbNormalForm.isQualitativeQuery(thresholds)) : "The query should be a qualitative quantile.";
		if (thresholds.size() != 1)
			throw new PrismException("Currently just one probability threshold is supported by qualitative quantile computations");
		int threshold = thresholds.get(0).intValue();
		RelOp thresholdRelation = expressionQuantile.getProbabilityRelation();
		if (!((threshold == 0 && thresholdRelation.equals(RelOp.GT)) || (threshold == 1 && thresholdRelation.equals(RelOp.GEQ))))
			throw new PrismException("The only supported combinations are \" > 0 \" and \" >= 1 \"");
		getLog().println("Solving quantile using algorithm for qualitative probability thresholds.");
		Context4ExpressionQuantileProb context = generateSuitableContext(model, expressionQuantile, statesOfInterest);
		return StateValues.createFromDoubleArray(context.calculateQualitativeQuantile(threshold), model.getModel());
	}

	private BitSet statesNotReachingPositiveReward(RewardWrapper model, boolean worstCase) throws PrismException
	{
		if (model.hasTransitionRewards()){
			return ((RewardWrapperMDP) model).restrictToZeroRewards(worstCase);
		}
		//no transition rewards defined  -->  use cheaper calculations
		switch (model.getModelType()) {
		case DTMC:
			return ((DTMCModelChecker) probModelChecker).prob0((DTMC) model.getModel(), null, model.getPositiveStateRewardStates());
		case MDP:
			return ((MDPModelChecker) probModelChecker).prob0((MDP) model.getModel(), null, model.getPositiveStateRewardStates(), worstCase, null);
		default:
			throw new PrismException("The model type " + model.getModelType() + " is not yet supported");
		}
	}

	private Pair<BitSet, BitSet> computeInvariantStatesAndGoalStates(Model model, ExpressionQuantileProbNormalForm expressionQuantile) throws PrismException
	{
		final ExpressionTemporal unboundedExpressionTemporal = expressionQuantile.getUnboundedExpressionTemporal();
		final BitSet invariantStates = probModelChecker.checkExpression(model, unboundedExpressionTemporal.getOperand1(), null).getBitSet();
		final BitSet goalStates = probModelChecker.checkExpression(model, unboundedExpressionTemporal.getOperand2(), null).getBitSet();
		return new Pair<>(invariantStates, goalStates);
	}

	private Context4ExpressionQuantileProb generateSuitableContext(RewardWrapper model, ExpressionQuantileProbNormalForm expressionQuantile, BitSet statesOfInterest)
			throws PrismException
	{
		final Pair<BitSet, BitSet> invariantStatesAndGoalStates = computeInvariantStatesAndGoalStates(model.getModel(), expressionQuantile);
		return generateSuitableContext(model, expressionQuantile, invariantStatesAndGoalStates.getFirst(), invariantStatesAndGoalStates.getSecond(), statesOfInterest);
	}

	private Context4ExpressionQuantileProb generateSuitableContext(RewardWrapper model, ExpressionQuantileProbNormalForm expressionQuantile, BitSet invariantStates, BitSet goalStates, BitSet statesOfInterest)
			throws PrismException
	{
		BitSet zeroStateRewardStates = model.getZeroStateRewardStates();

		BitSet zeroRewardForAtLeastOneChoiceStates = model.getZeroRewardForAtLeastOneChoiceStates();
		zeroRewardForAtLeastOneChoiceStates.and(zeroStateRewardStates);

		BitSet mixedChoicesRewardStates = model.getMixedChoicesRewardStates();
		mixedChoicesRewardStates.and(zeroStateRewardStates);

		final double[] extremalProbabilities = PrecomputationHelper.computeReachabilityProbabilities(model.getModel(), probModelChecker, invariantStates, goalStates, expressionQuantile.pickMinimum());
		final BitSet zeroProbabilityStates = new Support(extremalProbabilities, Relation.LEQ, 0).asBitSet();
		if (expressionQuantile.usesUpperRewardBound()) {
			return new Context4ExpressionQuantileProbUpperRewardBound(model,
					zeroRewardForAtLeastOneChoiceStates, mixedChoicesRewardStates, invariantStates, goalStates, zeroProbabilityStates, extremalProbabilities,
					statesOfInterest, this, expressionQuantile);
		}

		//lower reward bounds do need adapted zero probability states:
		//iteration 0 is already defined due to the extremalProbabilities
		//iteration r > 0 needs to be computed
		//  here, each state has zero probability whenever
		//    (i) it is not an invariant state or
		//   (ii) there is no possibility of reaching a positive reward
		//so, just assure both ...
		zeroProbabilityStates.or(BitSetTools.complement(model.getNumStates(), invariantStates));
		//since we have lower reward bounded quantiles, the best case tries to pick the maximum probabilities
		//in order to keep the probabilities as long as possible as high as possible
		zeroProbabilityStates.or(statesNotReachingPositiveReward(model, expressionQuantile.pickMaximum()));

		return new Context4ExpressionQuantileProbLowerRewardBound(model,
				zeroRewardForAtLeastOneChoiceStates, mixedChoicesRewardStates, invariantStates, goalStates, zeroProbabilityStates, extremalProbabilities,
				statesOfInterest, this, expressionQuantile);
	}

	private StateValues solveQuantitativeQuantile(RewardWrapper model, ExpressionQuantileProbNormalForm expressionQuantile, BitSet statesOfInterest) throws PrismException
	{
		getLog().println("Solving quantile using algorithm for quantitative probability thresholds.");
		final long timer = System.currentTimeMillis();
		final Context4ExpressionQuantileProb context;
		context = generateSuitableContext(model, expressionQuantile, statesOfInterest);
		getLog().println("\nTime for context generation: " + (System.currentTimeMillis() - timer) / 1000.0 + " seconds.");
		finiteQuantileStates = context.determineFiniteQuantileStates();
		restrictToStatesOfInterest(context.getStatesOfInterest());
		getLog().println("Time for precomputation: " + (System.currentTimeMillis() - timer) / 1000.0 + " seconds.\n");
		final BasicModelTransformation<? extends MDP, ? extends MDP> transformation = getTransformation(model);
		if (transformation != null){
			buildQuantileValuesMap(transformation.getTransformedStatesOfInterest());
		} else {
			buildQuantileValuesMap(statesOfInterest);
		}
		ModelCheckerResult res;
		if (finiteQuantileStatesAreDetermined())
			res = prepareResults(context.getModel().getNumStates(), timer, 0);
		else {
			if (probModelChecker.usesLpSolverOnly())
				res = LinearProgramComputer.lpSolverExclusively(context, probModelChecker.getLpSolverBound() + 1, this);
			else
				res = computeQuantileValues(context);
		}
		logQuantileValues(model.getModel(), transformation);
		return getTransformedValues(StateValues.createFromDoubleArray(res.soln, model.getModel()), transformation);
	}

	private static void verifyQuantileValue(final ProbModelChecker pmc, final Model model, final ExpressionQuantileProb expression, final double quantileValue, final int stateOfInterest)
			throws PrismException
	{
		final Expression formulaPforQuantileValue = expression.getInstantiatedExpression(quantileValue, model.getModelType());
		pmc.getLog().println("\nSatisfying formula:\n" + formulaPforQuantileValue);
		pmc.getLog().println("\nVerify that formula is satisfied for " + expression.getQuantileVariable() + "=" + quantileValue + ", formula = " + formulaPforQuantileValue);
		final StateValues svVerify = pmc.checkExpression(model, formulaPforQuantileValue, null);
		pmc.getLog().println("\nVerification of quantile result (" + quantileValue + ") = " + svVerify.getValue(stateOfInterest));
		if (svVerify.getValue(stateOfInterest) != Boolean.TRUE) {
			throw new PrismException("Verification of quantile result failed");
		}
	}

	private static void verifyPreviousValue(final ProbModelChecker pmc, final Model model, final ExpressionQuantileProb expression, double previousValue, final int stateOfInterest)
			throws PrismException
	{
		if (previousValue < 0){
			//negative bounds are forbidden
			previousValue = 0;
		}
		final Expression formulaPforPreviousValue = expression.getInstantiatedExpression(previousValue, model.getModelType());
		pmc.getLog().println("\nVerify that formula is not satisfied for " + expression.getQuantileVariable() + "=" + previousValue + ", formula = " + formulaPforPreviousValue);
		final StateValues svVerify = pmc.checkExpression(model, formulaPforPreviousValue, null);
		pmc.getLog().println("\nVerification of result for \"previous\" value (" + previousValue + ") = " + !(Boolean) svVerify.getValue(stateOfInterest));
		if (svVerify.getValue(stateOfInterest) != Boolean.FALSE){
			throw new PrismException("Verification of quantile result failed, formula would have been satisfied earlier");
		}
	}

	public static void verifyResult(final ProbModelChecker pmc, final Model model, final ExpressionQuantileProb expression, final StateValues result, final BitSet statesOfInterest)
			throws PrismException
	{
		if (statesOfInterest.cardinality() != 1){
			throw new PrismException("Result-verification only supported if exactly one state is defined");
		}
		final int stateOfInterest = statesOfInterest.nextSetBit(0);
		final Double v_init = (Double) result.getValue(stateOfInterest);
		if (!v_init.isInfinite() && !v_init.isNaN()){
			if (model.getModelType() == ModelType.CTMC){
				final double quantileValue = v_init.doubleValue();
				verifyQuantileValue(pmc, model, expression, quantileValue, stateOfInterest);
				if (expression.getMinMax().isMin() && quantileValue > 0){
					verifyPreviousValue(pmc, model, expression, quantileValue-pmc.getSettings().getDouble(PrismSettings.QUANTILE_CTMC_PRECISION), stateOfInterest);
				}
			} else {
				final int quantileValue = v_init.intValue();
				verifyQuantileValue(pmc, model, expression, quantileValue, stateOfInterest);
				if (expression.getMinMax().isMin() && quantileValue > 0){
					verifyPreviousValue(pmc, model, expression, quantileValue-1, stateOfInterest);
				}
			}
		}
	}

	public void verifyResult(final ProbModelChecker pmc, final Model model, final ExpressionQuantileProbNormalForm expression, final StateValues result, final BitSet statesOfInterest)
			throws PrismException
	{
		verifyResult(pmc, model, expression.toExpressionQuantile(model.getModelType(), thresholds), result, statesOfInterest);
	}

	public StateValues checkExpressionQuantile(final Model model, final ExpressionQuantileProbNormalForm expressionQuantile, BitSet statesOfInterest)
			throws PrismException
	{
		expressionQuantile.findAllBoundQuantileVariables();
		thresholds = expressionQuantile.getProbabilityThresholds(probModelChecker.getConstantValues());
		setFactory = new SetFactory(probModelChecker.getSetFactory(), probModelChecker.getAdaptiveSetThreshold());

		if (expressionQuantile.getInnerFormula().getType() instanceof TypeBool) {
			//XXX: das muss ich erstmal komplett ueberarbeiten, deshalb erstmal auskommentiert
			/*if (copy.isDualQuantileNeccessary()){
				if (getDebugLevel() >= 2)
					log.print("\nDual quantile transformation: " + copy + " --> ");
				copy = copy.getDualQuantile();
				if (getDebugLevel() >= 2)
					log.println(copy);
			}*/
			if (statesOfInterest == null){
				statesOfInterest = BitSetTools.asBitSet(model.getInitialStates());
			}
			final RewardWrapper wrappedModel = RewardWrapper.wrapModelAndReward(probModelChecker, model, expressionQuantile.buildRewardStructure(model, probModelChecker.getModulesFile(), probModelChecker.getConstantValues(), getLog()));
			if (ExpressionQuantileProbNormalForm.isQualitativeQuery(thresholds) && !probModelChecker.getQuantileUseQuantitative()) {
				// TODO: Remove 'if' when there is a qualitative algorithm / implementation
				// for Until with lower bound
				if (!expressionQuantile.usesLowerRewardBound()) {
					return solveQualitativeQuantile(wrappedModel, expressionQuantile, statesOfInterest);
				} else {
					// fall back to quantitative calculations below
				}
			}
			StateValues result = solveQuantitativeQuantile(wrappedModel, expressionQuantile, statesOfInterest);
			shutdownWorkerPool();
			return result;
		}
		throw new PrismException("The inner formula of a Quantile should be of a boolean type");
	}

	public StateValues checkExpressionProbPathFormulaSimple(final Model model, final ExpressionTemporal expressionTemporal, final MinMax minMax, final boolean negateResult, BitSet statesOfInterest)
			throws PrismException
	{
		setFactory = new SetFactory(probModelChecker.getSetFactory(), probModelChecker.getAdaptiveSetThreshold());
		getLog().println("Solving reward bounded reachability using backend for quantitative quantile calculations ...");
		final long timer = System.currentTimeMillis();
		final TemporalOperatorBounds boundsExpression = expressionTemporal.getBounds();
		assert (boundsExpression.countBounds() == 1);
		final TemporalOperatorBound temporalOperatorBound;
		final Rewards rewards;
		if (boundsExpression.getRewardBounds().isEmpty()){
			//a step-bound is given
			temporalOperatorBound = boundsExpression.getStepBoundForDiscreteTime();
			rewards = ExpressionQuantileHelpers.buildRewardStructure(model, probModelChecker.getModulesFile(), probModelChecker.getConstantValues(), getLog(), null);
		} else {
			temporalOperatorBound = boundsExpression.getRewardBounds().get(0);
			rewards = ExpressionQuantileHelpers.buildRewardStructure(model, probModelChecker.getModulesFile(), probModelChecker.getConstantValues(), getLog(), temporalOperatorBound.getRewardStructureIndex());
		}
		if (temporalOperatorBound.hasLowerBound() && temporalOperatorBound.hasUpperBound()){
			throw new PrismException("quantile-backend does not yet support lower and upper bounds at the same time");
		}
		final ExpressionQuantileProbNormalForm expressionQuantile = getExpressionQuantileProbNormalForm(expressionTemporal, minMax, temporalOperatorBound.hasUpperBound(), negateResult);
		RewardWrapper wrappedModel = RewardWrapper.wrapModelAndReward(probModelChecker, model, rewards);
		final Context4ExpressionQuantileProb context;
		context = generateSuitableContext(wrappedModel, expressionQuantile, statesOfInterest);
		getLog().println("\nTime for context generation: " + (System.currentTimeMillis() - timer) / 1000.0 + " seconds.");
		ModelCheckerResult res = computeRewardBoundedReachability(context, getBound(temporalOperatorBound), negateResult);
		shutdownWorkerPool();
		return getTransformedValues(StateValues.createFromDoubleArray(res.soln, wrappedModel.getModel()), getTransformation(wrappedModel));
	}

	private ExpressionQuantileProbNormalForm getExpressionQuantileProbNormalForm(final ExpressionTemporal expressionTemporal, final MinMax minMax, final boolean hasUpperBound, final boolean negateResult)
			throws PrismLangException
	{
		final ExpressionQuantileProbNormalForm expressionQuantile = new ExpressionQuantileProbNormalForm();
		final ExpressionTemporal untilForm = (ExpressionTemporal) expressionTemporal.convertToUntilForm();
		expressionQuantile.setInnerFormula(new ExpressionProb(untilForm, minMax, "=", null));
		if ((hasUpperBound && minMax.isMax()) || ((! hasUpperBound) && minMax.isMin())){
			expressionQuantile.setExistential();
		} else {
			expressionQuantile.setUniversal();
		}
		if (negateResult){
			if (expressionQuantile.isExistential()){
				expressionQuantile.setUniversal();
			} else {
				expressionQuantile.setExistential();
			}
		}
//		expressionQuantile.setResultAdjustment(getResultAdjustment());
		return expressionQuantile;
	}

	private int getBound(final TemporalOperatorBound temporalOperatorBound) throws PrismLangException
	{
		if (temporalOperatorBound.hasUpperBound()){
			final int bound = temporalOperatorBound.getUpperBound().evaluateInt(probModelChecker.getConstantValues());
			if (temporalOperatorBound.upperBoundIsStrict()){
				return bound-1;
			}
			return bound;
		}
		final int bound = temporalOperatorBound.getLowerBound().evaluateInt(probModelChecker.getConstantValues());
		if (temporalOperatorBound.lowerBoundIsStrict()){
			return bound+1;
		}
		return bound;
	}

	private static BitSet stateOfInterestAsBitSet(final int stateOfInterest)
	{
		final BitSet bitSet = new BitSet(stateOfInterest+1);
		bitSet.set(stateOfInterest);
		return bitSet;
	}

	private int checkExpressionQuantileNaiveLinearStepping(final Model model, final ExpressionQuantileProbNormalForm expressionQuantileProbNormalForm, final int stateOfInterest)
			throws PrismException
	{
		final BitSet stateOfInterestAsBitSet = stateOfInterestAsBitSet(stateOfInterest);
		final ExpressionQuantileProb expressionQuantileProb = expressionQuantileProbNormalForm.toExpressionQuantile(model.getModelType(), thresholds);
		int currentIteration = 0;
		while (true){
			final Expression expression = expressionQuantileProb.getInstantiatedExpression(currentIteration);
			final StateValues stateValues = probModelChecker.checkExpression(model, expression, stateOfInterestAsBitSet);
			log.println("\n=== Linear search, iteration " + currentIteration + ", testing bound = " + currentIteration + ", " + expression + "\n");
			if (stateValues.getValue(stateOfInterest) == Boolean.TRUE){
				return currentIteration;
			}
			currentIteration++;
		}
	}

	private double binarySearch(final CTMC model, final ExpressionQuantileProb expressionQuantileProb, final RelOp relationOperator, final double threshold, final BitSet stateOfInterest, final int lowerBound, final int upperBound, double epsilon, final double standardEpsilon, int iterations)
			throws PrismException
	{
		assert (stateOfInterest.cardinality() == 1);
		log.println("\n=== Starting binary search ===");
		double left = lowerBound;
		double right = upperBound;
		double center = left + ((right - left) / 2.0);
		while (true){
			final Expression expression = expressionQuantileProb.getInstantiatedExpressionForDoubleResults(center);
			final Triplet<Boolean, Double, Integer> resultForCurrentIteration = checkCurrentIteration(model, expression, relationOperator, threshold, stateOfInterest, epsilon, standardEpsilon, iterations);
			epsilon = resultForCurrentIteration.getSecond();
			iterations = resultForCurrentIteration.getThird();
			if (resultForCurrentIteration.getFirst()){
				right = center;
			} else {
				left = center;
			}
			center = left + ((right - left) / 2.0);
			if (right < left || right - left < probModelChecker.getSettings().getDouble(PrismSettings.QUANTILE_CTMC_PRECISION)){
				if (right < left){
					log.println("Binary search terminated due to empty interval, too small for double precision.");
				}
				return (expressionQuantileProb.chooseIntervalUpperBound())? right : left;
			}
		}
	}

	private int binarySearch(final Model model, final ExpressionQuantileProb expressionQuantileProb, final BitSet stateOfInterest, final int lowerBound, final int upperBound, int iterations)
			throws PrismException
	{
		assert (model.getModelType() != ModelType.CTMC);
		assert (stateOfInterest.cardinality() == 1);
		log.println("\n=== Starting binary search ===");
		final int indexOfInterest = stateOfInterest.nextSetBit(0);
		int left = lowerBound;
		int right = upperBound;
		int center = left + ((right - left) / 2);
		while (left < right){
			final Expression expression = expressionQuantileProb.getInstantiatedExpression(center);
			final StateValues stateValues = probModelChecker.checkExpression(model, expression, stateOfInterest);
			log.println("\n=== iteration " + (++iterations) + ", testing bound = " + center + ", " + expression + "\n");
			if (stateValues.getValue(indexOfInterest) == Boolean.TRUE){
				right = center;
			} else {
				left = center+1;
			}
			center = left + ((right - left) / 2);
		}
		return center;
	}

	private Triplet<Boolean, Double, Integer> checkCurrentIteration(final Model model, final Expression expression, final RelOp relationOperator, final double threshold, final BitSet stateOfInterest, double epsilon, final double standardEpsilon, int iterations)
			throws PrismException
	{
		assert (stateOfInterest.cardinality() == 1);
		final int indexOfInterest = stateOfInterest.nextSetBit(0);
		double result;
		do {
			probModelChecker.setTermCritParam(epsilon);
			log.println("\n=== iteration " + (++iterations) + ", testing expression " + expression + ", precision " + epsilon);
			final StateValues stateValues = probModelChecker.checkExpression(model, expression, stateOfInterest);
			result = (double) stateValues.getValue(indexOfInterest);
			log.println("\n=== Result for expression " + expression + ": " + result + " with precision " + epsilon + "\n");
			switch (relationOperator){
			case GT:
				if (result-epsilon > threshold){
					//it is sure that the result is true
					return new Triplet<>(true, epsilon, iterations);
				}
				if (result+epsilon <= threshold){
					//it is sure that the result is false
					return new Triplet<>(false, epsilon, iterations);
				}
				break;
			case GEQ:
				if (result-epsilon >= threshold){
					//it is sure that the result is true
					return new Triplet<>(true, epsilon, iterations);
				}
				if (result+epsilon < threshold){
					//it is sure that the result is false
					return new Triplet<>(false, epsilon, iterations);
				}
				break;
			case LT:
				if (result+epsilon < threshold){
					//it is sure that the result is true
					return new Triplet<>(true, epsilon, iterations);
				}
				if (result-epsilon >= threshold){
					//it is sure that the result is false
					return new Triplet<>(false, epsilon, iterations);
				}
				break;
			case LEQ:
				if (result+epsilon <= threshold){
					//it is sure that the result is true
					return new Triplet<>(true, epsilon, iterations);
				}
				if (result-epsilon > threshold){
					//it is sure that the result is false
					return new Triplet<>(false, epsilon, iterations);
				}
				break;
			default:
				throw new PrismException(relationOperator + " is not supported");
			}
			epsilon = Math.max(epsilon/10.0, standardEpsilon);
		} while (epsilon > standardEpsilon);
		return new Triplet<>(result > threshold, epsilon, iterations);
	}

	private double checkExpressionQuantileNaiveExponentialStepping(final Model model, final ExpressionQuantileProbNormalForm expressionQuantileProbNormalForm, final int stateOfInterest)
			throws PrismException
	{
		final BitSet stateOfInterestAsBitSet = stateOfInterestAsBitSet(stateOfInterest);
		final ExpressionQuantileProb expressionQuantileProb = expressionQuantileProbNormalForm.toExpressionQuantile(model.getModelType(), thresholds);
		int currentIteration = 0;
		int iterations = 0;
		final double standardPrecision = probModelChecker.getTermCritParam();
		final double startPrecision = probModelChecker.getSettings().getDouble(PrismSettings.QUANTILE_CTMC_START_PRECISION);
		double epsilon = (startPrecision > 0) ? startPrecision : standardPrecision;
		final double threshold = thresholds.get(0);
		log.println("\n=== Starting exponential search ===");
		while (true){
			final Expression expression = expressionQuantileProb.getInstantiatedExpressionForDoubleResults(currentIteration);
			final Triplet<Boolean, Double, Integer> resultForCurrentIteration = checkCurrentIteration(model, expression, expressionQuantileProbNormalForm.getProbabilityRelation(), threshold, stateOfInterestAsBitSet, epsilon, standardPrecision, iterations);
			epsilon = resultForCurrentIteration.getSecond();
			iterations = resultForCurrentIteration.getThird();
			if (resultForCurrentIteration.getFirst()){
				break;
			}
			//go for next iteration
			if (currentIteration == 0){
				currentIteration = 1;
			} else {
				currentIteration *= 2;
			}
		}
		//do integer division by /2 to get the last failing bound, so we get lowerBound=0 for bound 1
		final int lowerBound = currentIteration/2;
		final int upperBound = currentIteration;
		if (upperBound == 0){
			assert (lowerBound == 0);
			if ((model.getModelType() == ModelType.CTMC) && (expressionQuantileProb.chooseIntervalUpperBound() == false)){
				//if upper bound of interval should be chosen, we are looking for the maximal reward that can be achieved
				//but already the very first iteration revealed that there is no such maximal reward
				//we are looking for the minimal reward s.t. we are under our threshold for the first time
				//the computation revealed that this is already the case for reward-bound 0
				//so, for each bound greater 0, the probability is under the given threshold
				//so, there is no bound that can be greater the given threshold
				return Double.NaN;
			}
			return 0;
		}
		//exponential search revealed that the quantile-value must reside somewhere in the interval [lowerBound, upperBound]
		if (model.getModelType() == ModelType.CTMC){
			return binarySearch((CTMC) model, expressionQuantileProb, expressionQuantileProbNormalForm.getProbabilityRelation(), threshold, stateOfInterestAsBitSet, lowerBound, upperBound, epsilon, standardPrecision, iterations);
		}
		//reset termination epsilon
		probModelChecker.setTermCritParam(standardPrecision);
		return binarySearch(model, expressionQuantileProb, stateOfInterestAsBitSet, lowerBound, upperBound, iterations);
	}

	private double solveQualitativeQuantile(final CTMC model, final int stateOfInterest, final BitSet invariantStates, final BitSet goalStates, final ExpressionQuantileProbNormalForm expressionQuantileProbNormalForm)
			throws PrismException
	{
		final int threshold = thresholds.get(0).intValue();
		if (threshold == 0){
			if (goalStates.get(stateOfInterest)){
				//goal already reached
				return 0;
			}
			//goal not already reached ...
			if (((CTMCModelChecker) probModelChecker).prob0(model, invariantStates, goalStates).get(stateOfInterest)){
				//... and there is no possibility of reaching it
				//=> there is no quantile-value
				return (expressionQuantileProbNormalForm.chooseIntervalUpperBound())? Double.NaN : Double.POSITIVE_INFINITY;
			}
			//... but the goal can be reached with positive probability
			//choose the smallest positive time-step that is possible for the transition to goal
			//=> return epsilon
			return probModelChecker.getSettings().getDouble(PrismSettings.QUANTILE_CTMC_PRECISION);
		}
		assert (threshold == 1);
		if (expressionQuantileProbNormalForm.usesUpperRewardBound()){
			if (goalStates.get(stateOfInterest)){
				//goal already reached
				return 0;
			}
			//not already in goal
			//there is a path that will reside in a non-goal state arbitrarily long
			//so, probability 1 is not possible
			return (expressionQuantileProbNormalForm.chooseIntervalUpperBound())? Double.NaN : Double.POSITIVE_INFINITY;
		}
		assert (expressionQuantileProbNormalForm.usesLowerRewardBound());
		if (((CTMCModelChecker) probModelChecker).prob1(model, invariantStates, goalStates).get(stateOfInterest)){
			//there is a chance of fulfilling A U B with probability 1
			//so, the minimal time-bound that can be guaranteed is 0
			return 0;
		}
		//fulfilling A U B can not be guaranteed with probability 1
		//so, there is no minimal time-bound that can guarantee this
		return (expressionQuantileProbNormalForm.chooseIntervalUpperBound())? Double.NaN : Double.POSITIVE_INFINITY;
	}

	public StateValues checkExpressionQuantileNaive(final Model model, final ExpressionQuantileProbNormalForm expressionQuantileProbNormalForm, final BitSet statesOfInterest)
			throws PrismException
	{
		expressionQuantileProbNormalForm.findAllBoundQuantileVariables();
		thresholds = expressionQuantileProbNormalForm.getProbabilityThresholds(probModelChecker.getConstantValues());
		if (thresholds.size() > 1){
			throw new PrismException("multi-thresholds are not supported");
		}
		if (statesOfInterest.cardinality() != 1){
			throw new PrismException("exactly one state of interest must be specified");
		}
		final int stateOfInterest = statesOfInterest.nextSetBit(0);
		
		long timer = System.currentTimeMillis();
		//compute satisfaction sets of sub-formulas a and b (since a U b is considered ...)
		final ExpressionProb innerFormula = expressionQuantileProbNormalForm.getInnerFormula();
		innerFormula.setExpression(probModelChecker.handleMaximalStateFormulas((ModelExplicit) model, innerFormula.getExpression(), new LTLModelChecker(probModelChecker)));
		getLog().println("Time for expanding sub-formulas: " + (System.currentTimeMillis() - timer) / 1000.0 + " seconds.\n");
		
		timer = System.currentTimeMillis();
		final Pair<BitSet,BitSet> invariantStatesAndGoalStates = computeInvariantStatesAndGoalStates(model, expressionQuantileProbNormalForm);
		final BitSet invariantStates = invariantStatesAndGoalStates.getFirst();
		final BitSet goalStates = invariantStatesAndGoalStates.getSecond();
		
		if (model.getModelType() == ModelType.CTMC && ExpressionQuantileProbNormalForm.isQualitativeQuery(thresholds)){
			final double[] soln = new double[model.getNumStates()];
			soln[stateOfInterest] = solveQualitativeQuantile((CTMC) model, stateOfInterest, invariantStates, goalStates, expressionQuantileProbNormalForm);
			return StateValues.createFromDoubleArray(soln, model);
		}
		final double[] extremalProbabilities;
		if (expressionQuantileProbNormalForm.usesUpperRewardBound()){
			extremalProbabilities = PrecomputationHelper.computeReachabilityProbabilities(model, probModelChecker, invariantStates, goalStates, expressionQuantileProbNormalForm.pickMinimum());
		} else {
			if (model.getModelType() == ModelType.CTMC){
				extremalProbabilities = PrecomputationHelper.reachabilityProbabilitiesLowerRewardBound((CTMC) model, invariantStates, goalStates, (CTMCModelChecker) probModelChecker);
			} else {
				getLog().println("ATTENTION: lower-reward bounded quantiles in discrete-time models support NO precomputation ...");
				getLog().println("\tquantile-computation may end in non-terminating loop ...");
				extremalProbabilities = new double[model.getNumStates()];
			}
		}
		finiteQuantileStates = PrecomputationHelper.finiteQuantileStatesMap(expressionQuantileProbNormalForm.getProbabilityRelation(), extremalProbabilities, thresholds);
		restrictToStatesOfInterest(statesOfInterest);
		getLog().println("Time for precomputation: " + (System.currentTimeMillis() - timer) / 1000.0 + " seconds.\n");
		
		if (finiteQuantileStatesAreDetermined()){
			return StateValues.createFromDoubleArray(prepareResults(model.getNumStates(), timer, 0).soln, model);
		}
		final double[] soln = new double[model.getNumStates()];
		final double result;
		if (probModelChecker.getSettings().getBoolean(PrismSettings.QUANTILE_NAIVE_COMPUTATION_LINEAR)){
			if (model.getModelType() == ModelType.CTMC){
				throw new PrismException("linear stepping NOT supported for continuous-time -- try exponential stepping instead!");
			}
			result = checkExpressionQuantileNaiveLinearStepping(model, expressionQuantileProbNormalForm, stateOfInterest);
		} else {
			result = checkExpressionQuantileNaiveExponentialStepping(model, expressionQuantileProbNormalForm, stateOfInterest);
		}
		soln[stateOfInterest] = result + expressionQuantileProbNormalForm.getResultAdjustment();
		return StateValues.createFromDoubleArray(soln, model);
	}
}
