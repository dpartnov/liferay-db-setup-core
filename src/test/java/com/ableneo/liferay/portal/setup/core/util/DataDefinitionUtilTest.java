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

class DataDefinitionUtilTest {

    @Test
    void shouldAcceptFieldNameWhenItEndsWithInstanceId() {
        Assertions.assertTrue(DataDefinitionUtil.isValidFieldName("title12345678"));
        Assertions.assertTrue(DataDefinitionUtil.isValidFieldName("12345678"));
    }

    @Test
    void shouldRejectFieldNameWhenInstanceIdIsMissingOrTooShort() {
        Assertions.assertFalse(DataDefinitionUtil.isValidFieldName("title"));
        Assertions.assertFalse(DataDefinitionUtil.isValidFieldName("title1234567"));
        Assertions.assertFalse(DataDefinitionUtil.isValidFieldName("1234567"));
        Assertions.assertFalse(DataDefinitionUtil.isValidFieldName(null));
    }

    @Test
    void shouldKeepFieldNameWhenItIsAlreadyValid() {
        DataDefinitionField field = newField("title12345678", null);
        DataDefinition dataDefinition = newDataDefinition(field, newDataLayout("title12345678"));

        DataDefinitionUtil.updateDataDefinitionFields(dataDefinition, null);

        Assertions.assertEquals("title12345678", field.getName());
        Assertions.assertEquals("title12345678", firstLayoutFieldName(dataDefinition));
    }

    @Test
    void shouldGenerateFieldNameWhenStructureDoesNotExistYet() {
        DataDefinitionField field = newField("title", null);
        DataDefinition dataDefinition = newDataDefinition(field, newDataLayout("title"));

        DataDefinitionUtil.updateDataDefinitionFields(dataDefinition, null);

        Assertions.assertNotEquals("title", field.getName());
        Assertions.assertTrue(field.getName().startsWith("title"));
        Assertions.assertTrue(DataDefinitionUtil.isValidFieldName(field.getName()));
        Assertions.assertEquals(field.getName(), firstLayoutFieldName(dataDefinition));
    }

    @Test
    void shouldReuseExistingFieldNameWhenStructureIsUpdated() {
        DataDefinitionField field = newField("title", Collections.singletonMap("fieldReference", "titleReference"));
        DataDefinition dataDefinition = newDataDefinition(field, newDataLayout("title"));

        DataDefinitionUtil.updateDataDefinitionFields(
            dataDefinition,
            newDDMStructure("titleReference", "title87654321")
        );

        Assertions.assertEquals("title87654321", field.getName());
        Assertions.assertEquals("title87654321", firstLayoutFieldName(dataDefinition));
    }

    @Test
    void shouldGenerateFieldNameWhenFieldReferenceIsUnknown() {
        DataDefinitionField field = newField("title", Collections.singletonMap("fieldReference", "unknownReference"));
        DataDefinition dataDefinition = newDataDefinition(field, newDataLayout("title"));

        DataDefinitionUtil.updateDataDefinitionFields(
            dataDefinition,
            newDDMStructure("titleReference", "title87654321")
        );

        Assertions.assertNotEquals("title87654321", field.getName());
        Assertions.assertTrue(DataDefinitionUtil.isValidFieldName(field.getName()));
        Assertions.assertEquals(field.getName(), firstLayoutFieldName(dataDefinition));
    }

    @Test
    void shouldNotFailWhenDataDefinitionHasNoFields() {
        DataDefinition dataDefinition = new DataDefinition();

        Assertions.assertDoesNotThrow(() -> DataDefinitionUtil.updateDataDefinitionFields(dataDefinition, null));
    }

    private static DataDefinitionField newField(String name, Map<String, Object> customProperties) {
        DataDefinitionField field = new DataDefinitionField();
        field.setName(name);
        if (customProperties != null) {
            field.setCustomProperties(new HashMap<>(customProperties));
        }
        return field;
    }

    private static DataDefinition newDataDefinition(DataDefinitionField field, DataLayout dataLayout) {
        DataDefinition dataDefinition = new DataDefinition();
        dataDefinition.setDataDefinitionFields(new DataDefinitionField[] { field });
        dataDefinition.setDefaultDataLayout(dataLayout);
        return dataDefinition;
    }

    private static DataLayout newDataLayout(String fieldName) {
        DataLayoutColumn dataLayoutColumn = new DataLayoutColumn();
        dataLayoutColumn.setFieldNames(new String[] { fieldName });

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

    private static String firstLayoutFieldName(DataDefinition dataDefinition) {
        return dataDefinition
            .getDefaultDataLayout()
            .getDataLayoutPages()[0].getDataLayoutRows()[0].getDataLayoutColumns()[0].getFieldNames()[0];
    }
}
