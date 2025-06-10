package com.example.datalake.ingestionsvc.util;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class CsvUtils {
    public static List<Map<String, String>> parse(InputStream in)  {
        var parser = new CSVParserBuilder().withSeparator(',').build();
        var reader = new CSVReaderBuilder(new InputStreamReader(in)).withCSVParser(parser).build();
        try (reader) {
            String[] header = reader.readNext();
            if (header == null) return List.of();

            List<Map<String, String>> rows = new ArrayList<>();
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < header.length && i < line.length; i++) {
                    row.put(header[i], line[i]);
                }
                rows.add(row);
            }
            return rows;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse CSV", ex);
        }
    }
}
