package prism.conditional;


import jdd.JDDNode;
import prism.Model;
import prism.ModelTransformation;
import prism.Prism;
import prism.PrismException;
import prism.Product;
import prism.StateValues;

public class ConditionalTransformationProduct<M extends Model> implements ModelTransformation<M, M> {

	private Product<M> product;
	private JDDNode validStates;
	private Prism prism;
	
	public ConditionalTransformationProduct(Prism prism, final Product<M> product, JDDNode validStates) {
		this.product = product;
		this.validStates = validStates;
		this.prism = prism;
	}

	@Override
	public M getOriginalModel() {
		return product.getOriginalModel();
	}

	@Override
	public M getTransformedModel() {
		return product.getProductModel();
	}

	@Override
	public StateValues projectToOriginalModel(final StateValues svTransformed) throws PrismException {
		prism.getMainLog().println("svTransformed");
		svTransformed.print(prism.getMainLog());
		StateValues sv = product.projectToOriginalModel(svTransformed);
		prism.getMainLog().println("sv");
		sv.print(prism.getMainLog());
		
		sv.filter(validStates);
		prism.getMainLog().println("sv (filtered)");
		sv.print(prism.getMainLog());

		return sv;
	}

	@Override
	public void clear() {
		product.clear();
	}

	@Override
	public JDDNode getTransformedStatesOfInterest()
	{
		return product.getTransformedStatesOfInterest();
	}
}
