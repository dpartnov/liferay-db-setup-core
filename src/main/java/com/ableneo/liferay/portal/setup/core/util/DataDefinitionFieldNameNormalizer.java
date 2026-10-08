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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Appends the eight digit instance id that Liferay requires in a field name, which the portal only does in the form
 * builder JS and therefore not for a definition posted as a plain JSON file. The id is derived from the field
 * reference rather than random, so a repeated setup run keeps the names and does not orphan article content.
 */
public final class DataDefinitionFieldNameNormalizer {

    private static final String FIELD_REFERENCE = "fieldReference";
    private static final int INSTANCE_ID_LENGTH = 8;
    private static final long INSTANCE_ID_BOUND = 100_000_000L;
    private static final long HASH_OFFSET = 0xcbf29ce484222325L;
    private static final long HASH_PRIME = 0x100000001b3L;

    private DataDefinitionFieldNameNormalizer() {}

    public static boolean hasInstanceId(final String fieldName) {
        if (fieldName == null || fieldName.length() < INSTANCE_ID_LENGTH) {
            return false;
        }
        for (int i = fieldName.length() - INSTANCE_ID_LENGTH; i < fieldName.length(); i++) {
            char character = fieldName.charAt(i);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Renames the fields in place and rewrites the references to them in the default data layout.
     *
     * @param ddmStructure structure that is being updated, {@code null} when the structure is created
     */
    public static void normalize(final DataDefinition dataDefinition, final DDMStructure ddmStructure) {
        if (dataDefinition == null) {
            return;
        }
        Map<String, DDMFormField> storedFieldsByReference = readStoredFieldsByReference(ddmStructure);
        Map<String, String> renamedFieldNames = new LinkedHashMap<>();
        Set<String> usedFieldNames = new HashSet<>();

        renameFields(
            dataDefinition.getDataDefinitionFields(),
            storedFieldsByReference,
            usedFieldNames,
            renamedFieldNames
        );

        if (!renamedFieldNames.isEmpty()) {
            rewriteDataLayout(dataDefinition.getDefaultDataLayout(), renamedFieldNames);
        }
    }

    /**
     * Walks the fields, nested ones included, and collects the renames the data layout has to follow.
     */
    private static void renameFields(
        final DataDefinitionField[] dataDefinitionFields,
        final Map<String, DDMFormField> storedFieldsByReference,
        final Set<String> usedFieldNames,
        final Map<String, String> renamedFieldNames
    ) {
        if (dataDefinitionFields == null) {
            return;
        }
        for (DataDefinitionField dataDefinitionField : dataDefinitionFields) {
            String currentName = dataDefinitionField.getName();
            if (currentName != null && !currentName.isEmpty()) {
                String storableName = resolveFieldName(
                    dataDefinitionField,
                    currentName,
                    storedFieldsByReference,
                    usedFieldNames
                );
                usedFieldNames.add(storableName);
                if (!storableName.equals(currentName)) {
                    dataDefinitionField.setName(storableName);
                    renamedFieldNames.put(currentName, storableName);
                }
            }
            renameFields(
                dataDefinitionField.getNestedDataDefinitionFields(),
                storedFieldsByReference,
                usedFieldNames,
                renamedFieldNames
            );
        }
    }

    private static String resolveFieldName(
        final DataDefinitionField dataDefinitionField,
        final String currentName,
        final Map<String, DDMFormField> storedFieldsByReference,
        final Set<String> usedFieldNames
    ) {
        String fieldReference = readFieldReference(dataDefinitionField);
        DDMFormField storedField = fieldReference == null ? null : storedFieldsByReference.get(fieldReference);
        if (storedField != null && storedField.getName() != null) {
            // the content of the existing articles is stored under this name, renaming it would orphan the content
            return storedField.getName();
        }
        if (hasInstanceId(currentName)) {
            return currentName;
        }
        return generateFieldName(currentName, fieldReference, usedFieldNames);
    }

    /**
     * On a collision within the same definition the id is counted up until it is free.
     */
    private static String generateFieldName(
        final String currentName,
        final String fieldReference,
        final Set<String> usedFieldNames
    ) {
        String baseName = currentName.replace(" ", "");
        long instanceId = toInstanceId(fieldReference == null ? currentName : fieldReference);
        String candidate = baseName + formatInstanceId(instanceId);
        while (usedFieldNames.contains(candidate)) {
            instanceId = (instanceId + 1) % INSTANCE_ID_BOUND;
            candidate = baseName + formatInstanceId(instanceId);
        }
        return candidate;
    }

    /** FNV-1a over the seed, folded into the range of an eight digit instance id. */
    private static long toInstanceId(final String seed) {
        long hash = HASH_OFFSET;
        for (int i = 0; i < seed.length(); i++) {
            hash ^= seed.charAt(i);
            hash *= HASH_PRIME;
        }
        return Math.floorMod(hash, INSTANCE_ID_BOUND);
    }

    private static String formatInstanceId(final long instanceId) {
        StringBuilder instanceIdBuilder = new StringBuilder(Long.toString(instanceId));
        while (instanceIdBuilder.length() < INSTANCE_ID_LENGTH) {
            instanceIdBuilder.insert(0, '0');
        }
        return instanceIdBuilder.toString();
    }

    private static Map<String, DDMFormField> readStoredFieldsByReference(final DDMStructure ddmStructure) {
        if (ddmStructure == null) {
            return Collections.emptyMap();
        }
        DDMForm ddmForm = ddmStructure.getDDMForm();
        if (ddmForm == null) {
            return Collections.emptyMap();
        }
        Map<String, DDMFormField> storedFieldsByReference = ddmForm.getDDMFormFieldsReferencesMap(true);
        return storedFieldsByReference == null ? Collections.emptyMap() : storedFieldsByReference;
    }

    private static String readFieldReference(final DataDefinitionField dataDefinitionField) {
        Map<String, Object> customProperties = dataDefinitionField.getCustomProperties();
        if (customProperties == null) {
            return null;
        }
        Object fieldReference = customProperties.get(FIELD_REFERENCE);
        if (fieldReference == null) {
            return null;
        }
        String value = String.valueOf(fieldReference).trim();
        return value.isEmpty() ? null : value;
    }

    /** A name left behind in the layout would render an empty row, so every occurrence is replaced. */
    private static void rewriteDataLayout(final DataLayout dataLayout, final Map<String, String> renamedFieldNames) {
        if (dataLayout == null || dataLayout.getDataLayoutPages() == null) {
            return;
        }
        for (DataLayoutPage dataLayoutPage : dataLayout.getDataLayoutPages()) {
            DataLayoutRow[] dataLayoutRows = dataLayoutPage.getDataLayoutRows();
            if (dataLayoutRows == null) {
                continue;
            }
            for (DataLayoutRow dataLayoutRow : dataLayoutRows) {
                DataLayoutColumn[] dataLayoutColumns = dataLayoutRow.getDataLayoutColumns();
                if (dataLayoutColumns == null) {
                    continue;
                }
                for (DataLayoutColumn dataLayoutColumn : dataLayoutColumns) {
                    rewriteDataLayoutColumn(dataLayoutColumn, renamedFieldNames);
                }
            }
        }
    }

    private static void rewriteDataLayoutColumn(
        final DataLayoutColumn dataLayoutColumn,
        final Map<String, String> renamedFieldNames
    ) {
        String[] fieldNames = dataLayoutColumn.getFieldNames();
        if (fieldNames == null) {
            return;
        }
        boolean rewritten = false;
        for (int i = 0; i < fieldNames.length; i++) {
            String renamedFieldName = renamedFieldNames.get(fieldNames[i]);
            if (renamedFieldName != null) {
                fieldNames[i] = renamedFieldName;
                rewritten = true;
            }
        }
        if (rewritten) {
            dataLayoutColumn.setFieldNames(fieldNames);
        }
    }
}
