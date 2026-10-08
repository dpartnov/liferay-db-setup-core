package com.ableneo.liferay.portal.setup.core.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.liferay.data.engine.rest.dto.v2_0.DataDefinition;
import com.liferay.data.engine.rest.dto.v2_0.DataDefinitionField;
import com.liferay.data.engine.rest.dto.v2_0.DataLayout;
import com.liferay.data.engine.rest.dto.v2_0.DataLayoutColumn;
import com.liferay.data.engine.rest.dto.v2_0.DataLayoutPage;
import com.liferay.data.engine.rest.dto.v2_0.DataLayoutRow;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataDefinitionFieldNameNormalizerTest {

    @Test
    void shouldAcceptFieldNameWhenItEndsWithInstanceId() {
        Assertions.assertTrue(DataDefinitionFieldNameNormalizer.hasInstanceId("title12345678"));
        Assertions.assertTrue(DataDefinitionFieldNameNormalizer.hasInstanceId("12345678"));
    }

    @Test
    void shouldRejectFieldNameWhenInstanceIdIsMissingOrTooShort() {
        Assertions.assertFalse(DataDefinitionFieldNameNormalizer.hasInstanceId("title"));
        Assertions.assertFalse(DataDefinitionFieldNameNormalizer.hasInstanceId("title1234567"));
        Assertions.assertFalse(DataDefinitionFieldNameNormalizer.hasInstanceId("1234567"));
        Assertions.assertFalse(DataDefinitionFieldNameNormalizer.hasInstanceId("title1234567a"));
        Assertions.assertFalse(DataDefinitionFieldNameNormalizer.hasInstanceId(null));
    }

    @Test
    void shouldKeepFieldNameWhenItAlreadyHasInstanceId() {
        DataDefinitionField field = newField("title12345678", null);
        DataDefinition dataDefinition = newDataDefinition(newDataLayout("title12345678"), field);

        DataDefinitionFieldNameNormalizer.normalize(dataDefinition, null);

        Assertions.assertEquals("title12345678", field.getName());
        Assertions.assertEquals("title12345678", layoutFieldNames(dataDefinition)[0]);
    }

    @Test
    void shouldGenerateFieldNameWhenStructureDoesNotExistYet() {
        DataDefinitionField field = newField("title", null);
        DataDefinition dataDefinition = newDataDefinition(newDataLayout("title"), field);

        DataDefinitionFieldNameNormalizer.normalize(dataDefinition, null);

        Assertions.assertNotEquals("title", field.getName());
        Assertions.assertTrue(field.getName().startsWith("title"));
        Assertions.assertTrue(DataDefinitionFieldNameNormalizer.hasInstanceId(field.getName()));
        Assertions.assertEquals(field.getName(), layoutFieldNames(dataDefinition)[0]);
    }

    @Test
    void shouldGenerateSameFieldNameOnEveryRun() {
        DataDefinitionField firstRunField = newField("title", Collections.singletonMap("fieldReference", "titleRef"));
        DataDefinitionField secondRunField = newField("title", Collections.singletonMap("fieldReference", "titleRef"));

        DataDefinitionFieldNameNormalizer.normalize(newDataDefinition(newDataLayout("title"), firstRunField), null);
        DataDefinitionFieldNameNormalizer.normalize(newDataDefinition(newDataLayout("title"), secondRunField), null);

        Assertions.assertEquals(firstRunField.getName(), secondRunField.getName());
    }

    @Test
    void shouldGenerateDistinctFieldNamesForFieldsOfTheSameName() {
        DataDefinitionField first = newField("title", Collections.singletonMap("fieldReference", "firstRef"));
        DataDefinitionField second = newField("title", Collections.singletonMap("fieldReference", "secondRef"));
        DataDefinition dataDefinition = newDataDefinition(newDataLayout("title"), first, second);

        DataDefinitionFieldNameNormalizer.normalize(dataDefinition, null);

        Assertions.assertNotEquals(first.getName(), second.getName());
        Assertions.assertTrue(DataDefinitionFieldNameNormalizer.hasInstanceId(first.getName()));
        Assertions.assertTrue(DataDefinitionFieldNameNormalizer.hasInstanceId(second.getName()));
    }

    @Test
    void shouldRemoveSpacesFromGeneratedFieldName() {
        DataDefinitionField field = newField("main title", null);
        DataDefinition dataDefinition = newDataDefinition(newDataLayout("main title"), field);

        DataDefinitionFieldNameNormalizer.normalize(dataDefinition, null);

        Assertions.assertTrue(field.getName().startsWith("maintitle"));
        Assertions.assertEquals(field.getName(), layoutFieldNames(dataDefinition)[0]);
    }

    @Test
    void shouldReuseStoredFieldNameWhenStructureIsUpdated() {
        DataDefinitionField field = newField("title", Collections.singletonMap("fieldReference", "titleReference"));
        DataDefinition dataDefinition = newDataDefinition(newDataLayout("title"), field);

        DataDefinitionFieldNameNormalizer.normalize(dataDefinition, newDDMStructure("titleReference", "title87654321"));

        Assertions.assertEquals("title87654321", field.getName());
        Assertions.assertEquals("title87654321", layoutFieldNames(dataDefinition)[0]);
    }

    @Test
    void shouldReuseStoredFieldNameWhenItHasNoInstanceId() {
        DataDefinitionField field = newField("title", Collections.singletonMap("fieldReference", "titleReference"));
        DataDefinition dataDefinition = newDataDefinition(newDataLayout("title"), field);

        DataDefinitionFieldNameNormalizer.normalize(dataDefinition, newDDMStructure("titleReference", "legacyTitle"));

        Assertions.assertEquals("legacyTitle", field.getName());
        Assertions.assertEquals("legacyTitle", layoutFieldNames(dataDefinition)[0]);
    }

    @Test
    void shouldGenerateFieldNameWhenFieldReferenceIsUnknown() {
        DataDefinitionField field = newField("title", Collections.singletonMap("fieldReference", "unknownReference"));
        DataDefinition dataDefinition = newDataDefinition(newDataLayout("title"), field);

        DataDefinitionFieldNameNormalizer.normalize(dataDefinition, newDDMStructure("titleReference", "title87654321"));

        Assertions.assertNotEquals("title87654321", field.getName());
        Assertions.assertTrue(DataDefinitionFieldNameNormalizer.hasInstanceId(field.getName()));
        Assertions.assertEquals(field.getName(), layoutFieldNames(dataDefinition)[0]);
    }

    @Test
    void shouldRenameNestedFields() {
        DataDefinitionField nested = newField("nestedTitle", null);
        DataDefinitionField fieldSet = newField("fieldSet12345678", null);
        fieldSet.setNestedDataDefinitionFields(new DataDefinitionField[] { nested });
        DataDefinition dataDefinition = newDataDefinition(newDataLayout("fieldSet12345678"), fieldSet);

        DataDefinitionFieldNameNormalizer.normalize(dataDefinition, null);

        Assertions.assertEquals("fieldSet12345678", fieldSet.getName());
        Assertions.assertTrue(DataDefinitionFieldNameNormalizer.hasInstanceId(nested.getName()));
        Assertions.assertTrue(nested.getName().startsWith("nestedTitle"));
    }

    @Test
    void shouldRenameEveryLayoutOccurrenceOfTheField() {
        DataDefinitionField field = newField("title", null);
        DataDefinition dataDefinition = newDataDefinition(newDataLayout("title", "title"), field);

        DataDefinitionFieldNameNormalizer.normalize(dataDefinition, null);

        Assertions.assertArrayEquals(
            new String[] { field.getName(), field.getName() },
            layoutFieldNames(dataDefinition)
        );
    }

    @Test
    void shouldNotFailWhenDataDefinitionHasNoFields() {
        DataDefinition dataDefinition = new DataDefinition();

        Assertions.assertDoesNotThrow(() -> DataDefinitionFieldNameNormalizer.normalize(dataDefinition, null));
    }

    @Test
    void shouldNotFailWhenDataDefinitionHasNoLayout() {
        DataDefinitionField field = newField("title", null);
        DataDefinition dataDefinition = newDataDefinition(null, field);

        Assertions.assertDoesNotThrow(() -> DataDefinitionFieldNameNormalizer.normalize(dataDefinition, null));
        Assertions.assertTrue(DataDefinitionFieldNameNormalizer.hasInstanceId(field.getName()));
    }

    private static DataDefinitionField newField(String name, Map<String, Object> customProperties) {
        DataDefinitionField field = new DataDefinitionField();
        field.setName(name);
        if (customProperties != null) {
            field.setCustomProperties(new HashMap<>(customProperties));
        }
        return field;
    }

    private static DataDefinition newDataDefinition(DataLayout dataLayout, DataDefinitionField... fields) {
        DataDefinition dataDefinition = new DataDefinition();
        dataDefinition.setDataDefinitionFields(fields);
        dataDefinition.setDefaultDataLayout(dataLayout);
        return dataDefinition;
    }

    private static DataLayout newDataLayout(String... fieldNames) {
        DataLayoutColumn dataLayoutColumn = new DataLayoutColumn();
        dataLayoutColumn.setFieldNames(fieldNames);

        DataLayoutRow dataLayoutRow = new DataLayoutRow();
        dataLayoutRow.setDataLayoutColumns(new DataLayoutColumn[] { dataLayoutColumn });

        DataLayoutPage dataLayoutPage = new DataLayoutPage();
        dataLayoutPage.setDataLayoutRows(new DataLayoutRow[] { dataLayoutRow });

        DataLayout dataLayout = new DataLayout();
        dataLayout.setDataLayoutPages(new DataLayoutPage[] { dataLayoutPage });
        return dataLayout;
    }

    private static DDMStructure newDDMStructure(String fieldReference, String fieldName) {
        DDMForm ddmForm = mock(DDMForm.class);
        when(ddmForm.getDDMFormFieldsReferencesMap(true)).thenReturn(
            Collections.singletonMap(fieldReference, new DDMFormField(fieldName, "text"))
        );

        DDMStructure ddmStructure = mock(DDMStructure.class);
        when(ddmStructure.getDDMForm()).thenReturn(ddmForm);
        return ddmStructure;
    }

    private static String[] layoutFieldNames(DataDefinition dataDefinition) {
        return dataDefinition
            .getDefaultDataLayout()
            .getDataLayoutPages()[0].getDataLayoutRows()[0].getDataLayoutColumns()[0].getFieldNames();
    }
}
