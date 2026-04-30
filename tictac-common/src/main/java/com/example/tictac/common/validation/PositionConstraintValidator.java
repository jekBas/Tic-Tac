package com.example.tictac.common.validation;

import com.example.tictac.common.GameConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PositionConstraintValidator implements ConstraintValidator<PositionConstraint, Integer> {

	@Override
	public boolean isValid(Integer value,
												 ConstraintValidatorContext context) {
		if (value == null) {
			return buildViolation(context, "Position must not be null");
		}
		if (value < 0 || value >= GameConstants.BOARD_SIZE) {
			return buildViolation(context, "Position must be between 0 and " + (GameConstants.BOARD_SIZE - 1));
		}
		return true;
	}

	private boolean buildViolation(ConstraintValidatorContext context,
																 String errorMessage) {
		context.disableDefaultConstraintViolation();
		context
				.buildConstraintViolationWithTemplate("Validation Error: " + errorMessage)
				.addConstraintViolation();
		return false;
	}
}