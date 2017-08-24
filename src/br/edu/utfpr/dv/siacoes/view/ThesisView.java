package br.edu.utfpr.dv.siacoes.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.pdfbox.multipdf.PDFMergerUtility;

import com.vaadin.event.EventRouter;
import com.vaadin.event.SelectionEvent;
import com.vaadin.event.SelectionEvent.SelectionListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.renderers.DateRenderer;

import br.edu.utfpr.dv.siacoes.Session;
import br.edu.utfpr.dv.siacoes.bo.CertificateBO;
import br.edu.utfpr.dv.siacoes.bo.JuryAppraiserBO;
import br.edu.utfpr.dv.siacoes.bo.JuryBO;
import br.edu.utfpr.dv.siacoes.bo.JuryStudentBO;
import br.edu.utfpr.dv.siacoes.bo.ThesisBO;
import br.edu.utfpr.dv.siacoes.components.SemesterComboBox;
import br.edu.utfpr.dv.siacoes.components.YearField;
import br.edu.utfpr.dv.siacoes.model.Jury;
import br.edu.utfpr.dv.siacoes.model.JuryAppraiser;
import br.edu.utfpr.dv.siacoes.model.StatementReport;
import br.edu.utfpr.dv.siacoes.model.Thesis;
import br.edu.utfpr.dv.siacoes.model.Module.SystemModule;
import br.edu.utfpr.dv.siacoes.model.User.UserProfile;
import br.edu.utfpr.dv.siacoes.util.ExtensionUtils;
import br.edu.utfpr.dv.siacoes.util.ReportUtils;
import br.edu.utfpr.dv.siacoes.window.EditJuryAppraiserFeedbackWindow;
import br.edu.utfpr.dv.siacoes.window.EditJuryWindow;
import br.edu.utfpr.dv.siacoes.window.EditThesisWindow;

public class ThesisView extends ListView {
	
	public static final String NAME = "thesis";
	
	private final SemesterComboBox comboSemester;
	private final YearField textYear;
	private final Button buttonJury;
	private final Button buttonDownloadThesis;
	private final Button buttonStatements;
	private final Button buttonSendFeedback;
	private final Button buttonSupervisorStatement;
	private final Button buttonCosupervisorStatement;
	
	private Button.ClickListener listenerClickDownload;
	
