package explicit.conditional.transformer;

import java.util.Map;

import explicit.BasicModelTransformation;
import explicit.Model;
import explicit.ModelTransformation;

// FIXME ALG: add comment
public class TerminalTransformation<OM extends Model, TM extends Model> extends BasicModelTransformation<OM, TM>
{
	protected final Map<Integer, Integer> terminalMapping;

	/**
	 * @param terminalMapping terminal_transformed -> terminal_original
	 */
	public TerminalTransformation(final ModelTransformation<? extends OM, ? extends TM> transformation, final Map<Integer, Integer> terminalMapping)
	{
		super(transformation);
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