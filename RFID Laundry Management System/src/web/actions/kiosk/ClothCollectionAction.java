package web.actions.kiosk;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TreeMap;
import module.ale.handler.Kiosk1ClothCollectionHandler;
import module.ale.handler.Kiosk2ClothCollectionHandler;
import module.dao.general.HistoryCloth;
import module.dao.general.Receipt;
import module.dao.general.Receipt.ReceiptStatus;
import module.dao.general.Receipt.ReceiptType;
import module.dao.iface.CustomCriteriaHandler;
import module.dao.master.Cloth;
import module.dao.master.Department;
import module.dao.master.Staff;
import module.dao.master.Cloth.ClothStatus;
import module.dao.system.Users;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.log4j.Logger;
import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.InterceptorRefs;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.security.core.userdetails.UserDetails;
import utils.convertor.DateConverter;
import utils.spring.SpringUtils;
import web.actions.BaseActionKiosk;
import web.actions.form.ClothTypeCounter;
import web.actions.form.ReceiptCollectMainObject;
import web.actions.form.ReceiptCollectSub1Object;
import web.actions.form.ReceiptCollectSub2Object;

@Results({
	
	@Result(name="getCapturedRfidJson", type="json", params={
			"includeProperties" , 	"clothList\\[\\d+\\]\\.code, " +
									"clothList\\[\\d+\\]\\.rfid, " +
									"clothList\\[\\d+\\]\\.clothType\\.name, " + 
									"clothList\\[\\d+\\]\\.staff\\.code, " + 
									"clothList\\[\\d+\\]\\.staff\\.nameCht, " +
									"clothList\\[\\d+\\]\\.staff\\.nameEng, " + 
									"clothList\\[\\d+\\]\\.staff\\.dept\\.nameCht, " +
									"clothList\\[\\d+\\]\\.staff\\.dept\\.nameEng, " +
									"clothTotal, " +  
									"clothTypeCountList\\[\\d+\\]\\.type, " + 
									"clothTypeCountList\\[\\d+\\]\\.num"
	}),
	
	@Result(name="ajaxRemoveRfid", type="json", params={
			"includeProperties" , 	"clothTotal, " + 
									"clothTypeCountList\\[\\d+\\]\\.type, " + 
									"clothTypeCountList\\[\\d+\\]\\.num"
	}),
})
@InterceptorRefs({
	@InterceptorRef("prefixStack"),
	@InterceptorRef(value="validation",params={"includeMethods", "create"}),
	@InterceptorRef("postStack")
})
@ParentPackage("struts-action-default")
public class ClothCollectionAction extends BaseActionKiosk
{
	private static final long serialVersionUID = 2542995411109619880L;
	private static final Logger log4j = Logger.getLogger(ClothCollectionAction.class);
	
	// Session
	private static final String SESSION_KEY_CLOTHTYPE_COUNT_MAP = "_SESSION_KEY_CLOTHTYPE_COUNT_MAP";
	
	private List<Cloth> clothList;
	private Cloth cloth;
	private Integer clothTotal;
	private List<String> rfidToBeRemovedList;
	private List<ClothTypeCounter> clothTypeCountList;
	
	private Receipt receipt;
	private Staff staff;
	
	// ECSpec Handler
	Kiosk1ClothCollectionHandler k1Handler;
	Kiosk2ClothCollectionHandler k2Handler;
	private String kioskName;	// need this because of the menu links
	
	// iReport Receipt Printing
	private static final String JASPER_RECEIPT_COLLECT = "jasper_report/receiptCollect.jasper";
	private String subreportPath;		// absolute path of subreport file
	private List<ReceiptCollectMainObject> mainReportList;
	private List<ReceiptCollectSub1Object> subreport1List;
	private List<ReceiptCollectSub2Object> subreport2List;
	
	public String getMainPage()
	{
		this.kioskName = this.getServletRequest().getParameter(KIOSK_NAME);	// get Kiosk Name from URL
		//System.out.println( kioskName + ": get Cloth-Collection Page!" );
		
		this.setupKioskMainPage(this.kioskName);
		this.setTilesKey("cloth-collection.main");
		
		return TILES;
	}
	
