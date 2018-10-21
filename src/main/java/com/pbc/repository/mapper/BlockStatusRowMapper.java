package com.pbc.repository.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.pbc.repository.model.BlockStatus;

public class BlockStatusRowMapper implements RowMapper<BlockStatus> {

	@Override
	public BlockStatus mapRow(final ResultSet rs, final int rowNum) throws SQLException {
		return new BlockStatus().setTransactionId(rs.getString("transactionId")).setStatus(rs.getString("status"))
				.setTag(rs.getString("tag")).setReceiverAddress(rs.getString("receiverAddress"))
				.setCreatedAt(rs.getDate("createdAt")).setUpdatedAt(rs.getDate("updatedAt"));
	}
}
