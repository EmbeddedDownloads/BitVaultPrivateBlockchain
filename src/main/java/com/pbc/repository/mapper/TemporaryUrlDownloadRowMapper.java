package com.pbc.repository.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.pbc.repository.model.TemporaryUrlDownload;

public class TemporaryUrlDownloadRowMapper implements RowMapper<TemporaryUrlDownload> {

	@Override
	public TemporaryUrlDownload mapRow(final ResultSet rs, final int rowNum) throws SQLException {
		return new TemporaryUrlDownload(rs.getString("uuid"), rs.getString("fileLocation"), rs.getBoolean("status"),
				rs.getString("data_hash"));
	}
}