	private void setupKioskMainPage(String kioskName)
	{
		this.resetSessionVariables(kioskName);
		this.stopCapture();
		
		if (kioskName.equals(KioskName.kiosk1.toString()))
		{
			this.getKiosk1ClothCollectionHandler().clearMapRfidClothAll();
			this.getKiosk1ClothCollectionHandler().clearMapRfidClothNew();
		}
		else if (kioskName.equals(KioskName.kiosk2.toString()))
		{
			this.getKiosk2ClothCollectionHandler().clearMapRfidClothAll();
			this.getKiosk2ClothCollectionHandler().clearMapRfidClothNew();
		}
		else
		{
			String errorMsg = "[Error] Invalid Kiosk name!";
			System.out.println(errorMsg);
			log4j.error(errorMsg);
		}
		
		String receiptCode = this.getGeneralService().genKioskClothCollectionReceiptCode();
		receipt = new Receipt();
		receipt.setCode(receiptCode);
		
		staff = new Staff();
		staff.setCardNumber("00099999");
		
		clothTotal = 0;
	}
	
	public void validateCreate()
	{
		//System.out.println(kioskName + ": validating receipt ...");
		
		// check staff info
		if (staff.getCode() != null && !staff.getCode().isEmpty())
		{
			List<Staff> staffList = getMasterService().findByExample(Staff.class, new Staff(), null, null, 
					new CustomCriteriaHandler<Staff>()
					{
						@Override
						public void makeCustomCriteria(Criteria baseCriteria)
						{
							baseCriteria.add(Restrictions.eq("code", staff.getCode()));
						}
					}, null, Order.asc("id"));
			
			if (staffList == null || staffList.size() == 0)
			{
				addActionError(String.format(getText("errors.custom.invalid"), getText("staff.code")));
			}
			else
			{
				this.staff = staffList.get(0);
			}
		}
		else if (staff.getCardNumber() != null && !staff.getCardNumber().isEmpty())
		{
			final String cardId = this.computeCardId(staff.getCardNumber());
			
			List<Staff> staffList = getMasterService().findByExample(Staff.class, new Staff(), null, null, 
					new CustomCriteriaHandler<Staff>()
					{
						@Override
						public void makeCustomCriteria(Criteria baseCriteria)
						{
							baseCriteria.add(Restrictions.eq("cardNumber", cardId ));
						}
					}, null, Order.asc("id"));
			
			if (staffList == null || staffList.size() == 0)
			{
				addActionError(String.format(getText("errors.custom.invalid"), getText("staff.card")));
			}
			else
			{
				this.staff = staffList.get(0);
			}
		}
		else
		{
			addActionError(String.format(getText("errors.custom.required.or"), getText("staff.code"), getText("staff.card")));
		}
		
		// check cloth list
		if (kioskName.equals(KioskName.kiosk1.toString()))
		{
			Map<String, Cloth> mapRfidClothAll = this.getKiosk1ClothCollectionHandler().getMapRfidClothAll();
			if (mapRfidClothAll == null || mapRfidClothAll.size() == 0)
			{
				addActionError(getText("errors.no.cloth.found"));
			}
		}
		else if (kioskName.equals(KioskName.kiosk2.toString()))
		{
			Map<String, Cloth> mapRfidClothAll = this.getKiosk2ClothCollectionHandler().getMapRfidClothAll();
			if (mapRfidClothAll == null || mapRfidClothAll.size() == 0)
			{
				addActionError(getText("errors.no.cloth.found"));
			}
		}
		else
		{
			String errorMsg = "[Error] Invalid Kiosk name!";
			System.out.println(errorMsg);
			log4j.error(errorMsg);
		}
	}
	
