package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.google.errorprone.annotations.FormatMethod;

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
    private static final int MAX_PERCENTAGE = 100;

    private final String id;
    private final String name;
    private final String icon;
    private final C configuration;
    private final List<S> subScores;

    @SafeVarargs
    Score(final String id, final String name, final String icon, final C configuration, final S... scores) {
        Ensure.that(id).isNotEmpty();
        Ensure.that(name).isNotEmpty();

        this.id = id;
        this.name = name;
        this.icon = icon;

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

    public final String getIcon() {
        return icon;
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
     * Returns whether this score has a maximum score defined. If not, then the results will only be logged but not
     * counted.
     *
     * @return {@code true} if this score has a maximum score set, {@code false} otherwise
     */
    public boolean hasMaxScore() {
        return getMaxScore() > 0;
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
     * Returns the achieved percentage.
     *
     * @return the percentage
     */
    public int getPercentage() {
        if (hasMaxScore()) {
            return getValue() * MAX_PERCENTAGE / getMaxScore();
        }
        return MAX_PERCENTAGE;
    }

    protected int scale(final int impact, final int percentage) {
        var ratio = getMaxScore() / 100.0d;

        return Math.toIntExact(Math.round(ratio * impact * percentage));
    }

    /**
     * Renders a short summary text of the specific score.
     *
     * @return the summary text
     */
    protected abstract String createSummary();

    /**
     * Returns a formatted string using the specified format string and
     * arguments. The English locale is always used to format the string.
     *
     * @param  format
     *         A <a href="../util/Formatter.html#syntax">format string</a>
     *
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The number of arguments is
     *         variable and may be zero.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *         The behaviour on a
     *         {@code null} argument depends on the <a
     *         href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @throws  java.util.IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the <a
     *          href="../util/Formatter.html#detail">Details</a> section of the
     *          formatter class specification.
     *
     * @return  A formatted string
     *
     * @see  java.util.Formatter
     * @since  1.5
     */
    @FormatMethod
    protected String format(final String format, final Object... args) {
        return String.format(Locale.ENGLISH, format, args);
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var score = (Score<?, ?>) o;
        return Objects.equals(id, score.id)
                && Objects.equals(name, score.name)
                && Objects.equals(icon, score.icon)
                && Objects.equals(configuration, score.configuration)
                && Objects.equals(subScores, score.subScores);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(id, name, icon, configuration, subScores);
    }

    @Override
    @Generated
    public String toString() {
        return JacksonFacade.get().toJson(this);
    }
}
