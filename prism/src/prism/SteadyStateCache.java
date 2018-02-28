package prism;

import java.util.NoSuchElementException;
import java.util.Objects;

import explicit.DTMC;
import prism.SteadyStateProbs.SteadyStateProbsExplicit;
import prism.SteadyStateProbs.SteadyStateProbsSymbolic;

/**
 * Singleton cache for steady-state probabilities.
 * To avoid unnecessary memory consumption,
 * the cache holds the values for only one model at the same time.
 * This implementation is not thread safe.
 */
public class SteadyStateCache implements PrismSettingsListener
{
	protected static SteadyStateCache instance = new SteadyStateCache();

	protected Object model;
	protected SteadyStateProbs<?,?> probabilities;
	protected boolean enabled = false;
	protected double epsilon = 0.0;

	private SteadyStateCache() { }

	public static SteadyStateCache getInstance()
	{
		return instance;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public SteadyStateCache clear()
	{
		model = null;
		if (probabilities != null) {
			probabilities.clear();
			probabilities = null;
		}
		return this;
	}

	public boolean containsSteadyStateProbs(Object dtmc)
	{
		return probabilities != null && model != null && model.equals(dtmc);
	}

	public SteadyStateProbsExplicit getSteadyStateProbs(DTMC model)
	{
		if (!containsSteadyStateProbs(model)) {
			throw new NoSuchElementException("No steady-state probabilities stored for model.");
		}

		return (SteadyStateProbsExplicit) probabilities;
	}

	public SteadyStateProbsSymbolic getSteadyStateProbs(ProbModel model)
	{
		if (!containsSteadyStateProbs(model)) {
			throw new NoSuchElementException("No steady-state probabilities stored for model.");
		}

		return (SteadyStateProbsSymbolic) probabilities;
	}

	public SteadyStateProbsExplicit storeSteadyStateProbs(DTMC model, SteadyStateProbsExplicit probs, PrismSettings settings)
	{
		return store(model, probs, settings);
	}

	public SteadyStateProbsSymbolic storeSteadyStateProbs(ProbModel model, SteadyStateProbsSymbolic probs, PrismSettings settings)
	{
		return store(model, probs, settings);
	}

	protected <T extends SteadyStateProbs<?,?>> T store(Object model, T probs, PrismSettings settings)
	{
		Objects.requireNonNull(probs);
		if (!enabled) {
			return probs;
		}
		if (!containsSteadyStateProbs(model)) {
			clear();
			this.model    = model;
			probabilities = probs;
			epsilon       = settings.getDouble(PrismSettings.PRISM_TERM_CRIT_PARAM);
		}
		return probs;
	}

	/**
	 * Clear cache if caching is disabled in settings.
	 * This singleton cache should listen to only one PrismSettings object.
	 */
	@Override
	public void notifySettings(PrismSettings settings)
	{
		enabled  = settings.getBoolean(PrismSettings.PRISM_CACHE_STEADY_STATES);
		double e = settings.getDouble(PrismSettings.PRISM_TERM_CRIT_PARAM);

		if (!enabled || epsilon != e) {
			clear();
			epsilon = e;
		}
	}
}
