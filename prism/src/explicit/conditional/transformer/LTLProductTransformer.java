package explicit.conditional.transformer;

import java.util.BitSet;
import java.util.Vector;

import parser.ast.Expression;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceReach;
import acceptance.AcceptanceType;
import automata.DA;
import common.BitSetTools;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.PredecessorRelation;
import explicit.LTLModelChecker.LTLProduct;
import explicit.ProbModelChecker;

// FIXME ALG: add comment
public class LTLProductTransformer<M extends Model> extends PrismComponent
{
	public static final BitSet ALL_STATES = null;

	protected final ProbModelChecker modelChecker;
	protected final LTLModelChecker ltlModelChecker;

	public LTLProductTransformer(final ProbModelChecker modelChecker)
	{
		super(modelChecker);
		this.modelChecker = modelChecker;
		ltlModelChecker = new LTLModelChecker(this);
	}

	public ProbModelChecker getModelChecker(M model) throws PrismException
	{
		return modelChecker;
		// FIXME ALG: Check why this leads to constants that cannot be evaluated.
		// Create fresh model checker for model
		// return (ProbModelChecker) ProbModelChecker.createModelChecker(model.getModelType());
	}

	public boolean canHandle(final Model model, final Expression expression)
			throws PrismLangException
	{
		return LTLModelChecker.isSupportedLTLFormula(model.getModelType(), expression);
	}

	public LTLProduct<M> transform(final M model, final Expression expression, final BitSet statesOfInterest, final AcceptanceType... acceptanceTypes)
			throws PrismException
	{
		final LabeledDA labeledDA = constructDA(model, expression, acceptanceTypes);
		return constructProduct(model, labeledDA, statesOfInterest);
	}

	public LabeledDA constructDA(final M model, final Expression expression, final AcceptanceType...acceptanceTypes)
			throws PrismException
	{
		ProbModelChecker mc                     = getModelChecker(model);
		Vector<BitSet> labels                   = new Vector<BitSet>();
		DA<BitSet,? extends AcceptanceOmega> da = null;

		if (Expression.isCoSafeLTLSyntactic(expression, true)) {
			boolean containsRabin = false, containsReach = false;
			for (AcceptanceType type : acceptanceTypes) {
				containsRabin |= type == AcceptanceType.RABIN;
				containsReach |= type == AcceptanceType.REACH;
			}
			if (containsReach) {
				getLog().print("\n[" + expression + "] is co-safe, attempting to construct acceptance REACH ... ");
				da = ltlModelChecker.constructDAForLTLFormula(mc, model, expression, labels, AcceptanceType.REACH, AcceptanceType.RABIN);
				if (da.getAcceptance().getType() == AcceptanceType.REACH) {
					getLog().println("Success.");
				} else if (containsRabin){
					getLog().println("Failed. Falling back to acceptance RABIN.");
				} else {
					getLog().println("Failed. Falling back to other acceptance types and build new automaton.");
					da     = null;
					labels.clear();
				}
			}
		}
		// Either formula is not co-safe or construction of acceptance REACH failed and RABIN is not allowed.
		if (da == null) {
			da = ltlModelChecker.constructDAForLTLFormula(mc, model, expression, labels, acceptanceTypes);
		}

		return new LabeledDA(da, labels);
	}

	@SuppressWarnings("unchecked")
	public LTLProduct<M> constructProduct(final M model, final LabeledDA labeledDA, final BitSet statesOfInterest)
			throws PrismException
	{
		final DA<BitSet, ? extends AcceptanceOmega> automaton = labeledDA.getAutomaton();
		final Vector<BitSet> labels = labeledDA.getLabels();

		mainLog.println("\nConstructing " + model.getModelType() + "-" + automaton.getAutomataType() + " product...");
		final LTLProduct<M> product;
		if (model instanceof MDP) {
			product = (LTLProduct<M>) ltlModelChecker.constructProductModel(automaton, (MDP)model, labels, statesOfInterest);
		} else if (model instanceof DTMC) {
			product = (LTLProduct<M>) ltlModelChecker.constructProductModel(automaton, (DTMC)model, labels, statesOfInterest);
		} else {
			throw new PrismException("Unsupported model type " + model.getClass());
		}
		mainLog.println();
		mainLog.print(product.getProductModel().infoStringTable());
		return product;
	}

