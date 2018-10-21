package com.pbc.repository;

import java.util.List;

import com.pbc.repository.model.TemporaryUrlDownload;

public interface TemporaryUrlDownloadDao {

	void insert(final TemporaryUrlDownload temporaryUrlDownload);

	void updateStatus(final String uuid, final boolean status);

	TemporaryUrlDownload getFilePath(final String uuid);

	void bulkUrlInsert(final List<TemporaryUrlDownload> data);

}
