package com.hartwig.hmftools.common.utils.file;

import static com.hartwig.hmftools.common.utils.file.FileDelimiters.TSV_DELIM;
import static com.hartwig.hmftools.common.utils.file.FileWriterUtils.createBufferedWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.logging.log4j.util.BiConsumer;


/**
 * Example usage:
 *    new DelimFileWriter().write(fileName, List.of(CHROMOSOME, MEDIAN_RATIO, COUNT), ratios,
 *           (medianRatio, row) -> {
 *               row.set(CHROMOSOME, medianRatio.chromosome());
 *               row.set(MEDIAN_RATIO, medianRatio.medianRatio(), FORMAT);
 *               row.set(COUNT, medianRatio.count()); });
 */
public class DelimFileWriter
{
    // by default, use 4 decimal places for doubles
    private static final NumberFormat sDefaultNumberFormat = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.ENGLISH));
    private static final String sNullIndicator = "";

    String mDelim = TSV_DELIM;

    public DelimFileWriter()
    {
    }

    public void setDelimiter(String delimiter)
    {
        mDelim = delimiter;
    }

    /**
     * Example usage:
     *    new DelimFileWriter().write(fileName, List.of(CHROMOSOME, MEDIAN_RATIO, COUNT), ratios,
     *           (medianRatio, row) -> {
     *               row.set(CHROMOSOME, medianRatio.chromosome());
     *               row.set(MEDIAN_RATIO, medianRatio.medianRatio(), FORMAT);
     *               row.set(COUNT, medianRatio.count()); });
     *
     * @param filename   path to the file.
     * @param columns    column names of the file.
     * @param objects    iterable of objects to serialised.
     * @param mapper     function to populate a Row given an object of type T.
     */
    public <T> void write(String filename, Iterable<String> columns, Iterable<T> objects, BiConsumer<T, Row> mapper)
    {
        try(BufferedWriter writer = createBufferedWriter(filename))
        {
            write(writer, columns, objects, mapper);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Write to the input buffered writer.
     * @param writer     buffedWriter
     * @param columns    column names of the file.
     * @param objects    iterable of objects to serialised.
     * @param mapper     function to populate a Row given an object of type T.
     */
    public <T> void write(BufferedWriter writer, Iterable<String> columns, Iterable<T> objects, BiConsumer<T, Row> mapper)
    {
        try
        {
            // create a map of indices
            Map<String, Integer> columnIndexMap = new HashMap<>();
            int i = 0;
            for (String c : columns)
            {
                if (columnIndexMap.putIfAbsent(c, i++) != null)
                {
                    throw new RuntimeException("duplicate column: " + c);
                }
            }

            writer.write(String.join(mDelim, columns));
            writer.write('\n');

            for (T obj : objects)
            {
                Row row = new Row(columnIndexMap, i); // i is the number of columns
                mapper.accept(obj, row);
                StringJoiner joiner = new StringJoiner(mDelim);
                for(String e : row.mValues)
                {
                    joiner.add(e != null ? e : sNullIndicator);
                }
                writer.write(joiner.toString());
                writer.write('\n');
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public static class Row
    {
        private final Map<String, Integer> mColumnIndexMap;
        private final String[] mValues;

        private Row(Map<String, Integer> columnIndexMap, int numColumns)
        {
            mColumnIndexMap = columnIndexMap;
            mValues = new String[numColumns];
        }
        public void set(String key, String value)
        {
            Integer columnIndex = mColumnIndexMap.get(key);
            if (columnIndex == null)
            {
                throw new IllegalArgumentException("invalid column: " + key);
            }
            mValues[columnIndex] = value;
        }
        public void set(String key, int value)
        {
            set(key, Integer.toString(value));
        }

        // store bool as 1 and 0
        public void set(String key, boolean value)
        {
            set(key, value ? 1 : 0);
        }

        public void set(String key, char value)
        {
            set(key, String.valueOf(value));
        }

        public void set(String key, byte value)
        {
            set(key, Byte.toString(value));
        }

        public void set(String key, double value)
        {
            set(key, value, sDefaultNumberFormat);
        }

        //
        //  record.set("rate", 0.27562, "%.3f");
        //
        public void set(String key, double value, String format)
        {
            set(key, String.format(format, value));
        }

        // record.set("rate", 0.27572, new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.ENGLISH)));
        public void set(String key, double value, NumberFormat format)
        {
            set(key, format.format(value));
        }

        // overloads to allow using enum as key
        public void set(Enum<?> key, String value)
        {
            set(key.name(), value);
        }
        public void set(Enum<?> key, int value)
        {
            set(key.name(), value);
        }
        public void set(Enum<?> key, boolean value)
        {
            set(key.name(), value);
        }
        public void set(Enum<?> key, char value)
        {
            set(key.name(), value);
        }
        public void set(Enum<?> key, byte value)
        {
            set(key.name(), value);
        }
        public void set(Enum<?> key, double value)
        {
            set(key.name(), value);
        }
        public void set(Enum<?> key, double value, String format)
        {
            set(key.name(), value, format);
        }
        public void set(Enum<?> key, double value, NumberFormat format)
        {
            set(key.name(), value, format);
        }
    }
}
