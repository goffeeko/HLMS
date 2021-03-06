package web.actions.general;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import module.ale.handheld.HandheldHandlerFactory;
import module.ale.handheld.handler.ClothCollectionHandler;
import module.dao.general.HistoryCloth;
import module.dao.general.Receipt;
import module.dao.general.Receipt.ReceiptStatus;
import module.dao.general.Receipt.ReceiptType;
import module.dao.iface.CustomCriteriaHandler;
import module.dao.master.Cloth;
import module.dao.master.Staff;
import module.dao.master.Cloth.ClothStatus;
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
import utils.convertor.DateConverter;
import utils.spring.SpringUtils;
import web.actions.BaseActionGeneral;
import web.actions.form.ClothTypeCounter;
import web.actions.form.HandheldReceiptCollectMainObject;
import web.actions.form.HandheldReceiptCollectSub1Object;
import web.actions.form.HandheldReceiptCollectSub2Object;

@Results({
//	@Result(name="checkPageMaster", type="json", params = {
//			"includeProperties" , 	"masterIsMe"
//	}),

	@Result(name="getClothInfoJson", type="json", params = {
			"includeProperties" , 	"clothList\\[\\d+\\]\\.code, " +
					"clothList\\[\\d+\\]\\.rfid, " +
					"clothList\\[\\d+\\]\\.clothStatus, " +
					"clothList\\[\\d+\\]\\.lastReceiptCode, " +
					"clothList\\[\\d+\\]\\.clothType\\.name, " + 
					"clothList\\[\\d+\\]\\.zone\\.code, " + 
					"clothList\\[\\d+\\]\\.staff\\.code, " + 
					"clothList\\[\\d+\\]\\.staff\\.nameCht, " +
					"clothList\\[\\d+\\]\\.staff\\.nameEng, " + 
					"clothList\\[\\d+\\]\\.staff\\.dept\\.nameCht, " +
					"clothList\\[\\d+\\]\\.staff\\.dept\\.nameEng, " +
					"receiptClothTotal, " + 
					"clothTypeCountList\\[\\d+\\]\\.type, " + 
					"clothTypeCountList\\[\\d+\\]\\.num"
	}),

	@Result(name="ajaxRemoveRfid", type="json", params = {
			"includeProperties" , 	"receiptClothTotal, " +
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
public class ClothCollectionHandheldAction extends BaseActionGeneral
{
	private static final long serialVersionUID = -2857645601833422842L;
	private static final Logger log4j = Logger.getLogger(ClothCollectionHandheldAction.class);
	
	// Session
	private static final String SESSION_KEY_CLOTHTYPE_COUNT_MAP = "SESSION_KEY_CLOTHTYPE_COUNT_MAP";
	
	private Integer receiptClothTotal;
	private List<String> rfidToBeRemovedList;
	private List<ClothTypeCounter> clothTypeCountList;
	private List<Cloth> clothList;
	private Receipt receipt;
	private Staff staffHandledBy;
	
	// Handheld Handler
	private ClothCollectionHandler clothCollectionHandler;
	
	// iReport Receipt Printing
	private static final String JASPER_RECEIPT_COLLECT_HH = "jasper_report/handheldReceiptCollect.jasper";
	private String subreportPath;		// absolute path of subreport file
	private List<HandheldReceiptCollectMainObject> mainReportList;
	private List<HandheldReceiptCollectSub1Object> subreport1List;
	private List<HandheldReceiptCollectSub2Object> subreport2List;
	
	public String getMainPage()
	{
//		if ( !isPageAvailable() )
//		{
//			this.setTilesKey("handheld.cloth.collection.page.in.use");
//			return TILES;
//		}
		
		//System.out.println( "Get Handheld Cloth Collection Page!" );
		this.resetSessionVariables();
		this.staffHandledBy = this.getClothCollectionHandler().getHandleByStaff();
		
		String receiptCode = this.getGeneralService().genKioskClothCollectionReceiptCode();
		receipt = new Receipt();
		receipt.setCode(receiptCode);
		
		
		// clothes read by Handheld but already "real-time-appended-to-table" before
		this.clothList = new ArrayList<Cloth>();
		this.clothList.addAll(this.getClothCollectionHandler().getMapRfidClothOld().values());
		this.receiptClothTotal = this.clothList.size();
		////////////////////////////////////////////////
		// Update clothTypeCountMap
		////////////////////////////////////////////////
		try
		{
			this.clothTypeCountList = this.updateClothTypeCountMap(this.clothList);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		this.setTilesKey("handheld.cloth.collection.main");
		return TILES;
	}
	
	
	public void validateCreate()
	{
//		if ( !isActionValid() )
//		{
//			addActionError(getText("errors.page.no.longer.valid"));
//		}
//		else
//		{
			// check handled-by-staff code
			if (this.staffHandledBy.getCode() == null || this.staffHandledBy.getCode().isEmpty())
			{
				addActionError(String.format(getText("errors.custom.required"), getText("staff.code.handled.by")));
			}
			else
			{
				this.staffHandledBy = this.getStaffByCode(this.staffHandledBy.getCode());
				if (this.staffHandledBy == null)
				{
					addActionError(String.format(getText("errors.custom.invalid"), getText("staff.code.handled.by")));
				}
			}
			
			// check cloth list
			Map<String, Cloth> mapRfidClothAll = this.getClothCollectionHandler().getMapRfidClothAll();
			if (mapRfidClothAll == null || mapRfidClothAll.size() == 0)
			{
				addActionError(getText("errors.no.cloth.found"));
			}
//		}
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
		Map<String, Cloth> mapRfidClothAll = this.getClothCollectionHandler().getMapRfidClothAll();
		HashSet<Cloth> clothSet = new HashSet<Cloth>(mapRfidClothAll.values());
		Iterator<Cloth> itCloth = clothSet.iterator();
		
		// HistoryCloth is used for ReceiptStatus change  
		HashSet<HistoryCloth> historyClothSet = new HashSet<HistoryCloth>();
		HistoryCloth historyCloth = null;
		
		while (itCloth.hasNext())
		{
			Cloth cloth = itCloth.next();
			cloth.setClothStatus(ClothStatus.Washing);
			cloth.setLastReceiptCode(receipt.getCode());
//			cloth.setModifiedBy(systemUser);		// this step is auto-done
			
			// add HistoryCloth
			historyCloth = new HistoryCloth();
			historyCloth.setRfid(cloth.getRfid());
			historyClothSet.add(historyCloth);
		}
		
		receipt.setReceiptClothTotal(mapRfidClothAll.size());
		receipt.setReceiptType(ReceiptType.Collect);
		receipt.setReceiptStatus(ReceiptStatus.Processing);
		receipt.setClothSet( clothSet );
		receipt.setStaffHandledBy(this.staffHandledBy);		// staffHandledBy obj is got in validation
		receipt.setStaffPickedBy(null);
		receipt.setHistoryClothSet(historyClothSet);
		
		//////////////////////////////////////////////
		// Save receipt and transaction
		//////////////////////////////////////////////
		this.getGeneralService().saveReceiptAndTransaction(receipt);
		this.resetReceipt();
	}
	
	private void printReceipt(Receipt receipt) throws Exception
	{
		/////////////////////////////////////////////
		// 1. Preparing receipt data for printing
		/////////////////////////////////////////////
		
		// iReport setting
		String reportFilePath = this.getRealPath() + JASPER_RECEIPT_COLLECT_HH;
		this.subreportPath = this.getRealPath() + JASPER_FOLDER;
		Map parameters = null;
		String printerName = this.getBeansFactoryApplication().getSystemPrinterA4();
		
		String handledByStaffCode = receipt.getStaffHandledBy().getCode();
		String handledByStaffName = receipt.getStaffHandledBy().getNameEng();
		String receiptCode = receipt.getCode();
		Calendar printDateTime = Calendar.getInstance();
		String receiptDate = DateConverter.format(printDateTime, DateConverter.DATE_FORMAT);
		String receiptTime = DateConverter.format(printDateTime, DateConverter.HOUR_MINUTE_FORMAT);
		Integer receiptClothTotal = receipt.getReceiptClothTotal();
		String receiptRemark = receipt.getRemark();
		
		// Get all cloths
		ArrayList<Cloth> receiptClothList = new ArrayList<Cloth>(receipt.getClothSet());
		
		// Count each clothType and Summarizing clothes by staff
		Cloth tmpCloth = null;
		String clothTypeName = null;
		Integer clothTypeTotal = null;
		Map<String, Integer> clothTypeCounter = new HashMap<String, Integer>();
		Long staffId = null;
		Map<Long, ArrayList<Cloth>> staffClothListMap = new HashMap<Long, ArrayList<Cloth>>();
		ArrayList<Cloth> staffClothList = null;
		for (int i = 0; i < receiptClothList.size(); i++)
		{
			tmpCloth = receiptClothList.get(i);
			
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
		this.subreport1List = new ArrayList<HandheldReceiptCollectSub1Object>();
		ArrayList<String> clothTypeNameList = new ArrayList<String>(clothTypeCounter.keySet());
		Collections.sort(clothTypeNameList);
		HandheldReceiptCollectSub1Object rs1o = null;
		
		for (int j = 0; j < clothTypeNameList.size(); j++)
		{
			clothTypeName = clothTypeNameList.get(j);
			clothTypeTotal = clothTypeCounter.get(clothTypeName);
			
			rs1o = new HandheldReceiptCollectSub1Object();
			rs1o.setClothType(clothTypeName);
			rs1o.setQty(clothTypeTotal);
			this.subreport1List.add(rs1o);
		}
		
		
		// 1.4. Fill sub-report-2 (staff's clothes)
		this.subreport2List = new ArrayList<HandheldReceiptCollectSub2Object>();
		HandheldReceiptCollectSub2Object rs2o = null;
		ArrayList<Long> staffIdList = new ArrayList<Long>(staffClothListMap.keySet());
		Map<String, Integer> staffClothTypeCounter = null;
		Staff staff = null;
		String staffCode = null;
		String staffNameCht = null;
		String staffNameEng = null;
		String staffDept = null;
		Integer staffClothTotal = null;
		String staffClothDetail = null;
		for (int j = 0; j < staffIdList.size(); j++)
		{
			staffId = staffIdList.get(j);
			staffClothList = staffClothListMap.get(staffId);
			
			tmpCloth = staffClothList.get(0);
			staff = tmpCloth.getStaff();
			staffCode = staff.getCode();
			staffNameCht = staff.getNameCht();
			staffNameEng = staff.getNameEng();
			staffDept = staff.getDept().getNameCht();
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
			
			rs2o = new HandheldReceiptCollectSub2Object();
			rs2o.setStaffDept(staffDept);
			rs2o.setStaffCode(staffCode);
			rs2o.setStaffNameCht(staffNameCht);
			rs2o.setStaffNameEng(staffNameEng);
			rs2o.setClothDetail(staffClothDetail);
			rs2o.setStaffTotal(staffClothTotal);
			this.subreport2List.add(rs2o);
		}
		
		/////////////////////////////////////////////
		// 2. Send to Printer
		/////////////////////////////////////////////
		parameters = new HashMap();
		parameters.put("subreportPath", this.subreportPath);
		parameters.put("receiptCode", receiptCode);
		parameters.put("receiptDate", receiptDate);
		parameters.put("receiptTime", receiptTime);
		parameters.put("receiptClothTotal", receiptClothTotal);
		parameters.put("handleByStaffCode", handledByStaffCode);
		parameters.put("handleByStaffName", handledByStaffName);
		parameters.put("receiptRemark", receiptRemark);
		
		JRBeanCollectionDataSource dataSrc1 = new JRBeanCollectionDataSource(this.subreport1List);
		parameters.put("subreport1List", dataSrc1);
		JRBeanCollectionDataSource dataSrc2 = new JRBeanCollectionDataSource(this.subreport2List);
		parameters.put("subreport2List", dataSrc2);
		
		// add a dummy report-main-obj to defeat JasperReport bug
		this.mainReportList = new ArrayList<HandheldReceiptCollectMainObject>();
		this.mainReportList.add(new HandheldReceiptCollectMainObject());
		
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
	
	
	public synchronized String getCapturedRfidJson()
	{
//		if ( isActionValid() )
//		{
			////////////////////////////////////////////////
			// For display
			////////////////////////////////////////////////
			this.clothList = new ArrayList<Cloth>();
			this.clothList.addAll(this.getClothCollectionHandler().getMapRfidClothNew().values());
			this.receiptClothTotal = this.getClothCollectionHandler().getMapRfidClothAll().size();
			
			////////////////////////////////////////////////
			// Update clothTypeCountMap
			////////////////////////////////////////////////
			try
			{
				this.clothTypeCountList = this.updateClothTypeCountMap(this.clothList);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			// [Important] clear the newly capture list and move data to old list
			this.getClothCollectionHandler().moveNewClothMapToOldClothMap();
			this.getClothCollectionHandler().clearMapRfidClothNew1();
//		}
//		else
//		{
//			// Empty List return through json to prevent js/jquery error
//			this.clothList = new ArrayList<Cloth>();
//			this.receiptClothTotal = 0;
//			this.clothTypeCountList = new ArrayList<ClothTypeCounter>();
//		}
		
		return "getClothInfoJson";
	}
	
	public synchronized String ajaxRemoveRfid()
	{
//		if ( isActionValid() )
//		{
			Map<String, Cloth> mapRfidClothAll = this.getClothCollectionHandler().getMapRfidClothAll();
			Map<String, Cloth> mapRfidClothOld = this.getClothCollectionHandler().getMapRfidClothOld();
			TreeMap<String, Integer> clothTypeCountMap = this.getSessionClothTypeCountMap();
			
			
			for (int i = 0; i < rfidToBeRemovedList.size(); i++)
			{
				String curRfid = rfidToBeRemovedList.get(i);
				Cloth curCloth = mapRfidClothAll.get(curRfid);
				
				// 1. Delete from mapRfidClothAll so it can be re-captured
				mapRfidClothAll.remove(curRfid);
				mapRfidClothOld.remove(curRfid);
				
				// 2. update the cloth-type-counter
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
				
				//System.out.println("RFID " + curRfid + " is removed!");
			}
			
			// 3. update the receipt-cloth-total
			this.receiptClothTotal = mapRfidClothAll.size();
			
			// 4. convert to list for displaying
			this.clothTypeCountList = this.convertClothTypeCountMapToList(clothTypeCountMap);
//		}
		
		return "ajaxRemoveRfid";
	}
	
	private List<ClothTypeCounter> updateClothTypeCountMap(List<Cloth> clothList) throws Exception
	{
		// 1. get the cloth-type-count-map from session
		TreeMap<String, Integer> clothTypeCountMap = this.getSessionClothTypeCountMap();
		
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
		
		// 3. convert the cloth-type-count-map into a list so it can be sent back to HTML by json
		List<ClothTypeCounter> typeCountList = this.convertClothTypeCountMapToList(clothTypeCountMap);
		return typeCountList;
	}
	
	private TreeMap<String, Integer> getSessionClothTypeCountMap()
	{
		TreeMap<String, Integer> clothTypeCountMap = (TreeMap<String, Integer>) getSession().get(SESSION_KEY_CLOTHTYPE_COUNT_MAP);
		if (clothTypeCountMap == null)
		{
			clothTypeCountMap = new TreeMap<String, Integer>();
			getSession().put(SESSION_KEY_CLOTHTYPE_COUNT_MAP, clothTypeCountMap);
		}
		
		return clothTypeCountMap;
	}
	
	private void clearSessionClothTypeCountMap()
	{
		getSession().put(SESSION_KEY_CLOTHTYPE_COUNT_MAP, null);
	}

	public void resetReceipt()
	{
		//System.out.println("Reset Receipt!");
		this.resetSessionVariables();
		this.getClothCollectionHandler().clearMapRfidClothAll();
		this.getClothCollectionHandler().clearMapRfidClothNew1();
		this.getClothCollectionHandler().clearMapRfidClothOld();
		this.getClothCollectionHandler().resetAllStaff();
	}
	
	
	
	
	
	
	
	///////////////////////////////////////////////////////////////
	// Start - Check page availability
	///////////////////////////////////////////////////////////////
	/*
	 * Background: 
	 * 		This page is disallowed to be accessed by multiple user/IP/Session simultaneously,
	 * 		this page need to be checked before entering. If there is already someone (IP/Session) 
	 * 		using this page, go to the confirm-page first. The confirm-page will ask whether 
	 * 		you want to continue, if yes, you will enter the page but normally the another user's 
	 * 		data input will all be removed; if no, stay at the confirm page
	 * 
	 *		See detail in /ProjectName/Doc Code for Copy/Page-In-Use.txt
	 */
//	private static String masterHostIp = null;	// decide which IP can use this page / action
//	private static String masterSession = null;	// decide which session can use this page / action
//	private Boolean takePageOwnership;			// client use this var to decide whether it will take the ownership of Ironing Page
//	private Boolean masterIsMe;				// client use this var to check if he the page master currently
//
//	protected void setPageToAvailable()
//	{
//		masterHostIp = null;
//		masterSession = null;
//	}
//
//	protected boolean isPageAvailable()
//	{
//		boolean isAvailable = true;
//		String clientIp = getServletRequest().getRemoteAddr();
//		String mySessionId = getServletRequest().getSession().getId();
//
//		if (masterHostIp == null)	// 沒有人正在使用這個Page
//		{
//			masterHostIp = clientIp;
//			masterSession = mySessionId;
//			//System.out.println("Master: " + clientIp);
//		}
//		else if (!masterHostIp.equals(clientIp) )	// 別人正在使用
//		{
//			if (this.takePageOwnership == null || this.takePageOwnership == false)
//			{
//				isAvailable = false;
//			}
//			else
//			{
//				//System.out.println( "Master Change: " + masterHostIp + " -> " + clientIp );
//				masterHostIp = clientIp;
//				masterSession = mySessionId;
//			}
//		}
//		else	// 自己正在使用
//		{
//			if (masterSession == null)
//			{
//				masterSession = mySessionId;
//			}
//			else if (!masterSession.equals(mySessionId))
//			{
//				if (this.takePageOwnership == null || this.takePageOwnership == false)
//				{
//					isAvailable = false;
//				}
//				else
//				{
//					//System.out.println( "MasterSession Change: " + masterSession + " -> " + mySessionId );
//					//					masterHostIp = clientIp;
//					masterSession = mySessionId;
//				}
//			}
//		}
//
//		return isAvailable;
//	}
//
//	protected boolean isActionValid()
//	{
//		boolean isValid = true;
//
//		String clientIp = getServletRequest().getRemoteAddr();
//		String mySessionId = getServletRequest().getSession().getId();
//
//		if (masterHostIp != null )
//		{
//			if ( !masterHostIp.equals(clientIp) )	// 已被別人搶去使用權
//			{
//				isValid = false;
//			}
//			else
//			{
//				// 自己正在使用中, 但被另一個自己的Session搶了使用權
//				if (masterSession != null && !masterSession.equals(mySessionId))
//				{
//					isValid = false;
//				}
//			}
//		}
//
//		return isValid;
//	}
//
//	// JSP will use this var
//	public Boolean getTakePageOwnership()
//	{
//		return takePageOwnership;
//	}
//	public void setTakePageOwnership(Boolean takePageOwnership)
//	{
//		this.takePageOwnership = takePageOwnership;
//	}
//	public String checkPageMaster()
//	{
//		this.masterIsMe = this.isActionValid();
//		return "checkPageMaster";
//	}
//	public Boolean getMasterIsMe()
//	{
//		return masterIsMe;
//	}
//	public void setMasterIsMe(Boolean masterIsMe)
//	{
//		this.masterIsMe = masterIsMe;
//	}
	///////////////////////////////////////////////////////////////
	// End - Check page availability
	///////////////////////////////////////////////////////////////
	
	
	
	
	
	
	
	
	
	
	
	
	
	////////////////////////////////////////////////
	// Utility Methods
	////////////////////////////////////////////////	

	// Manually clear Session Var
	private void resetSessionVariables()
	{
		getSession().put(SESSION_KEY_CLOTHTYPE_COUNT_MAP, null);
	}
	
	public ClothCollectionHandler getClothCollectionHandler()
	{
		if (clothCollectionHandler == null)
		{
			HandheldHandlerFactory factory = SpringUtils.getBean(HandheldHandlerFactory.BEANNAME);
			clothCollectionHandler = (ClothCollectionHandler) factory.getHandler(ClothCollectionHandler.EVENT_NAME);
		}
		
		return clothCollectionHandler;
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

	private Staff getStaffByCode(final String code)
	{
		Staff staff = null;
		List<Staff> staffList = getMasterService().findByExample(Staff.class, null, null, null, 
				new CustomCriteriaHandler<Staff>()
				{
					@Override
					public void makeCustomCriteria(Criteria baseCriteria)
					{
						baseCriteria.add(Restrictions.eq("code", code));
					}
				}, null, Order.asc("id"));
		
		if (staffList != null && staffList.size() == 1)
		{
			staff = staffList.get(0);
		}
		else
		{
			staff = null;
		}
		
		return staff;
	}
	
	////////////////////////////////////////////////
	// Getter and Setter
	////////////////////////////////////////////////
	public Integer getReceiptClothTotal()
	{
		return receiptClothTotal;
	}
	public void setReceiptClothTotal(Integer receiptClothTotal)
	{
		this.receiptClothTotal = receiptClothTotal;
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
	public List<Cloth> getClothList()
	{
		return clothList;
	}
	public void setClothList(List<Cloth> clothList)
	{
		this.clothList = clothList;
	}
	public Receipt getReceipt()
	{
		return receipt;
	}
	public void setReceipt(Receipt receipt)
	{
		this.receipt = receipt;
	}
	public Staff getStaffHandledBy()
	{
		return staffHandledBy;
	}
	public void setStaffHandledBy(Staff staffHandledBy)
	{
		this.staffHandledBy = staffHandledBy;
	}
}
