package ore.rendering;

import ore.utilities.FilePathString;
import ore.utilities.RelativeFilePathStringType;
import org.joda.time.DateTime;

import java.io.*;
import java.util.Collection;

public class TSVRenderer {
	
	
	protected OutputStream getOutputStream(File file) throws FileNotFoundException {
		return new FileOutputStream(file);
	}
	
	
	protected OutputStream getOutputStream(String fileString) throws FileNotFoundException {
		return getOutputStream(new File(fileString));
	}	
	
	protected OutputStreamWriter getOutputStreamWriter(OutputStream outputStream) {
		return new OutputStreamWriter(outputStream);
	}	
	
	protected void writeTSVLine(String keyName, String value, OutputStreamWriter outputStreamWriter) throws IOException {
		String writeLine = keyName+"\t"+value+"\n";
		outputStreamWriter.write(writeLine);
	}	
	

	protected void writeTSVLine(String keyName, DateTime dateTime, OutputStreamWriter outputStreamWriter) throws IOException {
		String writeLine = keyName+"\t"+dateTime.toString()+"\n";
		outputStreamWriter.write(writeLine);
	}	
	
	protected void writeTSVLine(String keyName, boolean value, OutputStreamWriter outputStreamWriter) throws IOException {
		String writeLine = null;
		if (value) {
			writeLine = keyName+"\ttrue\n";
		} else {			
			writeLine = keyName+"\tfalse\n";
		}
		outputStreamWriter.write(writeLine);
	}
	
	protected void writeTSVLine(String keyName, long value, OutputStreamWriter outputStreamWriter) throws IOException {
		String writeLine = keyName+"\t"+ Long.toString(value)+"\n";
		outputStreamWriter.write(writeLine);
	}
	
	protected void writeTSVLine(Collection<String> values, OutputStreamWriter outputStreamWriter) throws IOException {
		String writeLine = null;
		for (String value : values) {
			if (writeLine == null) {
				writeLine = value;
			} else {
				writeLine = "\t"+value;
			}
		}
		writeLine = writeLine+"\n";
		outputStreamWriter.write(writeLine);
	}
	
	
	protected void writeTSVLine(String keyName, FilePathString value, OutputStreamWriter outputStreamWriter) throws IOException {
		String writeLine = null;
		if (value.isAbsolute()) {
			writeLine = keyName+"\tABSOLUTE\t"+value.getAbsoluteFilePathString().replace('\\','/')+"\n";
		} else if (value.isRelative()) {
			if (value.getRelativeType() == RelativeFilePathStringType.RELATIVE_TO_BASE_DIRECTORY) {
				writeLine = keyName+"\tRELATIVETOBASEDIRECTORY\t"+value.getRelativeFilePathString().replace('\\','/')+"\n";
			} else if (value.getRelativeType() == RelativeFilePathStringType.RELATIVE_TO_ONTOLOGIES_DIRECTORY) {
				writeLine = keyName+"\tRELATIVETOONTOLOGIESDIRECTORY\t"+value.getRelativeFilePathString().replace('\\','/')+"\n";
			} else if (value.getRelativeType() == RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY) {
				writeLine = keyName+"\tRELATIVETOQUERIESDIRECTORY\t"+value.getRelativeFilePathString().replace('\\','/')+"\n";
			} else if (value.getRelativeType() == RelativeFilePathStringType.RELATIVE_TO_CONVERSIONS_DIRECTORY) {
				writeLine = keyName+"\tRELATIVETOEXPECTATIONSDIRECTORY\t"+value.getRelativeFilePathString().replace('\\','/')+"\n";
			} else if (value.getRelativeType() == RelativeFilePathStringType.RELATIVE_TO_EXPECTATIONS_DIRECTORY) {
				writeLine = keyName+"\tRELATIVETOCONVERSIONSDIRECTORY\t"+value.getRelativeFilePathString().replace('\\','/')+"\n";
			} else if (value.getRelativeType() == RelativeFilePathStringType.RELATIVE_TO_RESPONSES_DIRECTORY) {
				writeLine = keyName+"\tRELATIVETORESPONSESDIRECTORY\t"+value.getRelativeFilePathString().replace('\\','/')+"\n";
			} else if (value.getRelativeType() == RelativeFilePathStringType.RELATIVE_TO_SOURCE_DIRECTORY) {
				writeLine = keyName+"\tRELATIVETOSOURCEDIRECTORY\t"+value.getRelativeFilePathString().replace('\\','/')+"\n";
			} else if (value.getRelativeType() == RelativeFilePathStringType.RELATIVE_TO_COMPETITIONS_DIRECTORY) {
				writeLine = keyName+"\tRELATIVETOCOMPETITIONSDIRECTORY\t"+value.getRelativeFilePathString().replace('\\','/')+"\n";
			} else if (value.getRelativeType() == RelativeFilePathStringType.RELATIVE_TO_TEMPLATES_DIRECTORY) {
				writeLine = keyName+"\tRELATIVETOTEMPLATESDIRECTORY\t"+value.getRelativeFilePathString().replace('\\','/')+"\n";
			} else if (value.getRelativeType() == RelativeFilePathStringType.RELATIVE_TO_SOURCE) {
				writeLine = keyName+"\tRELATIVETOSOURCE\t"+value.getRelativeFilePathString().replace('\\','/')+"\n";
			}
		}
		outputStreamWriter.write(writeLine);
	}		

}
