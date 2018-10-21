package com.pbc.repository.impl;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import com.pbc.exception.DataException;
import com.pbc.repository.TemporaryUrlDownloadDao;
import com.pbc.repository.mapper.TemporaryUrlDownloadRowMapper;
import com.pbc.repository.model.TemporaryUrlDownload;

@Repository("temporaryUrlDownloadDao")
public class TemporaryUrlDownloadDaoImpl implements TemporaryUrlDownloadDao {

	private static final Logger logger = Logger.getLogger(TemporaryUrlDownloadDaoImpl.class);
	private static final String TABLE_NAME = "download_url";

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public NamedParameterJdbcTemplate getNamedJdbcTemplate() {
		return namedParameterJdbcTemplate;
	}

	@Override
	public void insert(final TemporaryUrlDownload temporaryUrlDownload) {
		try {
			final String insertQuery = "insert into " + TABLE_NAME
					+ "(uuid, status, fileLocation, data_hash) values (:uuid, :status, :fileLocation, :dataHash)";

			final MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("uuid", temporaryUrlDownload.getUuid());
			parameters.addValue("status", temporaryUrlDownload.getStatus());
			parameters.addValue("fileLocation", temporaryUrlDownload.getFilePath());
			parameters.addValue("dataHash", temporaryUrlDownload.getDataHash());

			final int flag = getNamedJdbcTemplate().update(insertQuery, parameters);

			if (flag == 1) {
				logger.info("Block status inserted successfully");
			} else {
				logger.error("Problem inserting block");
			}
		} catch (final Exception e) {
			logger.error("Problem inserting data ", e);
			throw new DataException("Problem inserting data ", e);
		}

	}

	@Override
	public void updateStatus(final String uuid, final boolean status) {
		try {
			final String updateQuery = "update " + TABLE_NAME + " set status = :status where uuid = :uuid";

			final MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("status", status);
			parameters.addValue("uuid", uuid);

			final int flag = getNamedJdbcTemplate().update(updateQuery, parameters);

			if (flag == 1) {
				logger.info("status of temporary url has been changed for UUID " + uuid);
			} else {
				logger.info("Status of temporary url not changed for UUID::" + uuid);
			}
		} catch (final Exception e) {
			logger.error("Problem in updating block status ", e);
			throw new DataException("Problem in updating block status ", e);
		}
	}

	@Override
	public TemporaryUrlDownload getFilePath(final String uuid) {
		try {
			logger.info("getFilePath for uuid " + uuid);
			final String fileQuery = "SELECT * FROM " + TABLE_NAME + " WHERE uuid= :uuid LIMIT 1;";

			final MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("uuid", uuid);

			final TemporaryUrlDownload fileDownloadData = getNamedJdbcTemplate().queryForObject(fileQuery, parameters,
					new TemporaryUrlDownloadRowMapper());

			if (fileDownloadData != null) {
				logger.info("Returning filePath " + fileDownloadData.getFilePath());
				return fileDownloadData;
			} else {
				logger.info("FilePath value is null");
				return null;
			}
		} catch (final Exception e) {
			logger.error("Unable to get file path ", e);
			throw new DataException("Unable to get file path ", e);
		}
	}

	@Override
	public void bulkUrlInsert(final List<TemporaryUrlDownload> data) {
		try {
			final String insertQuery = "insert into " + TABLE_NAME
					+ "(uuid, status, fileLocation, data_hash) values (:uuid, :status, :filePath, :dataHash)";

			final SqlParameterSource[] params = SqlParameterSourceUtils.createBatch(data.toArray());
			namedParameterJdbcTemplate.batchUpdate(insertQuery, params);
		} catch (final Exception e) {
			logger.error("Error in bulk insert ", e);
		}
	}
}
