package com.ableneo.liferay.portal.setup.core.util;

import com.liferay.data.engine.rest.dto.v2_0.DataDefinition;
import com.liferay.data.engine.rest.dto.v2_0.DataDefinitionField;
import com.liferay.data.engine.rest.dto.v2_0.DataLayout;
import com.liferay.data.engine.rest.dto.v2_0.DataLayoutColumn;
import com.liferay.data.engine.rest.dto.v2_0.DataLayoutPage;
import com.liferay.data.engine.rest.dto.v2_0.DataLayoutRow;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.util.DDMFormFieldUtil;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import java.util.Map;

/**
 * Normalizes field names of a data definition to the {@code <name><eight digit instance id>} form Liferay expects,
 * before it is sent to {@code DataDefinitionResource}. On update the name already stored in the structure is reused,
 * matched by the {@code fieldReference} custom property, so that a repeated setup run does not rename the fields and
 * orphan the article content stored under the previous names.
 *
 * <p>
 * Liferay normalizes field names in the browser (form builder JS in {@code com.liferay.data.engine.js.components.web}),
 * so neither the REST resource nor the removed {@code com.liferay.journal.web.internal.util.DataDefinitionUtil} does it
 * server side. This library posts to the same resource with no browser in between, so it has to normalize itself.
 * </p>
 */
public final class DataDefinitionUtil {

    private static final String FIELD_REFERENCE = "fieldReference";
    private static final int FIELD_NAME_INSTANCE_ID_LENGTH = 8;

    private DataDefinitionUtil() {}

    /**
     * A field name is considered valid when it ends with a portal generated instance id, that is eight digits.
     */
    public static boolean isValidFieldName(final String fieldName) {
        if (fieldName == null) {
            return false;
        }
        int index = fieldName.length() - FIELD_NAME_INSTANCE_ID_LENGTH;
        return index >= 0 && Validator.isNumber(fieldName.substring(index));
    }

    /**
     * Renames every field of the data definition to a valid field name and propagates the rename into the default data
     * layout.
     *
     * @param dataDefinition data definition to update in place
     * @param ddmStructure structure that is being updated, {@code null} when a new structure is created
     */
    public static void updateDataDefinitionFields(
        final DataDefinition dataDefinition,
        final DDMStructure ddmStructure
    ) {
        DataDefinitionField[] dataDefinitionFields = dataDefinition.getDataDefinitionFields();
        if (dataDefinitionFields == null) {
            return;
        }
        for (DataDefinitionField dataDefinitionField : dataDefinitionFields) {
            String originalName = dataDefinitionField.getName();
            String fieldName = getFieldName(dataDefinitionField, ddmStructure, originalName);
            if (StringUtil.equals(fieldName, originalName)) {
                continue;
            }
            dataDefinitionField.setName(fieldName);
            updateDataLayoutFieldName(dataDefinition.getDefaultDataLayout(), fieldName, originalName);
        }
    }

    private static String getFieldName(
        final DataDefinitionField dataDefinitionField,
        final DDMStructure ddmStructure,
        final String originalName
    ) {
        if (isValidFieldName(originalName)) {
            return originalName;
        }
        String fieldName = originalName;
        if (ddmStructure != null) {
            String existingFieldName = getExistingFieldName(dataDefinitionField, ddmStructure);
            if (existingFieldName != null) {
                if (isValidFieldName(existingFieldName)) {
                    return existingFieldName;
                }
                fieldName = existingFieldName;
            }
        }
        return DDMFormFieldUtil.getDDMFormFieldName(fieldName);
    }

    /**
     * Looks up the name the field already has in the given structure, matched by its {@code fieldReference} custom
     * property.
     */
    private static String getExistingFieldName(
        final DataDefinitionField dataDefinitionField,
        final DDMStructure ddmStructure
    ) {
        Map<String, Object> customProperties = dataDefinitionField.getCustomProperties();
        if (customProperties == null) {
            return null;
        }
        String fieldReference = MapUtil.getString(customProperties, FIELD_REFERENCE);
        if (Validator.isNull(fieldReference)) {
            return null;
        }
        DDMForm ddmForm = ddmStructure.getDDMForm();
        Map<String, DDMFormField> ddmFormFieldsReferencesMap = ddmForm.getDDMFormFieldsReferencesMap(true);
        DDMFormField ddmFormField = ddmFormFieldsReferencesMap.get(fieldReference);
        if (ddmFormField == null) {
            return null;
        }
        return ddmFormField.getName();
    }

    private static void updateDataLayoutFieldName(
        final DataLayout dataLayout,
        final String fieldName,
        final String originalName
    ) {
        if (dataLayout == null || dataLayout.getDataLayoutPages() == null) {
            return;
        }
        for (DataLayoutPage dataLayoutPage : dataLayout.getDataLayoutPages()) {
            if (dataLayoutPage.getDataLayoutRows() == null) {
                continue;
            }
            for (DataLayoutRow dataLayoutRow : dataLayoutPage.getDataLayoutRows()) {
                if (dataLayoutRow.getDataLayoutColumns() == null) {
                    continue;
                }
                for (DataLayoutColumn dataLayoutColumn : dataLayoutRow.getDataLayoutColumns()) {
                    String[] fieldNames = dataLayoutColumn.getFieldNames();
                    if (fieldNames == null) {
                        continue;
                    }
                    for (int i = 0; i < fieldNames.length; i++) {
                        if (originalName.equals(fieldNames[i])) {
                            fieldNames[i] = fieldName;
                            return;
                        }
                    }
                }
            }
        }
    }
}
