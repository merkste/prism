package explicit.conditional.transformer;

import java.util.Map;

import explicit.BasicModelTransformation;
import explicit.Model;

public class ConditionalTerminalTransformation<OM extends Model, TM extends Model> extends BasicModelTransformation<OM, TM>
{
	private final Map<Integer, Integer> terminalMapping;

	public ConditionalTerminalTransformation(final OM originalModel, final TM transformedModel, final Integer[] mapping,
			final Map<Integer, Integer> terminalMapping)
	{
		super(originalModel, transformedModel, mapping);
		this.terminalMapping = terminalMapping;
	}

	/**
	 * @return the terminalMapping
	 */
	public Map<Integer, Integer> getTerminalMapping()
	{
		return terminalMapping;
	}
}