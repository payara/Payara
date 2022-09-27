package fish.payara.server.internal.tests.model;

import jakarta.persistence.*;

import java.math.*;

@Entity
@Table(name = "PERSONS")
public class Person
{
  @Id
  @SequenceGenerator(name = "PERSONS_ID_GENERATOR", sequenceName = "PERSONS_SEQ")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PERSONS_ID_GENERATOR")
  @Column(name = "PERSON_ID", unique = true, nullable = false, length = 8)
  private BigInteger id;
  @Column(name = "FIRST_NAME", nullable = false, length = 80)
  private String firstName;
  @Column(name = "LAST_NAME", nullable = false, length = 80)
  private String lastName;

  public Person()
  {
  }

  public Person(String firstName, String lastName)
  {
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public BigInteger getId()
  {
    return id;
  }

  public void setId(BigInteger id)
  {
    this.id = id;
  }

  public String getFirstName()
  {
    return firstName;
  }

  public void setFirstName(String firstName)
  {
    this.firstName = firstName;
  }

  public String getLastName()
  {
    return lastName;
  }

  public void setLastName(String lastName)
  {
    this.lastName = lastName;
  }
}
