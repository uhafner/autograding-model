package edu.hm.hafner.grading;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

import static org.assertj.core.api.Assertions.*;

class TruncatedStringTest {
    private static final String MESSAGE = "Truncated";  // length 9

    @ParameterizedTest(name = "chunkOnNewlines={0}, chunkOnChars={1}")
    @MethodSource("parameters")
    public void shouldBuildStrings(final boolean chunkOnNewlines, final boolean chunkOnChars) {
        var builder = createBuilder(chunkOnNewlines);

        builder.addText("Hello\n");
        assertThat(getRawString(builder)).isEqualTo("Hello\n");
        assertThat(build(builder, chunkOnChars, 1000)).isEqualTo("Hello\n");

        builder.addText(", world!");
        assertThat(getRawString(builder)).isEqualTo("Hello\n, world!");
        assertThat(build(builder, chunkOnChars, 1000)).isEqualTo("Hello\n, world!");
    }

    @ParameterizedTest(name = "chunkOnNewlines={0}, chunkOnChars={1}")
    @MethodSource("parameters")
    public void shouldTruncateStrings(final boolean chunkOnNewlines, final boolean chunkOnChars) {
        var builder = createBuilder(chunkOnNewlines);

        builder.addText("xxxxxxxxx\n"); // 10
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo("xxxxxxxxx\n");

        builder.addText("yyyy\n"); // 5, doesn't cause overflow
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo("xxxxxxxxx\nyyyy\n");

        builder.addText("zzzzzz\n"); // 7, does cause overflow
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo("xxxxxxxxx\nTruncated");
    }

    @ParameterizedTest(name = "chunkOnNewlines={0}, chunkOnChars={1}")
    @MethodSource("parameters")
    public void shouldHandleEdgeCases(final boolean chunkOnNewlines, final boolean chunkOnChars) {
        var builder = createBuilder(chunkOnNewlines);

        assertThat(build(builder, chunkOnChars, 10)).isEqualTo("");
        assertThat(getRawString(builder)).isEqualTo("");

        builder.addText("xxxxxxxxxxxxxx\n"); // 15
        assertThat(build(builder, chunkOnChars, 10)).isEqualTo(MESSAGE);
        assertThatIllegalArgumentException().isThrownBy(() -> build(builder, chunkOnChars, 5))
                .withMessage("Maximum length is less than truncation text.");
    }

    @ParameterizedTest(name = "chunkOnNewlines={0}, chunkOnChars={1}")
    @MethodSource("parameters")
    public void shouldHandleReversedChunking(final boolean chunkOnNewlines, final boolean chunkOnChars) {
        var builder = createBuilder(chunkOnNewlines).setTruncateStart();

        builder.addText("zzzz\n"); // 5
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo("zzzz\n");

        builder.addText("xxxx\n"); // 5, doesn't cause overflow
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo("zzzz\nxxxx\n");

        builder.addText("cccc\n"); // 5, doesn't cause overflow
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo("zzzz\nxxxx\ncccc\n");

        builder.addText("aaaaaa\n"); // 7, does cause overflow
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo("Truncatedcccc\naaaaaa\n");
    }

    @ParameterizedTest(name = "chunkOnNewlines={0}, chunkOnChars={1}")
    @MethodSource("parameters")
    public void shouldHandleEdgeCasesReversed(final boolean chunkOnNewlines, final boolean chunkOnChars) {
        var builder = createBuilder(chunkOnNewlines);
        builder.setTruncateStart();

        assertThat(build(builder, chunkOnChars, 10)).isEqualTo("");
        assertThat(getRawString(builder)).isEqualTo("");

        builder.addText("xxxxxxxxxxxxxx\n"); // 15
        assertThat(build(builder, chunkOnChars, 10)).isEqualTo(MESSAGE);
        assertThatIllegalArgumentException().isThrownBy(() -> build(builder, chunkOnChars, 5))
                .withMessage("Maximum length is less than truncation text.");
    }

    @ParameterizedTest(name = "chunkOnNewlines={0}, chunkOnChars={1}")
    @MethodSource("parameters")
    public void shouldChunkNewlinesDifferently(final boolean chunkOnNewlines, final boolean chunkOnChars) {
        var builder = createBuilder(chunkOnNewlines);
        builder.addText("xxxxxxxxxx"); // 10
        builder.addText("yyyyyyyyyyy"); // 11
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo(chunkOnNewlines ? MESSAGE : "xxxxxxxxxxTruncated");

        builder = createBuilder(chunkOnNewlines);
        builder.addText("wwww\n"); // 5
        builder.addText("xxxx\nyyyy\nzzzzz\n"); // 16
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo(
                chunkOnNewlines ? "wwww\nxxxx\nTruncated" : "wwww\nTruncated");
    }

    @ParameterizedTest(name = "chunkOnNewlines={0}, chunkOnChars={1}")
    @MethodSource("parameters")
    public void shouldTruncateByBytesOrChars(final boolean chunkOnNewlines, final boolean chunkOnChars) {
        var builder = createBuilder(chunkOnNewlines);

        builder.addText("☃☃☃\n"); // 3 + 1
        assertThat(getRawString(builder)).hasSize(4);
        assertThat(getRawString(builder).getBytes(StandardCharsets.UTF_8).length).isEqualTo(10);
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo("☃☃☃\n");

        builder.addText("🕴️🕴️\n"); // 2 + 1
        assertThat(getRawString(builder)).hasSize(11);
        assertThat(getRawString(builder).getBytes(StandardCharsets.UTF_8).length).isEqualTo(25);
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo(chunkOnChars ? "☃☃☃\n🕴️🕴️\n" : "☃☃☃\nTruncated");
    }

    @ParameterizedTest(name = "chunkOnNewlines={0}, chunkOnChars={1}")
    @MethodSource("parameters")
    public void shouldHandleLongCharsInTruncationText(final boolean chunkOnNewlines, final boolean chunkOnChars) {
        var builder = createBuilder(chunkOnNewlines);

        var truncationText = "E_TOO_MUCH_☃";
        assertThat(truncationText).hasSize(12);
        assertThat(truncationText.getBytes(StandardCharsets.UTF_8).length).isEqualTo(14);

        builder.withTruncationText(truncationText);
        builder.addText("xxxx\n"); // 5
        builder.addText("x\n"); // 2
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo("xxxx\nx\n");

        builder.addText("xxxxxxxxxxxxxxx"); // 15
        assertThat(build(builder, chunkOnChars, 20)).isEqualTo(
                chunkOnChars ? "xxxx\nx\nE_TOO_MUCH_☃" : "xxxx\nE_TOO_MUCH_☃");
    }

    private static Stream<Arguments> parameters() {
        return Stream.of(
                Arguments.of(false, false),
                Arguments.of(true, false),
                Arguments.of(false, true),
                Arguments.of(true, true));
    }

    private String getRawString(final TruncatedStringBuilder builder) {
        return builder.build().toString();
    }

    private TruncatedStringBuilder createBuilder(final boolean chunkOnNewlines) {
        var builder = new TruncatedStringBuilder().withTruncationText(MESSAGE);
        if (chunkOnNewlines) {
            builder.setChunkOnNewlines();
        }
        return builder;
    }

    private String build(final TruncatedStringBuilder builder, final boolean chunkOnChars, final int maxSize) {
        var truncatedString = builder.build();
        if (chunkOnChars) {
            return truncatedString.buildByChars(maxSize);
        }
        return truncatedString.buildByBytes(maxSize);
    }
}
