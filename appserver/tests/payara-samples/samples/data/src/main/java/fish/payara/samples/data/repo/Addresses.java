package fish.payara.samples.data.repo;


import fish.payara.samples.data.entity.Address;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import java.util.UUID;

/**
 * A repository that inherits from the built-in BasicRepository and adds no methods (like TCK Boxes).
 */
@Repository
public interface Addresses extends BasicRepository<Address, UUID> {
}
