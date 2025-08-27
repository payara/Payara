package fish.payara.samples.data.support;

import fish.payara.samples.data.entity.Address;
import fish.payara.samples.data.repo.Addresses;
import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;

public class JpaAddresses extends JpaBasicRepository<Address, UUID> implements Addresses {
    public JpaAddresses(EntityManager em) {
        super(em, Address.class);
    }

    @Override
    public <S extends Address> List<S> saveAll(List<S> entities) {
        return List.of();
    }

    @Override
    public Page<Address> findAll(PageRequest pageRequest, Order<Address> sortBy) {
        return null;
    }
}