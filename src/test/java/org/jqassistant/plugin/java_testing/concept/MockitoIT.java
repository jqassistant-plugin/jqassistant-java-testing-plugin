package org.jqassistant.plugin.java_testing.concept;

import java.util.List;
import java.util.stream.Collectors;

import com.buschmais.jqassistant.core.report.api.model.Column;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.report.api.model.Row;
import com.buschmais.jqassistant.core.rule.api.model.Concept;
import com.buschmais.jqassistant.plugin.java.api.model.MethodDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.TypeDescriptor;
import com.buschmais.jqassistant.plugin.java.test.AbstractJavaPluginIT;

import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.SUCCESS;
import static com.buschmais.jqassistant.plugin.java.test.assertj.MethodDescriptorCondition.methodDescriptor;
import static com.buschmais.jqassistant.plugin.java.test.assertj.TypeDescriptorCondition.typeDescriptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

class MockitoIT extends AbstractJavaPluginIT {

    @Test
    void mockitoVerifyMethod() throws Exception {
        scanClasses(AssertExample.class);

        final Result<Concept> conceptResult = applyConcept("java-testing-mockito:VerifyMethod");
        assertThat(conceptResult.getStatus()).isEqualTo(SUCCESS);

        store.beginTransaction();

        assertThat(conceptResult.getRows().size()).isEqualTo(3);

        assertThat(conceptResult.getRows()
                .get(0)
                .getColumns()
                .get("assertMethod")
                .getValue()).asInstanceOf(type(MethodDescriptor.class))
                .is(methodDescriptor(InOrder.class, "verify", Object.class));


        assertThat(conceptResult.getRows()
                .get(1)
                .getColumns()
                .get("assertMethod")
                .getValue()).asInstanceOf(type(MethodDescriptor.class))
                .is(methodDescriptor(MockedStatic.class, "verify", MockedStatic.Verification.class));

        assertThat(conceptResult.getRows()
                .get(2)
                .getColumns()
                .get("assertMethod")
                .getValue()).asInstanceOf(type(MethodDescriptor.class))
                .is(methodDescriptor(Mockito.class, "verify", Object.class));

        final TestResult methodQueryResultForMockito = getMethodQueryResultForMockito();
        assertThat(methodQueryResultForMockito.getRows().size()).isEqualTo(3);
        verifyMockitoVerifyExamplesContained(methodQueryResultForMockito);

        store.commitTransaction();
    }

    @Test
    void bddMockitoThenShouldMethod() throws Exception {
        scanClasses(AssertExample.class);

        final Result<Concept> conceptResult = applyConcept("java-testing-mockito:BddThenShouldMethod");
        assertThat(conceptResult.getStatus()).isEqualTo(SUCCESS);

        store.beginTransaction();

        assertThat(conceptResult.getRows().size()).isEqualTo(1);
        assertThat(conceptResult.getRows()
                .get(0)
                .getColumns()
                .get("assertMethod")
                .getValue()).asInstanceOf(type(MethodDescriptor.class))
                .is(methodDescriptor(BDDMockito.Then.class, "shouldHaveNoInteractions"));

        final TestResult methodQueryResultForMockito = getMethodQueryResultForMockito();
        assertThat(methodQueryResultForMockito.getRows().size()).isEqualTo(1);
        verifyBddMockitoThenShouldExampleContained(methodQueryResultForMockito);

        store.commitTransaction();
    }

    @Test
    void providedConceptAssertMethod() throws Exception {
        scanClasses(AssertExample.class);

        final Result<Concept> conceptResult = applyConcept("java:AssertMethod");
        assertThat(conceptResult.getStatus()).isEqualTo(SUCCESS);

        store.beginTransaction();

        final List<TypeDescriptor> declaringTypes = conceptResult.getRows().stream()
                .map(Row::getColumns)
                .map(columns -> columns.get("DeclaringType"))
                .map(Column::getValue)
                .map(TypeDescriptor.class::cast)
                .collect(Collectors.toList());
        assertThat(declaringTypes).haveExactly(1, typeDescriptor(Mockito.class));

        final TestResult methodQueryResultForMockito = getMethodQueryResultForMockito();
        assertThat(methodQueryResultForMockito.getRows().size()).isEqualTo(4);
        verifyMockitoVerifyExamplesContained(methodQueryResultForMockito);
        verifyBddMockitoThenShouldExampleContained(methodQueryResultForMockito);
        store.commitTransaction();
    }

