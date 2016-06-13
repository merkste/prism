package prism.conditional.transform;

import java.util.BitSet;
import java.util.Vector;

import parser.ast.Expression;
import prism.LTLModelChecker;
import prism.LTLModelChecker.LTLProduct;
import prism.Model;
import prism.NondetModel;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.StateModelChecker;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceOmegaDD;
import acceptance.AcceptanceReachDD;
import acceptance.AcceptanceType;
import automata.DA;
import jdd.JDD;
import jdd.JDDNode;

// FIXME ALG: add comment
public class LTLProductTransformer<M extends Model> extends PrismComponent
{
	private final StateModelChecker modelChecker;
	private final LTLModelChecker ltlModelChecker;

	public LTLProductTransformer(final StateModelChecker modelChecker) throws PrismException
	{
		super(modelChecker);
		this.modelChecker = modelChecker;
		ltlModelChecker = new LTLModelChecker(this);
	}

	public boolean canHandle(final Model model, final Expression expression)
			throws PrismLangException
	{
		return LTLModelChecker.isSupportedLTLFormula(model.getModelType(), expression);
	}

	public LTLProduct<M> transform(final M model, final Expression expression, final JDDNode statesOfInterest, final AcceptanceType... acceptanceTypes)
			throws PrismException
	{
		final LabeledDA labeledDA = constructDA(model, expression, acceptanceTypes);
		return constructProduct(model, labeledDA, statesOfInterest);
	}

	public LabeledDA constructDA(final M model, final Expression expression, final AcceptanceType...acceptanceTypes)
			throws PrismException
	{
		final Vector<JDDNode> labels            = new Vector<JDDNode>();
		DA<BitSet,? extends AcceptanceOmega> da = null;

		// If formula is co-safe, check whether acceptance REACH is allowed
		if (Expression.isCoSafeLTLSyntactic(expression, true)) {
			for (int i=0; i<acceptanceTypes.length; i++) {
				if (acceptanceTypes[i] == AcceptanceType.REACH) {
					getLog().print("\n[" + expression + "] is co-safe, attempting to construct acceptance REACH ... ");
					da = ltlModelChecker.constructDAForLTLFormula(modelChecker, model, expression, labels, AcceptanceType.REACH, AcceptanceType.RABIN);
					if (da.getAcceptance().getType() == AcceptanceType.REACH) {
						getLog().println("Success.");
					} else {
						getLog().println("Failed. Falling back to other acceptance types.");
					}
					break;
				}
			}
		}
		// Either formula is co-safe or construction of acceptance REACH failed.
		if (da == null) {
			da = ltlModelChecker.constructDAForLTLFormula(modelChecker, model, expression, labels, acceptanceTypes);
		}

		return new LabeledDA(da, labels);
	}

	@SuppressWarnings("unchecked")
	public LTLProduct<M> constructProduct(final M model, final LabeledDA labeledDA, final JDDNode statesOfInterest)
			throws PrismException
	{
		final DA<BitSet, ? extends AcceptanceOmega> automaton = labeledDA.getAutomaton();
		final Vector<JDDNode> labels = labeledDA.getLabels();

		mainLog.println("\nConstructing " + model.getModelType() + "MC-" + automaton.getAutomataType() + " product...");
		final LTLProduct<M> product;
		if (model instanceof NondetModel) {
			product = (LTLProduct<M>) ltlModelChecker.constructProductMDP((NondetModel)model, automaton, labels, statesOfInterest);;
		} else if (model instanceof ProbModel) {
			product = (LTLProduct<M>) ltlModelChecker.constructProductMC((ProbModel)model, automaton, labels, statesOfInterest);;
		} else {
			throw new PrismException("Unsupported model type " + model.getClass());
		}
		mainLog.println();
		product.getProductModel().printTransInfo(mainLog, false);
		return product;
	}

	public JDDNode findAcceptingStates(final LTLProduct<M> product)
			throws PrismException
	{
		return findAcceptingStates(product, null);
	}

	protected JDDNode findAcceptingStates(final M productModel, final AcceptanceOmegaDD acceptance)
			throws PrismException
	{
		return findAcceptingStates(productModel, acceptance, null);
	}

	public JDDNode findAcceptingStates(final LTLProduct<M> product, final JDDNode restrict)
			throws PrismException
	{
		return findAcceptingStates(product.getProductModel(), product.getAcceptance(), restrict);
	}

	protected JDDNode findAcceptingStates(final M productModel, final AcceptanceOmegaDD acceptance, JDDNode restrict)
			throws PrismException
	{
		if (acceptance.getType() == AcceptanceType.REACH) {
			// if restrict is null we allow all reachable states for MEC search
			JDDNode goalStates      = ((AcceptanceReachDD) acceptance).getGoalStates();
			restrict                = restrict == null ? productModel.getReach().copy() : JDD.And(restrict.copy(), productModel.getReach().copy());
			JDDNode acceptingStates = JDD.And(goalStates, restrict.copy());
			JDD.Deref(restrict);
			return acceptingStates;
		}
		if (productModel instanceof NondetModel) {
			return ltlModelChecker.findAcceptingECStates(acceptance, (NondetModel) productModel, null, null, false, restrict);
		}
		if (productModel instanceof ProbModel) {
			return ltlModelChecker.findAcceptingBSCCs(acceptance, (ProbModel) productModel, restrict);
		}
		throw new PrismException("Unsupported product model type: " + productModel.getClass());
	}



	public static class LabeledDA
	{
		final DA<BitSet, ? extends AcceptanceOmega> automaton;
		final Vector<JDDNode> labels;

		public LabeledDA(DA<BitSet, ? extends AcceptanceOmega> automaton, Vector<JDDNode> labels)
		{
			this.automaton = automaton;
			this.labels = labels;
		}

		public DA<BitSet, ? extends AcceptanceOmega> getAutomaton()
		{
			return automaton;
		}

		public Vector<JDDNode> getLabels()
		{
			return labels;
		}

		public LabeledDA liftToProduct(LTLProduct<? extends Model> product)
		{
//			JDDNode reach = product.getProductModel().getReach();
			Vector<JDDNode> lifted = new Vector<JDDNode>(labels.size());
			for (JDDNode label : labels) {
				// Limit label to reachable states in product.
//				lifted.add(JDD.And(label.copy(), reach.copy());
				// Just reference the labels.
				lifted.add(label.copy());
			}
			return new LabeledDA(automaton, lifted);
		}
	}
}