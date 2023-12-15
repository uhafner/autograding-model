package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.Generated;

/**
 * A score that has been obtained from a specific tool.
 *
 * @param <S>
 *         the actual {@link Score} type
 * @param <C>
 *         the associated {@link Configuration} type
 *
 * @author Ullrich Hafner
 */
public abstract class Score<S extends Score<S, C>, C extends Configuration> implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;

    private final String id;
    private final String name;
    private final C configuration;
    private final List<S> subScores;

    @SafeVarargs
    Score(final String id, final String name, final C configuration, final S... scores) {
        Ensure.that(id).isNotEmpty();
        Ensure.that(name).isNotEmpty();

        this.id = id;
        this.name = name;

        this.configuration = configuration;
        this.subScores = Arrays.asList(scores);
    }

    public List<S> getSubScores() {
        return subScores;
    }

    public final String getId() {
        return id;
    }

    public final String getName() {
        return name;
    }

    public final String getDisplayName() {
        return Objects.toString(getName(), configuration.getName());
    }

    public final C getConfiguration() {
        return configuration;
    }

    /**
     * Computes the impact of this score. The impact might be positive or negative depending on the configuration
     * property {@link Configuration#isPositive()}. If the impact is negative, then the score is capped at 0.
     * If the impact is positive, then the score is capped at the maximum score.
     *
     * @return the impact of this score
     */
    public abstract int getImpact();

    public int getMaxScore() {
        return configuration.getMaxScore();
    }

    /**
     * Evaluates the score value. The value is in the interval [0, {@link #getMaxScore()}]. If the configuration
     * property {@link Configuration#isPositive()} is set, then the score will increase by the impact. Otherwise,
     * the impact will reduce the maximum score.
     *
     * @return the value of this score
     */
    public int getValue() {
        if (getImpact() < 0) {
            return Math.max(0, getMaxScore() + getImpact());
        }
        else if (getImpact() > 0) {
            return Math.min(getMaxScore(), getImpact());
        }
        if (getConfiguration().isPositive()) {
            return 0;
        }
        return getMaxScore();
    }

    /**
     * Renders a short summary text of the specific score.
     *
     * @return the summary text
     */
    protected abstract String createSummary();

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Score<?, ?> score = (Score<?, ?>) o;

        if (!id.equals(score.id)) {
            return false;
        }
        if (!name.equals(score.name)) {
            return false;
        }
        if (!configuration.equals(score.configuration)) {
            return false;
        }
        return subScores.equals(score.subScores);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + configuration.hashCode();
        result = 31 * result + subScores.hashCode();
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return JacksonFacade.get().toJson(this);
    }
}