    private TestResult getMethodQueryResultForMockito() {
        return query(
                "MATCH (testMethod:Method)-[:INVOKES]->(assertMethod:Method) "
                        + "WHERE assertMethod:Mockito:Assert "
                        + "RETURN DISTINCT testMethod, assertMethod");
    }

    private void verifyMockitoVerifyExamplesContained(TestResult methodQueryResult) throws NoSuchMethodException {
        // mockitoVerifyExampleMethod -> Mockito.verify
        assertThat(methodQueryResult.<MethodDescriptor>getColumn("testMethod"))
                .haveExactly(1, methodDescriptor(AssertExample.class, "mockitoVerifyExampleMethod"));
        final List<MethodDescriptor> assertMethodsCalledByMockitoVerifyExampleMethod = methodQueryResult.getRows().stream()
                .filter(row -> ((MethodDescriptor) row.get("testMethod")).getName().equals("mockitoVerifyExampleMethod"))
                .map(row -> ((MethodDescriptor) row.get("assertMethod")))
                .collect(Collectors.toList());
        assertThat(assertMethodsCalledByMockitoVerifyExampleMethod)
                .haveExactly(1, methodDescriptor(Mockito.class, "verify", Object.class));

        // mockedStaticVerifyExampleMethod -> MockedStatic.verify
        assertThat(methodQueryResult.<MethodDescriptor>getColumn("testMethod"))
                .haveExactly(1, methodDescriptor(AssertExample.class, "mockedStaticVerifyExampleMethod"));
        final List<MethodDescriptor> assertMethodsCalledByMockedStaticVerifyExampleMethod = methodQueryResult.getRows().stream()
                .filter(row -> ((MethodDescriptor) row.get("testMethod")).getName().equals("mockedStaticVerifyExampleMethod"))
                .map(row -> ((MethodDescriptor) row.get("assertMethod")))
                .collect(Collectors.toList());
        assertThat(assertMethodsCalledByMockedStaticVerifyExampleMethod)
                .haveExactly(1, methodDescriptor(MockedStatic.class, "verify", MockedStatic.Verification.class));

        // mockitoInOrderVerifyExampleMethod -> InOrder.verify
        assertThat(methodQueryResult.<MethodDescriptor>getColumn("testMethod"))
                .haveExactly(1, methodDescriptor(AssertExample.class, "mockitoInOrderVerifyExampleMethod"));
        final List<MethodDescriptor> assertMethodsCalledByMockitoInOrderVerifyExampleMethod = methodQueryResult.getRows().stream()
                .filter(row -> ((MethodDescriptor) row.get("testMethod")).getName().equals("mockitoInOrderVerifyExampleMethod"))
                .map(row -> ((MethodDescriptor) row.get("assertMethod")))
                .collect(Collectors.toList());
        assertThat(assertMethodsCalledByMockitoInOrderVerifyExampleMethod)
                .haveExactly(1, methodDescriptor(InOrder.class, "verify", Object.class));
    }

    private void verifyBddMockitoThenShouldExampleContained(TestResult methodQueryResult) throws NoSuchMethodException {
        // bddMockitoThenShouldExampleMethod -> BDDMockito.Then.shouldHaveNoInteractions
        assertThat(methodQueryResult.<MethodDescriptor>getColumn("testMethod"))
                .haveExactly(1, methodDescriptor(AssertExample.class, "bddMockitoThenShouldExampleMethod"));
        final List<MethodDescriptor> assertMethodsCalledByMockitoInOrderVerifyExampleMethod = methodQueryResult.getRows().stream()
                .filter(row -> ((MethodDescriptor) row.get("testMethod")).getName().equals("bddMockitoThenShouldExampleMethod"))
                .map(row -> ((MethodDescriptor) row.get("assertMethod")))
                .collect(Collectors.toList());
        assertThat(assertMethodsCalledByMockitoInOrderVerifyExampleMethod)
                .haveExactly(1, methodDescriptor(BDDMockito.Then.class, "shouldHaveNoInteractions"));
    }

}
