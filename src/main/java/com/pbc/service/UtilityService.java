package com.pbc.service;

import org.springframework.stereotype.Service;

import com.pbc.models.BlockStatusEnum;
import com.pbc.models.CustomResponse;

@Service("utilityService")
public class UtilityService {
	public static CustomResponse<Object> getCustomResponseEnum(final CustomResponse<Object> customResponse,
			final Enum<BlockStatusEnum> enumValue) {
		customResponse.setMessage(enumValue.name());
		return customResponse;
	}
}
