package explicit.conditional;

import java.util.BitSet;

import common.functions.ArrayMapping;
import common.iterable.IterableBitSet;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.PrismException;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.StateValues;

//FIXME ALG: add comment
public class ConditionalTransformation<OM extends Model, TM extends Model> implements ModelTransformation<OM, TM>
{
	public static final boolean DEFAULT_BOOLEAN = false;
	public static final double DEFAULT_DOUBLE = Double.NaN;
	public static final int DEFAULT_INTEGER = -1;

	protected final OM originalModel;
	protected final TM transformedModel;
	protected final int numberOfStates;
	protected final Integer[] mapping;

	public ConditionalTransformation(final OM originalModel, final TM transformedModel)
	{
		this(originalModel, transformedModel, ArrayMapping.identity(originalModel.getNumStates()).getElements());
	}

	public ConditionalTransformation(final ConditionalTransformation<OM, TM> transformation)
	{
		this(transformation.originalModel, transformation.transformedModel, transformation.mapping);
	}

	public ConditionalTransformation(final OM originalModel, final TM transformedModel, final Integer[] mappingToTransformedModel)
	{
		this.originalModel = originalModel;
		this.transformedModel = transformedModel;
		this.numberOfStates = originalModel.getNumStates();
		this.mapping = mappingToTransformedModel;

		assert(mappingToTransformedModel.length == numberOfStates) : "Mapping does not cover all states of original model.";
	}

	@Override
	public OM getOriginalModel()
	{
		return originalModel;
	}

	@Override
	public TM getTransformedModel()
	{
		return transformedModel;
	}

	public Integer[] getMapping()
	{
		return mapping;
	}

	@Override
	public StateValues projectToOriginalModel(final StateValues sv) throws PrismException
	{
		if (sv.getType() instanceof TypeBool) {
			assert(sv.getBitSet() != null) : "State values are undefined.";

			final BitSet mapped = projectToOriginalModel(sv.getBitSet());
			return StateValues.createFromBitSet(mapped, originalModel);
		}
		if (sv.getType() instanceof TypeDouble) {
			assert(sv.getDoubleArray() != null) : "State values are undefined.";

			final double[] mapped = projectToOriginalModel(sv.getDoubleArray());
			return StateValues.createFromDoubleArray(mapped, originalModel);
		}
		if (sv.getType() instanceof TypeInt) {
			assert(sv.getIntArray() != null) : "State values are undefined.";

			final int[] mapped = projectToOriginalModel(sv.getIntArray());
			return StateValues.createFromIntegerArray(mapped, originalModel);
		}
		throw new PrismException("Unsupported type of state values");
	}

	public BitSet projectToOriginalModel(final BitSet values)
	{
		final BitSet result = new BitSet(numberOfStates);

		for (int state = 0; state < numberOfStates; state++) {
			final Integer mappedState = mapToTransformedModel(state);
			final boolean mappedValue = (mappedState == null) ? DEFAULT_BOOLEAN : values.get(mappedState);
			result.set(state, mappedValue);
		}
		return result;
	}

	public double[] projectToOriginalModel(final double[] values)
	{
		final double[] result = new double[numberOfStates];

		for (int state = 0; state < numberOfStates; state++) {
			final Integer mappedState = mapToTransformedModel(state);
			final double mappedValue = (mappedState == null) ? DEFAULT_DOUBLE : values[mappedState];
			result[state] = mappedValue;
		}
		return result;
	}

	public int[] projectToOriginalModel(final int[] values)
	{
		final int[] result = new int[numberOfStates];

		for (int state = 0; state < numberOfStates; state++) {
			final Integer mappedState = mapToTransformedModel(state);
			final int mappedValue = (mappedState == null) ? DEFAULT_INTEGER : values[mappedState];
			result[state] = mappedValue;
		}
		return result;
	}

	public BitSet projectToTransformedModel(final BitSet values)
	{
		final BitSet result = new BitSet();

		for (int state : new IterableBitSet(values)) {
			final Integer mappedState = mapToTransformedModel(state);
			if (mappedState != null) {
				result.set(mappedState);
			}
		}
		return result;
	}

	public Integer mapToTransformedModel(final int state)
	{
		return mapping[state];
	}

	@Override
	public BitSet getTransformedStatesOfInterest()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BitSet mapToTransformedModel(BitSet states)
	{
		// TODO Auto-generated method stub
		return null;
	}
}