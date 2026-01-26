package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link CoveragePathMatcher}.
 *
 * @author Apoorva Mahabaleshwara
 */
class CoveragePathMatcherTest {
    @Test
    void shouldMatchExactPath() {
        var modifiedLines = Map.of("src/main/java/Example.java", Set.of(10, 20));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("Example.java", "src/main/java", Paths.get("target/jacoco.xml"));
        
        assertThat(result).isEqualTo("src/main/java/Example.java");
    }
    
    @Test
    void shouldMatchWithoutSourcePath() {
        var modifiedLines = Map.of("com/example/MyClass.java", Set.of(15));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("com/example/MyClass.java", "", Paths.get("target/jacoco.xml"));
        
        assertThat(result).isEqualTo("com/example/MyClass.java");
    }
    
    @Test
    void shouldMatchJaCoCoStylePath() {
        // JaCoCo: coverage path is package-based, diff path is full repository path
        var modifiedLines = Map.of("src/main/java/com/example/Service.java", Set.of(5, 10, 15));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("com/example/Service.java", "src/main/java", Paths.get("target/jacoco.xml"));
        
        assertThat(result).isEqualTo("src/main/java/com/example/Service.java");
    }
    
    @Test
    void shouldMatchJaCoCoMultiModulePath() {
        // Multi-module: diff path includes module name
        var modifiedLines = Map.of(
                "app/src/main/java/com/intuit/MyClass.java", Set.of(10),
                "lib/src/main/java/com/intuit/Helper.java", Set.of(20)
        );
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        // Coverage from app module
        String result1 = matcher.findMatch("com/intuit/MyClass.java", "src/main/java", 
                Paths.get("app/target/site/jacoco/jacoco.xml"));
        
        // Coverage from lib module
        String result2 = matcher.findMatch("com/intuit/Helper.java", "src/main/java", 
                Paths.get("lib/target/site/jacoco/jacoco.xml"));
        
        assertThat(result1).isEqualTo("app/src/main/java/com/intuit/MyClass.java");
        assertThat(result2).isEqualTo("lib/src/main/java/com/intuit/Helper.java");
    }
    
    @Test
    void shouldDisambiguateDuplicateFilesUsingModule() {
        // Two modules with same file name
        var modifiedLines = Map.of(
                "module-a/src/main/java/com/example/Utils.java", Set.of(10),
                "module-b/src/main/java/com/example/Utils.java", Set.of(20)
        );
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String resultA = matcher.findMatch("com/example/Utils.java", "src/main/java", 
                Paths.get("module-a/target/jacoco.xml"));
        String resultB = matcher.findMatch("com/example/Utils.java", "src/main/java", 
                Paths.get("module-b/target/jacoco.xml"));
        
        assertThat(resultA).isEqualTo("module-a/src/main/java/com/example/Utils.java");
        assertThat(resultB).isEqualTo("module-b/src/main/java/com/example/Utils.java");
    }
    
