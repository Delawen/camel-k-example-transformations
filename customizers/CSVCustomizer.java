// camel-k: dependency=camel-csv

package customizers;

import org.apache.camel.BindToRegistry;
import org.apache.camel.model.dataformat.CsvDataFormat;

public class CSVCustomizer {

    @BindToRegistry("customCSV")
    public static CsvDataFormat getCsvDataFormat() {
        CsvDataFormat csvDataFormat = new CsvDataFormat();
        csvDataFormat.setAllowMissingColumnNames("true");
        csvDataFormat.setUseMaps("true");
        return csvDataFormat;
    }

}
