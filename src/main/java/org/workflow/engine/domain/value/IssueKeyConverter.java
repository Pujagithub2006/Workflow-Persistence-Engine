package org.workflow.engine.domain.value;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.workflow.engine.domain.valueobject.IssueKey;

@Converter(autoApply = true)
public class IssueKeyConverter implements AttributeConverter<IssueKey, String> {
    @Override
    public String convertToDatabaseColumn(IssueKey key) {
        return key == null ? null : key.getValue();
    }

    @Override
    public IssueKey convertToEntityAttribute(String value) {
        return value == null ? null : new IssueKey(value);
    }
}