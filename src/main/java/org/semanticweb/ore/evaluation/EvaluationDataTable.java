package org.semanticweb.ore.evaluation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.semanticweb.ore.utilities.FileSystemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvaluationDataTable<R,C,V> {
	
	final private static Logger mLogger = LoggerFactory.getLogger(EvaluationDataTable.class);

	private HashMap<R,String> mRowHeaderMap = null;
	private HashMap<C,String> mColumnHeaderMap = null;
	
	private HashMap<R,HashMap<C,V>> mRowColumnValueMap = new HashMap<R,HashMap<C,V>>();
	private HashMap<C,HashMap<R,V>> mColumnRowValueMap = new HashMap<C,HashMap<R,V>>();

	
	private int mNextRowIndex = 0;
	private int mNextColumnIndex = 0;
	private HashMap<Integer,R> mRowIndexMap = new HashMap<Integer,R>();
	private HashMap<Integer,C> mColumnIndexMap = new HashMap<Integer,C>();

	
	public void initColumnHeaders(HashMap<C,String> columnHeaderMap) {
		if (mColumnHeaderMap == null) {
			mColumnHeaderMap = new HashMap<C,String>();
		}
		mColumnHeaderMap.putAll(columnHeaderMap);
	}
	
	
	
	public void initColumnHeaders(Collection<String> columnHeaders) {
		if (mColumnHeaderMap == null) {
			mColumnHeaderMap = new HashMap<C,String>();
		}
		int nextColumnIndex = 0;
		for (String header : columnHeaders) {
			C column = mColumnIndexMap.get(nextColumnIndex++);
			if (column != null) {
				mColumnHeaderMap.put(column, header);
			}
		}
	}	
	

	public void initRowHeaders(HashMap<R,String> rowHeaderMap) {
		if (mRowHeaderMap == null) {
			mRowHeaderMap = new HashMap<R,String>();
		}
		mRowHeaderMap = rowHeaderMap;
	}
	
	

	
	public void initRowHeaders(Collection<String> rowHeaders) {
		if (mRowHeaderMap == null) {
			mRowHeaderMap = new HashMap<R,String>();
		}
		int nextRowIndex = 0;
		for (String header : rowHeaders) {
			R row = mRowIndexMap.get(nextRowIndex++);
			if (row != null) {
				mRowHeaderMap.put(row, header);
			}
		}
	}	
		
	
	public void setColumnHeader(C column, String header) {
		if (mColumnHeaderMap == null) {
			mColumnHeaderMap = new HashMap<C,String>();
		}
		mColumnHeaderMap.put(column, header);
	}
	
	
	public void setRowHeader(R row, String header) {
		if (mRowHeaderMap == null) {
			mRowHeaderMap = new HashMap<R,String>();
		}
		mRowHeaderMap.put(row, header);
	}	

	public void initTable(Collection<R> rowElements, Collection<C> columnElements) {
		initTable(rowElements, columnElements, null);
	}

	
	public void initTable(Collection<R> rowElements, Collection<C> columnElements, V initData) {
		for (R rowElement : rowElements) {
			mRowIndexMap.put(mNextRowIndex++,rowElement);
		}
		for (C columnElement : columnElements) {
			mColumnIndexMap.put(mNextColumnIndex++,columnElement);
		}
		mRowColumnValueMap = new HashMap<R,HashMap<C,V>>();
		mColumnRowValueMap = new HashMap<C,HashMap<R,V>>();
		for (R rowElement : rowElements) {
			for (C columnElement : columnElements) {
				setData(rowElement,columnElement,initData,false);
			}
		}
	}
	
	
	public void setData(R rowElement, C columnElement, V data) {
		setData(rowElement,columnElement,data,true);
	}
	
	public void setData(R rowElement, C columnElement, V data, boolean extendIndexes) {
		HashMap<C,V> columnValueMap = mRowColumnValueMap.get(rowElement);
		if (columnValueMap == null) {
			columnValueMap = new HashMap<C,V>(); 
			mRowColumnValueMap.put(rowElement,columnValueMap);
			if (extendIndexes) {
				mRowIndexMap.put(mNextRowIndex++,rowElement);
			}
		}
		columnValueMap.put(columnElement,data);
		
		HashMap<R,V> rowValueMap = mColumnRowValueMap.get(columnElement);
		if (rowValueMap == null) {
			rowValueMap = new HashMap<R,V>(); 
			mColumnRowValueMap.put(columnElement,rowValueMap);
			if (extendIndexes) {
				mColumnIndexMap.put(mNextColumnIndex++,columnElement);
			}
		}
		rowValueMap.put(rowElement,data);		
	}	
	
	

	public V getData(R rowElement, C columnElement) {
		HashMap<C,V> columnValueMap = mRowColumnValueMap.get(rowElement);
		if (columnValueMap == null) {
			return null;
		}		
		return columnValueMap.get(columnElement);				
	}		
	
	
	public void writeCSVTable(FileOutputStream outputStream) throws IOException {	
		writeTable(outputStream,",","","\r\n");
	}
	
	public void writeTSVTable(FileOutputStream outputStream) throws IOException {	
		writeTable(outputStream,"\t","","\r\n");
	}
	

	public void writeCSVTable(String outputFileString) {	
		writeTable(outputFileString,",","","\r\n");
	}
	
	public void writeTSVTable(String outputFileString) {	
		writeTable(outputFileString,"\t","","\r\n");
	}
	
	public void writeTable(String outputFileString, String betweenColumnDataString, String beginRowString, String endRowString) {
		try {
			FileSystemHandler.ensurePathToFileExists(outputFileString);
			FileOutputStream outputStream = new FileOutputStream(new File(outputFileString));
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);	
			writeTable(outputStream,betweenColumnDataString,beginRowString,endRowString);
			outputStreamWriter.close();
		} catch (IOException e) {
			mLogger.error("Writing to output stream failed, got IOException '{}'.",e.getMessage());
		}		
	}



	public String getColumnHeader(C column) {
		String columnHeader = null; 
		columnHeader = mColumnHeaderMap.get(column);
		return columnHeader;
	}


	public ArrayList<V> getColumnValueList(C column) {
		return getColumnValueList(column,true);
	}

	
	public ArrayList<V> getColumnValueList(C column, boolean sorted) {
		ArrayList<V> valueList = new ArrayList<V>(); 
		HashMap<R,V> rowValuesMap = mColumnRowValueMap.get(column);
		if (!sorted) {
			valueList.addAll(rowValuesMap.values());
		} else {
			for (int rowIdx = 0; rowIdx < mNextRowIndex; ++rowIdx) {
				R rowElement = mRowIndexMap.get(rowIdx);
				valueList.add(rowValuesMap.get(rowElement));
			}
		}				
		return valueList;
	}

	
	
	public void writeTable(FileOutputStream outputStream, String betweenColumnDataString, String beginRowString, String endRowString) throws IOException {	
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
		for (int rowIdx = 0; rowIdx < mNextRowIndex; ++rowIdx) {
			R rowElement = mRowIndexMap.get(rowIdx);
			if (rowIdx == 0 && mColumnHeaderMap != null) {
				outputStreamWriter.write(beginRowString);
				boolean firstDataValue = true;	
				if (mRowHeaderMap != null) {
					firstDataValue = false;
				}
				for (int colIdx = 0; colIdx < mNextColumnIndex; ++colIdx) {
					if (!firstDataValue) {
						outputStreamWriter.write(betweenColumnDataString);
					}					
					C columnElement = mColumnIndexMap.get(colIdx);
					String columnHeader = mColumnHeaderMap.get(columnElement);
					if (columnHeader != null) {
						outputStreamWriter.write(columnHeader.toString());
					}
					firstDataValue = false;
				}
				outputStreamWriter.write(endRowString);
			}
			outputStreamWriter.write(beginRowString);
			boolean firstDataValue = true;
			if (mRowHeaderMap != null) {
				String rowHeaderString = mRowHeaderMap.get(rowElement);				
				outputStreamWriter.write(rowHeaderString);
				firstDataValue = false;
			}
			for (int colIdx = 0; colIdx < mNextColumnIndex; ++colIdx) {
				if (!firstDataValue) {
					outputStreamWriter.write(betweenColumnDataString);
				}
				C columnElement = mColumnIndexMap.get(colIdx);
				V data = getData(rowElement,columnElement);
				if (data != null) {
					outputStreamWriter.write(data.toString());
				}
				firstDataValue = false;
			}
			outputStreamWriter.write(endRowString);
		}
		outputStreamWriter.close();
	}			
	
	
}