	public String create()
	{	  
		try
		{
			// 1. Save receipt and transaction
			createImpl();
			
			// 2. Print the receipt
			this.printReceipt(this.receipt);
			
			addActionMessage( getText( SuccessMessage_SaveSuccess ) );
			log4j.info( getText( SuccessMessage_SaveSuccess ) );
		}
		catch (Exception e)
		{
			log4j.error( e );
			
			while ( true )
			{
				Exception cause = (Exception)e.getCause();
				if ( cause == null )
				{
					addActionError( getText (ErrorMessage_SaveFail, new String[]{e.getMessage()} ) );
					e.printStackTrace();
					log4j.error( getText( ErrorMessage_SaveFail, new String[]{e.getMessage()} ) );
					break;
				}
				else
				{
					e = cause;
				}
			}
		}
		
		return "jsonValidateResult";
	}
	
	public void createImpl() throws Exception
	{
		// this is done in validation
//		this.staff = getMasterService().findByExample(...
		
		UserDetails userDetails = getSystemService().loadUserByUsername(BaseActionKiosk.KioskUserName);
		Users kioskUser = (Users) userDetails;
		
		Map<String, Cloth> mapRfidClothAll = null;
		if (kioskName.equals(KioskName.kiosk1.toString()))
		{
			mapRfidClothAll = this.getKiosk1ClothCollectionHandler().getMapRfidClothAll();
		}
		else if (kioskName.equals(KioskName.kiosk2.toString()))
		{
			mapRfidClothAll = this.getKiosk2ClothCollectionHandler().getMapRfidClothAll();
		}
		else
		{
			String errorMsg = "[Error] Invalid Kiosk name!";
			System.out.println(errorMsg);
			log4j.error(errorMsg);
		}
		
		// HistoryCloth is used for ReceiptStatus change  
		HashSet<HistoryCloth> historyClothSet = new HashSet<HistoryCloth>();
		HistoryCloth historyCloth = null;
		
		HashSet<Cloth> clothSet = new HashSet<Cloth>(mapRfidClothAll.values());
		Iterator<Cloth> itCloth = clothSet.iterator();
		while (itCloth.hasNext())
		{
			Cloth cloth = itCloth.next();
			cloth.setClothStatus(ClothStatus.Washing);
			cloth.setLastReceiptCode(receipt.getCode());
			cloth.setModifiedBy(kioskUser);
			
			// add HistoryCloth
			historyCloth = new HistoryCloth();
			historyCloth.setRfid(cloth.getRfid());
			historyCloth.setCreatedBy(kioskUser);
			historyClothSet.add(historyCloth);
		}
		
		receipt.setReceiptClothTotal(mapRfidClothAll.size());
		receipt.setReceiptType(ReceiptType.Collect);
		receipt.setReceiptStatus(ReceiptStatus.Processing);
		receipt.setClothSet( clothSet );
		receipt.setHistoryClothSet(historyClothSet);
		receipt.setStaffHandledBy(staff);
		receipt.setCreatedBy(kioskUser);
		
		
		//////////////////////////////////////////////
		// Save receipt and transaction
		//////////////////////////////////////////////
		this.getGeneralService().saveReceiptAndTransaction(receipt);
		this.resetSessionVariables(kioskName);
	}
	
