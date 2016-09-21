package prism;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.PrismException;

/**
 * Base class for the results of a product operation between a symbolic model and
 * an automaton. Provides infrastructure for converting information on the
 * states between the original model, the automaton and the product model.
 *
 * @param <M> The type of the product model, e.g, DTMC, MDP, ...
 */
public class Product<M extends Model> implements ModelTransformation<M, M>
{
	protected M originalModel = null;
	protected M productModel = null;
	protected JDDNode productStatesOfInterest = null;
	protected JDDVars automatonRowVars = null;

	/**
	 * Constructor.
 	 * @param productModel the product model
 	 * @param originalModel the original model
	 */
	public Product(M productModel, M originalModel, JDDNode productStatesOfInterest, JDDVars automatonRowVars) {
		this.originalModel = originalModel;
		this.productModel = productModel;
		this.productStatesOfInterest = productStatesOfInterest;
		this.automatonRowVars = automatonRowVars;
	}

	/**
	 * Get the product model.
	 */
	public M getProductModel()
	{
		return productModel;
	}
	
	@Override
	public M getTransformedModel()
	{
		return getProductModel();
	}
	
	@Override
	public M getOriginalModel()
	{
		return originalModel;
	}
	
	public JDDVars getAutomatonRowVars() {
		return automatonRowVars;
	}

	public void clear() {
		if (productModel != null) productModel.clear();
		if (productStatesOfInterest != null) JDD.Deref(productStatesOfInterest);
		if (automatonRowVars != null) automatonRowVars.derefAll();
	}

	/**
	 * Project state values from the product model back to the original model. Clears svTransformed
	 * @param svTransformed the state values in the product model
	 * @return the corresponding state values in the original model
	 * @throws PrismException
	 */
	@Override
	public StateValues projectToOriginalModel(StateValues svTransformed) throws PrismException {
		// Filter against the productStatesOfInterest, i.e.,
		// set values to 0 for all states that do not correspond to the states of interest
		svTransformed.filter(productStatesOfInterest);
		// Then sum over the DD vars introduced for the automata modes to
		// get StateValues in the original model
		StateValues svOriginal = svTransformed.sumOverDDVars(automatonRowVars,originalModel);

		svTransformed.clear();
		return svOriginal;
	}

	@Override
	public JDDNode getTransformedStatesOfInterest()
	{
		return productStatesOfInterest.copy();
	}
}
