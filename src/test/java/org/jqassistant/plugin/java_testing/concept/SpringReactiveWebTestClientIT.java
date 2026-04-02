package org.jqassistant.plugin.java_testing.concept;

import com.buschmais.jqassistant.core.report.api.model.Column;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.report.api.model.Row;
import com.buschmais.jqassistant.core.rule.api.model.Concept;
import com.buschmais.jqassistant.core.test.plugin.AbstractPluginIT;
import com.buschmais.jqassistant.plugin.java.api.model.MethodDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.TypeDescriptor;
import com.buschmais.jqassistant.plugin.java.test.AbstractJavaPluginIT;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.stream.Collectors;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.SUCCESS;
import static com.buschmais.jqassistant.plugin.java.test.assertj.MethodDescriptorCondition.methodDescriptor;
import static com.buschmais.jqassistant.plugin.java.test.assertj.TypeDescriptorCondition.typeDescriptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

public class SpringReactiveWebTestClientIT extends AbstractJavaPluginIT {

    @Test
    void springReactiveWebTestClientAssertMethod() throws Exception {
        scanClasses(AssertExample.class);

        final Result<Concept> conceptResult = applyConcept("java-testing-spring-reactive:AssertMethod");
        assertThat(conceptResult.getStatus()).isEqualTo(SUCCESS);

        store.beginTransaction();

        assertThat(conceptResult.getRows().size()).isEqualTo(1);
        assertThat(conceptResult.getRows()
                .get(0)
                .getColumns()
                .get("assertMethod")
                .getValue()).asInstanceOf(type(MethodDescriptor.class))
                .is(methodDescriptor(WebTestClient.ResponseSpec.class, "expectStatus"));

        verifyResultGraph();

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
        assertThat(declaringTypes).haveExactly(1, typeDescriptor(WebTestClient.ResponseSpec.class));

        verifyResultGraph();

        store.commitTransaction();
    }

    // Expects an open transaction
    private void verifyResultGraph() throws NoSuchMethodException {
        final AbstractPluginIT.TestResult methodQueryResult = query(
                "MATCH (testMethod:Method)-[:INVOKES]->(assertMethod:Method) "
                        + "WHERE assertMethod:Spring:Reactive:Assert "
                        + "RETURN testMethod, assertMethod");
        assertThat(methodQueryResult.<MethodDescriptor>getColumn("testMethod"))
                .haveExactly(1, methodDescriptor(AssertExample.class, "springWebTestClientAssertExampleMethod"));
        assertThat(methodQueryResult.<MethodDescriptor>getColumn("assertMethod"))
                .haveExactly(1, methodDescriptor(WebTestClient.ResponseSpec.class, "expectStatus"));
    }
}