	private void printReceipt(Receipt receipt) throws Exception
	{
		/////////////////////////////////////////////
		// 1. Preparing receipt data for printing
		/////////////////////////////////////////////
		
		// iReport setting
		String reportFilePath = this.getRealPath() + JASPER_RECEIPT_COLLECT;
		this.subreportPath = this.getRealPath() + JASPER_FOLDER;
		Map parameters = null;
		
		String printerName = null;
		if (kioskName.equals(KioskName.kiosk1.toString()))
		{
			printerName = this.getBeansFactoryApplication().getKiosk1Printer();
		}
		else if (kioskName.equals(KioskName.kiosk2.toString()))
		{
			printerName = this.getBeansFactoryApplication().getKiosk2Printer();
		}
		else
		{
			String errorMsg = "[Error] Invalid Kiosk Name so no printer selected!";
			System.out.println(errorMsg);
			log4j.error(errorMsg);
		}
		
		String receiptCode = receipt.getCode();
		Calendar printDateTime = Calendar.getInstance();
		String receiptDate = DateConverter.format(printDateTime, DateConverter.DATE_FORMAT);
		String receiptTime = DateConverter.format(printDateTime, DateConverter.HOUR_MINUTE_FORMAT);
		String staffCodeHandledBy = receipt.getStaffHandledBy().getCode();
		String staffNameHandledBy = receipt.getStaffHandledBy().getNameEng();
		String receiptRemark = receipt.getRemark();
		
		// 1.1. Grouping clothes by cloth's staff's dept
		Iterator<Cloth> itCloth = receipt.getClothSet().iterator();
		Map<Long, List<Cloth>> deptClothMap = new HashMap<Long, List<Cloth>>();
		Long deptId = null;
		List<Cloth> clothList = null;
		Cloth tmpCloth = null;
		while (itCloth.hasNext())
		{
			tmpCloth = itCloth.next();
			deptId = tmpCloth.getStaff().getDept().getId();
			
			if (deptClothMap.containsKey(deptId))
			{
				clothList = deptClothMap.get(deptId);
				clothList.add(tmpCloth);
			}
			else
			{
				clothList = new ArrayList<Cloth>();
				clothList.add(tmpCloth);
				deptClothMap.put(deptId, clothList);
			}
		}
		
		// 1.2. For each dept, Summarizing clothes by clothType and summarizing clothes by staff
		Iterator<Long> itDeptId = deptClothMap.keySet().iterator();
		ArrayList<Cloth> staffClothList = null;
		ArrayList<Long> staffIdList = null;
		ArrayList<String> clothTypeNameList = null;
		Department dept = null;
		String deptName = null;
		String clothTypeName = null;
		Integer deptClothTotal = null;
		Integer clothTypeTotal = null;
		Integer staffClothTotal = null;
		Staff staff = null;
		Integer qty = null;
		Map<String, Integer> clothTypeCounter = null;
		Map<Long, ArrayList<Cloth>> staffClothListMap = null;
		Map<String, Integer> staffClothTypeCounter = null;
		Long staffId = null;
		String staffCode = null;
		String staffNameCht = null;
		String staffNameEng = null;
		String staffClothDetail = null;
		ReceiptCollectSub1Object rcs1o = null;
		ReceiptCollectSub2Object rcs2o = null;
		
		while (itDeptId.hasNext())
		{
			deptId = itDeptId.next();
			dept = getMasterService().get(Department.class, deptId);
			deptName = dept.getNameCht() + "\n" + dept.getNameEng();
			
			clothList = deptClothMap.get(deptId);
			deptClothTotal = clothList.size();
			
			clothTypeCounter = new HashMap<String, Integer>();
			staffClothListMap = new HashMap<Long, ArrayList<Cloth>>();
			
			for (int i = 0; i < clothList.size(); i++)
			{
				tmpCloth = clothList.get(i);
				
				// Summarizing clothes by clothType (count each clothType)
				clothTypeName = tmpCloth.getClothType().getName();
				if (clothTypeCounter.containsKey(clothTypeName))
				{
					clothTypeTotal = clothTypeCounter.get(clothTypeName);
					clothTypeTotal++;
					clothTypeCounter.put(clothTypeName, clothTypeTotal);
				}
				else
				{
					clothTypeCounter.put(clothTypeName, 1);
				}
				
				// Summarizing clothes by staff
				staffId = tmpCloth.getStaff().getId();
				if (staffClothListMap.containsKey(staffId))
				{
					staffClothList = staffClothListMap.get(staffId);
					staffClothList.add(tmpCloth);
				}
				else
				{
					staffClothList = new ArrayList<Cloth>();
					staffClothList.add(tmpCloth);
					staffClothListMap.put(staffId, staffClothList);
				}
			}
			
			// 1.3. Fill sub-report-1 (clothType summary)
			this.subreport1List = new ArrayList<ReceiptCollectSub1Object>();
			clothTypeNameList = new ArrayList<String>(clothTypeCounter.keySet());
			Collections.sort(clothTypeNameList);
			
			
			for (int j = 0; j < clothTypeNameList.size(); j++)
			{
				clothTypeName = clothTypeNameList.get(j);
				qty = clothTypeCounter.get(clothTypeName);
				
				rcs1o = new ReceiptCollectSub1Object();
				rcs1o.setClothType(clothTypeName);
				rcs1o.setQty(qty);
				this.subreport1List.add(rcs1o);
			}
			
			// 1.4. Fill sub-report-2 (staff's clothes)
			this.subreport2List = new ArrayList<ReceiptCollectSub2Object>();
			
			staffIdList = new ArrayList<Long>(staffClothListMap.keySet());
			for (int j = 0; j < staffIdList.size(); j++)
			{
				staffId = staffIdList.get(j);
				staffClothList = staffClothListMap.get(staffId);
				
				tmpCloth = staffClothList.get(0);
				staff = tmpCloth.getStaff();
				staffCode = staff.getCode();
				staffNameCht = staff.getNameCht();
				staffNameEng = staff.getNameEng();
				staffClothTotal = staffClothList.size();
				
				// count clothType for this staff
				staffClothTypeCounter = new HashMap<String, Integer>();
				for (int k = 0; k < staffClothList.size(); k++)
				{
					tmpCloth = staffClothList.get(k);
					clothTypeName = tmpCloth.getClothType().getName();
					
					if (staffClothTypeCounter.containsKey(clothTypeName))
					{
						clothTypeTotal = staffClothTypeCounter.get(clothTypeName);
						clothTypeTotal++;
						staffClothTypeCounter.put(clothTypeName, clothTypeTotal);
					}
					else
					{
						staffClothTypeCounter.put(clothTypeName, 1);
					}
				}
				
				// Construct the cloth-detail-string for this staff
				clothTypeNameList = new ArrayList<String>(staffClothTypeCounter.keySet());
				Collections.sort(clothTypeNameList);
				staffClothDetail = "";
				for (int k = 0; k < clothTypeNameList.size(); k++)
				{
					clothTypeName = clothTypeNameList.get(k);
					clothTypeTotal = staffClothTypeCounter.get(clothTypeName);
					staffClothDetail += clothTypeName + " x " + clothTypeTotal + "\n";
				}
				
				rcs2o = new ReceiptCollectSub2Object();
				rcs2o.setStaffCode(staffCode);
				rcs2o.setStaffNameCht(staffNameCht);
				rcs2o.setStaffNameEng(staffNameEng);
				rcs2o.setClothDetail(staffClothDetail);
				rcs2o.setStaffTotal(staffClothTotal);
				this.subreport2List.add(rcs2o);
			}
			
			
			
			
			/////////////////////////////////////////////
			// 2. Send to Printer
			/////////////////////////////////////////////
			parameters = new HashMap();
			parameters.put("subreportPath", this.subreportPath);
			parameters.put("receiptCode", receiptCode);
			parameters.put("receiptDate", receiptDate);
			parameters.put("receiptTime", receiptTime);
			parameters.put("staffCode", staffCodeHandledBy);
			parameters.put("staffName", staffNameHandledBy);
			parameters.put("receiptRemark", receiptRemark);
			
			parameters.put("deptClothTotal", deptClothTotal);
			parameters.put("dept", deptName);
			
			JRBeanCollectionDataSource dataSrc1 = new JRBeanCollectionDataSource(this.subreport1List);
			parameters.put("subreport1List", dataSrc1);
			JRBeanCollectionDataSource dataSrc2 = new JRBeanCollectionDataSource(this.subreport2List);
			parameters.put("subreport2List", dataSrc2);
			
			
			// add a dummy report-main-obj to defeat JasperReport bug
			this.mainReportList = new ArrayList<ReceiptCollectMainObject>();
			this.mainReportList.add(new ReceiptCollectMainObject());
			
			try
			{
				printUnderWindowDriver(reportFilePath, parameters, this.mainReportList, 1, printerName);
			}
			catch (Exception e)
			{
				log4j.error( e );
				e.printStackTrace();
				System.out.println("[Error] Print failed!");
			}
		}
	}
	
	
	public void resetReceipt()
	{
		//System.out.println(kioskName + ": reset Receipt!");
		this.resetSessionVariables(kioskName);
		this.stopCapture();
		
		if (kioskName.equals(KioskName.kiosk1.toString()))
		{
			this.getKiosk1ClothCollectionHandler().clearMapRfidClothAll();
			this.getKiosk1ClothCollectionHandler().clearMapRfidClothNew();
		}
		else if (kioskName.equals(KioskName.kiosk2.toString()))
		{
			this.getKiosk2ClothCollectionHandler().clearMapRfidClothAll();
			this.getKiosk2ClothCollectionHandler().clearMapRfidClothNew();
		}
		else
		{
			String errorMsg = "[Error] Invalid Kiosk name!";
			System.out.println(errorMsg);
			log4j.error(errorMsg);
		}
		
		// no staff to reset (use fixed-reader)
	}
	
	
	public String startCapture()
	{
		//System.out.println(kioskName + ": start capturing RFID...");
		//Timer timer = new Timer();
		 // timer.schedule(this.create(), 1000, 2000);
		if (kioskName.equals(KioskName.kiosk1.toString()))
		{
			getKiosk1ClothCollectionHandler().startCapture();
		}
		else if (kioskName.equals(KioskName.kiosk2.toString()))
		{
			getKiosk2ClothCollectionHandler().startCapture();
		}
		else
		{
			String errorMsg = "[Error] Invalid Kiosk name!";
			System.out.println(errorMsg);
			log4j.error(errorMsg);
		}
		
		return null;
	}
	
