package com.pbc.repository.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.pbc.exception.DataException;
import com.pbc.models.BlockStatusEnum;
import com.pbc.repository.BlockStatusDao;
import com.pbc.repository.mapper.BlockStatusRowMapper;
import com.pbc.repository.model.BlockStatus;

@Repository("blockStatusDao")
public class BlockStatusDaoImpl implements BlockStatusDao {

	private static final Logger logger = Logger.getLogger(BlockStatusDaoImpl.class);
	private final String TABLE_BLOCK_STATUS = "block_status";

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public NamedParameterJdbcTemplate getNamedJdbcTemplate() {
		return namedParameterJdbcTemplate;
	}

	@Override
	public void insert(final BlockStatus blockStatus) {
		try {
			final String insertQuery = "INSERT INTO " + TABLE_BLOCK_STATUS
					+ "(transactionId, tag, receiverAddress, status) VALUES "
					+ "(:transactionId, :tag, :receiverAddress, :status)";
			final MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("transactionId", blockStatus.getTransactionId());
			parameters.addValue("tag", blockStatus.getTag());
			parameters.addValue("receiverAddress", blockStatus.getReceiverAddress());
			parameters.addValue("status", blockStatus.getStatus());
			final int flag = getNamedJdbcTemplate().update(insertQuery, parameters);
			logger.info("value of insert flag" + flag);
			if (flag == 1) {
				logger.info(
						"STATUS: " + blockStatus.getStatus() + " Block status inserted successfully for combinedKey: "
								+ blockStatus.getTag() + blockStatus.getTransactionId());
			} else {
				logger.info("Error inserting block for combinedKey: " + blockStatus.getTag()
						+ blockStatus.getTransactionId());
			}
		} catch (final Exception e) {
			logger.error("Problem into inserting data for combined key: " + blockStatus.getTag()
					+ blockStatus.getTransactionId(), e);
			throw new DataException("Problem into inserting data for combined key: " + blockStatus.getTag()
					+ blockStatus.getTransactionId(), e);
		}
	}

	@Override
	public void updateStatus(final String tag, final String transactionId, final String status, final String receiver) {
		try {

			final StringBuilder sb = new StringBuilder();
			sb.append("update ").append(TABLE_BLOCK_STATUS).append(" set status = :status, updatedAt = :updatedAt ");
			if (receiver != null) {
				sb.append(", receiverAddress = :receiver ");
			}
			sb.append("where transactionId = :transactionId and tag = :tag");
			final MapSqlParameterSource sources = new MapSqlParameterSource();
			sources.addValue("tag", tag);
			sources.addValue("status", status);
			sources.addValue("updatedAt", new Date());
			sources.addValue("transactionId", transactionId);
			if (receiver != null) {
				sources.addValue("receiver", receiver);
			}
			final int flag = getNamedJdbcTemplate().update(sb.toString(), sources);
			if (flag == 1) {
				logger.info("STATUS: " + status + " Block status update successfully for combined key: " + tag
						+ transactionId);
			} else {
				logger.info("Problem updating block for combined key: " + tag + transactionId);
			}
		} catch (final Exception e) {
			logger.error("Error while updating block status for combined key: " + tag + transactionId, e);
			throw new DataException("Error while updating block status for combined key: " + tag + transactionId, e);
		}
	}

	@Override
	public BlockStatus getStatus(final String tag, final String transactionId) {
		try {
			final String getQuery = "select * from " + TABLE_BLOCK_STATUS
					+ " where transactionId = :transactionId and tag = :tag";
			final MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("transactionId", transactionId);
			parameters.addValue("tag", tag);
			return getNamedJdbcTemplate().queryForObject(getQuery, parameters,
					new BeanPropertyRowMapper<>(BlockStatus.class));
		} catch (final Exception erda) {
			logger.error("Empty result exception found for combined key: " + tag + transactionId + "  Reason :: "
					+ erda.getMessage());
			return null;
		}
	}

