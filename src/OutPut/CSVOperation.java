package OutPut;
import java.io.File;
import java.io.IOException;

import com.csvreader.CsvWriter;

public class CSVOperation {


	public static void writeHitRate(String time, double hitrate)
	{
		String output = "hitRate.csv";
		boolean alreadyExists = new File(output).exists();

		try {
			CsvWriter csvOutput = new CsvWriter(output);

			// if the file didn't already exist then we need to write out the header line
			if (!alreadyExists)
			{
				csvOutput.write("time");
				csvOutput.write("hitRate");
				csvOutput.endRecord();
			}
			// else assume that the file already has the correct header line
			// write out record
			csvOutput.write(time);
			csvOutput.write(Double.toString(hitrate));
			csvOutput.endRecord();
			csvOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		writeHitRate("2010-10-10 00:00:00~2011-10-10 00:00:00", 0.6);
	}
}