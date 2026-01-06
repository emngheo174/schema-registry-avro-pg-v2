package com.example.schemaregistry.infrastructure.persistence;

import com.example.schemaregistry.domain.model.SchemaEntity;
import com.example.schemaregistry.domain.value.Md5Hash;
import com.example.schemaregistry.domain.value.SchemaId;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchemaRepository extends CrudRepository<SchemaEntity, SchemaId> {

    Optional<SchemaEntity> findByMd5Hash(Md5Hash hash);

    @Query("SELECT MAX(id) FROM schemas")
    Integer findMaxId();
}