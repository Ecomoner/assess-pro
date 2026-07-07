package com.frist.assesspro.validation;


import com.frist.assesspro.dto.TestDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, TestDTO> {
    @Override
    public boolean isValid(TestDTO dto, ConstraintValidatorContext context) {
        if (dto.getAvailableFrom() == null || dto.getAvailableTo() == null) {
            return true;
        }
        if (!dto.getAvailableTo().isAfter(dto.getAvailableFrom())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("availableTo")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}