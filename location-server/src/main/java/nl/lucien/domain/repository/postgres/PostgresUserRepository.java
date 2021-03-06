package nl.lucien.domain.repository.postgres;

import lombok.extern.slf4j.Slf4j;
import nl.lucien.adapter.UserDto;
import nl.lucien.configuration.postgresql.RdbcAdapter;
import nl.lucien.configuration.postgresql.SQLQuery;
import nl.lucien.domain.User;
import nl.lucien.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@Slf4j
public class PostgresUserRepository implements UserRepository {

    private RdbcAdapter rdbcAdapter;

    @Autowired
    public PostgresUserRepository(RdbcAdapter rdbcAdapter) {
        this.rdbcAdapter = rdbcAdapter;
    }

    @Override
    public Flux<User> findAll() {
        String sqlQuery = "select id, name, keywords from locationcloud.user";
        return rdbcAdapter.findAll(SQLQuery.from(sqlQuery, new String[]{}), User.class);
    }

    @Override
    public Mono<User> findUserByUserid(String userId) {
        String query = "select id, name, keywords from locationcloud.user where id = $1";
        String[] args = new String[] { userId };

        return rdbcAdapter.find(SQLQuery.from(query, args), User.class);
    }

    @Override
    public Mono<User> insert(UserDto userDto) {

        String userId = (userDto.getId() == null) ? UUID.randomUUID().toString() : userDto.getId();
        userDto.setId(userId);

        String query = "insert into locationcloud.user values ($1, $2, $3)";
        String[] args = new String[] { userId, userDto.getName(), userDto.keywordsAsString() };

        return rdbcAdapter.insert(SQLQuery.from(query, args))
            .doOnError(throwable -> log.error("Cannot insert '{}' due to exception: ", userId, throwable))
            .filter(inserted -> inserted)
            .flatMap(inserted -> findUserByUserid(userId));
    }

    @Override
    public Mono<User> update(String userId, UserDto userDto) {
        userDto.setId(userId);

        String query = "update locationcloud.user set name = $2, keywords = $3 where id = $1";
        String[] args = new String[] { userId, userDto.getName(), userDto.keywordsAsString() };

        return rdbcAdapter.update(SQLQuery.from(query, args))
            .doOnError(throwable -> log.error("Cannot update '{}' due to exception: ", userId, throwable))
            .filter(updated -> updated)
            .flatMap(updated -> findUserByUserid(userId));
    }

    @Override
    public Mono<User> deleteByUserId(String userId) {
        return findUserByUserid(userId)
            .flatMap(user -> {
                String query = "delete from locationcloud.user where id = $1";
                String[] args = new String[] { userId };
                return rdbcAdapter.delete(SQLQuery.from(query, args))
                    .doOnError(throwable -> log.error("Cannot delete '{}' due to exception: ", userId, throwable))
                    .filter(deleted -> deleted)
                    .map(deleted -> user);
            });

    }

}
