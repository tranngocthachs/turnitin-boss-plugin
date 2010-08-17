package boss.turnitin;

import static boss.turnitin.TIICommResult.GET_ORI_SCORE_ERROR_CODE_FCMD2_NOT_YET_AVAI;
import static boss.turnitin.TIICommResult.GET_ORI_SCORE_SUCCESS_CODE_FCMD2;
import static boss.turnitin.TurnItInComm.createInstructorAndLogin;
import static boss.turnitin.TurnItInComm.getOriginalityScore;
import static boss.turnitin.TurnitinAPI.urlEnc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import uk.ac.warwick.dcs.boss.frontend.PageContext;
import uk.ac.warwick.dcs.boss.frontend.sites.StaffPageFactory;
import uk.ac.warwick.dcs.boss.model.FactoryException;
import uk.ac.warwick.dcs.boss.model.FactoryRegistrar;
import uk.ac.warwick.dcs.boss.model.dao.DAOException;
import uk.ac.warwick.dcs.boss.model.dao.DAOFactory;
import uk.ac.warwick.dcs.boss.model.dao.IAssignmentDAO;
import uk.ac.warwick.dcs.boss.model.dao.IDAOSession;
import uk.ac.warwick.dcs.boss.model.dao.IEntityDAO;
import uk.ac.warwick.dcs.boss.model.dao.IModuleDAO;
import uk.ac.warwick.dcs.boss.model.dao.IStaffInterfaceQueriesDAO;
import uk.ac.warwick.dcs.boss.model.dao.IStaffInterfaceQueriesDAO.StaffSubmissionsQuerySortingType;
import uk.ac.warwick.dcs.boss.model.dao.beans.Assignment;
import uk.ac.warwick.dcs.boss.model.dao.beans.Module;
import uk.ac.warwick.dcs.boss.model.dao.beans.Person;
import uk.ac.warwick.dcs.boss.model.dao.beans.Submission;
import uk.ac.warwick.dcs.boss.model.dao.beans.queries.StaffSubmissionsQueryResult;
import uk.ac.warwick.dcs.boss.plugins.spi.extralinks.StaffAssignmentPluginEntryProvider;
import uk.ac.warwick.dcs.boss.plugins.spi.pages.StaffPluginPageProvider;

public class TurnItInSubmissionsPage extends StaffPluginPageProvider implements StaffAssignmentPluginEntryProvider {
	
	public String getPageName() {
		return "tii_submissions";
	}

	public String getLinkLabel() {
		return "Submit papers to TurnItIn";
	}

	public String getAssignmentParaString() {
		return "assignment";
	}

	public String getPageTemplate() {
		return "staff_tii_submissions";
	}
	
