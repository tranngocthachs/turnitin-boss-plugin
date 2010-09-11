package boss.turnitin;

import static boss.turnitin.comm.TIICommResult.OBJECT_ID;
import static boss.turnitin.comm.TurnItInComm.quickSubmitAPaper;
import static boss.turnitin.comm.TurnitinAPI.urlEnc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import boss.plugins.spi.pages.IStaffPluginPage;
import boss.turnitin.comm.MalformedTIIResponseException;
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
import uk.ac.warwick.dcs.boss.model.testing.impl.TemporaryDirectory;

public class TurnItInPerformSubmitPage extends IStaffPluginPage {

	@Override
	public String getPageName() {
		return "perform_turnitin_submit";
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

		// Get search targets
		boolean internetCheck = false;
		boolean paperCheck = false;
		boolean journalCheck = false;

		String internetCheckStr = pageContext.getParameter("internet_check");
		if (internetCheckStr != null && internetCheckStr.equals("1"))
			internetCheck = true;

		String paperCheckStr = pageContext.getParameter("s_paper_check");
		if (paperCheckStr != null && paperCheckStr.equals("1"))
			paperCheck = true;

		String journalCheckStr = pageContext.getParameter("journal_check");
		if (journalCheckStr != null && journalCheckStr.equals("1"))
			journalCheck = true;

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
				if (pageContext.getParameter(String.valueOf(sub.getId())) != null) {
					String filename = pageContext.getParameter(String
							.valueOf(sub.getId()));
					File tempDir;
					try {
						tempDir = TemporaryDirectory.createTempDir(
								"tii_submit",
								pageContext.getTestingDir());
					} catch (IOException e) {
						throw new ServletException(
								"couldn't create temp dir", e);
					}
					File subTempFile = new File(tempDir,
							sub.getResourceSubdirectory() + ".zip");
					InputStream subIS = f.getResourceDAOInstance()
							.openInputStream(sub.getResourceId());
					OutputStream out = new FileOutputStream(subTempFile);
					IOUtils.copy(subIS, out);
					ZipFile subZipFile = new ZipFile(subTempFile);
					Enumeration<? extends ZipEntry> enumeration = subZipFile
							.entries();
					while (enumeration.hasMoreElements()) {
						ZipEntry entry = enumeration.nextElement();
						String entryName = entry.getName();
						String fname = entryName.substring(entryName
								.lastIndexOf('/') + 1);
						if (fname.equals(filename)) {
							File fileToSend = new File(tempDir, filename);
							IOUtils.copy(subZipFile.getInputStream(entry),
									new FileOutputStream(fileToSend));

							// sending the file to TurnItIn
							TIICommResult result;
							try {
								result = quickSubmitAPaper(staff,
										submission.getPerson(), fileToSend,
										assignment.getName(), internetCheck,
										paperCheck, journalCheck);
							} catch (MalformedTIIResponseException e) {
								pageContext
								.performRedirect(pageContext
										.getPageUrl(
												StaffPageFactory.SITE_NAME,
												"tii_error?em="
														+ urlEnc("Unrecognised TurnItIn's returned message. "
																+ "Make sure the URL of TurnItIn API is correct.")));
								f.abortTransaction();
								return;
							}
							if (!result.isSuccessful()) {
								pageContext.performRedirect(pageContext
										.getPageUrl(StaffPageFactory.SITE_NAME,
												"tii_error")
										+ "?ec="
										+ result.getReturnCode()
										+ "&em="
										+ urlEnc(result.getReturnMessage()));
								f.abortTransaction();
								return;
							}

							// successfully send a file to TurnItIn
							String oid = result.getSingleValue(OBJECT_ID);
							if (oid != null) {
								oid = oid.trim();
							}

							// persit the change to db
							TurnItInSubmission entity = new TurnItInSubmission();
							entity.setSubmissionId(sub.getId());
							entity.setObjectId(oid);
							entity.setFilename(filename);
							tiiSubDao.createPersistentCopy(entity);

							// done
							break;
						}
					} // end while
					FileUtils.deleteDirectory(tempDir);
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
