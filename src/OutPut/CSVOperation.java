package OutPut;
import java.io.File;
import java.io.IOException;

import com.csvreader.CsvWriter;

public class CSVOperation {

//in every output file, we should print :name of the graph, parameters,
	public static void writeHitRate(String filename, int month, String range, double hitrate, int numCommits)
	{
		String output = "Results/"+filename+".csv";
		boolean alreadyExists = new File(output).exists();

		try {
			CsvWriter csvOutput = new CsvWriter(output);

			// if the file didn't already exist then we need to write out the header line
			if (!alreadyExists)
			{
			    csvOutput.write("Month");
				csvOutput.write("Range");
				csvOutput.write("HitRate");
				csvOutput.write("NumCommits");
				csvOutput.endRecord();
			}
			// else assume that the file already has the correct header line
			// write out record
			csvOutput.write(Integer.toString(month));
			csvOutput.write(range);
			csvOutput.write(Double.toString(hitrate));
			csvOutput.write(Integer.toString(numCommits));
			csvOutput.endRecord();
			csvOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		writeHitRate("example",3,"2010-10-10~2011-10-10", 0.6, 20);
	}
}