	public ThesisView(){
		super(SystemModule.SIGET);
		
		this.setProfilePerimissions(UserProfile.MANAGER);
		
		this.buttonDownloadThesis = new Button("Monografia");
		this.addActionButton(this.buttonDownloadThesis);
		
		this.buttonJury = new Button("Banca", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	juryClick();
            }
        });
		
		this.buttonSendFeedback = new Button("Feedback", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	sendFeedbackClick();
            }
        });
		
		this.buttonStatements = new Button("Declara��es", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	downloadStatements();
            }
        });
		
		this.buttonSupervisorStatement = new Button("Dec. Orienta��o", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	downloadSupervisorStatement();
            }
        });
		
		this.buttonCosupervisorStatement = new Button("Dec. Coorienta��o", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	downloadCosupervisorStatement();
            }
        });
		
		if(Session.isUserProfessor()){
			this.addActionButton(this.buttonSendFeedback);
		}
		if(Session.isUserManager(this.getModule())){
			this.addActionButton(this.buttonJury);	
			this.addActionButton(this.buttonStatements);
			this.addActionButton(this.buttonSupervisorStatement);
			this.addActionButton(this.buttonCosupervisorStatement);
		}
		
		this.setAddVisible(false);
		this.setEditVisible(false);
		this.setDeleteVisible(false);
		
		this.comboSemester = new SemesterComboBox();
		
		this.textYear = new YearField();
		
		this.addFilterField(new HorizontalLayout(this.comboSemester, this.textYear));
	}
	
	@Override
	protected void loadGrid() {
		this.getGrid().addColumn("Semestre", Integer.class);
		this.getGrid().addColumn("Ano", Integer.class);
		this.getGrid().addColumn("T�tulo", String.class);
		this.getGrid().addColumn("Acad�mico", String.class);
		this.getGrid().addColumn("Submiss�o", Date.class).setRenderer(new DateRenderer(new SimpleDateFormat("dd/MM/yyyy")));
		this.getGrid().addSelectionListener(new SelectionListener() {
			@Override
			public void select(SelectionEvent event) {
				prepareDownload();
			}
		});
		
		this.getGrid().getColumns().get(0).setWidth(100);
		this.getGrid().getColumns().get(1).setWidth(100);
		this.getGrid().getColumns().get(4).setWidth(125);
		
		this.prepareDownload();
		
		try {
			ThesisBO bo = new ThesisBO();
	    	List<Thesis> list = bo.listBySemester(Session.getUser().getDepartment().getIdDepartment(), this.comboSemester.getSemester(), this.textYear.getYear());
	    	
	    	for(Thesis p : list){
				Object itemId = this.getGrid().addRow(p.getSemester(), p.getYear(), p.getTitle(), p.getStudent().getName(), p.getSubmissionDate());
				this.addRowId(itemId, p.getIdThesis());
			}
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
			
			Notification.show("Listar Monografias", e.getMessage(), Notification.Type.ERROR_MESSAGE);
		}
	}
	
	private void downloadSupervisorStatement(){
		Object value = getIdSelected();
		
		if(value == null){
			Notification.show("Gerar Declara��o", "Selecione um registro para gerar a declara��o.", Notification.Type.WARNING_MESSAGE);
		}else{
			try{
				ThesisBO tbo = new ThesisBO();
				CertificateBO bo = new CertificateBO();
				
				Thesis thesis = tbo.findById((int)value);

				byte[] report = bo.getThesisProfessorStatement(thesis.getSupervisor(), thesis);
				
				Session.putReport(report);
				
				getUI().getPage().open("#!" + CertificateView.NAME + "/session/" + UUID.randomUUID().toString(), "_blank");
			}catch(Exception e){
				Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
	        	
	        	Notification.show("Gerar Declara��o", e.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
		}
	}
	
	private void downloadCosupervisorStatement(){
		Object value = getIdSelected();
		
		if(value == null){
			Notification.show("Gerar Declara��o", "Selecione um registro para gerar a declara��o.", Notification.Type.WARNING_MESSAGE);
		}else{
			try{
				ThesisBO tbo = new ThesisBO();
				CertificateBO bo = new CertificateBO();
				
				Thesis thesis = tbo.findById((int)value);

				if(thesis.getCosupervisor() == null){
					Notification.show("Gerar Declara��o", "N�o foi indicado um coorientador para a monografia.", Notification.Type.WARNING_MESSAGE);
				}else{
					byte[] report = bo.getThesisProfessorStatement(thesis.getCosupervisor(), thesis);
					
					Session.putReport(report);
					
					getUI().getPage().open("#!" + CertificateView.NAME + "/session/" + UUID.randomUUID().toString(), "_blank");
				}
			}catch(Exception e){
				Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
	        	
	        	Notification.show("Gerar Declara��o", e.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
		}
	}
	
	private void downloadStatements(){
		Object value = getIdSelected();
		
		if(value == null){
			Notification.show("Gerar Declara��es", "Selecione uma banca para gerar as declara��es.", Notification.Type.WARNING_MESSAGE);
		}else{
			try{
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				PDFMergerUtility pdfMerge = new PDFMergerUtility();
				pdfMerge.setDestinationStream(output);
				
				CertificateBO bo = new CertificateBO();

				byte[] reportProfessor = bo.getJuryProfessorStatementReportListByThesis((int)value);
				
				if(reportProfessor != null){
					pdfMerge.addSource(new ByteArrayInputStream(reportProfessor));
				}
				
				byte[] reportStudent = bo.getJuryStudentStatementReportListByThesis((int)value);
				
				if(reportStudent != null){
					pdfMerge.addSource(new ByteArrayInputStream(reportStudent));
				}
				
				if((reportProfessor != null) || (reportStudent != null)){
					pdfMerge.mergeDocuments(null);
					
					byte[] report = output.toByteArray();
					
					Session.putReport(report);
					
					getUI().getPage().open("#!" + CertificateView.NAME + "/session/" + UUID.randomUUID().toString(), "_blank");
				}else{
					Notification.show("Gerar Declara��es", "N�o h� declara��es para serem geradas.", Notification.Type.WARNING_MESSAGE);
				}
			}catch(Exception e){
				Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
	        	
	        	Notification.show("Gerar Declara��es", e.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
		}
	}
	
	private void prepareDownload(){
		Object value = getIdSelected();
		
		this.buttonDownloadThesis.removeClickListener(this.listenerClickDownload);
    	
    	if(value != null){
			try {
            	ThesisBO bo = new ThesisBO();
            	Thesis p = bo.findById((int)value);
            	
            	new ExtensionUtils().extendToDownload(p.getTitle() + p.getFileType().getExtension(), p.getFile(), this.buttonDownloadThesis);
        	} catch (Exception e) {
        		this.listenerClickDownload = new Button.ClickListener() {
		            @Override
		            public void buttonClick(ClickEvent event) {
		            	Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
		            	
		            	Notification.show("Download da Monografia", e.getMessage(), Notification.Type.ERROR_MESSAGE);
		            }
		        };
		        
        		this.buttonDownloadThesis.addClickListener(this.listenerClickDownload);
			}
    	}else{
    		new ExtensionUtils().removeAllExtensions(this.buttonDownloadThesis);
    		
    		this.listenerClickDownload = new Button.ClickListener() {
	            @Override
	            public void buttonClick(ClickEvent event) {
	            	Notification.show("Download da Monografia", "Selecione um registro para baixar a monografia.", Notification.Type.WARNING_MESSAGE);
	            }
	        };
    		
    		this.buttonDownloadThesis.addClickListener(this.listenerClickDownload);
    	}
	}

	private void juryClick(){
		Object id = getIdSelected();
    	
    	if(id == null){
    		Notification.show("Selecionar Registro", "Selecione o registro para marcar a banca.", Notification.Type.WARNING_MESSAGE);
    	}else{
    		try {
    			JuryBO bo = new JuryBO();
				Jury jury = bo.findByThesis((int)id);
				
				UI.getCurrent().addWindow(new EditJuryWindow(jury, this));
			} catch (Exception e) {
				Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
				
				Notification.show("Marcar Banca", e.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
    	}
	}
	
	private void sendFeedbackClick(){
		Object id = getIdSelected();
    	
    	if(id == null){
    		Notification.show("Selecionar Registro", "Selecione o registro para marcar a banca.", Notification.Type.WARNING_MESSAGE);
    	}else{
    		try {
    			JuryBO bo = new JuryBO();
				Jury jury = bo.findByThesis((int)id);
				
				JuryAppraiserBO bo2 = new JuryAppraiserBO();
    			
    			JuryAppraiser appraiser = bo2.findByAppraiser(jury.getIdJury(), Session.getUser().getIdUser());
    			
    			if(appraiser != null){
    				UI.getCurrent().addWindow(new EditJuryAppraiserFeedbackWindow(appraiser));
    			}else{
    				Notification.show("Enviar Feedback", "� necess�rio ser membro da banca para enviar o feedback.", Notification.Type.WARNING_MESSAGE);
    			}
			} catch (Exception e) {
				Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
				
				Notification.show("Enviar Feedback", e.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
    	}
	}
	
	@Override
	public void addClick() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void editClick(Object id) {
		try {
			ThesisBO bo = new ThesisBO();
			Thesis p = bo.findById((int)id);
			
			UI.getCurrent().addWindow(new EditThesisWindow(p, this));
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
			
			Notification.show("Editar Monografia", e.getMessage(), Notification.Type.ERROR_MESSAGE);
		}
	}

	@Override
	public void deleteClick(Object id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void filterClick() throws Exception {
		// TODO Auto-generated method stub
		
	}

}