package org.akaza.openclinica.domain.user;

//default package
//Generated Jul 31, 2013 2:03:33 PM by Hibernate Tools 3.4.0.CR1

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.akaza.openclinica.domain.DataMapDomainObject;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
* UserType generated by hbm2java
*/
@Entity
@Table(name = "user_type")
@GenericGenerator(name = "id-generator", strategy = "native", parameters = { @Parameter(name = "sequence", value = "user_type_user_type_id_seq") })

public class UserType  extends DataMapDomainObject implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int userTypeId;
	private String userType;
	private Set<UserAccount> userAccounts = new HashSet(0);

	public UserType() {
	}

	public UserType(int userTypeId) {
		this.userTypeId = userTypeId;
	}

	public UserType(int userTypeId, String userType, Set userAccounts) {
		this.userTypeId = userTypeId;
		this.userType = userType;
		this.userAccounts = userAccounts;
	}

	@Id
	@Column(name = "user_type_id", unique = true, nullable = false)
	public int getUserTypeId() {
		return this.userTypeId;
	}

	public void setUserTypeId(int userTypeId) {
		this.userTypeId = userTypeId;
	}

	@Column(name = "user_type", length = 50)
	public String getUserType() {
		return this.userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userType")
	public Set<UserAccount> getUserAccounts() {
		return this.userAccounts;
	}

	public void setUserAccounts(Set<UserAccount> userAccounts) {
		this.userAccounts = userAccounts;
	}

}

