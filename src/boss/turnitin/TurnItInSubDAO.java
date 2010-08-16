package boss.turnitin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import uk.ac.warwick.dcs.boss.model.dao.DAOException;
import uk.ac.warwick.dcs.boss.plugins.dbschema.SQLTableSchema;
import uk.ac.warwick.dcs.boss.plugins.spi.dao.PluginEntityDAO;

public class TurnItInSubDAO extends PluginEntityDAO<TurnItInSubmission> {

	@Override
	public String getTableName() {
		return "tii_submission";
	}

	@Override
	public Collection<String> getDatabaseFieldNames() {
		Collection<String> retval = new ArrayList<String>();
		retval.add("submission_id");
		retval.add("tii_oid");
		return retval;
	}

	@Override
	public Collection<Object> getDatabaseValues(TurnItInSubmission entity) {
		Collection<Object> retval = new ArrayList<Object>();
		retval.add(entity.getSubmissionId());
		retval.add(entity.getObjectId());
		return retval;
	}

	@Override
	public TurnItInSubmission createInstanceFromDatabaseValues(
			String tableName, ResultSet databaseValues) throws SQLException,
			DAOException {
		TurnItInSubmission tiiSub = new TurnItInSubmission();
		tiiSub.setSubmissionId(databaseValues.getLong(tableName + ".submission_id"));
		tiiSub.setObjectId(databaseValues.getString(tableName + ".tii_oid"));
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
		schema.setForeignKey("submission_id", "submission");
		return schema;
	}

}