    @Test
    void shouldMatchCloverAbsolutePath() {
        // Clover: coverage path is absolute, diff path is repository-relative
        var modifiedLines = Map.of("src/js/actions/File1.js", Set.of(5, 10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("/home/jenkins/workspace/project/src/js/actions/File1.js", 
                "", Paths.get("target/clover.xml"));
        
        assertThat(result).isEqualTo("src/js/actions/File1.js");
    }
    
    @Test
    void shouldMatchOpenCoverWindowsPath() {
        // OpenCover: Windows absolute path with backslashes
        var modifiedLines = Map.of("src/Helper.cs", Set.of(15));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("C:\\work\\project\\src\\Helper.cs", 
                "", Paths.get("coverage.xml"));
        
        assertThat(result).isEqualTo("src/Helper.cs");
    }
    
    @Test
    void shouldMatchCoberturaPHPPath() {
        // Cobertura: PHP relative path
        var modifiedLines = Map.of("app/code/Model/DataProvider/File.php", Set.of(25));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("Model/DataProvider/File.php", 
                "", Paths.get("app/target/cobertura.xml"));
        
        assertThat(result).isEqualTo("app/code/Model/DataProvider/File.php");
    }
    
    @Test
    void shouldMatchGoModulePath() {
        // Go: coverage path has module URL prefix
        var modifiedLines = Map.of("pkg/file.go", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("github.com/org/repo/pkg/file.go", 
                "", Paths.get("coverage.out"));
        
        assertThat(result).isEqualTo("pkg/file.go");
    }
    
    @Test
    void shouldExtractModuleRootFromMavenPath() {
        var modifiedLines = Map.of("mymodule/src/main/java/Test.java", Set.of(1));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("Test.java", "src/main/java", 
                Paths.get("mymodule/target/site/jacoco/jacoco.xml"));
        
        assertThat(result).isEqualTo("mymodule/src/main/java/Test.java");
    }
    
    @Test
    void shouldExtractModuleRootFromGradlePath() {
        var modifiedLines = Map.of("app/src/main/java/App.java", Set.of(5));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("App.java", "src/main/java", 
                Paths.get("app/build/reports/jacoco/test/jacocoTestReport.xml"));
        
        assertThat(result).isEqualTo("app/src/main/java/App.java");
    }
    
    @Test
    void shouldExtractModuleRootFromDotNetPath() {
        var modifiedLines = Map.of("Helper.Test/Helper.cs", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("Helper.cs", "", 
                Paths.get("src/Helper.Test/bin/Debug/coverage.xml"));
        
        assertThat(result).isEqualTo("Helper.Test/Helper.cs");
    }
    
    @Test
    void shouldNotMatchWithDifferentModuleContext() {
        var modifiedLines = Map.of("module-a/src/File.java", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        // Coverage report from module-b but diff file is in module-a
        String result = matcher.findMatch("File.java", "src", 
                Paths.get("module-b/target/jacoco.xml"));
        
        assertThat(result).isNull();
    }
    
    @Test
    void shouldHandleNoModuleInSingleModuleProject() {
        // Root-level target directory (no module)
        var modifiedLines = Map.of("src/main/java/Main.java", Set.of(5));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("Main.java", "src/main/java", 
                Paths.get("target/jacoco.xml"));
        
        assertThat(result).isEqualTo("src/main/java/Main.java");
    }
    
    @Test
    void shouldNormalizeWindowsBackslashes() {
        var modifiedLines = Map.of("src/main/java/Test.java", Set.of(1));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        // Coverage path with Windows backslashes
        String result = matcher.findMatch("src\\main\\java\\Test.java", "", 
                Paths.get("target/jacoco.xml"));
        
        assertThat(result).isEqualTo("src/main/java/Test.java");
    }
    
    @Test
    void shouldHandleLeadingSlashes() {
        var modifiedLines = Map.of("src/File.java", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("/src/File.java", "", Paths.get("target/report.xml"));
        
        assertThat(result).isEqualTo("src/File.java");
    }
    
    @Test
    void shouldHandleTrailingSlashes() {
        var modifiedLines = Map.of("src/File.java", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("File.java", "src/", Paths.get("target/report.xml"));
        
        assertThat(result).isEqualTo("src/File.java");
    }
    
    @Test
    void shouldReturnNullWhenNoMatch() {
        var modifiedLines = Map.of("src/FileA.java", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("FileB.java", "src", Paths.get("target/jacoco.xml"));
        
        assertThat(result).isNull();
    }
    
    @Test
    void shouldReturnNullForEmptyModifiedLines() {
        var matcher = new CoveragePathMatcher(Map.of());
        
        String result = matcher.findMatch("Test.java", "src", Paths.get("target/jacoco.xml"));
        
        assertThat(result).isNull();
    }
    
    @Test
    void shouldHandleEmptyCoveragePath() {
        var modifiedLines = Map.of("src/File.java", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        // Empty string will normalize to empty and match if sourcePath provides the full path
        String result = matcher.findMatch("", "src/File.java", Paths.get("target/jacoco.xml"));
        
        assertThat(result).isEqualTo("src/File.java");
    }
    
    @Test
    void shouldHandleEmptySourcePath() {
        var modifiedLines = Map.of("File.java", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("File.java", "", Paths.get("target/jacoco.xml"));
        
        assertThat(result).isEqualTo("File.java");
    }
    
    @ParameterizedTest
    @CsvSource({
            "com/example/Test.java, src/main/java/com/example/Test.java, true",
            "example/Different.java, com/example/Test.java, false",
            "Test.java, com/example/Test.java, true",
            "Other.java, com/example/Test.java, false"
    })
    void shouldMatchBidirectionalSuffix(final String coveragePath, final String diffPath, final boolean shouldMatch) {
        var modifiedLines = Map.of(diffPath, Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch(coveragePath, "", Paths.get("target/jacoco.xml"));
        
        if (shouldMatch) {
            assertThat(result).isEqualTo(diffPath);
        }
        else {
            assertThat(result).isNull();
        }
    }
    
    @Test
    void shouldMatchNestedModulePath() {
        // Deeply nested module structure
        var modifiedLines = Map.of("parent/child/src/main/java/Test.java", Set.of(5));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("Test.java", "src/main/java", 
                Paths.get("parent/child/target/jacoco.xml"));
        
        assertThat(result).isEqualTo("parent/child/src/main/java/Test.java");
    }
    
    @Test
    void shouldPrioritizeExactMatchOverSuffixMatch() {
        // Both exact and suffix matches exist
        var modifiedLines = Map.of(
                "src/Test.java", Set.of(10),
                "module/src/Test.java", Set.of(20)
        );
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        // Exact match should win
        String result = matcher.findMatch("Test.java", "src", Paths.get("target/jacoco.xml"));
        
        assertThat(result).isEqualTo("src/Test.java");
    }
    
    @Test
    void shouldMatchWithSlashInSuffix() {
        var modifiedLines = Map.of("src/main/java/com/example/Test.java", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        // Coverage path has partial suffix with slashes
        String result = matcher.findMatch("example/Test.java", "", Paths.get("target/jacoco.xml"));
        
        assertThat(result).isEqualTo("src/main/java/com/example/Test.java");
    }
    
    @Test
    void shouldHandleMixedPathSeparators() {
        var modifiedLines = Map.of("src/main/java/Test.java", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        // Mixed separators in coverage path
        String result = matcher.findMatch("src\\main/java\\Test.java", "", 
                Paths.get("target/jacoco.xml"));
        
        assertThat(result).isEqualTo("src/main/java/Test.java");
    }
    
    @Test
    void shouldMatchGradleObjDirectoryForDotNet() {
        var modifiedLines = Map.of("MyProject/MyClass.cs", Set.of(15));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("MyClass.cs", "", 
                Paths.get("src/MyProject/obj/Debug/coverage.xml"));
        
        assertThat(result).isEqualTo("MyProject/MyClass.cs");
    }
    
    @Test
    void shouldNotMatchPartialWordSuffix() {
        // "File.java" is a string suffix of "TestFile.java" but NOT a path suffix
        // This should NOT match - they are different files!
        var modifiedLines = Map.of("src/TestFile.java", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("File.java", "", Paths.get("target/jacoco.xml"));
        
        // Should NOT match - File.java != TestFile.java
        assertThat(result).isNull();
    }
    
    @Test
    void shouldHandleMultiplePotentialMatches() {
        // Multiple files could match, but module context should disambiguate
        var modifiedLines = Map.of(
                "app/src/Utils.java", Set.of(10),
                "lib/src/Utils.java", Set.of(20),
                "core/src/Utils.java", Set.of(30)
        );
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String resultApp = matcher.findMatch("Utils.java", "src", 
                Paths.get("app/target/jacoco.xml"));
        String resultLib = matcher.findMatch("Utils.java", "src", 
                Paths.get("lib/target/jacoco.xml"));
        String resultCore = matcher.findMatch("Utils.java", "src", 
                Paths.get("core/target/jacoco.xml"));
        
        assertThat(resultApp).isEqualTo("app/src/Utils.java");
        assertThat(resultLib).isEqualTo("lib/src/Utils.java");
        assertThat(resultCore).isEqualTo("core/src/Utils.java");
    }
    
    
    @Test
    void shouldNotMatchPartialFilenameFile() {
        // BUG TEST: File.java should NOT match TestFile.java
        // These are completely different files!
        var modifiedLines = Map.of("src/TestFile.java", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("File.java", "", Paths.get("target/jacoco.xml"));
        
        // This SHOULD be null - File.java is NOT the same as TestFile.java
        assertThat(result).isNull();
    }
    
    @Test
    void shouldNotMatchPartialFilenameHelper() {
        // Helper.java should NOT match MyHelper.java
        var modifiedLines = Map.of("src/MyHelper.java", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("Helper.java", "", Paths.get("target/jacoco.xml"));
        
        assertThat(result).isNull();
    }
    
    @Test
    void shouldNotMatchPartialFilenameService() {
        // Service.java should NOT match UserService.java
        var modifiedLines = Map.of("src/UserService.java", Set.of(10));
        var matcher = new CoveragePathMatcher(modifiedLines);
        
        String result = matcher.findMatch("Service.java", "", Paths.get("target/jacoco.xml"));
        
        assertThat(result).isNull();
    }
}
