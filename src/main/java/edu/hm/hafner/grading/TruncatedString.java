package edu.hm.hafner.grading;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.grading.TruncatedString.Joiner.Accumulator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Utility wrapper that silently truncates output with a message at a certain size.
 *
 * <p>
 * The GitHub Checks API has a size limit on text fields. Because it also accepts Markdown, it is not trivial to
 * truncate to the required length as this could lead to unterminated syntax. The use of this class allows for adding
 * chunks of complete Markdown until an overflow is detected, at which point a message will be added and all future
 * additions will be silently discarded.
 * </p>
 *
 * @author Bill Collins
 */
public final class TruncatedString {
    private final List<String> chunks;
    private final String truncationText;
    private final boolean truncateStart;
    private final boolean chunkOnNewlines;

    private TruncatedString(final List<String> chunks, final String truncationText,
            final boolean truncateStart, final boolean chunkOnNewlines) {
        this.chunks = Collections.unmodifiableList(Objects.requireNonNull(chunks));
        this.truncationText = Objects.requireNonNull(truncationText);
        this.truncateStart = truncateStart;
        this.chunkOnNewlines = chunkOnNewlines;
    }

    /**
     * Wrap the provided string as a {@link TruncatedString}.
     *
     * @param string
     *         String to wrap as a {@link TruncatedString}
     *
     * @return a {@link TruncatedString} wrapping the provided input
     */
    static TruncatedString fromString(final String string) {
        return new TruncatedStringBuilder().setChunkOnNewlines().addText(string).build();
    }

    /**
     * Builds the string without truncation.
     *
     * @return A string comprising the joined chunks.
     */
    @Override
    public String toString() {
        return String.join("", chunks);
    }

    private List<String> getChunks() {
        if (chunkOnNewlines) {
            return Arrays.asList(String.join("", chunks).split("(?<=\r?\n)"));
        }
        return new ArrayList<>(chunks);
    }

    /**
     * Builds the string such that it does not exceed maxSize in bytes, including the truncation string.
     *
     * @param maxSize
     *         the maximum size of the resultant string.
     *
     * @return A string comprising as many of the joined chunks that will fit in the given size, plus the truncation
     *         string if truncation was necessary.
     */
    public String buildByBytes(final int maxSize) {
        return build(maxSize, false);
    }

    /**
     * Builds the string such that it does not exceed maxSize in chars, including the truncation string.
     *
     * @param maxSize
     *         the maximum size of the resultant string.
     *
     * @return A string comprising as many of the joined chunks that will fit in the given size, plus the truncation
     *         string if truncation was necessary.
     */
    public String buildByChars(final int maxSize) {
        return build(maxSize, true);
    }

    private String build(final int maxSize, final boolean chunkOnChars) {
        List<String> parts = getChunks();
        if (truncateStart) {
            Collections.reverse(parts);
        }
        List<String> truncatedParts = parts.stream().collect(new Joiner(truncationText, maxSize, chunkOnChars));
        if (truncateStart) {
            Collections.reverse(truncatedParts);
        }
        return String.join("", truncatedParts);
    }

    /**
     * TruncatedStringBuilder for {@link TruncatedString}.
     */
    public static class TruncatedStringBuilder {
        private String truncationText = "Output truncated.";
        private boolean truncateStart;
        private boolean chunkOnNewlines;
        private final List<String> chunks = new ArrayList<>();

        /**
         * Builds the {@link TruncatedString}.
         *
         * @return the build {@link TruncatedString}.
         */
        public TruncatedString build() {
            return new TruncatedString(chunks, truncationText, truncateStart, chunkOnNewlines);
        }

        /**
         * Adds a chunk of text to the builder.
         *
         * @param text
         *         the chunk of text to append to this builder
         *
         * @return this builder
         */
        @CanIgnoreReturnValue
        public TruncatedStringBuilder addText(final String text) {
            chunks.add(text);
            return this;
        }

        /**
         * Adds a chunk of text to the builder, if the specified guard is {@code true}.
         *
         * @param text
         *         the chunk of text to append to this builder
         * @param guard
         *         determines if the text should be added
         *
         * @return this builder
         */
        @CanIgnoreReturnValue
        public TruncatedStringBuilder addTextIf(final String text, final boolean guard) {
            if (guard) {
                chunks.add(text);
            }
            return this;
        }

        /**
         * Adds a newline to the builder.
         *
         * @return this builder
         */
        @CanIgnoreReturnValue
        public TruncatedStringBuilder addNewline() {
            chunks.add("\n");
            return this;
        }

        /**
         * Adds a paragraph to the builder. A paragraph consists of two newlines.
         *
         * @return this builder
         */
        @CanIgnoreReturnValue
        public TruncatedStringBuilder addParagraph() {
            chunks.add("\n\n");
            return this;
        }

        /**
         * Sets the truncation text.
         *
         * @param truncationText
         *         the text to append on overflow
         *
         * @return this builder
         */
        @CanIgnoreReturnValue
        @SuppressWarnings({"HiddenField", "ParameterHidesMemberVariable"})
        public TruncatedStringBuilder withTruncationText(final String truncationText) {
            this.truncationText = truncationText;
            return this;
        }

        /**
         * Sets truncator to remove excess text from the start, rather than the end.
         *
         * @return this builder
         */
        @CanIgnoreReturnValue
        public TruncatedStringBuilder setTruncateStart() {
            this.truncateStart = true;
            return this;
        }

        /**
         * Sets truncator to chunk on newlines rather than the chunks.
         *
         * @return this builder
         */
        @CanIgnoreReturnValue
        public TruncatedStringBuilder setChunkOnNewlines() {
            this.chunkOnNewlines = true;
            return this;
        }

        @Override
        public String toString() {
            return build().toString();
        }
    }

    private record Joiner(String truncationText, int maxLength, boolean chunkOnChars)
            implements Collector<String, Accumulator, List<String>> {
            private Joiner(final String truncationText, final int maxLength, final boolean chunkOnChars) {
                this.truncationText = truncationText;
                this.maxLength = maxLength;
                this.chunkOnChars = chunkOnChars;
                if (maxLength < getLength(truncationText)) {
                    throw new IllegalArgumentException("Maximum length is less than truncation text.");
                }
            }

            private int getLength(final String text) {
                return chunkOnChars ? text.length() : text.getBytes(StandardCharsets.UTF_8).length;
            }

            @Override
            public Supplier<Accumulator> supplier() {
                return Accumulator::new;
            }

            @Override
            public BiConsumer<Accumulator, String> accumulator() {
                return Accumulator::add;
            }

            @Override
            public BinaryOperator<Accumulator> combiner() {
                return Accumulator::combine;
            }

            @Override
            public Function<Accumulator, List<String>> finisher() {
                return Accumulator::truncate;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }

            private class Accumulator {
                private final List<String> chunks = new ArrayList<>();
                private int length;
                private boolean truncated;

                @CanIgnoreReturnValue
                Accumulator combine(final Accumulator other) {
                    other.chunks.forEach(this::add);
                    return this;
                }

                void add(final String chunk) {
                    if (truncated) {
                        return;
                    }
                    if (length + getLength(chunk) > maxLength) {
                        truncated = true;
                        return;
                    }
                    chunks.add(chunk);
                    length += getLength(chunk);
                }

                List<String> truncate() {
                    if (truncated) {
                        if (length + getLength(truncationText) > maxLength) {
                            chunks.remove(chunks.size() - 1);
                        }
                        chunks.add(truncationText);
                    }
                    return chunks;
                }
            }
        }
}
