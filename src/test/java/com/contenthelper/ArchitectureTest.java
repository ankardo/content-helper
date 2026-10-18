package com.contenthelper;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Architecture rules: the shell sits on top, the feature packages stay independent of it. */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.contenthelper");
    }

    @Test
    void noCyclesBetweenPackages() {
        slices().matching("com.contenthelper.(*)..").should().beFreeOfCycles().check(classes);
    }

    @Test
    void blogDoesNotDependOnShell() {
        noClasses()
                .that()
                .resideInAPackage("..blog..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..shell..")
                .check(classes);
    }

    @Test
    void shellSitsAboveTheFeaturePackages() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Shell")
                .definedBy("..shell..")
                .layer("Features")
                .definedBy("..ai..", "..draft..", "..git..", "..blog..", "..observe..")
                .whereLayer("Shell")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Features")
                .mayOnlyBeAccessedByLayers("Shell")
                .check(classes);
    }
}
