package com.finance.tracker.transaction;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Receita e despesa exigem categoria; transferência não tem (D14). O erro sai no campo categoryId.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CategoryRequiredUnlessTransfer.Validator.class)
public @interface CategoryRequiredUnlessTransfer {

    String message() default "Categoria é obrigatória";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<CategoryRequiredUnlessTransfer, TransactionRequest> {
        @Override
        public boolean isValid(TransactionRequest request, ConstraintValidatorContext context) {
            if (request.type() == null || request.type() == TransactionType.TRANSFER || request.categoryId() != null) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("categoryId")
                    .addConstraintViolation();
            return false;
        }
    }
}