	public void handleGet(PageContext pageContext, Template template,
			VelocityContext templateContext) throws ServletException,
			IOException {
		IDAOSession f;
		try {
			DAOFactory df = (DAOFactory)FactoryRegistrar.getFactory(DAOFactory.class);
			f = df.getInstance();
		} catch (FactoryException e) {
			throw new ServletException("dao init error", e);
		}
		
		// Get assignmentId
		String assignmentString = pageContext.getParameter(getAssignmentParaString());
		if (assignmentString == null) {
			throw new ServletException("No assignment parameter given");
		}
		Long assignmentId = Long
				.valueOf(pageContext.getParameter(getAssignmentParaString()));
		
		// Render page
		try {
			f.beginTransaction();
			
			IStaffInterfaceQueriesDAO staffInterfaceQueriesDao = f.getStaffInterfaceQueriesDAOInstance();
			IAssignmentDAO assignmentDao = f.getAssignmentDAOInstance();
			Assignment assignment = assignmentDao.retrievePersistentEntity(assignmentId);
			
			if (!staffInterfaceQueriesDao.isStaffModuleAccessAllowed(pageContext.getSession().getPersonBinding().getId(), assignment.getModuleId())) {
				f.abortTransaction();
				throw new ServletException("permission denied");
			}
			
			Person staff = pageContext.getSession().getPersonBinding();
			// Make sure the corresponding instructor account exists in TurnItIn system
			TIICommResult commResult = createInstructorAndLogin(staff);
			if (!commResult.isSuccessful()) {
				pageContext.performRedirect(pageContext.getPageUrl(StaffPageFactory.SITE_NAME, "tii_error?ec="+commResult.getReturnCode()+"&em="+urlEnc(commResult.getReturnMessage())));
				return;
			}
			
			IModuleDAO moduleDao = f.getModuleDAOInstance();
			Module module = moduleDao.retrievePersistentEntity(assignment.getModuleId());
			templateContext.put("greet", pageContext.getSession().getPersonBinding().getChosenName());
			templateContext.put("module", module);
			templateContext.put("assignment", assignment);
			IStaffInterfaceQueriesDAO staffInterfaceQueriesDAO = f.getStaffInterfaceQueriesDAOInstance();
			IEntityDAO<TurnItInSubmission> tiiSubDao = f.getAdditionalDAOInstance(TurnItInSubmission.class);
			Collection<StaffSubmissionsQueryResult> submissions = staffInterfaceQueriesDAO.performStaffSubmissionsQuery(StaffSubmissionsQuerySortingType.SUBMISSION_TIME_DESCENDING, assignmentId);
			Collection<String> reqFilenames = assignmentDao.fetchRequiredFilenames(assignmentId);
			// example of TurnItInSubmission used to query
			TurnItInSubmission tiiSub = new TurnItInSubmission();
			
			// map of files that were already submitted
			HashMap<Long, String> tiiOIDMapping = new HashMap<Long, String>();
			
			// map of files of which originality reports have been generated
			HashMap<Long, String> tiiReportURLMapping = new HashMap<Long, String>();
			
			// map of files that are available to submit
			HashMap<Long, List<String>> subFiles = new HashMap<Long, List<String>>();
			for (StaffSubmissionsQueryResult sub : submissions) {
				Submission submission = sub.getSubmission();
				tiiSub.setSubmissionId(submission.getId());
				Collection<TurnItInSubmission> tiiSubs = tiiSubDao.findPersistentEntitiesByExample(tiiSub);
				if (tiiSubs.size() == 1) { // already sent to TurnItIn
					TurnItInSubmission tiiSubmission = tiiSubs.iterator().next(); 
					String oid = tiiSubmission.getObjectId();
					tiiOIDMapping.put(submission.getId(), tiiSubmission.getFilename());
					
					// calling TII service to see if score is available
					commResult = getOriginalityScore(staff, oid);
					if (commResult.isSuccessful() && commResult.getReturnCode() == GET_ORI_SCORE_SUCCESS_CODE_FCMD2) {
						// score is available
						tiiReportURLMapping.put(submission.getId(), TurnItInComm.getOriginalityReportURL(staff, oid));
					}
					else if (!commResult.isSuccessful() && commResult.getReturnCode() == GET_ORI_SCORE_ERROR_CODE_FCMD2_NOT_YET_AVAI) {
						// score is not yet available, do nothing
					}
					else {
						// something wrong, redirect to error page
						pageContext.performRedirect(pageContext.getPageUrl(StaffPageFactory.SITE_NAME, "tii_error?ec="+commResult.getReturnCode()+"&em="+urlEnc(commResult.getReturnMessage())));
						return;
					}
				}
				else if (tiiSubs.size() == 0) { // not yet sent
					List<String> availFilenames = new LinkedList<String>();
					InputStream resIS = f.getResourceDAOInstance().openInputStream(submission.getResourceId());
					ZipInputStream resZipIS = new ZipInputStream(resIS);
					ZipEntry ze = resZipIS.getNextEntry();
					while (ze != null) {
						String entryName = ze.getName();
						String filename = entryName.substring(entryName.lastIndexOf('/') + 1);
						if (reqFilenames.contains(filename)) {
							availFilenames.add(filename);
						}
						ze = resZipIS.getNextEntry();
					}
					subFiles.put(submission.getId(), availFilenames);
				}
			}
			templateContext.put("submitted", tiiOIDMapping);
			templateContext.put("toSubmit", subFiles);
			templateContext.put("submissions", submissions);
			templateContext.put("reportUrls", tiiReportURLMapping);
			f.endTransaction();
			pageContext.renderTemplate(template, templateContext);
		} catch (DAOException e) {
			f.abortTransaction();
			throw new ServletException("dao exception", e);
		}
	}

	public void handlePost(PageContext pageContext, Template template,
			VelocityContext templateContext) throws ServletException,
			IOException {
		throw new ServletException("Unexpected POST");
	}

}
