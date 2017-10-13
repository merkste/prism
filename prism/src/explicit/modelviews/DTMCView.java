package explicit.modelviews;

import explicit.DTMC;
import prism.ModelType;

public abstract class DTMCView extends MCView implements DTMC, Cloneable
{
	public DTMCView()
	{
		super();
	}

	public DTMCView(final ModelView model)
	{
		super(model);
	}



	//--- Object ---



	//--- Model ---

	@Override
	public ModelType getModelType()
	{
		return ModelType.DTMC;
	}
	


	//--- DTMC ---



	//--- ModelView ---



}