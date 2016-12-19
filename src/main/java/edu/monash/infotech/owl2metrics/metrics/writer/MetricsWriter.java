package edu.monash.infotech.owl2metrics.metrics.writer;

import com.opencsv.CSVWriter;
import edu.monash.infotech.owl2metrics.model.OntMetrics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public class MetricsWriter {
    private boolean measureExpressivity;

    public void writeHeader(String csvFileName, char delimiter, boolean measureExpressivity) throws IOException {
        this.measureExpressivity = measureExpressivity;
        CSVWriter writer = new CSVWriter(new FileWriter(csvFileName), delimiter);
        try {
            writer.writeNext(MetricsFormatter.getHeader(measureExpressivity));
        } finally {
            writer.close();
        }
    }

    public void writeHeader(String csvFileName, char delimiter, String[] headers) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(csvFileName), delimiter);
        try {
            writer.writeNext(headers);
        } finally {
            writer.close();
        }
    }

    public void writeHeader(Writer _writer, char delimiter) throws IOException {
        CSVWriter writer = new CSVWriter(_writer, delimiter);
        try {
            writer.writeNext(MetricsFormatter.getHeader(measureExpressivity));
        } finally {
            writer.close();
        }
    }

    public void writeMetrics(String csvFileName, char delimiter, String ontName, OntMetrics metrics, boolean writeHeader, boolean append) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(csvFileName, append), delimiter);

        try {
            MetricsFormatter formatter = new MetricsFormatter(measureExpressivity);
            String[] strings = formatter.formatMetrics(ontName, metrics);

            if (writeHeader) {
                writer.writeNext(MetricsFormatter.getHeader(measureExpressivity));
            }
            writer.writeNext(strings);
        } finally {
            writer.close();
        }
    }

    public void writeMetrics(Writer _writer, char delimiter, String ontName, OntMetrics metrics, boolean writeHeader) throws IOException {
        CSVWriter writer = new CSVWriter(_writer, delimiter);

        try {
            MetricsFormatter formatter = new MetricsFormatter(measureExpressivity);
            String[] strings = formatter.formatMetrics(ontName, metrics);
            if (writeHeader) {
                writer.writeNext(MetricsFormatter.getHeader(measureExpressivity));
            }
            writer.writeNext(strings);
        } finally {
            writer.close();
        }
    }

    public void writeMetrics(String csvFileName, char delimiter, String[] values, boolean append) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(csvFileName, append), delimiter);

        try {
            writer.writeNext(values);
        } finally {
            writer.close();
        }
    }
}
