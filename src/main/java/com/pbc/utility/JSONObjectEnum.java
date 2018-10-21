package com.pbc.utility;

/**
 * Enum is used to decide about which json object to create. Enum is used by
 * SendNotificationService to identify what notification should it send.
 */
public enum JSONObjectEnum {

	CRC("crc"), VALIDITY("validity");

	private String value;

	private JSONObjectEnum(final String value) {
		this.value = value;
	}

	public JSONObjectEnum getByValue(final String value) {
		for (final JSONObjectEnum jsonEnum : values()) {
			if (jsonEnum.value.equals(value)) {
				return jsonEnum;
			}
		}
		return null;
	}
}