	public BitSet findAcceptingStates(final LTLProduct<M> product)
			throws PrismException
	{
		return findAcceptingStates(product, ALL_STATES);
	}

	public BitSet findAcceptingStates(final M productModel, final AcceptanceOmega acceptance)
			throws PrismException
	{
		return findAcceptingStates(productModel, acceptance, ALL_STATES);
	}

	public BitSet findAcceptingStates(final LTLProduct<M> product, final BitSet remain)
			throws PrismException
	{
		return findAcceptingStates(product.getProductModel(), product.getAcceptance(), remain);
	}

	public BitSet findAcceptingStates(final M productModel, final AcceptanceOmega acceptance, final BitSet remain)
			throws PrismException
	{
		if (acceptance.getType() == AcceptanceType.REACH) {
			return findAcceptingStates(productModel, (AcceptanceReach) acceptance, remain);
		}
		if (productModel instanceof MDP) {
			return ltlModelChecker.findAcceptingECStates((MDP) productModel, acceptance, remain);
		}
		if (productModel instanceof DTMC) {
			return ltlModelChecker.findAcceptingBSCCs((DTMC) productModel, acceptance, remain);
		}
		throw new PrismException("Unsupported product model type: " + productModel.getClass());
	}

	public BitSet findAcceptingStates(final M productModel, final AcceptanceReach acceptance, BitSet remain)
			throws PrismException
	{
		BitSet acceptingStates = acceptance.getGoalStates();
		if (remain == ALL_STATES || BitSetTools.isSubset(acceptingStates, remain)) {
			return acceptingStates;
		}
		// restrict goalStates to reachable states
		acceptingStates = BitSetTools.intersect(acceptingStates, remain);
		if (productModel instanceof MDP) {
			// Prob1E (G accepting) = Prob1E (!F !accepting) = Prob0E (F !accepting)
			MDPModelChecker mc        = (MDPModelChecker) getModelChecker(productModel);
			BitSet nonAcceptingStates = BitSetTools.complement(productModel.getNumStates(), acceptingStates);
			PredecessorRelation pre   = productModel.getPredecessorRelation(mc, true);
			return mc.prob0((MDP) productModel, ALL_STATES, nonAcceptingStates, true, null, pre);
		}
		if (productModel instanceof DTMC) {
			// Prob1 (G accepting) = Prob1 (!F !accepting) = Prob0 (F !accepting)
			DTMCModelChecker mc       = (DTMCModelChecker) getModelChecker(productModel);
			BitSet nonAcceptingStates = BitSetTools.complement(productModel.getNumStates(), acceptingStates);
			PredecessorRelation pre   = productModel.getPredecessorRelation(mc, true);
			return mc.prob0((DTMC) productModel, ALL_STATES, nonAcceptingStates, pre);
		}
		throw new PrismException("Unsupported product model type: " + productModel.getClass());
	}



	public static class LabeledDA implements Cloneable
	{
		final protected DA<BitSet, ? extends AcceptanceOmega> automaton;
		protected Vector<BitSet> labels;

		public LabeledDA(DA<BitSet, ? extends AcceptanceOmega> automaton, Vector<BitSet> labels)
		{
			this.automaton = automaton;
			this.labels = labels;
		}

		@SuppressWarnings("unchecked")
		@Override
		public LabeledDA clone()
		{
			try {
				LabeledDA clone = (LabeledDA) super.clone();
				clone.labels    = (Vector<BitSet>) labels.clone();
				return clone;
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				throw new RuntimeException("Object#clone is expected to work for Cloneable objects.", e);
			}
		}

		public DA<BitSet, ? extends AcceptanceOmega> getAutomaton()
		{
			return automaton;
		}

		public Vector<BitSet> getLabels()
		{
			return labels;
		}

		public LabeledDA liftToProduct(LTLProduct<? extends Model> product)
		{
			Vector<BitSet> lifted = new Vector<BitSet>(labels.size());
			for (BitSet label : labels) {
				lifted.add(product.liftFromModel(label));
			}
			return new LabeledDA(automaton, lifted);
		}
	}
}