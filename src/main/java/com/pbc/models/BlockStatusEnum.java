package com.pbc.models;

public enum BlockStatusEnum {

	UNVAILABLE("block_unvailable"), SAVED("block_saved"), INPROCESS("block_inprocess"), BLOCK_TO_BE_CREATED(
			"block_to_be_created"), SAVE_FAILED("block_save_failed"), ERROR_OCCURED(
					"error occured"), DELETED("block_deleted"), BLOCK_DELETE_IN_PROCESS("block_delete_processing");

	private String value;

	private BlockStatusEnum(final String value) {
		this.value = value;
	}

	public BlockStatusEnum getByValue(final String value) {
		for (final BlockStatusEnum blockEnum : values()) {
			if (blockEnum.value.equals(value)) {
				return blockEnum;
			}
		}
		return null;
	}
}
