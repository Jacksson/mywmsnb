/*
 * Copyright (c) 2011-2013 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.mobile.processes.picking;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.mywms.facade.FacadeException;
import org.mywms.model.Client;

import de.linogistix.los.model.State;
import de.linogistix.mobile.common.gui.bean.BasicDialogBean;
import de.linogistix.mobile.common.system.JSFHelper;
import de.linogistix.mobileserver.processes.picking.PickingMobileOrder;

/**
 * @author krane
 *
 */
public class PickingMobileBean extends BasicDialogBean {
	Logger log = Logger.getLogger(PickingMobileBean.class);


	// ***********************************************************************
	// Switches. Configured with system properties
	// ***********************************************************************
	
	protected Long selectedOrderId;
	protected List<SelectItem> orderSelectList;
	
	protected PickingMobileData data;
	protected List<String> serialList = null;
	
	protected String inputCode = "";
	protected String inputPrinter = "";
	protected BigDecimal amountTaken;
	protected BigDecimal amountRemaining;
	protected boolean locationCounted = false;
	protected String currentPrinter = null;
	
	protected boolean showClient = true;

	
	public PickingMobileBean() {
		super();
		
		data = new PickingMobileData();
		
		Client client = data.getDefaultClient();
		showClient = (client == null);
	}

	@Override
	public String getNavigationKey() {
		return PickingMobileNavigation.PICK_START.name();
	}

	@Override
	public String getTitle() {
		return resolve("TitlePickSingle");
	}

	String selClient;
	String selCustomerOrderNumber;
	String selCustomerOrderExternalNumber;
	String selNumPos;
	String selDate;
	String selStrategy;
	
	public void init() {
		log.debug("init");
		amountTaken = null;
		amountRemaining = null;
		locationCounted = false;
		inputCode = "";
		currentPrinter = null;
		orderSelectList = null;

		selClient = "";
		selCustomerOrderNumber = "";
		selCustomerOrderExternalNumber = "";
		selNumPos = "";
		selDate = "";
		selStrategy  ="";
		
		data.reset();
	}
	
	@Override
	public void init(String[] args) {
		super.init(args);
		init();
	}

	
	protected void initPos() {
		log.debug("Initialize position");
		inputCode = null;
		amountTaken = null;
		amountRemaining = null;
		locationCounted = false;
	}

	// ***********************************************************************
	// StartPicking.jsp
	// ***********************************************************************
	public String processStartPicking() {
    	log.debug("processStartPicking");
		init();
		try {
			data.resume(true);
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage() );
			return "";
		}

