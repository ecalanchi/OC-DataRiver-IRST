package org.akaza.openclinica.service.subject;

import org.akaza.openclinica.bean.core.Status;
import org.akaza.openclinica.bean.login.UserAccountBean;
import org.akaza.openclinica.bean.managestudy.StudyBean;
import org.akaza.openclinica.bean.managestudy.StudySubjectBean;
import org.akaza.openclinica.bean.managestudy.SubjectTransferBean;
import org.akaza.openclinica.bean.service.StudyParameterValueBean;
import org.akaza.openclinica.bean.submit.SubjectBean;
import org.akaza.openclinica.core.SessionManager;
import org.akaza.openclinica.dao.hibernate.datariver.StudySubjectCustomLabelDao;
import org.akaza.openclinica.dao.login.UserAccountDAO;
import org.akaza.openclinica.dao.managestudy.StudyDAO;
import org.akaza.openclinica.dao.managestudy.StudySubjectDAO;
import org.akaza.openclinica.dao.service.StudyParameterValueDAO;
import org.akaza.openclinica.dao.submit.SubjectDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

public class SubjectService implements SubjectServiceInterface {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());
    SubjectDAO subjectDao;
    StudyParameterValueDAO studyParameterValueDAO;
    StudySubjectDAO studySubjectDao;
    UserAccountDAO userAccountDao;
    StudyDAO studyDao;
    DataSource dataSource;
    StudySubjectCustomLabelDao studySubjectCustomLabelDao;
    
    public SubjectService(DataSource dataSource, StudySubjectCustomLabelDao studySubjectCustomLabelDao) {   
        this.dataSource = dataSource;
        this.studySubjectCustomLabelDao = studySubjectCustomLabelDao;
    }

    public SubjectService(SessionManager sessionManager) {
        this.dataSource = sessionManager.getDataSource();
    }

    public List<StudySubjectBean> getStudySubject(StudyBean study) {
        return getStudySubjectDao().findAllByStudy(study);

    }

    /*
     * (non-Javadoc)
     * @see org.akaza.openclinica.service.subject.SubjectServiceInterface#createSubject(org.akaza.openclinica.bean.submit.SubjectBean,
     * org.akaza.openclinica.bean.managestudy.StudyBean)
     */
    public String createSubject(SubjectBean subjectBean, StudyBean studyBean, Date enrollmentDate, String secondaryId) {
        if (subjectBean.getUniqueIdentifier() != null && subjectBean.getUniqueIdentifier().trim().length()> 0 && 
        		getSubjectDao().findByUniqueIdentifier(subjectBean.getUniqueIdentifier()).getId() != 0) {
        	//we need to keep the label to transfer it to the StudySubjectBean later
        	String label = subjectBean.getLabel();
        	subjectBean = getSubjectDao().findByUniqueIdentifier(subjectBean.getUniqueIdentifier());
        	subjectBean.setLabel(label);
        } else {
            subjectBean.setStatus(Status.AVAILABLE);
            subjectBean = getSubjectDao().create(subjectBean);
        }
        
        StudySubjectBean studySubject = createStudySubject(subjectBean, studyBean, enrollmentDate, secondaryId);
        getStudySubjectDao().createWithoutGroup(studySubject);
        return studySubject.getLabel();
    }

    private StudySubjectBean createStudySubject(SubjectBean subject, StudyBean studyBean, Date enrollmentDate, String secondaryId) {
        StudySubjectBean studySubject = new StudySubjectBean();
        studySubject.setSecondaryLabel(secondaryId);
        studySubject.setOwner(getUserAccount());
        studySubject.setEnrollmentDate(enrollmentDate);
        studySubject.setSubjectId(subject.getId());
        studySubject.setStudyId(studyBean.getId());
        studySubject.setStatus(Status.AVAILABLE);
        
        int handleStudyId = studyBean.getParentStudyId() > 0 ? studyBean.getParentStudyId() : studyBean.getId();
        StudyParameterValueBean subjectIdGenerationParameter = getStudyParameterValueDAO().findByHandleAndStudy(handleStudyId, "subjectIdGeneration");
        String idSetting = subjectIdGenerationParameter.getValue();
//        if (idSetting.equals("auto editable") || idSetting.equals("auto non-editable")) {
//        	// Warning: Here we have a race condition. 
//        	// At least, a uniqueness constraint should be set on the database! Better provide an atomic method which stores a new label in the database and returns it.  
//            int nextLabel = getStudySubjectDao().findTheGreatestLabel() + 1;
//            studySubject.setLabel(Integer.toString(nextLabel));
  
        if (idSetting.equals("auto editable") || idSetting.equals("auto non-editable")) {           

            /* CUSTOMIZED SUBJECT LABEL */ 
            //+DR added by DataRiver (EC) 11/03/2015
            int parentStudyId = studyBean.getParentStudyId() > 0 ? studyBean.getParentStudyId() : studyBean.getId();
        	if (studySubjectCustomLabelDao.getByStudyId(parentStudyId) != null){
        		//DataRiver custom label
            	String nextCode = "" + (studySubjectCustomLabelDao.getByStudyId(parentStudyId).getStudyLabelCounter() + 1);
            	//add as many zeros as needed to achieve the required fixed length (max 6 digits)
            	//set label_code_length to 0 in study_subject_custom_label table if you don't want a fixed length code
            	while (nextCode.length() < studySubjectCustomLabelDao.getByStudyId(parentStudyId).getLabelCodeLength() && nextCode.length() <= 6){
            		nextCode = "0" + nextCode;
            	} 
            	String prefix = studySubjectCustomLabelDao.getByStudyId(parentStudyId).getLabelPrefix() == null ? "" : parseStudySubjectlabel(studySubjectCustomLabelDao.getByStudyId(parentStudyId).getLabelPrefix(), studyBean);
            	String suffix = studySubjectCustomLabelDao.getByStudyId(parentStudyId).getLabelSuffix() == null ? "" : parseStudySubjectlabel(studySubjectCustomLabelDao.getByStudyId(parentStudyId).getLabelSuffix(), studyBean);
            	String nextLabel = trimCustomLabel(prefix, nextCode, suffix);
                studySubject.setLabel(nextLabel);
                studySubjectCustomLabelDao.incrementStudyLabelCounter(parentStudyId);
        	} else {
        		//OpenClinica default (integer) label
        		int nextLabel = getStudySubjectDao().findTheGreatestLabel() + 1;
        		studySubject.setLabel(Integer.toString(nextLabel));
        	}
        	
        	//// Warning: Here we have a race condition. 
        	//// At least, a uniqueness constraint should be set on the database! Better provide an atomic method which stores a new label in the database and returns it.  
        	//int nextLabel = getStudySubjectDao().findTheGreatestLabel() + 1;
        	//studySubject.setLabel(Integer.toString(nextLabel));

        	//+DR end added by DataRiver (EC) 11/03/2015        
            
            
        } else {
        	studySubject.setLabel(subject.getLabel());
        	subject.setLabel(null);
        }
        
        return studySubject;

    }

    public void validateSubjectTransfer(SubjectTransferBean subjectTransferBean) {
        // TODO: Validate here
    }

    /**
     * Getting the first user account from the database. This would be replaced by an authenticated user who is doing the SOAP requests .
     * 
     * @return UserAccountBean
     */
    private UserAccountBean getUserAccount() {

        UserAccountBean user = new UserAccountBean();
        user.setId(1);
        return user;
    }

    /**
     * @return the subjectDao
     */
    public SubjectDAO getSubjectDao() {
        subjectDao = subjectDao != null ? subjectDao : new SubjectDAO(dataSource);
        return subjectDao;
    }
    
    public StudyParameterValueDAO getStudyParameterValueDAO() {
        return this.studyParameterValueDAO != null ? studyParameterValueDAO : new StudyParameterValueDAO(dataSource);
    }

    /**
     * @return the subjectDao
     */
    public StudyDAO getStudyDao() {
        studyDao = studyDao != null ? studyDao : new StudyDAO(dataSource);
        return studyDao;
    }

    /**
     * @return the subjectDao
     */
    public StudySubjectDAO getStudySubjectDao() {
        studySubjectDao = studySubjectDao != null ? studySubjectDao : new StudySubjectDAO(dataSource);
        return studySubjectDao;
    }

    /**
     * @return the UserAccountDao
     */
    public UserAccountDAO getUserAccountDao() {
        userAccountDao = userAccountDao != null ? userAccountDao : new UserAccountDAO(dataSource);
        return userAccountDao;
    }

    /**
     * @return the datasource
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * @param datasource
     *            the datasource to set
     */
    public void setDatasource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    
    /**
     * Replace label with current study parameters
     * 
     * @author DataRiver (EC) 11/03/2015
     * @param rawLabel
     * @return label with updated parameters' value
     */
    public String parseStudySubjectlabel(String rawLabel, StudyBean currentStudy){
    	String s = rawLabel;
    	//parse {siteCode}
    	s = s.replaceAll("\\{siteCode\\}", currentStudy.getOid().split("_")[1]);
    	//parse {siteName}
    	s = s.replaceAll("\\{siteName\\}", currentStudy.getAbbreviatedName());
    	
    	s = s. replaceAll("\\s+","");
    	//prefix and suffix length must be limited to avoid that prefix is completely lost when trimCustomLabel is called
    	if (s.length() > 16) s = s.substring(0, 16);
    	
    	return s.toUpperCase();
    }
    
    /**
     * Merge parameters to create the new label and truncate prefix if label is longer than database field size of 30 chars 
     * @param prefix
     * @param code
     * @param suffix
     * @return nextLabel
     */
    public String trimCustomLabel(String prefix, String code, String suffix){
    	String nextLabel = "";
    	int labelLength = prefix.length() + code.length() + suffix.length();
    	if (labelLength > 30) {
    		nextLabel = prefix.substring(0, prefix.length() - (labelLength - 30)) + code + suffix;
    	} else {
    		nextLabel = prefix + code + suffix;
    	}
    	
    	return nextLabel;
    }

}