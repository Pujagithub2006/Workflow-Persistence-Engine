package org.workflow.engine.domain.value;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.workflow.engine.domain.valueobject.Email;

@Converter(autoApply = true)
public class EmailConverter implements AttributeConverter<Email, String> {

    @Override
    public String convertToDatabaseColumn(Email email) {
        return email == null ? null : email.getValue();
    }

    @Override
    public Email convertToEntityAttribute(String value) {
        return value == null ? null : new Email(value);
    }
}
