package com.killerprotocol.cuentas.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchitectureTest {

    private static final String[] DOMINIO_PROHIBIDO = {
            "..application..",
            "..adapter..",
            "org.springframework..",
            "jakarta.persistence..",
            "javax.persistence..",
            "com.nimbusds..",
            "org.springframework.amqp..",
    };

    private static final String[] APLICACION_PROHIBIDO = {
            "..adapter..",
            "org.springframework..",
            "jakarta.persistence..",
            "javax.persistence..",
            "com.nimbusds..",
            "org.springframework.amqp..",
    };

    private final JavaClasses clases = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.killerprotocol.cuentas");

    @Test
    void elDominioNoDependeDeLasDemasCapasNiDeInfraestructura() {
        ArchRule regla = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(DOMINIO_PROHIBIDO);
        regla.check(clases);
    }

    @Test
    void laAplicacionNoDependeDeLosAdaptadoresNiDeInfraestructura() {
        ArchRule regla = noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(APLICACION_PROHIBIDO)
                .allowEmptyShould(true);
        regla.check(clases);
    }

    @Test
    void losServiciosNoTienenAnotacionesDeSpring() {
        ArchRule regla = noClasses().that().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .allowEmptyShould(true);
        regla.check(clases);
    }
}
