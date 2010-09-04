package boss.turnitin;

import static boss.turnitin.comm.TurnItInComm.deleteAPaper;
import static boss.turnitin.comm.TurnitinAPI.urlEnc;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import boss.plugins.spi.pages.IStaffPluginPage;
import boss.turnitin.comm.TIICommResult;

import uk.ac.warwick.dcs.boss.frontend.PageContext;
import uk.ac.warwick.dcs.boss.frontend.sites.StaffPageFactory;
import uk.ac.warwick.dcs.boss.model.FactoryException;
import uk.ac.warwick.dcs.boss.model.FactoryRegistrar;
import uk.ac.warwick.dcs.boss.model.dao.DAOException;
import uk.ac.warwick.dcs.boss.model.dao.DAOFactory;
import uk.ac.warwick.dcs.boss.model.dao.IAssignmentDAO;
import uk.ac.warwick.dcs.boss.model.dao.IDAOSession;
import uk.ac.warwick.dcs.boss.model.dao.IEntityDAO;
import uk.ac.warwick.dcs.boss.model.dao.IStaffInterfaceQueriesDAO;
import uk.ac.warwick.dcs.boss.model.dao.IStaffInterfaceQueriesDAO.StaffSubmissionsQuerySortingType;
import uk.ac.warwick.dcs.boss.model.dao.beans.Assignment;
import uk.ac.warwick.dcs.boss.model.dao.beans.Person;
import uk.ac.warwick.dcs.boss.model.dao.beans.Submission;
import uk.ac.warwick.dcs.boss.model.dao.beans.queries.StaffSubmissionsQueryResult;

public class TurnItInPerformDeletePage extends IStaffPluginPage {

	@Override
	public String getPageName() {
		return "perform_turnitin_delete";
	}

	@Override
	public String getPageTemplate() {
		return "multi_edited";
	}

	@Override
	public void handleGet(PageContext pageContext, Template template,
			VelocityContext templateContext) throws ServletException,
			IOException {
		throw new ServletException("Unexpected GET request");
	}

	@Override
	public void handlePost(PageContext pageContext, Template template,
			VelocityContext templateContext) throws ServletException,
			IOException {
		IDAOSession f;
		try {
			DAOFactory df = (DAOFactory) FactoryRegistrar
					.getFactory(DAOFactory.class);
			f = df.getInstance();
		} catch (FactoryException e) {
			throw new ServletException("dao init error", e);
		}

		// Get assignmentId
		String assignmentString = pageContext.getParameter("assignment");
		if (assignmentString == null) {
			throw new ServletException("No assignment parameter given");
		}
		Long assignmentId = Long.valueOf(assignmentString);

		// Render page
		try {
			f.beginTransaction();

			IStaffInterfaceQueriesDAO staffInterfaceQueriesDao = f
					.getStaffInterfaceQueriesDAOInstance();
			IAssignmentDAO assignmentDao = f.getAssignmentDAOInstance();
			Assignment assignment = assignmentDao
					.retrievePersistentEntity(assignmentId);

			if (!staffInterfaceQueriesDao.isStaffModuleAccessAllowed(
					pageContext.getSession().getPersonBinding().getId(),
					assignment.getModuleId())) {
				f.abortTransaction();
				throw new ServletException("permission denied");
			}

			Person staff = pageContext.getSession().getPersonBinding();

			templateContext.put("greet", pageContext.getSession()
					.getPersonBinding().getChosenName());
			IStaffInterfaceQueriesDAO staffInterfaceQueriesDAO = f
					.getStaffInterfaceQueriesDAOInstance();
			IEntityDAO<TurnItInSubmission> tiiSubDao = f
					.getPluginDAOInstance(TurnItInSubmission.class);
			Collection<StaffSubmissionsQueryResult> submissions = staffInterfaceQueriesDAO
					.performStaffSubmissionsQuery(
							StaffSubmissionsQuerySortingType.SUBMISSION_TIME_DESCENDING,
							assignmentId);
			for (StaffSubmissionsQueryResult submission : submissions) {
				Submission sub = submission.getSubmission();
				String val = pageContext.getParameter(String.valueOf(sub
						.getId()));
				if (val != null && val.equals("1")) {
					// query the oid
					TurnItInSubmission ex = new TurnItInSubmission();
					ex.setSubmissionId(sub.getId());
					Collection<TurnItInSubmission> tiiSubs = tiiSubDao.findPersistentEntitiesByExample(ex);
					if (tiiSubs.size() == 1) {
						TurnItInSubmission tiiSub = tiiSubs.iterator().next();
						
						// delete the file on Turn
						TIICommResult result = deleteAPaper(staff, tiiSub.getObjectId());
						if (!result.isSuccessful()) {
							pageContext.performRedirect(pageContext.getPageUrl(
									StaffPageFactory.SITE_NAME, "tii_error")
									+ "?ec="
									+ result.getReturnCode()
									+ "&em="
									+ urlEnc(result.getReturnMessage()));
							return;
						}

						// successfully delete the file on TurnItIn
						// persit the change to db
						tiiSubDao.deletePersistentEntity(tiiSub.getId());
					}
				}
			}

			templateContext.put("success", true);
			templateContext.put("nextPage", pageContext.getPageUrl(
					StaffPageFactory.SITE_NAME, "tii_submissions"));
			templateContext.put("nextPageParamName", "assignment");
			templateContext.put("nextPageParamValue", assignmentId);
			pageContext.renderTemplate(template, templateContext);
			f.endTransaction();
		} catch (DAOException e) {
			f.abortTransaction();
			throw new ServletException("dao exception", e);
		}
	}
}
