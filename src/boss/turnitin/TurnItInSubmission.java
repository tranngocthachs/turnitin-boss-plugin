package boss.turnitin;

import uk.ac.warwick.dcs.boss.plugins.spi.dao.PluginEntity;

public class TurnItInSubmission extends PluginEntity {
	private Long submissionId; // id the submission in BOSS
	private String objectId; // id the submission on TurnItIn
	
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
}
