package csvreader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class CSVReader {

	/**
	 * Returns an integer representation of the string input. Uses defaultValue in
	 * the case of an exception.
	 * 
	 * @param str          the string to convert to integer
	 * @param defaultValue the integer value to use in the case of an exception
	 * @return the integer representation of the string input
	 */
	private static int StringToIntDef(String str, int defaultValue) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	/**
	 * Reads all CSV files in the specified input folder. The input CSV files
	 * contain User account records for insurance companies.
	 * 
	 * @param inputFolder the input folder to containing the CSV files
	 * @return HashMap containing user data, organized by insurance company.
	 */
	private static HashMap<String, HashMap<String, User>> ReadCSVFiles(String inputFolder) {

		// Key is the insurance company name -> Value is HashMap of User records. User records HashMap Key is userId -> Value is User object
		// If there are duplicate userIds for a given insurance company, only the User record with the highest version number is stored
		HashMap<String, HashMap<String, User>> csvUserRecords = new HashMap<>();

		File inputDir = new File(inputFolder);
		FileFilter fileFilter = file -> !file.isDirectory() && file.getName().toLowerCase().endsWith(".csv");

		for (File file : inputDir.listFiles(fileFilter)) {
			System.out.println("Processng input file: " + file.getAbsolutePath());

			try (BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
				int rowNum = 0;

				// skip the first line, the CSV files are assumed to have a header row
				String line = br.readLine();
				rowNum++;

				while ((line = br.readLine()) != null && !line.isEmpty()) {
					rowNum++;
					String[] fields = line.split(",");

					// Each line in the CSV file must have 5 fields, each field in the User object is required.
					if (fields.length == 5) {

						// Version number must be an integer value. Assumes the valid version numbers are positive integers.
						int version = StringToIntDef(fields[3], -1);
						if (version > 0) {
							User newUser = new User(fields[0], fields[1], fields[2], version, fields[4]);

							// Key is userId -> Value is User Object
							HashMap<String, User> insuranceCompanyUsers = csvUserRecords.get(newUser.getInsuranceCompany());

							// If this a new insurance company, initialize a new HashMap to store its users and add the new User object
							if (insuranceCompanyUsers == null) {
								insuranceCompanyUsers = new HashMap<>();
								insuranceCompanyUsers.put(newUser.getUserId(), newUser);
								csvUserRecords.put(newUser.getInsuranceCompany(), insuranceCompanyUsers);
								System.out.println("New Company, First User: " + newUser);

							// Else, user records for this insurance company already exist
							} else {
							
								User lookupUser = insuranceCompanyUsers.get(newUser.getUserId());

								// If this is new user for this insurance company, store the user
								if (lookupUser == null) {
									insuranceCompanyUsers.put(newUser.getUserId(), newUser);
									System.out.println("New User Added: " + newUser);

								// Else, a record for this user already exists for this company,
								// only store the user if the version number is greater
								} else if (newUser.getVersion() > lookupUser.getVersion()) {
									insuranceCompanyUsers.replace(newUser.getUserId(), newUser);
									System.out.println("Existing User, Higher Version, Replace: " + newUser + " " + lookupUser);

								} else {
									System.out.println("Existing User, Lower/Equal Version, Discard: " + newUser + " " + lookupUser);
								}
							}
						} else {
							System.out.println(String.format(
									"WARNING: Invalid Version field in file [%s] on row %d.  Line: [%s] skipped.",
									file.getAbsolutePath(), rowNum, line));
						}
					} else {
						System.out.println(String.format(
								"WARNING: Incorrect CSV field count in file [%s] on row %d.  Line: [%s] skipped.",
								file.getAbsolutePath(), rowNum, line));
					}
				}
			} catch (FileNotFoundException e) {
				System.out.println(String.format("ERROR: File [%s] not found.", file.getAbsolutePath()));
			} catch (IOException e) {
				System.out.println(String.format("ERROR: Failed during processing file [%s].", file.getAbsolutePath()));
			}

		}

		return csvUserRecords;
	}

	// Comparator that is used to sort User objects by LastName then FirstName
	private static class UserSortByLastNameFirstName implements Comparator<User> {
		@Override
		public int compare(User user1, User user2) {
			int result = user1.getLastName().compareTo(user2.getLastName());
			if (result == 0) {
				result = user1.getFirstName().compareTo(user2.getFirstName());
			}
			return result;
		}
	}

	/**
	 * Writes user record data to CSV files in the specified output folder. The output CSV filenames
	 * correspond to the insurance company.  User records within each file are sorted by User Last Name and First Name.
	 * @param csvUserRecords the user records grouped by insurance company
	 * @param outputFolder   the output folder to use when storing the CSV for each insurance company
	 */
	private static void WriteCSVFiles(HashMap<String, HashMap<String, User>> csvUserRecords, String outputFolder) {
		File outputDirectory = new File(outputFolder);
		
		// Force creation of entire folder structure, still need to check if full structure exists
		outputDirectory.mkdirs();
		if (outputDirectory.exists()) {

			// Iterate over the user records by insurance company
			for (String insuranceCompany : csvUserRecords.keySet()) {

				// Key is userId -> Value is User object
				HashMap<String, User> insuranceCompanyUsers = csvUserRecords.get(insuranceCompany);

				// Sort user records from this insurance company by LastName then FirstName
				List<User> sortedUsers = new ArrayList<>(insuranceCompanyUsers.values());
				Collections.sort(sortedUsers, new UserSortByLastNameFirstName());

				// Iterate over user records, building CSV row for each user, starting with a header row
				String rows = "user_id,first_name,last_name,version,insurance_company\n";
				for (User u : sortedUsers) {
					rows += String.join(",", u.getUserId(), u.getFirstName(), u.getLastName(),
							String.valueOf(u.getVersion()), u.getInsuranceCompany()) + "\n";
				}

				// Write all user records for this insurance company to CSV file 
				File file = new File(outputFolder + insuranceCompany + ".csv");
				try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
					bw.write(rows);

					System.out.println(String.format("Wrote %d User records for %s to file %s",
							insuranceCompanyUsers.size(),
							insuranceCompany, file.getAbsolutePath()));
				} catch (IOException e) {
					System.out.println(String.format("ERROR: Failed to write records for company [%s] to file [%s].",
							insuranceCompany, file.getAbsolutePath()));
				}

			}
		} else {
			System.out.println(
					String.format("ERROR: Failed to create output directory [%s].", outputDirectory.getAbsolutePath()));
		}
	}

	public static void main(String[] args) {
		if (args.length == 2) {
			String inputDirectory = args[0];
			String outputDirectory = args[1];

			// Key is the insurance company name -> Value is HashMap of User records.  User records HashMap Key is userId -> Value is User object
			HashMap<String, HashMap<String, User>> csvUserRecords = ReadCSVFiles(inputDirectory);

			WriteCSVFiles(csvUserRecords, outputDirectory);

		} else {
			System.out.println(
					"Usage: Specify the input directory as the first argument and the output directory as the second argument.");
		}
	}

}
