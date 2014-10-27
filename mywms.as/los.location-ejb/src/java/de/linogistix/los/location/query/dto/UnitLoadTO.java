/*
 * Copyright (c) 2006 - 2012 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.los.location.query.dto;

import de.linogistix.los.location.model.LOSUnitLoad;
import de.linogistix.los.query.BODTO;

public class UnitLoadTO extends BODTO<LOSUnitLoad> {

	private static final long serialVersionUID = 1L;
	
	private String storageLocation;
	private int lock;
	private String clientNumber;
	private Long numChildUnitLoads = null;
	private boolean carrier;
	
	public UnitLoadTO(LOSUnitLoad ul){
		this(ul.getId(), ul.getVersion(), ul.getLabelId(), ul.getClient().getNumber(), ul.getLock(), ul.getStorageLocation().getName(), ul.isCarrier());
	}

	public UnitLoadTO(Long id, int version, String name, String clientNumber, int lock, String storageLocation, boolean carrier){
		super(id, version, name);
		this.storageLocation = storageLocation;
		this.lock = lock;
		this.carrier = carrier;
		this.clientNumber = clientNumber;
	}

	public String getStorageLocation() {
		return storageLocation;
	}
	public void setStorageLocation(String storageLocation) {
		this.storageLocation = storageLocation;
	}

	public int getLock() {
		return lock;
	}
	public void setLock(int lock) {
		this.lock = lock;
	}

	public String getClientNumber() {
		return clientNumber;
	}
	public void setClientNumber(String clientNumber) {
		this.clientNumber = clientNumber;
	}

	public boolean isCarrier() {
		return carrier;
	}

	public void setCarrier(boolean carrier) {
		this.carrier = carrier;
	}

	public Long getNumChildUnitLoads() {
		return numChildUnitLoads;
	}
	public void setNumChildUnitLoads(Long numChildUnitLoads) {
		this.numChildUnitLoads = numChildUnitLoads;
	}
	
}
