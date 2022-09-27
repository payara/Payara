package fish.payara.server.internal.tests.facade;

import fish.payara.server.internal.tests.model.*;
import jakarta.enterprise.context.*;
import jakarta.persistence.*;
import jakarta.transaction.*;

import java.util.*;

@ApplicationScoped
public class PersonFacade
{
  @PersistenceContext
  private EntityManager entityManager;

  @Transactional
  public Person createPerson (Person person)
  {
    entityManager.persist(person);
    return person;
  }

  public List<Person> findAll()
  {
    return entityManager.createQuery("select p from Person p").getResultList();
  }
}
