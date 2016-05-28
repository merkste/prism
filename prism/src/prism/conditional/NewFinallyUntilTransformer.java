package prism.conditional;



import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.Model;
import prism.ModelExpressionTransformation;
import prism.ModelTransformation;
import prism.ModelTransformationNested;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.conditional.transform.GoalFailStopTransformer;
import prism.conditional.transform.GoalFailStopTransformer.GoalFailStopTransformation;



// FIXME ALG: add comment
public interface NewFinallyUntilTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewResetConditionalTransformer<M, MC>
{
	default boolean canHandle(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		return getNormalFormTransformer().canHandle(model, expression);
	}

	@Override
	default boolean canHandleObjective(M model, ExpressionConditional expression) throws PrismLangException
	{
		assert false : "Method should not be called, normally";
		return getNormalFormTransformer().canHandleObjective(model, expression);
	}

	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression) throws PrismLangException
	{
		assert false : "Method should not be called, normally";
		return getNormalFormTransformer().canHandleCondition(model, expression);
	}

	@Override
	default ModelExpressionTransformation<M, M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

//try {
//	model.exportToFile(Prism.EXPORT_DOT, true, new java.io.File("original_sym.dot"));
//} catch (FileNotFoundException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}

		// 1) Normal-Form Transformation
		GoalFailStopTransformer<M, MC> transformer = getNormalFormTransformer();
		GoalFailStopTransformation<M> normalFormTransformation = transformer.transform(model, expression, statesOfInterest);
		M normalFormModel     = normalFormTransformation.getTransformedModel();
		getLog().println("Normal-form transformation: " + normalFormTransformation.getTransformedExpression());

//try {
//	normalFormModel.exportToFile(Prism.EXPORT_DOT, true, new java.io.File("normal_sym.dot"));
//} catch (FileNotFoundException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}

		// 2) Reset Transformation
		JDDNode badStates = computeBadStates(normalFormTransformation);
////>>> Debug: print badStates
//getLog().println("badStates:");
//new StateValuesMTBDD(badStates.copy(), normalFormModel).print(getLog());
		// reset deterministic from states that do not satisfy the condition
		JDDNode unsatisfiedStates = normalFormTransformation.getConditonUnsatisfiedStates();
////>>> Debug: print unsatisfiedStates
//getLog().println("unsatisfiedStates:");
//new StateValuesMTBDD(unsatisfiedStates.copy(), normalFormModel).print(getLog());
		// reset from fail state as well
////>>> Debug: print goalState
//JDDNode goalState = normalFormModel.getLabelDD(normalFormTransformation.getGoalLabel());
//getLog().println("goalState:");
//new StateValuesMTBDD(goalState.copy(), normalFormModel).print(getLog());
		JDDNode failState = normalFormModel.getLabelDD(normalFormTransformation.getFailLabel());
////>>> Debug: print failState
//getLog().println("failState:");
//new StateValuesMTBDD(failState.copy(), normalFormModel).print(getLog());
////>>> Debug: print StopState
//JDDNode stopState = normalFormModel.getLabelDD(normalFormTransformation.getStopLabel());
//getLog().println("stopState:");
//new StateValuesMTBDD(stopState.copy(), normalFormModel).print(getLog());
		unsatisfiedStates = JDD.Or(unsatisfiedStates, failState.copy());
		JDDNode transformedStatesOfInterest = normalFormTransformation.getTransformedStatesOfInterest();
		ModelTransformation<M, ? extends M> resetTransformation = transformReset(normalFormModel, unsatisfiedStates, badStates, transformedStatesOfInterest);

//try {
//	resetTransformation.getTransformedModel().exportToFile(Prism.EXPORT_DOT, true, new java.io.File("reset_sym.dot"));
//} catch (FileNotFoundException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}

		// 3) Compose Transformations
		ModelTransformationNested<M, M, ? extends M> nested = new ModelTransformationNested<>(normalFormTransformation, resetTransformation);

		return new ModelExpressionTransformation<M, M>()
		{
			@Override
			public M getOriginalModel()
			{
				return nested.getOriginalModel();
			}

			@Override
			public M getTransformedModel()
			{
				return nested.getTransformedModel();
			}

			@Override
			public void clear()
			{
				nested.clear();
			}

			@Override
			public prism.StateValues projectToOriginalModel(prism.StateValues svTransformedModel) throws PrismException
			{
				return nested.projectToOriginalModel(svTransformedModel);
			}

			@Override
			public JDDNode getTransformedStatesOfInterest()
			{
				return nested.getTransformedStatesOfInterest();
			}

			@Override
			public ExpressionProb getTransformedExpression()
			{
				// FIXME ALG: use ExpressionQuant for objective type
				return (ExpressionProb) normalFormTransformation.getTransformedExpression().getObjective();
			}

			@Override
			public Expression getOriginalExpression()
			{
				return normalFormTransformation.getOriginalExpression();
			}
		};
	}

	default JDDNode computeBadStates(GoalFailStopTransformation<M> transformation)
	{
		return transformation.getConditonMaybeUnsatisfiedStates();
	}

	GoalFailStopTransformer<M, MC> getNormalFormTransformer();




	public static class DTMC extends NewResetConditionalTransformer.DTMC implements NewFinallyUntilTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public GoalFailStopTransformer.DTMC getNormalFormTransformer()
		{
			return new GoalFailStopTransformer.DTMC(modelChecker);
		}
	}



	public static class MDP extends NewResetConditionalTransformer.MDP implements NewFinallyUntilTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public GoalFailStopTransformer.MDP getNormalFormTransformer()
		{
			return new GoalFailStopTransformer.MDP(modelChecker);
		}
	}
}
