package csvreader;

class User {

	private String userId;
	private String firstName;
	private String lastName;
	private int version;
	private String insuranceCompany;

	public User(String userId, String firstName, String lastName, int version, String insuranceCompany) {
		this.userId = userId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.version = version;
		this.insuranceCompany = insuranceCompany;
	}

	public String getUserId() {
		return userId;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public int getVersion() {
		return version;
	}

	public String getInsuranceCompany() {
		return insuranceCompany;
	}

	@Override
	public String toString() {
		return "User [userId=" + userId + ", firstName=" + firstName + ", lastName=" + lastName
				+ ", version=" + version + ", insuranceCompany=" + insuranceCompany + "]";
	}

}