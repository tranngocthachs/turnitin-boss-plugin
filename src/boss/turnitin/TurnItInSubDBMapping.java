package boss.turnitin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import boss.plugins.dbschema.SQLTableSchema;
import boss.plugins.spi.dao.IPluginDBMapping;

import uk.ac.warwick.dcs.boss.model.dao.DAOException;

public class TurnItInSubDBMapping extends IPluginDBMapping<TurnItInSubmission> {

	@Override
	public String getTableName() {
		return "tii_submission";
	}

	@Override
	public Collection<String> getDatabaseFieldNames() {
		Collection<String> retval = new ArrayList<String>();
		retval.add("submission_id");
		retval.add("tii_oid");
		retval.add("filename");
		return retval;
	}

	@Override
	public Collection<Object> getDatabaseValues(TurnItInSubmission entity) {
		Collection<Object> retval = new ArrayList<Object>();
		retval.add(entity.getSubmissionId());
		retval.add(entity.getObjectId());
		retval.add(entity.getFilename());
		return retval;
	}

	@Override
	public TurnItInSubmission createInstanceFromDatabaseValues(
			String tableName, ResultSet databaseValues) throws SQLException,
			DAOException {
		TurnItInSubmission tiiSub = new TurnItInSubmission();
		tiiSub.setSubmissionId(databaseValues.getLong(tableName + ".submission_id"));
		tiiSub.setObjectId(databaseValues.getString(tableName + ".tii_oid"));
		tiiSub.setFilename(databaseValues.getString(tableName + ".filename"));
		return tiiSub;
	}

	@Override
	public String getMySQLSortingString() {
		return "id DESC";
	}

	@Override
	public SQLTableSchema getTableSchema() {
		SQLTableSchema schema = new SQLTableSchema(getTableName());
		schema.addIntColumn("submission_id", true, true);
		schema.addVarCharColumn("tii_oid", 15, true, true);
		schema.addVarCharColumn("filename", 64, true);
		schema.setForeignKey("submission_id", "submission");
		return schema;
	}

}