		if( data.getNumOrders()>0 ) {
			JSFHelper.getInstance().message( resolve("MsgRestore") );
			return PickingMobileNavigation.PICK_ORDER_SUMMARY.name();
		}
		return PickingMobileNavigation.PICK_ORDER_SELECT.name();
	}
	
	
	
	// ***********************************************************************
	// OrderSelect.jsp
	// ***********************************************************************
	protected List<SelectItem> getCalculatedPickingOrders(String code) {
		return data.getCalculatedPickingOrders( code, true, true );
	}
	public String processOrderSelect(){
		String code = inputCode == null ? "" : inputCode.trim();
		inputCode = "";
		
		if( code.length() > 0 ) {
			orderSelectList = getCalculatedPickingOrders( code );
			if( orderSelectList == null || orderSelectList .size() == 0 ) {
				orderSelectList = new ArrayList<SelectItem>();
				JSFHelper.getInstance().message( resolve("MsgNothingFound") );
				selectedOrderId=null;
				loadSelectedOrder();
				return "";
			}
			try{
				selectedOrderId = (Long)orderSelectList.get(0).getValue();
			} catch( Throwable t ) {
		    	log.error("Exception: "+t.getClass().getSimpleName()+", "+t.getMessage(), t);
			}
			if( orderSelectList.size() > 1 ) {
				loadSelectedOrder();
				return "";
			}
			orderSelectList = null;
		}

		if( selectedOrderId == null ) {
			JSFHelper.getInstance().message( resolve("MsgSelectEmpty") );
			orderSelectList = null;
			return "";
		}
		
		try {
			data.loadOrder(selectedOrderId);
			orderSelectList = null;
			if( isShowOrderInfo() ) {
				return PickingMobileNavigation.PICK_ORDER_INFO.name();
			}
			else {
				data.startPicking();
				if( !data.setFirstPick() ) {
					JSFHelper.getInstance().message( resolve("MsgNoPos") );
					return "";
				}
				return PickingMobileNavigation.PICK_PICKFROM.name();
			}
		} catch (FacadeException e) {
			orderSelectList = null;
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}

		
	}
	
	public String processOrderSelectCancel() {
		try {
			data.removeAllOrders();
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
			
		init();
		return PickingMobileNavigation.PICK_MENU.name();
	}

	public List<SelectItem> getOrderList(){
		String logStr = "getOrderList ";

		if( orderSelectList == null ) {
			selectedOrderId = null;

			orderSelectList = getCalculatedPickingOrders( null );
			if( orderSelectList == null ) {
				orderSelectList = new ArrayList<SelectItem>();
			}
			if( orderSelectList.size() > 0 ) {
				try{
					selectedOrderId = (Long)orderSelectList.get(0).getValue();
				} catch( Throwable t ) {
			    	log.error("Exception: "+t.getClass().getSimpleName()+", "+t.getMessage(), t);
				}
			}
			loadSelectedOrder();
		}
		
		log.info(logStr+"Found "+orderSelectList.size()+" orders");
		return orderSelectList;
	}

	public void setSelectedOrder(String sel){
		selectedOrderId = null;
		try{
			selectedOrderId = Long.valueOf(sel);
		} catch( Throwable t ) {}
	}
	
	public String getSelectedOrder(){
		if( selectedOrderId == null ) {
			return null;
		}
		
		return selectedOrderId.toString();
	}
	
    public void orderSelectionChanged(ValueChangeEvent vce) {
    	String sel = (String)vce.getNewValue();
    	setSelectedOrder(sel);
    	loadSelectedOrder();
    }

    protected void loadSelectedOrder() {
    	if( selectedOrderId != null ) {
	    	PickingMobileOrder order = data.readOrder(selectedOrderId);
	    	selClient = order.clientNumber;
	    	selCustomerOrderNumber = order.customerOrderNumber;
	    	selCustomerOrderExternalNumber = order.customerOrderExternalNumber;
	    	selNumPos = String.valueOf(order.numPos);
	    	selDate = order.created;
	    	selStrategy = order.strategy;
    	}
    	else {
	    	selClient = "";
	    	selCustomerOrderNumber = "";
	    	selCustomerOrderExternalNumber = "";
	    	selNumPos = "";
	    	selDate = "";
	    	selStrategy = "";
    	}
    }
    
	// ***********************************************************************
	// PickToPrint.jsp
	// ***********************************************************************
	public String processPickToPrintSelect(){
		String code = inputPrinter == null ? "" : inputPrinter.trim();
		inputPrinter = "";
		
		currentPrinter = (code.length()==0 ? null : code);

		try {
			data.printLabel( currentPrinter );
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}

		return PickingMobileNavigation.PICK_PICKTO_TARGET.name();
	}
	
	public String processPickToPrintSkip(){
		inputPrinter = "";
		currentPrinter = null;
		return PickingMobileNavigation.PICK_PICKTO_TARGET.name();
	}

	// ***********************************************************************
	// OrderSummary.jsp
	// ***********************************************************************
	public String processOrderSummary(){
		return processOrderInfo();
	}
	
	public String processOrderSummaryCancel(){
		return processOrderInfoCancel();
	}
	
	// ***********************************************************************
	// OrderInfo.jsp
	// ***********************************************************************
	public String processOrderInfo(){
		String logStr="processOrderInfo ";
		try {
			data.startPicking();
			
			if( data.setFirstPick() ) {
				return PickingMobileNavigation.PICK_PICKFROM.name();
			}
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
		
		log.error(logStr+"No pick found to process");
		JSFHelper.getInstance().message( resolve("MsgNoPos") );
		return processOrderInfoCancel();
	}
	
	public String processOrderInfoCancel(){
		inputCode = "";

		try {
			data.startPutAway();
			if( data.getCurrentPickTo() != null ) {
				return PickingMobileNavigation.PICK_PICKTO_TARGET.name();
			}
			data.removeAllOrders();
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
		
		init();
		
		return PickingMobileNavigation.PICK_ORDER_SELECT.name();
	}
	
	// ***********************************************************************
	// PickFrom.jsp
	// ***********************************************************************
	public String processPickFrom(){
		String code = inputCode == null ? "" : inputCode.trim();
		inputCode = "";
		
		if( code.length() == 0 ) {
			JSFHelper.getInstance().message( resolve("MsgEnterSomething") );
			return "";
		}
		
		initPos();

		try {
			data.checkPickSource(code);
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
		
		return PickingMobileNavigation.PICK_PICK.name();
	}
	
	public String processPickFromFunction(){
		return PickingMobileNavigation.PICK_PICKFROM_FUNCTION.name();
	}

	// ***********************************************************************
	// Pick.jsp
	// ***********************************************************************
	public String processPick(){
		amountTaken = null;
		amountRemaining = null;
		locationCounted = false;
		
		try {
			if( getNumSerial() > (serialList == null ? 0 : serialList.size()) ) {
				return PickingMobileNavigation.PICK_PICK_SERIAL.name();
			}
			
			if( data.isLocationEmpty() ) {
				return PickingMobileNavigation.PICK_PICK_EMPTY.name();
			}

			return confirmSelectSerial();

		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage() );
			return "";
		}
	}
	
	public String processPickFunction(){
		amountTaken = null;
		return PickingMobileNavigation.PICK_PICK_FUNCTION.name();
	}
	
	// ***********************************************************************
	// PickSerial.jsp
	// ***********************************************************************
	public String processSerial(){
		String code = inputCode == null ? "" : inputCode.trim();
		inputCode = "";
		
		if( code.length() == 0 ) {
			JSFHelper.getInstance().message( resolve("MsgEnterSomething") );
			return "";
		}
		
		try {
			data.checkSerial(serialList, code);
		}
		catch( FacadeException e ) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
		if( serialList == null ) {
			serialList = new ArrayList<String>();
		}
		serialList.add(code);

		try {
			return confirmSelectSerial();
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
	}
	
	public String processSerialCancel(){
		amountTaken = null;
		amountRemaining = null;
		locationCounted = false;
		serialList = null;
		return PickingMobileNavigation.PICK_PICK.name();
	}
	
	public String confirmSelectSerial() throws FacadeException {
		log.info("SERIAL: required="+getNumSerial()+"selected="+(serialList==null?0:serialList.size()));
		if( (serialList==null?0:serialList.size()) < getNumSerial() ) {
			return PickingMobileNavigation.PICK_PICK_SERIAL.name();
		}

		data.confirmPick( amountTaken, amountRemaining, serialList, locationCounted );
		
		amountTaken = null;
		amountRemaining = null;
		locationCounted = false;
		serialList = null;
		
		if( data.setNextPick() ) {
			return PickingMobileNavigation.PICK_PICKFROM.name();
		}
		return PickingMobileNavigation.PICK_PICK_DONE.name();
	}
	
	// ***********************************************************************
	// PickFunction.jsp
	// ***********************************************************************
	public String processPickFunctionNext(){
		try {
			if( data.setNextPick() ) {
				return PickingMobileNavigation.PICK_PICKFROM.name();
			}
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
		return PickingMobileNavigation.PICK_PICK_DONE.name();
	}
	
	public String processPickFunctionCancel(){
		try {
			data.startPutAway();
			if( data.getCurrentPickTo()==null ) {
				data.removeAllOrders();
				init();
				return PickingMobileNavigation.PICK_ORDER_DONE.name();
			}
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
		
		return PickingMobileNavigation.PICK_PICK_DONE.name();
	}

	public String processPickFunctionPrev(){
		try {
			if( data.setPrevPick() ) {
				return PickingMobileNavigation.PICK_PICKFROM.name();
			}
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
		return PickingMobileNavigation.PICK_PICK_DONE.name();
	}
	public String processPickFunctionFinish(){
		return PickingMobileNavigation.PICK_PICKTO_COMPLETED.name();
	}
	public String processPickFunctionMissing(){
		return PickingMobileNavigation.PICK_PICK_MISSING.name();
	}


	// ***********************************************************************
	// PickEmpty.jsp
	// ***********************************************************************
	public String processPickEmptyYes(){
		try {
			amountRemaining = BigDecimal.ZERO;
			locationCounted = true;
			return confirmSelectSerial();
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
	}
	
	public String processPickEmptyNo(){
		amountRemaining = null;
		locationCounted = false;
		
		// Only if the location is empty, a partial pick is allowed  
		if( amountTaken != null ) {
			amountTaken = null;
			JSFHelper.getInstance().message( resolve("MsgPickRequestedAmount") );
			return PickingMobileNavigation.PICK_PICK.name();
		}
		
		return PickingMobileNavigation.PICK_PICK_REMAINING.name();
	}
	
	// ***********************************************************************
	// PickRemaining.jsp
	// ***********************************************************************
	public String processPickRemaining(){
		String code = inputCode == null ? "" : inputCode.trim();
		inputCode = "";
		
		if( code.length() == 0 ) {
			JSFHelper.getInstance().message( resolve("MsgEnterSomething") );
			return "";
		}
		BigDecimal amountRemain = null;
		try {
			amountRemain = new BigDecimal(code);
		}
		catch( Throwable t ) {
			JSFHelper.getInstance().message( "Please enter valid amount" );
			return "";
		}
		if( BigDecimal.ZERO.compareTo(amountRemain)>0 ) {
			JSFHelper.getInstance().message( "Please enter valid amount" );
			return "";
		}

		this.amountRemaining = amountRemain;
		locationCounted = true;
		try {
			return confirmSelectSerial();
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
	}
	
	public String processPickRemainingCancel(){
		inputCode = "";
		amountTaken = null;
		amountRemaining = null;
		locationCounted = false;
		return PickingMobileNavigation.PICK_PICK.name();
	}
	
	// ***********************************************************************
	// PickMissing.jsp
	// ***********************************************************************
	public String processPickMissing(){
		String code = inputCode == null ? "" : inputCode.trim();
		inputCode = "";
		
		BigDecimal amount = null;
		if( code.length() == 0 ) {
			JSFHelper.getInstance().message( resolve("MsgEnterSomething") );
			return "";
		}
		try {
			amount = new BigDecimal(code);
		}
		catch( Throwable t ) {
			JSFHelper.getInstance().message( resolve("MsgEnterValidAmount") );
			return "";
		}
		if( BigDecimal.ZERO.compareTo(amount)>0 ) {
			JSFHelper.getInstance().message( resolve("MsgEnterValidAmount") );
			return "";
		}
		
		// Entered amount must be < requested amount
		if( data.getAmount().compareTo(amount)<=0 ) {
			JSFHelper.getInstance().message( resolve("MsgAmountLessRequested") );
			return "";
		}
		
		amountTaken = amount;
		amountRemaining = BigDecimal.ZERO;
		locationCounted = false;
		
		try {
			return confirmSelectSerial();
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return PickingMobileNavigation.PICK_PICK.name();
		}
	}
	
	public String processPickMissingCancel(){
		amountTaken = null;
		amountRemaining = null;
		locationCounted = false;
		return PickingMobileNavigation.PICK_PICK.name();
	}

	
	// ***********************************************************************
	// LocationEmpty.jsp
	// ***********************************************************************
	public String processLocationEmptyYes(){
		try {
			amountRemaining = BigDecimal.ZERO;
			locationCounted = true;
			return confirmSelectSerial();
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
	}
	
	public String processLocationEmptyNo(){
		return PickingMobileNavigation.PICK_PICK_REMAINING.name();
	}
	
	// ***********************************************************************
	// PickToCompleted.jsp
	// ***********************************************************************
	public String processPickToCompletedYes(){
		return PickingMobileNavigation.PICK_PICK_DONE.name();
	}
	
	public String processPickToCompletedNo(){
		return PickingMobileNavigation.PICK_PICKFROM.name();
	}

	
	// ***********************************************************************
	// PickToLabel.jsp
	// ***********************************************************************
	public String processPickToLabelPrint(){
		String code = inputCode == null ? "" : inputCode.trim();
		inputCode = "";
		
		if( code.length() > 0 ) {
			try {
				data.changePickToLabel(code);
			} catch (FacadeException e) {
				JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
				return "";
			}
		}
		return PickingMobileNavigation.PICK_PICKTO_PRINT.name();
	}
	public String processPickToLabelGenerate(){
		inputCode = "";
		try {
			data.changePickToLabel(null);
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
		return PickingMobileNavigation.PICK_PICKTO_PRINT.name();
	}
	
	// ***********************************************************************
	// PickToTarget.jsp
	// ***********************************************************************
	public String processPickToTarget(){
		String code = inputCode == null ? "" : inputCode.trim();
		inputCode = "";
		if( code.length() == 0 ) {
			JSFHelper.getInstance().message( resolve("MsgEnterSomething") );
			return "";
		}

		try {
			data.transferPickTo(code, State.PICKED);
			data.finishProcess();
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}

		return PickingMobileNavigation.PICK_ORDER_DONE.name();
	}
	
	public String processPickToTargetLabel(){
		return PickingMobileNavigation.PICK_PICKTO_LABEL_POSTPICK.name();
	}
	
	// ***********************************************************************
	// PickDone.jsp
	// ***********************************************************************
	public String processPickDone(){

		try {
			data.startPutAway();
			if( data.getCurrentPickTo() != null ) {
				return PickingMobileNavigation.PICK_PICKTO_TARGET.name();
			}
			data.finishProcess();
		} catch (FacadeException e) {
			JSFHelper.getInstance().message( e.getLocalizedMessage(getUIViewRoot().getLocale()) );
			return "";
		}
		
		return PickingMobileNavigation.PICK_ORDER_DONE.name();
	}

	// ***********************************************************************
	// OrderDone.jsp
	// ***********************************************************************
	public String processOrderDone(){
		init();
		return PickingMobileNavigation.PICK_ORDER_SELECT.name();
	}

	// ***********************************************************************
	// Attributes
	// ***********************************************************************
	public String getInputCode() {
		return inputCode;
	}
	public void setInputCode(String inputCode) {
		this.inputCode = inputCode;
	}
	public String getInputPrinter() {
		return inputPrinter;
	}
	public void setInputPrinter(String inputCode) {
		this.inputPrinter = inputCode;
	}
	
	// ***********************************************************************
	public boolean isShowClient() {
		return showClient;
	}

	// ***********************************************************************
	public boolean isShowOrderInfo() {
		return true;
	}

	// ***********************************************************************
	public String getClientNumber() {
		return data.getCurrentOrder().clientNumber;
	}
	public String getCustomerOrderNumber() {
		return data.getCurrentOrder().customerOrderNumber;
	}
	public String getCustomerOrderExternalNumber() {
		return data.getCurrentOrder().customerOrderExternalNumber;
	}
	public String getOrderInfo() {
		String ret = data.getCurrentOrder().customerOrderNumber;
		if( ret == null ) {
			ret = "";
		}
		if( showClient ) {
			ret = getClientNumber() + " / " + ret;
		}
		return ret;
	}
	public String getOrderStrategy() {
		return data.getCurrentOrder().strategy;
	}
	public String getOrderCreated() {
		return data.getCurrentOrder().created;
	}
	public String getCustomerNumber() {
		return data.getCurrentOrder().customerNumber;
	}
	public String getCustomerName() {
		return data.getCurrentOrder().customerName;
	}
	
	public String getNumPos() {
		return ""+data.getCurrentOrder().numPos;
	}
	public String getPickFromName() {
		return data.getCurrentPick() == null ? "" : data.getCurrentPick().locationName;
	}	
	public String getPickFromLabel() {
		return data.getCurrentPick() == null ? "" : data.getCurrentPick().unitLoadLabel;
	}	
	public String getPickAmount() {
		return data.getCurrentPick() == null ? "" : data.getCurrentPick().amount.toString();
	}	
	public String getPickUnit() {
		return data.getCurrentPick() == null ? "" : data.getCurrentPick().unitName;
	}	
	public String getPickItemName() {
		return data.getCurrentPick() == null ? "" : data.getCurrentPick().itemName;
	}	
	public String getPickItemNumber() {
		return data.getCurrentPick() == null ? "" : data.getCurrentPick().itemNo;
	}
	public String getPickToLabel() {
		return data.getCurrentPickTo() == null ? "" : data.getCurrentPickTo().label;
	}
	public String getPickToTargetName() {
		return data.getCurrentOrder().targetName;
	}
	
	public String getSelCustomerOrderNumber() {
		return selCustomerOrderNumber;
	}
	public String getSelCustomerOrderExternalNumber() {
		return selCustomerOrderExternalNumber;
	}
	public String getSelOrderClient() {
		return selClient;
	}
	public String getSelOrderCreated() {
		return selDate;
	}
	public String getSelOrderNumPos() {
		return selNumPos;
	}
	public String getSelStrategy() {
		return selStrategy;
	}
	
	public String getOrderSummary() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		if( data.getCurrentOrder()==null ) {
			sb.append("No Picking Order");
		}
		else {
			sb.append("Picking Order: "+data.getCurrentOrder().pickingOrderNumber+"<br/>");
			int numPick = data.getCurrentOrder().numPos;
			sb.append(numPick==1?"1 Pick":(""+numPick+" Picks"));
		}
		sb.append("</html>");
		
		return sb.toString();
	}
	
	public String getSerialInfo() {
		return "" + (serialList == null ? 1 : (serialList.size()+1)) + " / " + getNumSerial();
	}

	public boolean isShowCancel() {
		if( data.getCurrentPickTo() == null ) {
			return true;
		}
		if( data.getCurrentPickTo().state < State.STARTED ) {
			return true;
		}
		return false;
	}
	
	
	
	protected int getNumSerial() {
		if( data.getCurrentPick() == null ) {
			return 0;
		}
		if( !data.getCurrentPick().serialRequired ) {
			return 0;
		}
		if( amountTaken != null ) {
			return amountTaken.intValue();
		}
		return data.getCurrentPick().amount.intValue();
	}
	
	
	// ***********************************************************************
	@Override
	protected ResourceBundle getResourceBundle() {
		ResourceBundle bundle;
		Locale loc;
		loc = getUIViewRoot().getLocale();
		bundle = ResourceBundle.getBundle("de.linogistix.mobile.processes.picking.PickingMobileBundle", loc);
		return bundle;
	}

}
