package com.sirionlabs.test.reportRenderer;

import java.util.List;
import java.util.Map;

/**
 * @author manoj.upreti
 */
public class FilterData {
	private String filterName;
	private String filterUiType ;
	private int filterId;
	private boolean autoComplete;
	private List<DataClass> dataClassList;




	public FilterData(String filterName, int filterId, boolean autoComplete, List<DataClass> dataClassList) {
		this.setFilterName(filterName);
		this.setFilterId(filterId);
		this.setAutoComplete(autoComplete);
		this.setDataClassList(dataClassList);
	}


	public FilterData(String filterName, String filterUiType , int filterId, boolean autoComplete, List<DataClass> dataClassList ) {
		this.setFilterName(filterName);
		this.setFilterId(filterId);
		this.setAutoComplete(autoComplete);
		this.setDataClassList(dataClassList);
		this.setFilterUiType(filterUiType);
	}


	public String getFilterUiType() {
		return filterUiType;
	}

	public void setFilterUiType(String filterUiType) {
		this.filterUiType = filterUiType;
	}




	public List<DataClass> getDataClassList() {
		return dataClassList;
	}


	public void setDataClassList(List<DataClass> dataClassList) {
		this.dataClassList = dataClassList;
	}


	public boolean isAutoComplete() {
		return autoComplete;
	}


	public void setAutoComplete(boolean autoComplete) {
		this.autoComplete = autoComplete;
	}


	public int getFilterId() {
		return filterId;
	}


	public void setFilterId(int filterId) {
		this.filterId = filterId;
	}


	public String getFilterName() {
		return filterName;
	}


	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}

	public static class DataClass {
		private String dataName;
		private String dataValue;
		private Map<String, String> mapOfData;

		public String getDataName() {
			return this.dataName;
		}

		public void setDataName(String dataName) {
			this.dataName = dataName;
		}

		public String getDataValue() {
			return this.dataValue;
		}

		public void setDataValue(String dataValue) {
			this.dataValue = dataValue;
		}

		public Map<String, String> getMapOfData() {
			return this.mapOfData;
		}

		public void setMapOfData(Map<String, String> mapOfData) {
			this.mapOfData = mapOfData;
		}


	}
}