	public String stopCapture()
	{
		//System.out.println(kioskName + ": stop capturing!");
		
		if (kioskName.equals(KioskName.kiosk1.toString()))
		{
			getKiosk1ClothCollectionHandler().stopCapture();
		}
		else if (kioskName.equals(KioskName.kiosk2.toString()))
		{
			getKiosk2ClothCollectionHandler().stopCapture();
		}
		else
		{
			String errorMsg = "[Error] Invalid Kiosk name!";
			System.out.println(errorMsg);
			log4j.error(errorMsg);
		}
		
		return null;
	}
	
	public String keepSessionAlive()
	{
		//System.out.println(this.kioskName + ": Collect-Page KeepAlive Msg recv!");
		return null;
	}
	
	
	public synchronized String getCapturedRfidJson()
	{
		this.clothList = new ArrayList<Cloth>();
		this.clothTotal = 0;
		if (kioskName.equals(KioskName.kiosk1.toString()))
		{
			this.clothList.addAll(this.getKiosk1ClothCollectionHandler().getMapRfidClothNew().values());
			this.clothTotal = this.getKiosk1ClothCollectionHandler().getMapRfidClothAll().size();
		}
		else if (kioskName.equals(KioskName.kiosk2.toString()))
		{
			this.clothList.addAll(this.getKiosk2ClothCollectionHandler().getMapRfidClothNew().values());
			this.clothTotal = this.getKiosk2ClothCollectionHandler().getMapRfidClothAll().size();
		}
		else
		{
			String errorMsg = "[Error] Invalid Kiosk name!";
			System.out.println(errorMsg);
			log4j.error(errorMsg);
		}
		
		
		try
		{
			 this.updateclothTypeCountMap(this.clothList);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		
		
		// clear the newly capture list is very important!
		if (kioskName.equals(KioskName.kiosk1.toString()))
		{
			this.getKiosk1ClothCollectionHandler().clearMapRfidClothNew();
		}
		else if (kioskName.equals(KioskName.kiosk2.toString()))
		{
			this.getKiosk2ClothCollectionHandler().clearMapRfidClothNew();
		}
		else
		{
			String errorMsg = "[Error] Invalid Kiosk name!";
			System.out.println(errorMsg);
			log4j.error(errorMsg);
		}
		
		return "getCapturedRfidJson";
	}
	
	public synchronized String ajaxRemoveRfid()
	{
		Map<String, Cloth> mapRfidClothAll = null;
		if (kioskName.equals(KioskName.kiosk1.toString()))
		{
			mapRfidClothAll = this.getKiosk1ClothCollectionHandler().getMapRfidClothAll();
		}
		else if (kioskName.equals(KioskName.kiosk2.toString()))
		{
			mapRfidClothAll = this.getKiosk2ClothCollectionHandler().getMapRfidClothAll();
		}
		else
		{
			mapRfidClothAll = new HashMap<String, Cloth>();
			String errorMsg = "[Error] Invalid Kiosk name!";
			System.out.println(errorMsg);
			log4j.error(errorMsg);
		}
		
		
		TreeMap<String, Integer> clothTypeCountMap = this.getSessionClothTypeCountMap(kioskName);
		
		for (int i = 0; i < rfidToBeRemovedList.size(); i++)
		{
			String curRfid = rfidToBeRemovedList.get(i);
			Cloth curCloth = mapRfidClothAll.get(curRfid);
			
			mapRfidClothAll.remove(curRfid);
			this.clothTotal = mapRfidClothAll.size();
			
			//System.out.println("RFID " + curRfid + " is removed!");
			
			
			// Very important to update the clothTypeCounter
			String curType = curCloth.getClothType().getName();
			Integer curCounter = clothTypeCountMap.get(curType);
			curCounter--;
			if (curCounter == 0)
			{
				clothTypeCountMap.remove(curType);
			}
			else
			{
				clothTypeCountMap.put(curType, curCounter);
			}
		}
		
		this.clothTypeCountList = this.convertClothTypeCountMapToList(clothTypeCountMap);

		return "ajaxRemoveRfid";
	}
	

	////////////////////////////////////////////////
	// Utility Method
	////////////////////////////////////////////////
	
	// Manually clear Session Var
	private void resetSessionVariables(String kioskName)
	{
		String sessionName = kioskName + SESSION_KEY_CLOTHTYPE_COUNT_MAP; 
		getSession().put(sessionName, null);
	}
	
	private Kiosk1ClothCollectionHandler getKiosk1ClothCollectionHandler()
	{
		if (k1Handler == null)
			k1Handler = SpringUtils.getBean(Kiosk1ClothCollectionHandler.BEANNAME);
		
		return k1Handler;
	}
	private Kiosk2ClothCollectionHandler getKiosk2ClothCollectionHandler()
	{
		if (k2Handler == null)
			k2Handler = SpringUtils.getBean(Kiosk2ClothCollectionHandler.BEANNAME);
		
		return k2Handler;
	}

	private TreeMap<String, Integer> getSessionClothTypeCountMap(String kioskName)
	{
		String sessionName = kioskName + SESSION_KEY_CLOTHTYPE_COUNT_MAP;
		TreeMap<String, Integer> clothTypeCountMap = (TreeMap<String, Integer>) getSession().get(sessionName);
		if (clothTypeCountMap == null)
		{
			clothTypeCountMap = new TreeMap<String, Integer>();
		}
		
		return clothTypeCountMap;
	}
	
	private void setSessionClothTypeCountMap(String kioskName, TreeMap<String, Integer> clothTypeCountMap)
	{
		String sessionName = kioskName + SESSION_KEY_CLOTHTYPE_COUNT_MAP; 
		getSession().put(sessionName, clothTypeCountMap);
	}
	
	private List<ClothTypeCounter> convertClothTypeCountMapToList(TreeMap<String, Integer> clothTypeCountMap)
	{
		ArrayList<Entry<String, Integer>> ctcList = new ArrayList<Entry<String,Integer>>(clothTypeCountMap.entrySet());
		ArrayList<ClothTypeCounter> resultList = new ArrayList<ClothTypeCounter>();
		
		for (int i = 0; i < ctcList.size(); i++)
		{
			Entry<String, Integer> e = ctcList.get(i);
			
			String type = e.getKey();
			Integer num = e.getValue();
			
			ClothTypeCounter typeCounter = new ClothTypeCounter();
			typeCounter.setType(type);
			typeCounter.setNum(num);
			
			resultList.add(typeCounter);
		}
		
		return resultList;
	}
	
	private void updateclothTypeCountMap(List<Cloth> clothList) throws Exception
	{
		// 1. get the cloth-type-count-map from session
		TreeMap<String, Integer> clothTypeCountMap = this.getSessionClothTypeCountMap(kioskName);
		
		// 2. examine the newly-captured-cloth-list and increment the counter of cloth type
		for (int i = 0; i < clothList.size(); i++)
		{
			String type = clothList.get(i).getClothType().getName();
			Integer counter = null;
			if (clothTypeCountMap.containsKey(type))
			{
				counter = clothTypeCountMap.get(type);
				counter++;
				clothTypeCountMap.put(type, counter);
			}
			else
			{
				clothTypeCountMap.put(type, 1);
			}
		}
		
		// 3. save the cloth-type-count-map to the session (MUST do to prevent error, said by Horace) 
		this.setSessionClothTypeCountMap(kioskName, clothTypeCountMap);
		
		// 4. convert the cloth-type-count-map into a list so it can be sent back to HTML by json
		this.clothTypeCountList = this.convertClothTypeCountMapToList(clothTypeCountMap);
	}
	
	///////////////////////////////////////////
	// Getter and Setter
	///////////////////////////////////////////
	public List<Cloth> getClothList()
	{
		return clothList;
	}
	public void setClothList(List<Cloth> clothList)
	{
		this.clothList = clothList;
	}
	public Cloth getCloth()
	{
		return cloth;
	}
	public void setCloth(Cloth cloth)
	{
		this.cloth = cloth;
	}
	public Receipt getReceipt()
	{
		return receipt;
	}
	public void setReceipt(Receipt receipt)
	{
		this.receipt = receipt;
	}
	public Staff getStaff()
	{
		return staff;
	}
	public void setStaff(Staff staff)
	{
		this.staff = staff;
	}
	public Integer getClothTotal()
	{
		return clothTotal;
	}
	public void setClothTotal(Integer clothTotal)
	{
		this.clothTotal = clothTotal;
	}
	public List<String> getRfidToBeRemovedList()
	{
		return rfidToBeRemovedList;
	}
	public void setRfidToBeRemovedList(List<String> rfidToBeRemovedList)
	{
		this.rfidToBeRemovedList = rfidToBeRemovedList;
	}
	public List<ClothTypeCounter> getClothTypeCountList()
	{
		return clothTypeCountList;
	}
	public void setClothTypeCountList(List<ClothTypeCounter> clothTypeCountList)
	{
		this.clothTypeCountList = clothTypeCountList;
	}
	public String getSubreportPath()
	{
		return subreportPath;
	}
	public void setSubreportPath(String subreportPath)
	{
		this.subreportPath = subreportPath;
	}
	public List<ReceiptCollectMainObject> getMainReportList()
	{
		return mainReportList;
	}
	public void setMainReportList(List<ReceiptCollectMainObject> mainReportList)
	{
		this.mainReportList = mainReportList;
	}
	public List<ReceiptCollectSub1Object> getSubreport1List()
	{
		return subreport1List;
	}
	public void setSubreport1List(List<ReceiptCollectSub1Object> subreport1List)
	{
		this.subreport1List = subreport1List;
	}
	public List<ReceiptCollectSub2Object> getSubreport2List()
	{
		return subreport2List;
	}
	public void setSubreport2List(List<ReceiptCollectSub2Object> subreport2List)
	{
		this.subreport2List = subreport2List;
	}
	public String getKioskName()
	{
		return kioskName;
	}
	public void setKioskName(String kioskName)
	{
		this.kioskName = kioskName;
	}
}
