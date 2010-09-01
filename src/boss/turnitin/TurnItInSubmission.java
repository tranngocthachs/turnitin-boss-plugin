package boss.turnitin;

import uk.ac.warwick.dcs.boss.plugins.spi.dao.IPluginEntity;

public class TurnItInSubmission extends IPluginEntity {
	private Long submissionId; // id the submission in BOSS
	private String objectId; // id the submission on TurnItIn
	private String filename; // the filename of the submission
	
	public Long getSubmissionId() {
		return submissionId;
	}
	public void setSubmissionId(Long submissionId) {
		this.submissionId = submissionId;
	}
	public String getObjectId() {
		return objectId;
	}
	public void setObjectId(String objectId) {
		if (objectId != null) {
			objectId = objectId.trim();
		}
		this.objectId = objectId;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
}