	@Override
	public List<BlockStatus> getStatus(final String[] tagList, final String[] txnIdList) {
		final List<BlockStatus> blockStatusList = new ArrayList<>();
		int i = 0;
		for (final String txnId : txnIdList) {
			final BlockStatus blockStatus = getStatus(tagList[i], txnId);
			i++;
			if (null != blockStatus) {
				blockStatusList.add(blockStatus);
			}
		}
		return blockStatusList;
	}

	@Override
	public List<BlockStatus> getBlockList(final List<String> receiverAddressList) {
		try {
			final MapSqlParameterSource source = new MapSqlParameterSource();
			source.addValue("addresses", receiverAddressList);
			final String query = "SELECT * FROM " + TABLE_BLOCK_STATUS
					+ " WHERE receiverAddress IN (:addresses) AND status = '" + BlockStatusEnum.SAVED.name() + "'";

			return namedParameterJdbcTemplate.query(query, source, new BlockStatusRowMapper());
		} catch (final Exception e) {
			logger.error("Exception while retrieving block info from database for given receiver addresses.", e);
			throw new DataException("Exception while retrieving block info from database for given receiver addresses.",
					e);
		}
	}

	@Override
	public long getTotalBlockCount() {
		final String query = "select count(*) from block_status;";
		return namedParameterJdbcTemplate.queryForObject(query, new MapSqlParameterSource(), Long.class).longValue();
	}

	@Override
	public long getAvailableBlockCount() {
		final MapSqlParameterSource source = new MapSqlParameterSource();
		source.addValue("SAVED", BlockStatusEnum.SAVED.name());
		source.addValue("BLOCK_DELETE_IN_PROCESS", BlockStatusEnum.BLOCK_DELETE_IN_PROCESS.name());
		final String query = "select count(*) from block_status where (status= :SAVED  OR status= :BLOCK_DELETE_IN_PROCESS)";
		return namedParameterJdbcTemplate.queryForObject(query, source, Long.class).longValue();
	}

	@Override
	public long getDeletedBlockCount() {
		final MapSqlParameterSource source = new MapSqlParameterSource();
		source.addValue("DELETED", BlockStatusEnum.DELETED.name());
		final String query = "select count(*) from block_status where (status= :DELETED)";
		return namedParameterJdbcTemplate.queryForObject(query, source, Long.class).longValue();
	}

	@Override
	public List<BlockStatus> getBlockToBeCreatedList() {
		final MapSqlParameterSource source = new MapSqlParameterSource();
		source.addValue("block_to_be_created", BlockStatusEnum.BLOCK_TO_BE_CREATED.name());
		final String query = "select * from " + TABLE_BLOCK_STATUS + " where status= :block_to_be_created";
		return namedParameterJdbcTemplate.query(query, source, new BlockStatusRowMapper());
	}

	@Override
	public BlockStatus getStatusifSaved(final String tag, final String transactionId) {
		try {
			final String getQuery = "select * from " + TABLE_BLOCK_STATUS
					+ " where transactionId = :transactionId and tag = :tag and status = :saved";
			final MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("transactionId", transactionId);
			parameters.addValue("tag", tag);
			parameters.addValue("saved", BlockStatusEnum.SAVED.name());
			return getNamedJdbcTemplate().queryForObject(getQuery, parameters,
					new BeanPropertyRowMapper<>(BlockStatus.class));
		} catch (final Exception erda) {
			logger.error("Empty result exception found for combined key: " + tag + transactionId + "  Reason :: "
					+ erda.getMessage());
			return null;
		}
	}

	@Override
	public List<BlockStatus> getBlockStatusListByPage(final int pageNo) {
		if (pageNo <= 0) {
			return null;
		}
		final String query = "select * from " + TABLE_BLOCK_STATUS + " order by createdAt desc limit "
				+ (pageNo - 1) * 100 + ", 100;";
		return namedParameterJdbcTemplate.query(query, new BlockStatusRowMapper());
	}

}