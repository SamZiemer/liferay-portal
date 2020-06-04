/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.dynamic.data.mapping.internal.upgrade.v3_7_1;

import com.liferay.dynamic.data.mapping.io.DDMFormDeserializer;
import com.liferay.dynamic.data.mapping.io.DDMFormDeserializerDeserializeRequest;
import com.liferay.dynamic.data.mapping.io.DDMFormDeserializerDeserializeResponse;
import com.liferay.dynamic.data.mapping.io.DDMFormSerializer;
import com.liferay.dynamic.data.mapping.io.DDMFormSerializerSerializeRequest;
import com.liferay.dynamic.data.mapping.io.DDMFormSerializerSerializeResponse;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMFormFieldValidation;
import com.liferay.dynamic.data.mapping.model.DDMFormFieldValidationExpression;
import com.liferay.dynamic.data.mapping.model.DDMFormRule;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.PortalUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Sam Ziemer
 */
public class UpgradeEmptyValidation extends UpgradeProcess {

	public UpgradeEmptyValidation(
		DDMFormDeserializer ddmFormDeserializer,
		DDMFormSerializer ddmFormSerializer) {

		_ddmFormDeserializer = ddmFormDeserializer;
		_ddmFormSerializer = ddmFormSerializer;
	}

	@Override
	public void doUpgrade() throws Exception {
		try (PreparedStatement ps1 = connection.prepareStatement(
				"select DDMStructure.definition, DDMStructure.structureId " +
					"from DDMStructure where classNameId = ? and definition " +
						"like '%validation%'");
			PreparedStatement ps2 =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection,
					"update DDMStructure set definition = ? where " +
						"structureId = ?");
			PreparedStatement ps3 = connection.prepareStatement(
				"select structureVersionId, definition from " +
					"DDMStructureVersion where structureId = ?");
			PreparedStatement ps4 =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection,
					"update DDMStructureVersion set definition = ? where " +
						"structureVersionId = ?")) {

			long classNameId = PortalUtil.getClassNameId(
				"com.liferay.dynamic.data.lists.model.DDLRecordSet");

			ps1.setLong(1, classNameId);

			try (ResultSet rs = ps1.executeQuery()) {
				while (rs.next()) {
					String definition = rs.getString(1);

					String newDefinition = _updateEmptyValidation(definition);

					if (newDefinition.equals(definition)) {
						continue;
					}

					long structureId = rs.getLong(2);

					ps2.setString(1, newDefinition);

					ps2.setLong(2, structureId);

					ps2.addBatch();

					ps3.setLong(1, structureId);

					try (ResultSet rs2 = ps3.executeQuery()) {
						while (rs2.next()) {
							definition = rs2.getString("definition");

							newDefinition = _updateEmptyValidation(definition);

							if (newDefinition.equals(definition)) {
								continue;
							}

							ps4.setString(1, newDefinition);

							long structureVersionId = rs2.getLong(
								"structureVersionId");

							ps4.setLong(2, structureVersionId);

							ps4.addBatch();
						}
					}
				}

				ps2.executeBatch();

				ps4.executeBatch();
			}
		}
	}

	private String _updateEmptyValidation(String definition) {
		DDMFormDeserializerDeserializeRequest.Builder deserializerBuilder =
			DDMFormDeserializerDeserializeRequest.Builder.newBuilder(
				definition);

		DDMFormDeserializerDeserializeResponse
			ddmFormDeserializerDeserializeResponse =
				_ddmFormDeserializer.deserialize(deserializerBuilder.build());

		DDMForm ddmForm = ddmFormDeserializerDeserializeResponse.getDDMForm();

		Map<String, DDMFormField> ddmFormFieldsMap =
			ddmForm.getDDMFormFieldsMap(true);

		List<DDMFormRule> ddmFormRules = ddmForm.getDDMFormRules();

		List<DDMFormField> ddmformfieldsList = new ArrayList<>();

		for (DDMFormField ddmFormField : ddmFormFieldsMap.values()) {
			DDMFormFieldValidation ddmFormFieldValidation =
				ddmFormField.getDDMFormFieldValidation();

			if (ddmFormFieldValidation != null) {
				DDMFormFieldValidationExpression
					ddmFormFieldValidationExpression =
						ddmFormFieldValidation.
							getDDMFormFieldValidationExpression();

				String name = ddmFormFieldValidationExpression.getName();
				String value = ddmFormFieldValidationExpression.getValue();

				if (name.isEmpty() && value.isEmpty()) {
					ddmFormField.setDDMFormFieldValidation(null);
				}
			}

			ddmformfieldsList.add(ddmFormField);
		}

		ddmForm.setDDMFormRules(ddmFormRules);

		ddmForm.setDDMFormFields(ddmformfieldsList);

		DDMFormSerializerSerializeRequest.Builder serializerBuilder =
			DDMFormSerializerSerializeRequest.Builder.newBuilder(ddmForm);

		DDMFormSerializerSerializeResponse ddmFormSerializerSerializeResponse =
			_ddmFormSerializer.serialize(serializerBuilder.build());

		return ddmFormSerializerSerializeResponse.getContent();
	}

	private final DDMFormDeserializer _ddmFormDeserializer;
	private final DDMFormSerializer _ddmFormSerializer;

}